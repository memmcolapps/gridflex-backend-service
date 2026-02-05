package org.memmcol.gridflexbackendservice.service.vend;

import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.memmcol.gridflexbackendservice.mapper.VendMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.AdjustmentComputationResult;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.model.vend.*;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.service.debit_credit_adjustment.CreditDebitAdjustmentSettlementService;
import org.memmcol.gridflexbackendservice.service.audit.SafeAuditService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.HeaderFooterPageEvent;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;

import static org.memmcol.gridflexbackendservice.components.HandleValidUser.handleUserValidation;

@Service
public class VendingServiceImpl implements VendingService {
    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private GenericHandler genericHandler;

    @Autowired
    private ResponseProperties status;

//    @Autowired
//    private AuditRepository auditRepository;

    @Autowired
    private SafeAuditService safeAuditService;

    @Autowired
    private VendMapper vendMapper;

    @Autowired
    private TokenGenClientService tokenGenClient;

    @Autowired
    private CreditDebitAdjustmentSettlementService adjustmentSettlementService;

    @Transactional
    public Map<String, Object> createCreditToken(CreditToken creditToken) {
        UserModel user = handleUserValidation();

        try {
            // --- Meter Validation ---
            MeterView meter = vendMapper.getMeterInfo(
                            creditToken.getMeterNumber(),
                            creditToken.getAccountNumber(),
                            user.getOrgId()
                    ).stream().findFirst()
                    .orElseThrow(() -> new GlobalExceptionHandler.NotFoundException("Meter not found"));

            if (!"Prepaid".equalsIgnoreCase(meter.getMeterCategory())
                    || !"Assigned".equalsIgnoreCase(meter.getMeterStage())
                    || !"Active".equalsIgnoreCase(meter.getStatus())) {
                throw new GlobalExceptionHandler.NotFoundException(
                        "Vending allowed only for active, assigned prepaid meters"
                );
            }

            // --- Adjustment Computation ---
            AdjustmentComputationResult adjResult = adjustmentSettlementService.computeAdjustmentImpact(
                    user.getOrgId(),
                    meter.getMeterId(),
                    creditToken.getInitialAmount()
            );

            BigDecimal effectiveAmount = adjResult.getEffectiveTendered();

            if (effectiveAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new GlobalExceptionHandler.NotFoundException(
                        "Vending amount is insufficient after applying adjustments"
                );
            }

            // --- Compute netBalance (existing VAT, Tariff, units, etc) ---
            BigDecimal totalDebit = vendMapper.calculateTotalByType(meter.getMeterId(), user.getOrgId(),"debit");
            BigDecimal totalCredit = vendMapper.calculateTotalByType(meter.getMeterId(), user.getOrgId(), "credit");
            BigDecimal netBalance = totalCredit.add(effectiveAmount).subtract(totalDebit);

            // --- Token Generation ---
            TokenGenRequest request = new TokenGenRequest();
            request.setAmount(netBalance); // vending effective amount
            request.setMeterNo(meter.getMeterNumber());
            request.setSgc(Integer.parseInt(meter.getNewSgc()));
            request.setTi(Integer.parseInt(meter.getNewTariffIndex().toString()));
            request.setMeterType("STS6");

            TokenGenResponse tokenResponse = tokenGenClient.generateToken(request, "/tokenGen", creditToken.getTokenType());

            if (tokenResponse.getCode() == null || !"SUCCESS".equalsIgnoreCase(tokenResponse.getCode())) {
                throw new GlobalExceptionHandler.NotFoundException("Token generation failed");
            }

            // --- Persist Transaction ---
            UUID transactionId = UUID.randomUUID();
            Transaction transaction = new Transaction();
            transaction.setId(transactionId);
            transaction.setMeterId(meter.getMeterId());
            transaction.setInitialAmount(creditToken.getInitialAmount());
            transaction.setFinalAmount(netBalance);
            transaction.setToken(tokenResponse.getTokens().get(0));
            transaction.setUnit(netBalance.divide(new BigDecimal(meter.getTariffRate()), 2, BigDecimal.ROUND_HALF_UP));
            transaction.setUnitCost(netBalance);
            transaction.setTokenType(creditToken.getTokenType());
            transaction.setStatus("Successful");
            transaction.setReceiptNo(generateReceiptNumber(creditToken.getMeterNumber()));
            transaction.setOrgId(user.getOrgId());
            transaction.setUserId(user.getId());
            transaction.setCustomerId(meter.getCustomerId());

            int created = vendMapper.createCreditToken(transaction);
            if (created == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Credit token creation failed");
            }

            // --- Adjustments Settlement AFTER token generation success ---
            adjustmentSettlementService.settleAdjustments(
                    user.getOrgId(),
                    transactionId,
                    adjResult.getAllocations()
            );

            return ResponseMap.response(status.getSuccessCode(),
                    "Credit token generated successfully",
                    transaction
            );

        } catch (Exception ex) {
            genericHandler.logAndSaveException(ex, "creating credit token");
            throw ex;
        }
    }

