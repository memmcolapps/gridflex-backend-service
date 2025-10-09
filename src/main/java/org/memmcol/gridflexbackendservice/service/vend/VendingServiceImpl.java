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
    public Map<String, Object> createCreditToken(Transaction transaction) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel user = handleUserValidation();

            System.out.println(">>>>meter:: "+transaction.getMeterNumber());
            // --- Fetch meter info ---
            List<MeterView> meters = vendMapper.getMeterInfo(transaction.getMeterNumber(), transaction.getMeterAccountNumber(), user.getOrgId());
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
            BigDecimal netBalance = calculateNetBalance(totalCredit, transaction.getAmount(), totalDebit);

            // --- VAT and Unit Calculations ---
            BigDecimal vatRate = new BigDecimal("0.075");
            BigDecimal vatAmount = calculateVatAmount(netBalance, vatRate);
            BigDecimal totalWithVat = netBalance.add(vatAmount);

            BigDecimal tariffRate = new BigDecimal(meter.getTariffRate());
            BigDecimal units = netBalance.divide(tariffRate, 2, RoundingMode.HALF_UP);
            BigDecimal costPerUnit = totalWithVat.divide(units, 2, RoundingMode.HALF_UP);

            // --- Update Transaction ---
            transaction.setUnitCost(costPerUnit);
            transaction.setUnit(units);
            transaction.setVatAmount(vatAmount);
            transaction.setMeterId(meter.getMeterId());
            transaction.setTariffId(meter.getTariffId());
            transaction.setToken("1130987906543214590007");
            transaction.setStatus("Completed");
            transaction.setReceiptNo(generateReceiptNumber(transaction));
            transaction.setOrgId(user.getOrgId());
            transaction.setUserId(user.getId());
            transaction.setCustomerId(meter.getCustomerId());

            // --- Persist ---
            int created = vendMapper.createCreditToken(transaction);
            if (created == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Credit token creation failed.");
            }

//            Transaction savedTransaction = vendMapper.getCreditTokenTransaction(transaction.getId());

            // Audit (optional)
