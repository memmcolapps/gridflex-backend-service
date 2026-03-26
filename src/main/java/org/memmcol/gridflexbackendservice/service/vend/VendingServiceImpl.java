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
import org.memmcol.gridflexbackendservice.model.debt_setting.PercentageRange;
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
import java.util.stream.Collectors;

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
    // TODO
    public Map<String, Object> createCreditToken(CreditToken creditToken) {
        UserModel user = handleUserValidation();

        try {
            // --- Input Validation ---
            if (creditToken.getMeterNumber() != null && creditToken.getMeterNumber().trim().isEmpty()) {
                throw new GlobalExceptionHandler.NotFoundException("Meter number is required");
            }

            if (creditToken.getAccountNumber() != null && creditToken.getAccountNumber().trim().isEmpty()) {
                throw new GlobalExceptionHandler.NotFoundException("Account number is required");
            }

            if (creditToken.getInitialAmount() == null) {
                throw new GlobalExceptionHandler.NotFoundException("Initial amount is required");
            }

            if (creditToken.getInitialAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new GlobalExceptionHandler.NotFoundException("Amount cannot be negative");
            }

            // --- Get Meter Info ---
            List<MeterView> meters = vendMapper.getMeterRec(
                    creditToken.getMeterNumber(),
                    creditToken.getAccountNumber(),
                    user.getOrgId()
            );

            if (meters.isEmpty()) {

                if (creditToken.getAccountNumber() != null) {
                    throw new GlobalExceptionHandler.NotFoundException("Account number not found.");
                }

                if (creditToken.getMeterNumber() != null) {
                    throw new GlobalExceptionHandler.NotFoundException("Meter number not found.");
                }

            }

            MeterView meter = meters.get(0);

            System.out.println("getMeterCategory: "+meter.getMeterCategory());
            System.out.println("getMeterStage: "+meter.getMeterStage());
            System.out.println("getStatus: "+meter.getStatus());

            // --- Meter Validation ---
            if (!"Prepaid".equalsIgnoreCase(meter.getMeterCategory())
                    || !"Assigned".equalsIgnoreCase(meter.getMeterStage())
                    || !"Active".equalsIgnoreCase(meter.getStatus())) {
                throw new GlobalExceptionHandler.NotFoundException(
                        "Vending allowed only for active, assigned prepaid meters"
                );
            }

//            String debitPaymentMode = meter.getDebitPaymentMode();
//            String debitPaymentPlan = meter.getDebitPaymentPlan();
//            String creditPaymentMode = meter.getCreditPaymentMode();
//            String creditPaymentPlan = meter.getCreditPaymentPlan();

            // --- Process Debits Sequentially ---
            DebtPaymentResult debtResult = processDebtsSequentially(meters, creditToken.getInitialAmount(), user.getOrgId(), meter.getMeterId());
            
            if (debtResult.getErrorMessage() != null) {
                throw new GlobalExceptionHandler.NotFoundException(debtResult.getErrorMessage());
            }

            // Validate minimum vendable amount when no unpaid debts
//            if (debtResult.getMaximumVendable() != null && debtResult.getMaximumVendable().compareTo(BigDecimal.ZERO) > 0) {
//                if (creditToken.getInitialAmount().compareTo(debtResult.getMaximumVendable()) < 0) {
//                    throw new GlobalExceptionHandler.NotFoundException("Minimum that can be vended is " + debtResult.getMaximumVendable().toPlainString());
//                }
//            }

            BigDecimal debitToDeduct = debtResult.getTotalDeducted();
            BigDecimal amountAfterDebit = debtResult.getRemainingPayment();

            // --- VAT Calculation (Applied after debit, before credit) ---
            BigDecimal vatMultiplier =
                    "Paying".equalsIgnoreCase(meter.getVat())
                            ? new BigDecimal("1.075")
                            : BigDecimal.ONE;

            BigDecimal netTenderAfterDebit =
                    amountAfterDebit.divide(vatMultiplier, 6, RoundingMode.HALF_UP);

            BigDecimal vatAmount =
                    amountAfterDebit.subtract(netTenderAfterDebit);

            // --- Process Credits Sequentially (Applied after VAT) ---
            CreditPaymentResult creditResult = processCreditsSequentially(meters, netTenderAfterDebit, user.getOrgId(), meter.getMeterId());
            BigDecimal creditDeducted = creditResult.getTotalCreditDeducted();

            // --- Final Net Tender (After Debit, VAT, and Credit) ---
            BigDecimal finalNetTender = netTenderAfterDebit.add(creditDeducted);

            BigDecimal tariffRate = new BigDecimal(meter.getTariffRate());

            if (tariffRate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Invalid tariff rate");
            }

//            // --- Get Payment Strategies ---
//            PaymentStrategy debitStrategy =
//                    PaymentStrategyFactory.getStrategy(debitPaymentMode, "debit", debitPaymentPlan);
//
//            PaymentStrategy creditStrategy =
//                    PaymentStrategyFactory.getStrategy(creditPaymentMode, "credit", creditPaymentPlan);
//
//            BigDecimal debitToDeduct =
//                    debitStrategy.calculateDebitToDeduct(totalDebit);
//
//            BigDecimal creditUnitsToApply =
//                    creditStrategy.calculateCreditUnits(totalCreditUnits);

            // --- Calculate Net Amount ---
//            BigDecimal amountAfterDebit =
//                    creditToken.getInitialAmount().subtract(debitToDeduct);
//
//            if (amountAfterDebit.compareTo(BigDecimal.ZERO) < 0) {
//                amountAfterDebit = BigDecimal.ZERO;
//            }

            // --- Calculate Units from Final Net Tender ---
            BigDecimal unitsFromTender =
                    finalNetTender.divide(tariffRate, 3, RoundingMode.HALF_UP);

            BigDecimal finalUnits = unitsFromTender;

            BigDecimal costPerUnit =
                    finalUnits.compareTo(BigDecimal.ZERO) > 0
                            ? finalNetTender.divide(finalUnits, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

            // --- Token Generation ---
            TokenGenRequest request = new TokenGenRequest();
            request.setAmount(finalNetTender);
            request.setMeterNo(meter.getMeterNumber());
            request.setSgc(Integer.parseInt(meter.getNewSgc()));
            request.setTi(Integer.parseInt(meter.getNewTariffIndex().toString()));
            request.setMeterType("STS6");

            // --- Persist Transaction ---
            Transaction transaction = new Transaction();
<<<<<<< dev17
            transaction.setTxNodeId(nodeId);
            transaction.setCustomerFullname(meter.getCustomerFullname());
            transaction.setMeterNumber(meter.getMeterNumber());
            transaction.setMeterAccountNumber(meter.getMeterAccountNumber());
            transaction.setUserFullname(user.getFirstname()+' '+user.getLastname());
=======
>>>>>>> main
            transaction.setMeterId(meter.getMeterId());
            transaction.setInitialAmount(creditToken.getInitialAmount());
            transaction.setFinalAmount(finalNetTender);
            transaction.setToken(generateDummyToken());
            transaction.setUnit(finalUnits);
            transaction.setUnitCost(costPerUnit);
            transaction.setTokenType(creditToken.getTokenType());
            transaction.setStatus("Successful");
            transaction.setReceiptNo(generateReceiptNumber(creditToken.getMeterNumber()));
            transaction.setOrgId(user.getOrgId());
            transaction.setUserId(user.getId());
            transaction.setCustomerId(meter.getCustomerId());
            transaction.setKct1(generateDummyToken());
            transaction.setKct2(generateDummyToken());
            transaction.setVatAmount(vatAmount);

            int created = vendMapper.createCreditToken(transaction);
            if (created == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Credit token creation failed");
            }

            UUID transactionId = transaction.getId();

            // --- Settle Debts After Token Generation ---
            if (debtResult.getDebtPayments() != null && !debtResult.getDebtPayments().isEmpty()) {
                List<CreditDebitAdjustmentSettlementService.SettledDebt> settledDebts = debtResult.getDebtPayments().stream()
                        .map(dp -> CreditDebitAdjustmentSettlementService.SettledDebt.builder()
                                .amountPaid(dp.getAmountPaid())
                                .balanceBefore(dp.getBalanceBefore())
                                .build())
                        .collect(Collectors.toList());

                
                adjustmentSettlementService.settleDebtsSequentially(
                        user.getOrgId(),
                        meter.getMeterId(),
                        transactionId,
                        settledDebts
                );
            }

            // --- Settle Credits After Token Generation ---
            if (creditResult.getCreditPayments() != null && !creditResult.getCreditPayments().isEmpty()) {
                List<CreditDebitAdjustmentSettlementService.SettledCredit> settledCredits = creditResult.getCreditPayments().stream()
                        .map(cp -> CreditDebitAdjustmentSettlementService.SettledCredit.builder()
                                .amountPaid(cp.getAmountPaid())
                                .balanceBefore(cp.getBalanceBefore())
                                .build())
                        .collect(Collectors.toList());

                adjustmentSettlementService.settleCreditsSequentially(
                        user.getOrgId(),
                        meter.getMeterId(),
                        transactionId,
                        settledCredits
                );
            }

            return ResponseMap.response(status.getSuccessCode(),
                    "Credit token generated successfully",
                    transaction
            );

        } catch (Exception ex) {
            genericHandler.logAndSaveException(ex, "creating credit token");
            throw ex;
        }
    }

//    @Transactional
//    public Map<String, Object> createCreditTokenBackup(CreditToken creditToken) {
//        try {
//            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
//            UserModel user = handleUserValidation();
//            Meter meterResult = vendMapper.getMeter(user.getOrgId(), creditToken.getMeterNumber(), creditToken.getAccountNumber());
//
//            if (meterResult == null) {
//                throw new GlobalExceptionHandler.NotFoundException("Invalid meter for this organization.");
//            }
//
//            boolean isValidForVending =
//                    "Prepaid".equalsIgnoreCase(meterResult.getMeterCategory())
//                            && "Assigned".equalsIgnoreCase(meterResult.getMeterStage())
//                            && "Active".equalsIgnoreCase(meterResult.getStatus());
//
//            if (!isValidForVending) {
//                throw new GlobalExceptionHandler.NotFoundException(
//                        "Vending is only allowed for active, assigned prepaid meters."
//                );
//            }
//
//            if(!creditToken.getTokenType().equalsIgnoreCase("credit-token")) {
//                throw new GlobalExceptionHandler.NotFoundException("Token type not found or attempt to generate wrong token");
//            }
//
//            // --- Fetch meter info ---
//            List<MeterView> meters = vendMapper.getMeterInfo(creditToken.getMeterNumber(), creditToken.getAccountNumber(), user.getOrgId());
//
//            if (meters.isEmpty()) {
//                throw new GlobalExceptionHandler.NotFoundException("Meter not found");
//            }
//
//            MeterView meter = meters.get(0);
//
//            // --- Balance Calculations ---
//            BigDecimal totalDebit = calculateTotalByType(meters, "debit");
//            BigDecimal totalCredit = calculateTotalByType(meters, "credit");
//
//            // (credit + amountTendered) - debit
//            BigDecimal netBalance = calculateNetBalance(totalCredit, creditToken.getInitialAmount(), totalDebit);
//
//            // --- VAT and Unit Calculations ---
//            BigDecimal vatRate = meter.getVat().equalsIgnoreCase("Paying")
//                    ? new BigDecimal("1.075") : BigDecimal.ONE;
//
////            BigDecimal vatRate = new BigDecimal("0.075");
////            BigDecimal vatAmount = calculateVatAmount(netBalance, vatRate);
////            BigDecimal totalWithVat = netBalance.add(vatAmount);
//
//            // --- Tariff Rate (safe parse) ---
//            BigDecimal tariffRate;
//            try {
//                tariffRate = new BigDecimal(meter.getTariffRate());
//            } catch (Exception e) {
//                tariffRate = BigDecimal.ZERO; // invalid rate should act as 0
//            }
//
//            BigDecimal units;
//            BigDecimal costPerUnit;
//
//            if (tariffRate.compareTo(BigDecimal.ZERO) > 0) {
//                costPerUnit = netBalance.divide(vatRate,  2, RoundingMode.HALF_UP);
//                // Normal calculation if tariffRate is valid
//                units = costPerUnit.divide(tariffRate, 2, RoundingMode.HALF_UP);
//                System.out.println("unit1:: "+units);
////                if (units.compareTo(BigDecimal.ZERO) > 0) {
////                    costPerUnit = totalWithVat.divide(units, 2, RoundingMode.HALF_UP);
////                }
//            } else {
//                costPerUnit = netBalance.divide(vatRate,  2, RoundingMode.HALF_UP);
//                units = costPerUnit.divide(tariffRate.equals(BigDecimal.ZERO) ? BigDecimal.ONE : tariffRate, 2, RoundingMode.HALF_UP);
//            }
//
////            if (tariffRate.compareTo(BigDecimal.ZERO) > 0) {
////                // Normal calculation if tariffRate is valid
////                units = netBalance.divide(tariffRate, 2, RoundingMode.HALF_UP);
////
////                if (units.compareTo(BigDecimal.ZERO) > 0) {
////                    costPerUnit = totalWithVat.divide(units, 2, RoundingMode.HALF_UP);
////                }
////            }
//
////            BigDecimal finalAmount = netBalance.subtract(vatAmount);
////            System.out.println("unit:: "+units);
//
//            BigDecimal vatAmount = creditToken.getInitialAmount().subtract(costPerUnit);
//            vatRate = vatRate.equals(BigDecimal.ONE) ? BigDecimal.ZERO : vatRate;
//
//            TokenGenRequest request = new TokenGenRequest();
//            request.setAmount(units);
//            request.setMeterNo(meter.getMeterNumber());
//            request.setSgc(Integer.parseInt(meter.getNewSgc()));
//            request.setTi(Integer.parseInt(meter.getNewTariffIndex().toString()));
//            request.setMeterType("STS6");
//
////            TokenGenResponse tokenResponse = tokenGenClient.generateToken(request, "/tokenGen", creditToken.getTokenType());
////
////            if (tokenResponse.getCode() == null || !"SUCCESS".equalsIgnoreCase(tokenResponse.getCode())) {
////                throw new GlobalExceptionHandler.NotFoundException("Token generation failed");
////            }
//
//            // --- Update Transaction ---
//            Transaction transaction = new Transaction();
//            transaction.setUnitCost(costPerUnit);
//            transaction.setUnit(units);
//            transaction.setVatAmount(vatAmount);
//            transaction.setMeterId(meter.getMeterId());
//            transaction.setTariffId(meter.getTariffId());
//            transaction.setToken(generateDummyToken());
////            transaction.setToken(tokenResponse.getTokens().get(0));
//            transaction.setStatus("Successful");
//            transaction.setReceiptNo(generateReceiptNumber(creditToken.getMeterNumber()));
//            transaction.setOrgId(user.getOrgId());
//            transaction.setUserId(user.getId());
//            transaction.setCustomerId(meter.getCustomerId());
//            transaction.setInitialAmount(creditToken.getInitialAmount());
//            transaction.setFinalAmount(netBalance.subtract(vatAmount));
//            transaction.setTokenType(creditToken.getTokenType());
//            transaction.setKct1(generateDummyToken());
//            transaction.setKct2(generateDummyToken());
//
//            // --- Persist ---
//            int created = vendMapper.createCreditToken(transaction);
//            if (created == 0) {
//                throw new GlobalExceptionHandler.NotFoundException("Credit token creation failed.");
//            }
//
//            Transaction savedTransaction = vendMapper.getCreditTokenTransaction(transaction.getId(), user.getOrgId());
//
//            // Audit (optional)
//             AuditLog auditLog = buildAuditLog(user, "Credit token created", "Vend", savedTransaction, metadata, null);
//             safeAuditService.saveAudit(auditLog);
//
//
//
//            return ResponseMap.response(status.getSuccessCode(), "Credit token generated successfully", savedTransaction);
//
//        } catch (Exception ex) {
//            genericHandler.logIncidentReport("Creating credit token service failed");
//            genericHandler.logAndSaveException(ex, "creating credit token");
//            throw ex;
//        }
//    }

    @Transactional
    @Override
    public Map<String, Object> calculateCreditToken(CreditToken creditToken) {

        try {

//            if (creditToken.getMeterNumber() == null || creditToken.getMeterNumber().isBlank()) {
//                throw new GlobalExceptionHandler.NotFoundException("Meter number is required");
//            }

            if (creditToken.getMeterNumber() != null && creditToken.getMeterNumber().trim().isEmpty()) {
                throw new GlobalExceptionHandler.NotFoundException("Meter number is required");
            }

            if (creditToken.getAccountNumber() != null && creditToken.getAccountNumber().trim().isEmpty()) {
                throw new GlobalExceptionHandler.NotFoundException("Account number is required");
            }


            if (creditToken.getInitialAmount() == null) {
                throw new GlobalExceptionHandler.NotFoundException("Initial amount is required");
            }

            if (creditToken.getInitialAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new GlobalExceptionHandler.NotFoundException("Amount cannot be negative");
            }
            UserModel user = handleUserValidation();

            List<MeterView> meters = vendMapper.getMeterRec(
                    creditToken.getMeterNumber(),
                    creditToken.getAccountNumber(),
                    user.getOrgId()
            );

            if (meters.isEmpty()) {

                if (creditToken.getAccountNumber() != null) {
                    throw new GlobalExceptionHandler.NotFoundException("Account number not found.");
                }

                if (creditToken.getMeterNumber() != null) {
                    throw new GlobalExceptionHandler.NotFoundException("Meter number not found.");
                }

            }

            MeterView meter = meters.get(0);

//            String debitPaymentMode = meter.getDebitPaymentMode();
//            String debitPaymentPlan = meter.getDebitPaymentPlan();
//            String creditPaymentMode = meter.getCreditPaymentMode();
//            String creditPaymentPlan = meter.getCreditPaymentPlan();


//            String paymentType = meter.getPaymentType();

//            BigDecimal totalDebit = calculateTotalByType(meters, "debit");
//            BigDecimal totalCreditUnits = calculateTotalByType(meters,"credit");

            // --- Process Debits Sequentially ---
            DebtPaymentResult debtResult = processDebtsSequentially(meters, creditToken.getInitialAmount(), user.getOrgId(), meter.getMeterId());
            
            if (debtResult.getErrorMessage() != null) {
                throw new GlobalExceptionHandler.NotFoundException(debtResult.getErrorMessage());
            }

            // Validate minimum vendable amount when no unpaid debts
//            if (debtResult.getMaximumVendable() != null && debtResult.getMaximumVendable().compareTo(BigDecimal.ZERO) > 0) {
//                if (creditToken.getInitialAmount().compareTo(debtResult.getMaximumVendable()) < 0) {
//                    throw new GlobalExceptionHandler.NotFoundException("Minimum that can be vended is " + debtResult.getMaximumVendable().toPlainString());
//                }
//            }

            BigDecimal debitToDeduct = debtResult.getTotalDeducted();
            BigDecimal amountAfterDebit = debtResult.getRemainingPayment();

            // --- VAT Calculation (Applied after debit, before credit) ---
            BigDecimal vatMultiplier =
                    "Paying".equalsIgnoreCase(meter.getVat())
                            ? new BigDecimal("1.075")
                            : BigDecimal.ONE;

            BigDecimal netTenderAfterDebit =
                    amountAfterDebit.divide(vatMultiplier, 6, RoundingMode.HALF_UP);

            BigDecimal vatAmount =
                    amountAfterDebit.subtract(netTenderAfterDebit);

            // --- Process Credits Sequentially (Applied after VAT) ---
            CreditPaymentResult creditResult = processCreditsSequentially(meters, netTenderAfterDebit, user.getOrgId(), meter.getMeterId());
            BigDecimal creditDeducted = creditResult.getTotalCreditDeducted();

            // --- Final Net Tender (After Debit, VAT, and Credit) ---
            BigDecimal finalNetTender = netTenderAfterDebit.add(creditDeducted);

            BigDecimal tariffRate = new BigDecimal(meter.getTariffRate());
            if (tariffRate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Invalid tariff rate");
            }

            // --- Calculate Units from Final Net Tender ---
            BigDecimal unitsFromTender =
                    finalNetTender.divide(tariffRate, 3, RoundingMode.HALF_UP);

            BigDecimal finalUnits = unitsFromTender;

            BigDecimal costPerUnit =
                    finalUnits.compareTo(BigDecimal.ZERO) > 0
                            ? finalNetTender.divide(finalUnits, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

            // Calculate totals for response
            BigDecimal totalDebit = calculateTotalByType(meters, "debit");
            BigDecimal totalCreditUnits = calculateTotalByType(meters, "credit");

            // Set response
            creditToken.setVat(
                    vatMultiplier.equals(BigDecimal.ONE)
                            ? BigDecimal.ZERO
                            : vatMultiplier
            );

            creditToken.setVatAmount(vatAmount);
            creditToken.setUnit(finalUnits);
            creditToken.setCostOfUnit(costPerUnit);
            creditToken.setFinalAmount(finalNetTender);

            Map<String, Object> responseData = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            data.put("customerName", meter.getCustomerFullname());
            data.put("meterNumber", meter.getMeterNumber());
            data.put("accountNo", meter.getMeterAccountNumber());
            data.put("operator", user.getFirstname() +" "+ user.getLastname());
            responseData.put("totalDebitBalance", totalDebit);
            responseData.put("totalCreditBalance", totalCreditUnits);
            responseData.put("debitDeducted", debitToDeduct);
            responseData.put("creditDeducted", creditDeducted);
//            responseData.put("creditUnitsApplied", creditUnitsToApply);
            responseData.put("meterNumber", creditToken.getMeterNumber());
            responseData.put("costOfUnit", creditToken.getCostOfUnit());
            responseData.put("vat", creditToken.getVat());
            responseData.put("vatAmount", creditToken.getVatAmount());
            responseData.put("unit", creditToken.getUnit());
            responseData.put("finalAmount", creditToken.getFinalAmount());
            responseData.put("initialAmount", creditToken.getInitialAmount());
            responseData.put("creditPaymentMode", meter.getCreditPaymentMode());
            responseData.put("debitPaymentMode", meter.getDebitPaymentMode());
            responseData.put("data", data);

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Credit token calculated successfully",
                    responseData
            );

        } catch (Exception ex) {
            genericHandler.logAndSaveException(ex, "calculate credit token");
            throw ex;
        }
    }

//    private BigDecimal calculateTotalDebit(List<MeterView> meters) {
//
//        return meters.stream()
//                .map(MeterView::getDebitAmount)
//                .filter(Objects::nonNull)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//    }
//
//
//    private BigDecimal calculateTotalCreditUnits(List<MeterView> meters) {
//
//        return meters.stream()
//                .map(MeterView::getCreditAmount)
//                .filter(Objects::nonNull)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//    }


///-----------------
    ///------------------------------------------
//    @Transactional
//    @Override
//    public Map<String, Object> calculateCreditToken(CreditToken creditToken) {
//
//        try {
//            if(creditToken.getMeterNumber() == null || creditToken.getMeterNumber().isBlank()){
//                throw new GlobalExceptionHandler.NotFoundException("Meter number is required");
//            }
//
//            if(creditToken.getInitialAmount() == null){
//                throw new GlobalExceptionHandler.NotFoundException("Initial amount is required");
//            }
//
//            UserModel user = handleUserValidation();
//
//            List<MeterView> meters = vendMapper.getMeterInfo(
//                    creditToken.getMeterNumber(),
//                    creditToken.getAccountNumber(),
//                    user.getOrgId()
//            );
//
//            if (meters.isEmpty()) {
//                throw new GlobalExceptionHandler.NotFoundException("Meter not found.");
//            }
//
//            MeterView meter = meters.get(0);
//
//            String paymentMode = meter.getPaymentMode();
//            String paymentPlan = meter.getPaymentPlan();
//            String paymentType = meter.getPaymentPlan();
//
//            // ------------------------------------------------------------
//            // 1️⃣ Debit (money) & Credit (units)
//            // ------------------------------------------------------------
//            BigDecimal totalDebit = calculateTotalByType(meters, "debit");     // MONEY
//            BigDecimal totalCreditUnits = calculateTotalByType(meters, "credit"); // ALREADY UNITS
//
//            // ------------------------------------------------------------
//            // 2️⃣ VAT
//            // ------------------------------------------------------------
//            BigDecimal vatMultiplier =
//                    meter.getVat().equalsIgnoreCase("Paying")
//                            ? new BigDecimal("1.075")
//                            : BigDecimal.ONE;
//
//            // ------------------------------------------------------------
//            // 3️⃣ Tariff
//            // ------------------------------------------------------------
//            BigDecimal tariffRate = new BigDecimal(meter.getTariffRate());
//
//            if (tariffRate.compareTo(BigDecimal.ZERO) <= 0) {
//                throw new IllegalArgumentException("Invalid tariff rate");
//            }
//
//            // ------------------------------------------------------------
//            // 4️⃣ Remove debit FIRST
//            // ------------------------------------------------------------
//            BigDecimal amountAfterDebit =
//                    creditToken.getInitialAmount().subtract(totalDebit);
//
//            if (amountAfterDebit.compareTo(BigDecimal.ZERO) < 0) {
//                amountAfterDebit = BigDecimal.ZERO;
//            }
//
//            // ------------------------------------------------------------
//            // 5️⃣ Remove VAT after debit
//            // ------------------------------------------------------------
//            BigDecimal netTender =
//                    amountAfterDebit.divide(vatMultiplier, 6, RoundingMode.HALF_UP);
//
//            BigDecimal vatAmount =
//                    amountAfterDebit.subtract(netTender);
//
//            // ------------------------------------------------------------
//            // 6️⃣ Units from money
//            // ------------------------------------------------------------
//            BigDecimal unitsFromTender =
//                    netTender.divide(tariffRate, 3, RoundingMode.HALF_UP);
//
//            // ------------------------------------------------------------
//            // 7️⃣ Add credit units directly
//            // ------------------------------------------------------------
//            BigDecimal finalUnits =
//                    unitsFromTender.add(totalCreditUnits);
//
//            // ------------------------------------------------------------
//            // 8️⃣ Effective cost per unit (money only)
//            // ------------------------------------------------------------
//            BigDecimal costPerUnit = finalUnits.compareTo(BigDecimal.ZERO) > 0
//                    ? netTender.divide(finalUnits, 2, RoundingMode.HALF_UP)
//                    : BigDecimal.ZERO;
//
//            // ------------------------------------------------------------
//            // 9️⃣ Set response
//            // ------------------------------------------------------------
//            creditToken.setVat(
//                    vatMultiplier.equals(BigDecimal.ONE)
//                            ? BigDecimal.ZERO
//                            : vatMultiplier
//            );
//
//            creditToken.setVatAmount(vatAmount);
//            creditToken.setUnit(finalUnits);
//            creditToken.setCostOfUnit(costPerUnit);
//            creditToken.setFinalAmount(netTender);
//
//            Map<String, Object> responseData = new HashMap<>();
//            responseData.put("data", creditToken);
//            responseData.put("totalDebitBalance", totalDebit);
//            responseData.put("totalCreditUnits", totalCreditUnits);
//            responseData.put("netTender", netTender);
//            responseData.put("unitsFromTender", unitsFromTender);
//            responseData.put("meter", meters);
//
//            return ResponseMap.response(
//                    status.getSuccessCode(),
//                    "Credit token calculated successfully",
//                    responseData
//            );
//
//        } catch (Exception ex) {
//            genericHandler.logAndSaveException(ex, "calculate credit token");
//            throw ex;
//        }
//    }


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

    private boolean hasUnpaidDebts(List<MeterView> meters) {
        return meters.stream()
                .filter(m -> "debit".equalsIgnoreCase(m.getAdjustmentType()))
                .anyMatch(m -> {
                    BigDecimal balance = m.getBalanceAfterAdjustment();
                    String status = m.getAdjustmentStatus();
                    boolean hasBalance = balance != null && balance.compareTo(BigDecimal.ZERO) > 0;
                    boolean isNotPaid = status == null || !"paid".equalsIgnoreCase(status);
                    return hasBalance && isNotPaid;
                });
    }

    private boolean hasUnpaidCredits(List<MeterView> meters) {
        return meters.stream()
                .filter(m -> "credit".equalsIgnoreCase(m.getAdjustmentType()))
                .anyMatch(m -> {
                    BigDecimal balance = m.getBalanceAfterAdjustment();
                    String status = m.getAdjustmentStatus();
                    boolean hasBalance = balance != null && balance.compareTo(BigDecimal.ZERO) > 0;
                    boolean isNotPaid = status == null || !"paid".equalsIgnoreCase(status);
                    return hasBalance && isNotPaid;
                });
    }

    private DebtPaymentResult processDebtsSequentially(List<MeterView> meters, BigDecimal paymentAmount, UUID orgId, UUID meterId) {
        
        List<MeterView> debitMeters = meters.stream()
                .filter(m -> "debit".equalsIgnoreCase(m.getAdjustmentType()))
                .sorted(Comparator.comparing(MeterView::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        if (debitMeters.isEmpty() || !hasUnpaidDebts(meters)) {
            return DebtPaymentResult.builder()
                    .totalDeducted(BigDecimal.ZERO)
                    .remainingPayment(paymentAmount != null ? paymentAmount : BigDecimal.ZERO)
                    .errorMessage(null)
                    .failedDebt(null)
                    .maximumVendable(BigDecimal.ZERO)
                    .debtPayments(new ArrayList<>())
                    .build();
        }

        BigDecimal remainingPayment = paymentAmount != null ? paymentAmount : BigDecimal.ZERO;
        BigDecimal totalDeducted = BigDecimal.ZERO;
        String errorMessage = null;
        MeterView failedDebt = null;
        BigDecimal maximumVendable = BigDecimal.ZERO;
        
        List<DebtPaymentResult.DebtPayment> debtPayments = new ArrayList<>();
        
        // Calculate percentage once before processing debts
        BigDecimal percentageValue = BigDecimal.ZERO;
        boolean hasPercentageDebt = debitMeters.stream()
                .anyMatch(d -> "percentage".equalsIgnoreCase(d.getDebitPaymentMode()));
        if (hasPercentageDebt) {
            PercentageRange pr = vendMapper.findPercentageByRange(orgId, meterId, paymentAmount);
            if (pr != null && pr.getPercentage() != null) {
                try {
                    percentageValue = new BigDecimal(pr.getPercentage());
                    if (percentageValue.compareTo(new BigDecimal("100")) > 0) {
                        percentageValue = new BigDecimal("100");
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        for (MeterView debt : debitMeters) {
            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal balance = debt.getBalanceAfterAdjustment() != null
                    ? debt.getBalanceAfterAdjustment() 
                    : BigDecimal.ZERO;
            
            if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            String paymentMode = debt.getDebitPaymentMode();
            String paymentPlan = debt.getDebitPaymentPlan();
            BigDecimal deduction = BigDecimal.ZERO;

            if ("no-payment".equalsIgnoreCase(paymentMode)) {
                continue;
            }

            if ("monthly".equalsIgnoreCase(paymentMode)) {
                int currentYear = LocalDateTime.now().getYear();
                int currentMonth = LocalDateTime.now().getMonthValue();
                int paymentCount = vendMapper.countPaymentsThisMonth(meterId, debt.getCreditDebitAdjId(), currentYear, currentMonth, "debit");
                
                if (paymentCount > 0) {
                    continue;
                }

                int months = 1;
                try {
                    if (paymentPlan != null && !paymentPlan.isBlank()) {
                        months = Integer.parseInt(paymentPlan);
                    }
                } catch (NumberFormatException ignored) {}
                
                BigDecimal debitAmt = debt.getDebitAmount() != null ? debt.getDebitAmount() : balance;
                BigDecimal expectedMonthly = debitAmt.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
                
                BigDecimal minimumRequired;
                
                // Compare balance with expectedMonthly
                if (balance.compareTo(expectedMonthly) < 0) {
                    // Balance is NOT >= expectedMonthly installment
                    // Minimum required = balance + 1000
                    minimumRequired = balance.add(new BigDecimal("1000"));
                    
                    if (remainingPayment.compareTo(minimumRequired) < 0) {
                        errorMessage = "Minimum that can be vended is " + minimumRequired.toPlainString();
                        failedDebt = debt;
                        break;
                    }
                    
                    // Deduct the balance (not expectedMonthly)
                    deduction = balance;
                } else {
                    // Balance is >= expectedMonthly installment
                    minimumRequired = expectedMonthly.add(new BigDecimal("1000"));
                    
                    if (remainingPayment.compareTo(minimumRequired) < 0) {
                        errorMessage = "Minimum that can be vended is " + minimumRequired.toPlainString();
                        failedDebt = debt;
                        break;
                    }
                    
                    // Deduct expectedMonthly
                    deduction = expectedMonthly;
                }
                
                maximumVendable = minimumRequired;
                
            } else if ("percentage".equalsIgnoreCase(paymentMode)) {
                // Use pre-calculated percentage (calculated once before the loop)
                if (percentageValue.compareTo(BigDecimal.ZERO) > 0) {
                    deduction = remainingPayment.multiply(percentageValue).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    
                    // CAP: Don't exceed balance (no over-payment)
                    if (deduction.compareTo(balance) > 0) {
                        deduction = balance;
                    }
                }
                
            } else {
                // One-off mode - validate payment must be debitAmount + 1000
                BigDecimal debitAmt = debt.getDebitAmount() != null ? debt.getDebitAmount() : balance;
                
                BigDecimal minimumRequired;
                if (balance.compareTo(debitAmt) < 0) {
                    minimumRequired = balance.add(new BigDecimal("1000"));
                    deduction = balance;
                } else {
                    minimumRequired = debitAmt.add(new BigDecimal("1000"));
                    deduction = debitAmt;
                }
                
                maximumVendable = minimumRequired;
                
                if (remainingPayment.compareTo(minimumRequired) < 0) {
                    errorMessage = "Minimum that can be vended is " + maximumVendable.toPlainString();
                    failedDebt = debt;
                    break;
                }
            }

            if (deduction.compareTo(BigDecimal.ZERO) > 0) {
                if (deduction.compareTo(remainingPayment) > 0) {
                    deduction = remainingPayment;
                }
                remainingPayment = remainingPayment.subtract(deduction);
                totalDeducted = totalDeducted.add(deduction);
                
                // Track the debt payment for settlement
                DebtPaymentResult.DebtPayment debtPayment = DebtPaymentResult.DebtPayment.builder()
                        .liabilityCauseId(null)
                        .liabilityName(debt.getLiabilityName())
                        .amountPaid(deduction)
                        .balanceBefore(balance)
                        .balanceAfter(balance.subtract(deduction))
                        .build();
                debtPayments.add(debtPayment);
            }
        }

        return DebtPaymentResult.builder()
                .totalDeducted(totalDeducted)
                .remainingPayment(remainingPayment)
                .errorMessage(errorMessage)
                .failedDebt(failedDebt)
                .maximumVendable(maximumVendable)
                .debtPayments(debtPayments)
                .build();
    }

    private CreditPaymentResult processCreditsSequentially(List<MeterView> meters, BigDecimal paymentAmount, UUID orgId, UUID meterId) {
        List<MeterView> creditMeters = meters.stream()
                .filter(m -> "credit".equalsIgnoreCase(m.getAdjustmentType()))
                .sorted(Comparator.comparing(MeterView::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        if (creditMeters.isEmpty() || !hasUnpaidCredits(meters)) {
            return CreditPaymentResult.builder()
                    .totalCreditUnits(BigDecimal.ZERO)
                    .totalCreditDeducted(BigDecimal.ZERO)
                    .remainingPayment(paymentAmount)
                    .build();
        }

        BigDecimal remainingPayment = paymentAmount != null ? paymentAmount : BigDecimal.ZERO;
        BigDecimal totalCreditUnits = BigDecimal.ZERO;
        BigDecimal totalCreditDeducted = BigDecimal.ZERO;
        List<CreditPaymentResult.CreditPayment> creditPayments = new ArrayList<>();
        
        // Calculate percentage once before processing credits
        BigDecimal percentageValue = BigDecimal.ZERO;
        boolean hasPercentageCredit = creditMeters.stream()
                .anyMatch(c -> "percentage".equalsIgnoreCase(c.getCreditPaymentMode()));
        if (hasPercentageCredit) {
            PercentageRange pr = vendMapper.findPercentageByRange(orgId, meterId, paymentAmount);
            if (pr != null && pr.getPercentage() != null) {
                try {
                    percentageValue = new BigDecimal(pr.getPercentage());
                    if (percentageValue.compareTo(new BigDecimal("100")) > 0) {
                        percentageValue = new BigDecimal("100");
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        for (MeterView credit : creditMeters) {
            BigDecimal balance = credit.getBalanceAfterAdjustment() != null
                    ? credit.getBalanceAfterAdjustment()
                    : BigDecimal.ZERO;
            
            if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            String paymentMode = credit.getCreditPaymentMode();
            String paymentPlan = credit.getCreditPaymentPlan();
            BigDecimal creditDeducted = BigDecimal.ZERO;
            BigDecimal creditUnits = BigDecimal.ZERO;

            if ("no-payment".equalsIgnoreCase(paymentMode)) {
                continue;
            }

            if ("monthly".equalsIgnoreCase(paymentMode)) {
                int currentYear = LocalDateTime.now().getYear();
                int currentMonth = LocalDateTime.now().getMonthValue();
                int paymentCount = vendMapper.countPaymentsThisMonth(meterId, credit.getCreditDebitAdjId(), currentYear, currentMonth, "credit");
                
                if (paymentCount > 0) {
                    continue;
                }

                int months = 1;
                try {
                    if (paymentPlan != null && !paymentPlan.isBlank()) {
                        months = Integer.parseInt(paymentPlan);
                    }
                } catch (NumberFormatException ignored) {}
                
                BigDecimal creditAmt = credit.getDebitAmount() != null ? credit.getDebitAmount() : balance;
                BigDecimal expectedMonthly = creditAmt.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
                
                if (balance.compareTo(expectedMonthly) < 0) {
                    creditDeducted = balance;
                } else {
                    creditDeducted = expectedMonthly;
                }
                
            } else if ("percentage".equalsIgnoreCase(paymentMode)) {
                BigDecimal creditAmt = credit.getDebitAmount() != null ? credit.getDebitAmount() : balance;
                if (percentageValue.compareTo(BigDecimal.ZERO) > 0) {
                    creditDeducted = creditAmt.multiply(percentageValue)
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    
                    if (balance.compareTo(creditDeducted) < 0) {
                        creditDeducted = balance;
                    }
                }
                
            } else {
                BigDecimal creditAmt = credit.getDebitAmount() != null ? credit.getDebitAmount() : balance;
                if (balance.compareTo(creditAmt) < 0) {
                    creditDeducted = balance;
                } else {
                    creditDeducted = creditAmt;
                }
            }
            
            // Calculate credit units for display (creditDeducted / tariffRate will be done in main flow)
            // Store creditDeducted as totalCreditDeducted
            totalCreditDeducted = totalCreditDeducted.add(creditDeducted);
            
            // Also track credit units for legacy/display purposes
            creditUnits = creditDeducted;
            totalCreditUnits = totalCreditUnits.add(creditUnits);
            
            // Track credit payment for settlement
            BigDecimal balanceAfter = balance.subtract(creditDeducted);
            if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
                balanceAfter = BigDecimal.ZERO;
            }
            
            CreditPaymentResult.CreditPayment creditPayment = CreditPaymentResult.CreditPayment.builder()
                    .adjustmentId(credit.getCreditDebitAdjId())
                    .adjustmentName(credit.getLiabilityName())
                    .amountPaid(creditDeducted)
                    .balanceBefore(balance)
                    .balanceAfter(balanceAfter)
                    .build();
            creditPayments.add(creditPayment);
        }

        return CreditPaymentResult.builder()
                .totalCreditUnits(totalCreditUnits)
                .totalCreditDeducted(totalCreditDeducted)
                .remainingPayment(remainingPayment)
                .creditPayments(creditPayments)
                .build();
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