    @Transactional
    public Map<String, Object> createCreditTokenBackup(CreditToken creditToken) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel user = handleUserValidation();
            Meter meterResult = vendMapper.getMeter(user.getOrgId(), creditToken.getMeterNumber(), creditToken.getAccountNumber());

            if (meterResult == null) {
                throw new GlobalExceptionHandler.NotFoundException("Invalid meter for this organization.");
            }

            boolean isValidForVending =
                    "Prepaid".equalsIgnoreCase(meterResult.getMeterCategory())
                            && "Assigned".equalsIgnoreCase(meterResult.getMeterStage())
                            && "Active".equalsIgnoreCase(meterResult.getStatus());

            if (!isValidForVending) {
                throw new GlobalExceptionHandler.NotFoundException(
                        "Vending is only allowed for active, assigned prepaid meters."
                );
            }

            if(!creditToken.getTokenType().equalsIgnoreCase("credit-token")) {
                throw new GlobalExceptionHandler.NotFoundException("Token type not found or attempt to generate wrong token");
            }

            // --- Fetch meter info ---
            List<MeterView> meters = vendMapper.getMeterInfo(creditToken.getMeterNumber(), creditToken.getAccountNumber(), user.getOrgId());

            if (meters.isEmpty()) {
                throw new GlobalExceptionHandler.NotFoundException("Meter not found");
            }

            MeterView meter = meters.get(0);

            // --- Balance Calculations ---
            BigDecimal totalDebit = calculateTotalByType(meters, "debit");
            BigDecimal totalCredit = calculateTotalByType(meters, "credit");

            // (credit + amountTendered) - debit
            BigDecimal netBalance = calculateNetBalance(totalCredit, creditToken.getInitialAmount(), totalDebit);

            // --- VAT and Unit Calculations ---
            BigDecimal vatRate = meter.getVat().equalsIgnoreCase("Paying")
                    ? new BigDecimal("1.075") : BigDecimal.ONE;

//            BigDecimal vatRate = new BigDecimal("0.075");
//            BigDecimal vatAmount = calculateVatAmount(netBalance, vatRate);
//            BigDecimal totalWithVat = netBalance.add(vatAmount);

            // --- Tariff Rate (safe parse) ---
            BigDecimal tariffRate;
            try {
                tariffRate = new BigDecimal(meter.getTariffRate());
            } catch (Exception e) {
                tariffRate = BigDecimal.ZERO; // invalid rate should act as 0
            }

            BigDecimal units;
            BigDecimal costPerUnit;

            if (tariffRate.compareTo(BigDecimal.ZERO) > 0) {
                costPerUnit = netBalance.divide(vatRate,  2, RoundingMode.HALF_UP);
                // Normal calculation if tariffRate is valid
                units = costPerUnit.divide(tariffRate, 2, RoundingMode.HALF_UP);
                System.out.println("unit1:: "+units);
//                if (units.compareTo(BigDecimal.ZERO) > 0) {
//                    costPerUnit = totalWithVat.divide(units, 2, RoundingMode.HALF_UP);
//                }
            } else {
                costPerUnit = netBalance.divide(vatRate,  2, RoundingMode.HALF_UP);
                units = costPerUnit.divide(tariffRate.equals(BigDecimal.ZERO) ? BigDecimal.ONE : tariffRate, 2, RoundingMode.HALF_UP);
            }

//            if (tariffRate.compareTo(BigDecimal.ZERO) > 0) {
//                // Normal calculation if tariffRate is valid
//                units = netBalance.divide(tariffRate, 2, RoundingMode.HALF_UP);
//
//                if (units.compareTo(BigDecimal.ZERO) > 0) {
//                    costPerUnit = totalWithVat.divide(units, 2, RoundingMode.HALF_UP);
//                }
//            }

//            BigDecimal finalAmount = netBalance.subtract(vatAmount);
//            System.out.println("unit:: "+units);

            BigDecimal vatAmount = creditToken.getInitialAmount().subtract(costPerUnit);
            vatRate = vatRate.equals(BigDecimal.ONE) ? BigDecimal.ZERO : vatRate;