//             AuditLog auditLog = buildAuditLog(user, "Credit token created", "Vend", savedTransaction, metadata);
//             auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(), "Credit token generated successfully", "savedTransaction");

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
            BigDecimal netBalance = calculateNetBalance(totalCredit, creditToken.getAmount(), totalDebit);

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

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("data", creditToken);
            responseData.put("transaction", meter);
            responseData.put("totalAmountTendered", netBalance);
            responseData.put("totalDebitBalance", totalDebit);
            responseData.put("totalCreditBalance", totalCredit);

            return ResponseMap.response(status.getSuccessCode(), "Credit token calculated successfully", responseData);

        } catch (Exception ex) {
            genericHandler.logIncidentReport("Calculating credit token service failed");
            genericHandler.logAndSaveException(ex, "calculate credit token");
            throw ex;
        }
    }


    private String generateReceiptNumber(Transaction transaction) {
        String prefix = "RCPT";
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        return String.format("%s-%s-%s", prefix, timestamp, transaction.getMeterNumber());
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



//    @Override
//    public Map<String, Object> createCreditToken(Transaction transaction) {
//        try {
//            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
//            UserModel um = handleUserValidation();
//
////            Meter meter = vendMapper.getMeter(um.getOrgId(), creditToken.getMeterNumber(), creditToken.getAccountNumber());
////            MeterView meterView = new MeterView();
//            List<MeterView> meter = vendMapper.getMeterInfo(transaction.getMeterNumber(), transaction.getMeterAccountNumber());
//
//            // --- BALANCE CALCULATION ---
//            BigDecimal totalDebitBalance = meter.stream()
//                    .filter(m -> "debit".equalsIgnoreCase(m.getAdjustmentType()))
//                    .map(MeterView::getBalanceAfterAdjustment)
//                    .filter(Objects::nonNull)
//                    .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//            BigDecimal totalCreditBalance = meter.stream()
//                    .filter(m -> "credit".equalsIgnoreCase(m.getAdjustmentType()))
//                    .map(MeterView::getBalanceAfterAdjustment)
//                    .filter(Objects::nonNull)
//                    .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//            // formula: (credit + amountTendered) - debit
//            BigDecimal netBalance = totalCreditBalance
//                    .add(transaction.getAmount())
//                    .subtract(totalDebitBalance)
//                    .setScale(2, RoundingMode.HALF_UP);
//
//            BigDecimal vatRate  = new BigDecimal("0.075"); // 7.5%
//
//            BigDecimal vatAmount = netBalance.multiply(vatRate).setScale(2, RoundingMode.HALF_UP);
//
//            // Amount including VAT
//            BigDecimal totalWithVat = netBalance.add(vatAmount);
//
//            // Cost per unit (assuming tariffRate is numeric, e.g., "225.6")
//            BigDecimal tariffRate = new BigDecimal(meter.get(0).getTariffRate());
//            BigDecimal unit = netBalance.divide(tariffRate, 2, RoundingMode.HALF_UP);
//
//            // Cost of each unit (VAT-inclusive cost per unit)
//            BigDecimal costOfUnit = totalWithVat.divide(unit, 2, RoundingMode.HALF_UP);
//
//            transaction.setUnitCost(costOfUnit);
//            transaction.setUnit(unit);
//            transaction.setVatAmount(vatAmount);
//            transaction.setMeterId(meter.get(0).getMeterId());
//            transaction.setTariffId(meter.get(0).getTariffId());
//            transaction.setToken("1130987906543214590007");
//
//
//            int resp = vendMapper.createCreditToken(transaction);
//            if(resp == 0){
//                throw new GlobalExceptionHandler.NotFoundException("Credit token failed to create");
//            }
//
//            Transaction vend = vendMapper.getCreditTokenTransaction(transaction.getTransactionId());
//
////            AuditLog auditLog = buildAuditLog(um, "Credit token created","Vend", vend, metadata);
////            auditRepository.save(auditLog);
//
//            return ResponseMap.response(status.getSuccessCode(),  "Credit token created successfully", vend);
//        } catch (Exception exception) {
//
//            genericHandler.logIncidentReport("Creating credit token service failed");
//            genericHandler.logAndSaveException(exception, "creating credit token");
//            throw exception;
//        }
//    }
//
//    @Transactional
//    @Override
//    public Map<String, Object> calculateCreditToken(CreditToken creditToken) {
//        try {
//
//            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
//            UserModel um = handleUserValidation();
//
//            List<MeterView> meter = vendMapper.getMeterInfo(creditToken.getMeterNumber(), creditToken.getAccountNumber());
//
//            // --- BALANCE CALCULATION ---
//            BigDecimal totalDebitBalance = meter.stream()
//                    .filter(m -> "debit".equalsIgnoreCase(m.getAdjustmentType()))
//                    .map(MeterView::getBalanceAfterAdjustment)
//                    .filter(Objects::nonNull)
//                    .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//            BigDecimal totalCreditBalance = meter.stream()
//                    .filter(m -> "credit".equalsIgnoreCase(m.getAdjustmentType()))
//                    .map(MeterView::getBalanceAfterAdjustment)
//                    .filter(Objects::nonNull)
//                    .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//            // formula: (credit + amountTendered) - debit
//            BigDecimal netBalance = totalCreditBalance
//                    .add(creditToken.getAmount())
//                    .subtract(totalDebitBalance)
//                    .setScale(2, RoundingMode.HALF_UP);
//
//            BigDecimal vatRate  = new BigDecimal("0.075"); // 7.5%
//
//            BigDecimal vatAmount = netBalance.multiply(vatRate).setScale(2, RoundingMode.HALF_UP);
//
//            // Amount including VAT
//            BigDecimal totalWithVat = netBalance.add(vatAmount);
//
//            // Cost per unit (assuming tariffRate is numeric, e.g., "225.6")
//            BigDecimal tariffRate = new BigDecimal(meter.get(0).getTariffRate());
//            BigDecimal unit = netBalance.divide(tariffRate, 2, RoundingMode.HALF_UP);
//
//            // Cost of each unit (VAT-inclusive cost per unit)
//            BigDecimal costOfUnit = totalWithVat.divide(unit, 2, RoundingMode.HALF_UP);
//
//            creditToken.setVat(vatRate);
//            creditToken.setCostOfUnit(costOfUnit);
//            creditToken.setUnit(unit);
//            creditToken.setVatAmount(vatAmount);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("data", creditToken);
//            response.put("transaction", meter);
//            response.put("totalAmountTendered", netBalance);
//            response.put("totalDebitBalance", totalDebitBalance);
//            response.put("totalCreditBalance", totalCreditBalance);
//
////            Transaction vend = vendMapper.getCreditTokenTransaction(creditToken.getId());
//
////            AuditLog auditLog = buildAuditLog(um, "Credit token calculated","Vend", vend, metadata);
////            auditRepository.save(auditLog);
//
//            return ResponseMap.response(status.getSuccessCode(),  "Credit token calculated successfully", response);
//        } catch (Exception exception) {
//            genericHandler.logIncidentReport("Calculating credit token service failed");
//            genericHandler.logAndSaveException(exception, "calculate credit token");
//            throw exception;
//        }
//    }

    @Override
    public Map<String, Object> createKctToken(KctToken kctToken) {
        return Map.of();
    }

    @Override
    public Map<String, Object> createClearTamperToken(ClearTamper clearTamper) {
        return Map.of();
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

    private AuditLog buildAuditLog(UserModel creator, String description, String type, Transaction vend, Map<String, String> metadata) {
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
        return log;
    }
}
