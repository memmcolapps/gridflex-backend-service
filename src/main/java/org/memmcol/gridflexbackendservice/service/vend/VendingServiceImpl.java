package org.memmcol.gridflexbackendservice.service.vend;

import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.memmcol.gridflexbackendservice.mapper.VendMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.model.vend.*;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.memmcol.gridflexbackendservice.components.handleValidUser.handleUserValidation;

@Service
public class VendingServiceImpl implements VendingService {
    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GenericHandler genericHandler;

    @Autowired
    private ResponseProperties status;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private VendMapper vendMapper;

    @Transactional
    @Override
    public Map<String, Object> createCreditToken(CreditToken creditToken) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel user = handleUserValidation();

            if(!creditToken.getTokenType().equalsIgnoreCase("credit-token")) {
                throw new GlobalExceptionHandler.NotFoundException("Token type not found or attempt to generate wrong token");
            }

            System.out.println(">>>>meter:: "+creditToken.getMeterNumber());
            // --- Fetch meter info ---
            List<MeterView> meters = vendMapper.getMeterInfo(creditToken.getMeterNumber(), creditToken.getAccountNumber(), user.getOrgId());
            System.out.println(">>>>meter:: "+meters.get(0).getCustomerFullname());
            if (meters.isEmpty()) {
                throw new GlobalExceptionHandler.NotFoundException("Meter not found");
            }

            MeterView meter = meters.get(0);

            System.out.println(">>>>customerId:: "+meter.getCustomerId());

            // --- Balance Calculations ---
            BigDecimal totalDebit = calculateTotalByType(meters, "debit");
            BigDecimal totalCredit = calculateTotalByType(meters, "credit");

            // (credit + amountTendered) - debit
            BigDecimal netBalance = calculateNetBalance(totalCredit, creditToken.getInitialAmount(), totalDebit);

            // --- VAT and Unit Calculations ---
            BigDecimal vatRate = new BigDecimal("0.075");
            BigDecimal vatAmount = calculateVatAmount(netBalance, vatRate);
            BigDecimal totalWithVat = netBalance.add(vatAmount);

            BigDecimal tariffRate = new BigDecimal(meter.getTariffRate());
            BigDecimal units = netBalance.divide(tariffRate, 2, RoundingMode.HALF_UP);
            BigDecimal costPerUnit = totalWithVat.divide(units, 2, RoundingMode.HALF_UP);

            // --- Update Transaction ---
            Transaction transaction = new Transaction();
            transaction.setUnitCost(costPerUnit);
            transaction.setUnit(units);
            transaction.setVatAmount(vatAmount);
            transaction.setMeterId(meter.getMeterId());
            transaction.setTariffId(meter.getTariffId());
            transaction.setToken("1130987906543214590007");
            transaction.setStatus("Completed");
            transaction.setReceiptNo(generateReceiptNumber(creditToken.getMeterNumber()));
            transaction.setOrgId(user.getOrgId());
            transaction.setUserId(user.getId());
            transaction.setCustomerId(meter.getCustomerId());
            transaction.setInitialAmount(creditToken.getInitialAmount());
            transaction.setFinalAmount(netBalance);
            transaction.setTokenType(creditToken.getTokenType());

            // --- Persist ---
            int created = vendMapper.createCreditToken(transaction);
            if (created == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Credit token creation failed.");
            }

            Transaction savedTransaction = vendMapper.getCreditTokenTransaction(transaction.getId());

            // Audit (optional)
             AuditLog auditLog = buildAuditLog(user, "Credit token created", "Vend", savedTransaction, metadata, null);
             auditRepository.save(auditLog);

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

            // --- Fetch meter info ---
            List<MeterView> meters = vendMapper.getMeterInfo(creditToken.getMeterNumber(), creditToken.getAccountNumber(),  user.getOrgId());
            if (meters.isEmpty()) {
                throw new GlobalExceptionHandler.NotFoundException("Meter not found for provided details.");
            }

            MeterView meter = meters.get(0);

            // --- Balance Calculations ---
            BigDecimal totalDebit = calculateTotalByType(meters, "debit");
            BigDecimal totalCredit = calculateTotalByType(meters, "credit");

            // (credit + amountTendered) - debit
            BigDecimal netBalance = calculateNetBalance(totalCredit, creditToken.getInitialAmount(), totalDebit);

            // --- VAT and Unit Calculations ---
            BigDecimal vatRate = new BigDecimal("0.075");
            BigDecimal vatAmount = calculateVatAmount(netBalance, vatRate);
            BigDecimal totalWithVat = netBalance.add(vatAmount);

            BigDecimal tariffRate = new BigDecimal(meter.getTariffRate());
            BigDecimal units = netBalance.divide(tariffRate, 2, RoundingMode.HALF_UP);
            BigDecimal costPerUnit = totalWithVat.divide(units, 2, RoundingMode.HALF_UP);