            TokenGenRequest request = new TokenGenRequest();
            request.setAmount(units);
            request.setMeterNo(meter.getMeterNumber());
            request.setSgc(Integer.parseInt(meter.getNewSgc()));
            request.setTi(Integer.parseInt(meter.getNewTariffIndex().toString()));
            request.setMeterType("STS6");

//            TokenGenResponse tokenResponse = tokenGenClient.generateToken(request, "/tokenGen", creditToken.getTokenType());
//
//            if (tokenResponse.getCode() == null || !"SUCCESS".equalsIgnoreCase(tokenResponse.getCode())) {
//                throw new GlobalExceptionHandler.NotFoundException("Token generation failed");
//            }

            // --- Update Transaction ---
            Transaction transaction = new Transaction();
            transaction.setUnitCost(costPerUnit);
            transaction.setUnit(units);
            transaction.setVatAmount(vatAmount);
            transaction.setMeterId(meter.getMeterId());
            transaction.setTariffId(meter.getTariffId());
            transaction.setToken(generateDummyToken());
//            transaction.setToken(tokenResponse.getTokens().get(0));
            transaction.setStatus("Successful");
            transaction.setReceiptNo(generateReceiptNumber(creditToken.getMeterNumber()));
            transaction.setOrgId(user.getOrgId());
            transaction.setUserId(user.getId());
            transaction.setCustomerId(meter.getCustomerId());
            transaction.setInitialAmount(creditToken.getInitialAmount());
            transaction.setFinalAmount(netBalance.subtract(vatAmount));
            transaction.setTokenType(creditToken.getTokenType());
            transaction.setKct1(generateDummyToken());
            transaction.setKct2(generateDummyToken());

            // --- Persist ---
            int created = vendMapper.createCreditToken(transaction);
            if (created == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Credit token creation failed.");
            }

            Transaction savedTransaction = vendMapper.getCreditTokenTransaction(transaction.getId(), user.getOrgId());

            // Audit (optional)
             AuditLog auditLog = buildAuditLog(user, "Credit token created", "Vend", savedTransaction, metadata, null);
             safeAuditService.saveAudit(auditLog);



