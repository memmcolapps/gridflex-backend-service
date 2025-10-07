package org.memmcol.gridflexbackendservice.service.vend;

import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.memmcol.gridflexbackendservice.mapper.VendMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.memmcol.gridflexbackendservice.model.user.Group;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.model.vend.*;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Override
    public Map<String, Object> createCreditToken(CreditToken creditToken) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel um = handleUserValidation();

            int resp = vendMapper.createCreditToken(creditToken);
            if(resp == 0){
                throw new GlobalExceptionHandler.NotFoundException("Credit token failed to create");
            }

            Transaction vend = vendMapper.getCreditTokenTransaction(creditToken.getId());

            AuditLog auditLog = buildAuditLog(um, "Credit token created","Vend", vend, metadata);
            auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(),  "Credit token created successfully", "");
        } catch (Exception exception) {

            genericHandler.logIncidentReport("Creating credit token service failed");
            genericHandler.logAndSaveException(exception, "creating credit token");
            throw exception;
        }
    }

    @Override
    public Map<String, Object> calculateCreditToken(CreditToken creditToken) {
        try {

            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel um = handleUserValidation();

            List<Transaction> transaction = vendMapper.getMinTransaction(creditToken.getMeterNumber(), creditToken.getAccountNumber());

            BigDecimal vatRate  = new BigDecimal("0.075"); // 7.5%

            BigDecimal vatAmount = creditToken.getAmount().multiply(vatRate).setScale(2, RoundingMode.HALF_UP);

            // Amount including VAT
            BigDecimal totalWithVat = creditToken.getAmount().add(vatAmount);

            // Cost per unit (assuming tariffRate is numeric, e.g., "225.6")
            BigDecimal tariffRate = new BigDecimal(transaction.get(0).getTariffRate());
            BigDecimal unit = creditToken.getAmount().divide(tariffRate, 2, RoundingMode.HALF_UP);

            // Cost of each unit (VAT-inclusive cost per unit)
            BigDecimal costOfUnit = totalWithVat.divide(unit, 2, RoundingMode.HALF_UP);

            // --- BALANCE CALCULATION ---
            BigDecimal totalDebitBalance = transaction.stream()
                    .filter(t -> "debit".equalsIgnoreCase(t.getAdjustmentType()))
                    .map(Transaction::getBalanceAfterAdjustment)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalCreditBalance = transaction.stream()
                    .filter(t -> "credit".equalsIgnoreCase(t.getAdjustmentType()))
                    .map(Transaction::getBalanceAfterAdjustment)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // formula: (credit + amountTendered) - debit
            BigDecimal netBalance = totalCreditBalance
                    .add(creditToken.getAmount())
                    .subtract(totalDebitBalance)
                    .setScale(2, RoundingMode.HALF_UP);

            creditToken.setVat(vatRate );
            creditToken.setCostOfUnit(costOfUnit);
            creditToken.setUnit(unit);
            creditToken.setVatAmount(vatAmount);

            Map<String, Object> response = new HashMap<>();
            response.put("data", creditToken);
//            response.put("transaction", transaction);
            response.put("totalAmountTendered", netBalance);
            response.put("totalDebitBalance", totalDebitBalance);
            response.put("totalCreditBalance", totalCreditBalance);

//            Transaction vend = vendMapper.getCreditTokenTransaction(creditToken.getId());
//
//            AuditLog auditLog = buildAuditLog(um, "Credit token created","Vend", vend, metadata);
//            auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(),  "Credit token calculated successfully", response);
        } catch (Exception exception) {
            genericHandler.logIncidentReport("Calculating credit token service failed");
            genericHandler.logAndSaveException(exception, "calculate credit token");
            throw exception;
        }
    }

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