            // --- Build Response Data ---
            creditToken.setVat(vatRate);
            creditToken.setCostOfUnit(costPerUnit);
            creditToken.setUnit(units);
            creditToken.setVatAmount(vatAmount);
            creditToken.setFinalAmount(netBalance);

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
        return totalCredit
                .add(amount)
                .subtract(totalDebit)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateVatAmount(BigDecimal netBalance, BigDecimal vatRate) {
        return netBalance.multiply(vatRate).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    @Override
    public Map<String, Object> createKctToken(KctToken kctToken) {
       try{
           Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
           UserModel user = handleUserValidation();

           if(!kctToken.getTokenType().equalsIgnoreCase("kct")) {
               throw new GlobalExceptionHandler.NotFoundException("Token type not found or attempt to generate wrong token");
           }

           MeterView meter = vendMapper.getMeterRecord(kctToken.getMeterNumber(), kctToken.getAccountNumber(), user.getOrgId());

           if(!kctToken.getOldSgc().equalsIgnoreCase(meter.getOldSgc())) {
               throw new GlobalExceptionHandler.NotFoundException("Old SGC not found");
           }
           if(!kctToken.getNewSgc().equalsIgnoreCase(meter.getNewSgc())) {
               throw new GlobalExceptionHandler.NotFoundException("New SGC not found");
           }

           if(!kctToken.getOldKrn().equalsIgnoreCase(meter.getOldKrn())) {
               throw new GlobalExceptionHandler.NotFoundException("Old KRN not found");
           }
           if(!kctToken.getNewKrn().equalsIgnoreCase(meter.getNewKrn())) {
               throw new GlobalExceptionHandler.NotFoundException("New KRN not found");
           }

           if(!kctToken.getNewTariffIndex().equals(meter.getNewTariffIndex())) {
               throw new GlobalExceptionHandler.NotFoundException("New Tariff index not found");
           }

           if(!kctToken.getOldTariffIndex().equals(meter.getOldTariffIndex())) {
               throw new GlobalExceptionHandler.NotFoundException("Old Tariff index not found");
           }

           kctToken.setKct1("0009981278890223211");
           kctToken.setKct2("2209981690890223290");
           kctToken.setMeterId(meter.getMeterId());
           kctToken.setStatus("Completed");
           kctToken.setOrgId(user.getOrgId());
           kctToken.setCustomerId(meter.getCustomerId());
           kctToken.setUserId(user.getId());
           kctToken.setReceiptNo(generateReceiptNumber(kctToken.getMeterNumber()));
           kctToken.setTariffId(meter.getTariffId());

           int kct = vendMapper.createKctToken(kctToken);
           if(kct == 0) {
               throw new GlobalExceptionHandler.NotFoundException("Generate kct token failed");
           }


           Transaction transaction = vendMapper.getCreditTokenTransaction(kctToken.getId());

           // Audit (optional)
           AuditLog auditLog = buildAuditLog(user, "kct token generated", "vend", transaction, metadata, kctToken.getReason());
           auditRepository.save(auditLog);

           return ResponseMap.response(status.getSuccessCode(), "Kct token generated successfully", transaction);
       } catch (Exception ex) {
           genericHandler.logIncidentReport("Creating kct token service failed");
           genericHandler.logAndSaveException(ex, "creating kct token");
           throw ex;
       }
    }

    @Override
    public Map<String, Object> createClearTamperToken(ClearTamper clearTamper) {
        try{
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel user = handleUserValidation();

            if(!clearTamper.getTokenType().equalsIgnoreCase("kct")) {
                throw new GlobalExceptionHandler.NotFoundException("Token type not found or attempt to generate wrong token");
            }

            MeterView meter = vendMapper.getMeterRecord(clearTamper.getMeterNumber(), clearTamper.getAccountNumber(), user.getOrgId());

            clearTamper.setToken("0009981278890223211");
            clearTamper.setMeterId(meter.getMeterId());
            clearTamper.setStatus("Completed");
            clearTamper.setOrgId(user.getOrgId());
            clearTamper.setCustomerId(meter.getCustomerId());
            clearTamper.setUserId(user.getId());
            clearTamper.setReceiptNo(generateReceiptNumber(clearTamper.getMeterNumber()));
            clearTamper.setTariffId(meter.getTariffId());

            int kct = vendMapper.createClearToken(clearTamper);
            if(kct == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Generate kct token failed");
            }


            Transaction transaction = vendMapper.getCreditTokenTransaction(clearTamper.getId());

            // Audit (optional)
            AuditLog auditLog = buildAuditLog(user, "clear token generated", "vend", transaction, metadata, clearTamper.getReason());
            auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(), "Clear tamper token generated successfully", transaction);
        } catch (Exception ex) {
            genericHandler.logIncidentReport("Creating clear tamper token service failed");
            genericHandler.logAndSaveException(ex, "creating clear tamper token");
            throw ex;
        }
    }

    @Override
    public Map<String, Object> createClearCreditToken(ClearCredit clearCredit) {
        return Map.of();
    }

    @Override
    public Map<String, Object> createKctClearTamperToken(kctAndClearTamper kctAndClearTamper) {
        return Map.of();
    }

    @Override
    public Map<String, Object> createCompensationToken(KctToken kctToken) {
        return Map.of();
    }

    @Override
    public Map<String, Object> getAllToken() {
        return Map.of();
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