            return ResponseMap.response(status.getSuccessCode(), "Credit token generated successfully", savedTransaction);

        } catch (Exception ex) {
            genericHandler.logIncidentReport("Creating credit token service failed");
            genericHandler.logAndSaveException(ex, "creating credit token");
            throw ex;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> calculateCreditToken(CreditToken creditToken) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel user = handleUserValidation();

            Meter meterResult = vendMapper.getMeter(user.getOrgId(), creditToken.getMeterNumber(), creditToken.getAccountNumber());

            if (meterResult == null) {
                throw new GlobalExceptionHandler.NotFoundException("Invalid meter for this organization.");
            }

            boolean isValidForVending =
                    "Prepaid".equalsIgnoreCase(meterResult.getMeterCategory())
                            && "Assigned".equalsIgnoreCase(meterResult.getMeterStage())
                            && "Active".equalsIgnoreCase(meterResult.getStatus());

            if (!isValidForVending) {
                throw new GlobalExceptionHandler.NotFoundException(
                        "Vending is only allowed for active, assigned prepaid meters."
                );
            }

            // --- Fetch meter info ---
            List<MeterView> meters = vendMapper.getMeterInfo(
                    creditToken.getMeterNumber(),
                    creditToken.getAccountNumber(),
                    user.getOrgId()
            );
            if (meters.isEmpty()) {
                throw new GlobalExceptionHandler.NotFoundException("Meter not found for provided details.");
            }


            MeterView meter = meters.get(0);

            // --- Balance Calculations ---
            BigDecimal totalDebit = calculateTotalByType(meters, "debit");
            BigDecimal totalCredit = calculateTotalByType(meters, "credit");

            BigDecimal netBalance = calculateNetBalance(totalCredit, creditToken.getInitialAmount(), totalDebit);

            // --- VAT and Unit Calculations ---
            BigDecimal vatRate = meter.getVat().equalsIgnoreCase("Paying")
                    ? new BigDecimal("1.075") : BigDecimal.ONE;
//            BigDecimal vatAmount = calculateVatAmount(netBalance, vatRate);
//            BigDecimal totalWithVat = netBalance.add(vatAmount);

            // --- Tariff Rate (safe parse) ---
            BigDecimal tariffRate;
            try {
                tariffRate = new BigDecimal(meter.getTariffRate());
            } catch (Exception e) {
                tariffRate = BigDecimal.ZERO; // invalid rate should act as 0
            }

            BigDecimal units = BigDecimal.ZERO;
            BigDecimal costPerUnit = BigDecimal.ZERO;

            if (tariffRate.compareTo(BigDecimal.ZERO) > 0) {
                costPerUnit = netBalance.divide(vatRate,  2, RoundingMode.HALF_UP);
                // Normal calculation if tariffRate is valid
                units = costPerUnit.divide(tariffRate, 2, RoundingMode.HALF_UP);
//                if (units.compareTo(BigDecimal.ZERO) > 0) {
//                    costPerUnit = totalWithVat.divide(units, 2, RoundingMode.HALF_UP);
//                }
            } else {
                costPerUnit = netBalance.divide(vatRate,  2, RoundingMode.HALF_UP);
                units = costPerUnit.divide(tariffRate.equals(BigDecimal.ZERO) ? BigDecimal.ONE : tariffRate, 2, RoundingMode.HALF_UP);
            }

            BigDecimal vatAmount = creditToken.getInitialAmount().subtract(costPerUnit);
            //            BigDecimal finalAmount = netBalance.subtract(vatAmount);

            vatRate = vatRate.equals(BigDecimal.ONE) ? BigDecimal.ZERO : vatRate;
            // --- Build Response Data ---
            creditToken.setVat(vatRate);
            creditToken.setCostOfUnit(costPerUnit);
            creditToken.setUnit(units);
            creditToken.setVatAmount(vatAmount);
            creditToken.setFinalAmount(netBalance.subtract(vatAmount));

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("data", creditToken);
            responseData.put("meter", meters);
            responseData.put("totalDebitBalance", totalDebit);
            responseData.put("totalCreditBalance", totalCredit);

            return ResponseMap.response(status.getSuccessCode(), "Credit token calculated successfully", responseData);

        } catch (Exception ex) {
            genericHandler.logIncidentReport("Calculating credit token service failed");
            genericHandler.logAndSaveException(ex, "calculate credit token");
            throw ex;
        }
    }


    private String generateReceiptNumber(String meterNumber) {
        String prefix = "RCPT";
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        return String.format("%s-%s-%s", prefix, timestamp, meterNumber);
    }


    private BigDecimal calculateTotalByType(List<MeterView> meters, String type) {
        return meters.stream()
                .filter(m -> type.equalsIgnoreCase(m.getAdjustmentType()))
                .map(MeterView::getBalanceAfterAdjustment)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateNetBalance(BigDecimal totalCredit, BigDecimal amount, BigDecimal totalDebit) {
        // Treat nulls as zero
        BigDecimal safeCredit = totalCredit != null ? totalCredit : BigDecimal.ZERO;
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        BigDecimal safeDebit = totalDebit != null ? totalDebit : BigDecimal.ZERO;
//        System.out.println("safeAmount:: "+safeAmount);
        return safeCredit
                .add(safeAmount)
                .subtract(safeDebit)
                .setScale(2, RoundingMode.HALF_UP);
    }


//    private BigDecimal calculateVatAmount(BigDecimal netBalance, BigDecimal vatRate) {
//        return netBalance.multiply(vatRate).setScale(2, RoundingMode.HALF_UP);
//    }

    @Transactional
    @Override
    public Map<String, Object> createKctToken(KctToken kctToken) {
        try{
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel user = handleUserValidation();

            Meter meterResult = vendMapper.getMeter(user.getOrgId(), kctToken.getMeterNumber(), kctToken.getAccountNumber());
            if (meterResult == null) {
                throw new GlobalExceptionHandler.NotFoundException("Invalid meter for this organization.");
            }

            boolean isValidForVending =
                    "Prepaid".equalsIgnoreCase(meterResult.getMeterCategory())
                            && "Assigned".equalsIgnoreCase(meterResult.getMeterStage())
                            && "Active".equalsIgnoreCase(meterResult.getStatus());

            if (!isValidForVending) {
                throw new GlobalExceptionHandler.NotFoundException(
                        "Vending is only allowed for active, assigned prepaid meters."
                );
            }

            if(!kctToken.getTokenType().equalsIgnoreCase("kct")) {
                throw new GlobalExceptionHandler.NotFoundException("Token type not found or attempt to generate wrong token");
            }

            MeterView meter = vendMapper.getMeterRecord(kctToken.getMeterNumber(), kctToken.getAccountNumber(), user.getOrgId());

            TokenGenRequest request = new TokenGenRequest();
            request.setMeterNo(meter.getMeterNumber());
            request.setSgc(Integer.parseInt(meter.getNewSgc()));
            request.setTosgc(Integer.parseInt(meter.getNewSgc()));
            request.setTi(Integer.parseInt(meter.getOldTariffIndex().toString()));
            request.setToti(Integer.parseInt(meter.getNewTariffIndex().toString()));
            request.setMeterType("STS6");
            request.setTometerType("STS6");
            request.setAllow(kctToken.getAllow());
            request.setAllowkrn(true);

//            TokenGenResponse tokenResponse = tokenGenClient.generateToken(request, "/kcttokenGen", kctToken.getTokenType());
//
//            if (tokenResponse.getCode() == null || !"SUCCESS".equalsIgnoreCase(tokenResponse.getCode())) {
//                throw new GlobalExceptionHandler.NotFoundException("Token generation failed");
//            }

//            kctToken.setKct1(tokenResponse.getTokens().get(0));
//            kctToken.setKct2(tokenResponse.getTokens().get(1));
            kctToken.setKct1(generateDummyToken());
            kctToken.setKct2(generateDummyToken());
            kctToken.setMeterId(meter.getMeterId());
            kctToken.setStatus("Successful");
            kctToken.setOrgId(user.getOrgId());
            kctToken.setCustomerId(meter.getCustomerId());
            kctToken.setUserId(user.getId());
            kctToken.setReceiptNo(generateReceiptNumber(kctToken.getMeterNumber()));
            kctToken.setTariffId(meter.getTariffId());
            kctToken.setToken(generateDummyToken());

            int kct = vendMapper.createKctToken(kctToken);
            if(kct == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Generate kct token failed");
            }


            Transaction transaction = vendMapper.getCreditTokenTransaction(kctToken.getId(), user.getOrgId());

            // Audit (optional)
            AuditLog auditLog = buildAuditLog(user, "kct token generated", "vend", transaction, metadata, kctToken.getReason());
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(), "Kct token generated successfully", transaction);
        } catch (Exception ex) {
            genericHandler.logIncidentReport("Creating kct token service failed");
            genericHandler.logAndSaveException(ex, "creating kct token");
            throw ex;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> getKctMeterInfo(KctToken kctToken) {
       try{
//           Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
           UserModel user = handleUserValidation();

           if(!kctToken.getTokenType().equalsIgnoreCase("kct")) {
               throw new GlobalExceptionHandler.NotFoundException("Token type not found or attempt to generate wrong token");
           }

           MeterView meter = vendMapper.getMeterRecord(kctToken.getMeterNumber(), kctToken.getAccountNumber(), user.getOrgId());

           System.out.println("oldSgc: "+meter.getOldSgc());

           kctToken.setOldSgc(meter.getOldSgc());
           kctToken.setNewSgc(meter.getNewSgc());
           kctToken.setNewKrn(meter.getNewKrn());
           kctToken.setOldKrn(meter.getOldKrn());
           kctToken.setNewTariffIndex(meter.getNewTariffIndex());
           kctToken.setOldTariffIndex(meter.getOldTariffIndex());

//           Transaction transaction = vendMapper.getCreditTokenTransaction(kctToken.getId());

//           // Audit (optional)
//           AuditLog auditLog = buildAuditLog(user, "kct token generated", "vend", transaction, metadata, "");
//           auditRepository.save(auditLog);

           return ResponseMap.response(status.getSuccessCode(), "Meter data fetched successfully", kctToken);
       } catch (Exception ex) {
           genericHandler.logIncidentReport("Fetching meter kct service failed");
           genericHandler.logAndSaveException(ex, "Fetching meter kct service");
           throw ex;
       }
    }

    @Override
    public Map<String, Object> createClearTamperToken(ClearTamper clearTamper) {
        try{
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel user = handleUserValidation();

            Meter meterResult = vendMapper.getMeter(user.getOrgId(), clearTamper.getMeterNumber(), clearTamper.getAccountNumber());
            if (meterResult == null) {
                throw new GlobalExceptionHandler.NotFoundException("Invalid meter for this organization.");
            }

            boolean isValidForVending =
                    "Prepaid".equalsIgnoreCase(meterResult.getMeterCategory())
                            && "Assigned".equalsIgnoreCase(meterResult.getMeterStage())
                            && "Active".equalsIgnoreCase(meterResult.getStatus());

            if (!isValidForVending) {
                throw new GlobalExceptionHandler.NotFoundException(
                        "Vending is only allowed for active, assigned prepaid meters."
                );
            }

            if(!clearTamper.getTokenType().equalsIgnoreCase("clear-tamper")) {
                throw new GlobalExceptionHandler.NotFoundException("Token type not found or attempt to generate wrong token");
            }

            MeterView meter = vendMapper.getMeterRecord(clearTamper.getMeterNumber(), clearTamper.getAccountNumber(), user.getOrgId());

            TokenGenRequest request = new TokenGenRequest();
            request.setMeterNo(meter.getMeterNumber());
            request.setSgc(Integer.parseInt(meter.getNewSgc()));
            request.setTi(Integer.parseInt(meter.getNewTariffIndex().toString()));
            request.setSbc(5);
            request.setMeterType("STS6");

//            TokenGenResponse tokenResponse = tokenGenClient.generateToken(request, "/msetokenGen", clearTamper.getTokenType());
//
//            if (tokenResponse.getCode() == null || !"SUCCESS".equalsIgnoreCase(tokenResponse.getCode())) {
//                throw new GlobalExceptionHandler.NotFoundException("Token generation failed");
//            }
//
//            clearTamper.setToken(tokenResponse.getTokens().get(0));
            clearTamper.setToken(generateDummyToken());
            clearTamper.setMeterId(meter.getMeterId());
            clearTamper.setStatus("Successful");
            clearTamper.setOrgId(user.getOrgId());
            clearTamper.setCustomerId(meter.getCustomerId());
            clearTamper.setUserId(user.getId());
            clearTamper.setReceiptNo(generateReceiptNumber(clearTamper.getMeterNumber()));
            clearTamper.setTariffId(meter.getTariffId());
            clearTamper.setKct1(generateDummyToken());
            clearTamper.setKct2(generateDummyToken());

            int clear = vendMapper.createClearToken(clearTamper);
            if(clear == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Generate kct token failed");
            }

            Transaction transaction = vendMapper.getCreditTokenTransaction(clearTamper.getId(), user.getOrgId());

            // Audit (optional)
            AuditLog auditLog = buildAuditLog(user, "clear token generated", "vend", transaction, metadata, clearTamper.getReason());
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(), "Clear tamper token generated successfully", transaction);
        } catch (Exception ex) {
            genericHandler.logIncidentReport("Creating clear tamper token service failed");
            genericHandler.logAndSaveException(ex, "creating clear tamper token");
            throw ex;
        }
    }

    @Override
    public Map<String, Object> createClearCreditToken(ClearCredit clearCredit) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel user = handleUserValidation();

            Meter meterResult = vendMapper.getMeter(user.getOrgId(), clearCredit.getMeterNumber(), clearCredit.getAccountNumber());
            if (meterResult == null) {
                throw new GlobalExceptionHandler.NotFoundException("Invalid meter for this organization.");
            }

            boolean isValidForVending =
                    "Prepaid".equalsIgnoreCase(meterResult.getMeterCategory())
                            && "Assigned".equalsIgnoreCase(meterResult.getMeterStage())
                            && "Active".equalsIgnoreCase(meterResult.getStatus());

            if (!isValidForVending) {
                throw new GlobalExceptionHandler.NotFoundException(
                        "Vending is only allowed for active, assigned prepaid meters."
                );
            }

            if(!clearCredit.getTokenType().equalsIgnoreCase("clear-credit")) {
                throw new GlobalExceptionHandler.NotFoundException("Token type not found or attempt to generate wrong token");
            }

            MeterView meter = vendMapper.getMeterRecord(clearCredit.getMeterNumber(), clearCredit.getAccountNumber(), user.getOrgId());



            TokenGenRequest request = new TokenGenRequest();
            request.setMeterNo(meter.getMeterNumber());
            request.setSgc(Integer.parseInt(meter.getNewSgc()));
            request.setTi(Integer.parseInt(meter.getNewTariffIndex().toString()));
            request.setSbc(1);
            request.setMeterType("STS6");

//            TokenGenResponse tokenResponse = tokenGenClient.generateToken(request, "/msetokenGen", clearCredit.getTokenType());
//
//            if (tokenResponse.getCode() == null || !"SUCCESS".equalsIgnoreCase(tokenResponse.getCode())) {
//                throw new GlobalExceptionHandler.NotFoundException("Token generation failed");
//            }
//
//            clearCredit.setToken(tokenResponse.getTokens().get(0));
            clearCredit.setToken(generateDummyToken());
            clearCredit.setMeterId(meter.getMeterId());
            clearCredit.setStatus("Successful");
            clearCredit.setOrgId(user.getOrgId());
            clearCredit.setCustomerId(meter.getCustomerId());
            clearCredit.setUserId(user.getId());
            clearCredit.setReceiptNumber(generateReceiptNumber(clearCredit.getMeterNumber()));
            clearCredit.setTariffId(meter.getTariffId());
            clearCredit.setKct1(generateDummyToken());
            clearCredit.setKct2(generateDummyToken());

            int clear = vendMapper.createClearCredit(clearCredit);
            if(clear == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Generate clear credit token failed");
            }

            Transaction transaction = vendMapper.getCreditTokenTransaction(clearCredit.getId(), user.getOrgId());

            // Audit (optional)
            AuditLog auditLog = buildAuditLog(user, "clear credit token generated", "vend", transaction, metadata, clearCredit.getReason());
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(), "Clear credit token generated successfully", transaction);

        }catch (Exception ex) {
            genericHandler.logIncidentReport("Creating clear credit token service failed");
            genericHandler.logAndSaveException(ex, "creating clear credit token");
            throw ex;
        }
    }

    @Override
    public Map<String, Object> createCompensationToken(KctToken kctToken) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel user = handleUserValidation();

            Meter meterResult = vendMapper.getMeter(user.getOrgId(), kctToken.getMeterNumber(), kctToken.getAccountNumber());
            if (meterResult == null) {
                throw new GlobalExceptionHandler.NotFoundException("Invalid meter for this organization.");
            }
            
            boolean isValidForVending =
                    "Prepaid".equalsIgnoreCase(meterResult.getMeterCategory())
                            && "Assigned".equalsIgnoreCase(meterResult.getMeterStage())
                            && "Active".equalsIgnoreCase(meterResult.getStatus());

            if (!isValidForVending) {
                throw new GlobalExceptionHandler.NotFoundException(
                        "Vending is only allowed for active, assigned prepaid meters."
                );
            }

            if(!kctToken.getTokenType().equalsIgnoreCase("compensation")) {
                throw new GlobalExceptionHandler.NotFoundException("Token type not found or attempt to generate wrong token");
            }

            MeterView meter = vendMapper.getMeterRecord(kctToken.getMeterNumber(), kctToken.getAccountNumber(), user.getOrgId());

            TokenGenRequest request = new TokenGenRequest();
            request.setMeterNo(meter.getMeterNumber());
            request.setSgc(Integer.parseInt(meter.getNewSgc()));
            request.setTi(Integer.parseInt(meter.getNewTariffIndex().toString()));
            request.setMeterType("STS6");

//            TokenGenResponse tokenResponse = tokenGenClient.generateToken(request, "/tokenGen", kctToken.getTokenType());
//
//            if (tokenResponse.getCode() == null || !"SUCCESS".equalsIgnoreCase(tokenResponse.getCode())) {
//                throw new GlobalExceptionHandler.NotFoundException("Token generation failed");
//            }


//            kctToken.setToken(tokenResponse.getTokens().get(0));
            kctToken.setToken(generateDummyToken());
            kctToken.setMeterId(meter.getMeterId());
            kctToken.setStatus("Successful");
            kctToken.setOrgId(user.getOrgId());
            kctToken.setCustomerId(meter.getCustomerId());
            kctToken.setUserId(user.getId());
            kctToken.setReceiptNo(generateReceiptNumber(kctToken.getMeterNumber()));
            kctToken.setTariffId(meter.getTariffId());
            kctToken.setKct1(generateDummyToken());
            kctToken.setKct2(generateDummyToken());

            int clear = vendMapper.createCompensationToken(kctToken);
            if(clear == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Generate compensation token failed");
            }

            Transaction transaction = vendMapper.getCreditTokenTransaction(kctToken.getId(), user.getOrgId());

            // Audit (optional)
            AuditLog auditLog = buildAuditLog(user, "Compensation token generated", "vend", transaction, metadata, kctToken.getReason());
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(), "Compensation token generated successfully", transaction);

        }catch (Exception ex) {
            genericHandler.logIncidentReport("Creating compensation token service failed");
            genericHandler.logAndSaveException(ex, "creating compensation token");
            throw ex;
        }
    }

    @Override
    public Map<String, Object> getAllToken(String meterNumber, String accountNumber,
                                           String tariffName, String tokenType, String stat,
                                           int page, int size) {
        UserModel user = handleUserValidation();
        UUID uId = user.getOrgId();

        try {
            int offset = page * size;
            List<Transaction> allReadings = vendMapper.getAllToken(
                    uId, meterNumber, accountNumber, tariffName, tokenType, stat, offset, size
            );

            int totalCount = allReadings.size();
            List<Transaction> paginatedReadings;


            if (size == 0) {
                paginatedReadings = allReadings;
            } else {
                int fromIndex = Math.min(page * size, totalCount);
                int toIndex = Math.min(fromIndex + size, totalCount);

                if (fromIndex > toIndex) {
                    fromIndex = toIndex;
                }

                paginatedReadings = allReadings.subList(fromIndex, toIndex);
            }

            // Build response
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("messages", paginatedReadings);
            responseData.put("totalCount", totalCount);
            responseData.put("page", page);
            responseData.put("size", size);
            responseData.put("totalPages", size == 0 ? 1 : (int) Math.ceil((double) totalCount / size));

            return ResponseMap.response(status.getSuccessCode(), "Token fetched successfully", responseData);

        } catch (Exception ex) {
            genericHandler.logIncidentReport("Token fetched service failed");
            genericHandler.logAndSaveException(ex, "token fetched");
            throw ex;
        }
    }

    @Override
    public ByteArrayInputStream printToken(String tokenType, UUID id) {

        try {
            UserModel user = handleUserValidation();
            Transaction transaction = vendMapper.getCreditTokenTransaction(id, user.getOrgId());//getVendingReceipt(id, tokenType);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Font setup
            PdfFont font = PdfFontFactory.createFont();
            String headerText = "Printed on: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            // Add page numbering or header/footer if you have a handler
            pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new HeaderFooterPageEvent(document, font, headerText));

            // Title
            document.add(new Paragraph("Token Receipt")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(14)
                    .setBold());

            // Create a two-column table
            float[] columnWidths = {3, 3}; // adjust width ratio if needed
            Table table = new Table(columnWidths);
            table.setWidth(UnitValue.createPercentValue(100));

            // Helper for adding key-value rows
            BiConsumer<String, String> addRow = (label, value) -> {
                table.addCell(new Cell().add(new Paragraph(label))
                        .setFont(font)
                        .setBold()
                        .setFontSize(10));
                table.addCell(new Cell().add(new Paragraph(getSafeString(value)))
                        .setFont(font)
                        .setFontSize(10));
            };

            // Add key-value pairs
            if("credit-token".equalsIgnoreCase(tokenType)){
                addRow.accept("Customer Name", transaction.getCustomerFullname());
                addRow.accept("Address", transaction.getAddress());
                addRow.accept("Tariff", transaction.getTariffName());
                addRow.accept("Rate", transaction.getTariffRate());
                addRow.accept("Account No", String.valueOf(transaction.getMeterAccountNumber()));
                addRow.accept("Meter No", String.valueOf(transaction.getMeterNumber()));
                addRow.accept("Operator ID", transaction.getUserFullname());
                addRow.accept("Date", String.valueOf(transaction.getCreatedAt()));
                addRow.accept("Receipt No", transaction.getReceiptNo());
                addRow.accept("Last Amount Vended", "0");
                addRow.accept("Cost of Unit", transaction.getUnitCost().toString());
                addRow.accept("VAT Amount", transaction.getVatAmount().toString());
                addRow.accept("Credit Adjustment", "0");
                addRow.accept("Debit Adjustment", "0");
                addRow.accept("Initial Amount", transaction.getInitialAmount().toString());
                addRow.accept("Token", transaction.getToken());
                addRow.accept("Credit Adjustment Balance", "0");
                addRow.accept("Debit Adjustment Balance", "0");
            } else {
                throw new GlobalExceptionHandler.NotFoundException("Wrong token type provided");
            }

            document.add(table);

            document.close();

            try (OutputStream fos = new FileOutputStream(tokenType+"_receipt.pdf")) {
                out.writeTo(fos);
            }

            return new ByteArrayInputStream(out.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    private String getSafeString(String value) {
        return value != null ? value : "";
    }

    private String generateDummyToken() {
        return "DUMMY-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
    }


    private AuditLog buildAuditLog(UserModel creator, String description, String type, Transaction vend, Map<String, String> metadata, String reason) {
        AuditLog log = new AuditLog();
        log.setCreator(creator);
        log.setDescription(description);
        log.setType(type);
        log.setVend(vend);
//                createdEntity instanceof UserModel ? (UserModel) createdEntity : null);
        log.setIpAddress(metadata.get("ipAddress"));
        log.setUserAgent(metadata.get("userAgent"));
        log.setEndpoint(metadata.get("endpoint"));
        log.setHttpMethod(metadata.get("httpMethod"));
        log.setReason(reason);
        return log;
    }
}
