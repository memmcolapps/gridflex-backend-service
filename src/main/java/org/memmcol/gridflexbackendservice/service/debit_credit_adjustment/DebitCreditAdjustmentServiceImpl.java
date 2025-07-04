package org.memmcol.gridflexbackendservice.service.debit_credit_adjustment;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditAdjust;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditPayment;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.MeterAndLiabilityCause;
import org.memmcol.gridflexbackendservice.model.debt_setting.LiabilityCause;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.user.CustomUserPrincipal;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.service.tariff.TariffServiceImpl;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.util.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.memmcol.gridflexbackendservice.util.GenericHandler.capitalizeFirstLetter;
import static org.memmcol.gridflexbackendservice.util.GenericHandler.getClientIp;
import static org.memmcol.gridflexbackendservice.util.handleValidUser.handleUserValidation;

@Transactional
@Service
public class DebitCreditAdjustmentServiceImpl implements DebitCreditAdjustmentService {
    private static final Logger log = LoggerFactory.getLogger(TariffServiceImpl.class);

    @Autowired
    private AuthMapper operatorMapper;

    @Autowired
    private DebitCreditAdjustmentMapper mapper;

    @Autowired
    private ResponseProperties status;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;

    @Autowired
    private HttpServletRequest httpServletRequest;

    private final IMap<String, Object> debitCreditCache;
//
    private final IMap<String, Object> auditCache;

    private String debit = "Debit Adjustment";

    private String credit = "Credit Adjustment";

    public DebitCreditAdjustmentServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.debitCreditCache = hazelcastInstance.getMap("debit-credit-cache");
        this.auditCache = hazelcastInstance.getMap("audit-Cache");
    }

    @Override
    public Map<String, Object> createDebitAdjustment(DebitCreditAdjust request) {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            int result;

            UserModel um = handleUserValidation();

            System.out.println("type: "+request.getType());

            if (request.getType() == null ||
                    (!request.getType().trim().equalsIgnoreCase("credit")
                            && !request.getType().trim().equalsIgnoreCase("debit"))) {
                throw new GlobalExceptionHandler.NotFoundException("Parameter type must be; type: 'credit' or 'debit'");
            }

            String statement = "adjustment newly created";

            String desc = Objects.equals(request.getType(), "debit") ? "Debit" + statement : "Credit" + statement;

            Meter meter = mapper.getMeterById(request.getMeterId());
            if (meter == null) {
                throw new GlobalExceptionHandler.NotFoundException("Meter not found");
            }

            LiabilityCause liabilityCause = mapper.getLiabilityCauseById(request.getLiabilityCauseId());
            if (liabilityCause == null) {
                throw new GlobalExceptionHandler.NotFoundException("Liability cause not found");
            }

            request.setOrgId(um.getOrgId());
            request.setStatus("unpaid");
            result = mapper.createDebitAdjustment(request);

            if(result == 0){
                throw new GlobalExceptionHandler.NotFoundException(debit + " " + status.getNotFoundDesc());
            }

            DebitCreditAdjust debitAdjustment = mapper.getDebitAdjustmentById(request.getId(), um.getOrgId());
            um.setPassword("");
            handleAddCache(debitAdjustment);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setType("debit-credit");
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setDebitCreditAdjust(debitAdjustment);
            auditRepository.save(auditNotificationDTO);

            if(request.getType().equalsIgnoreCase("credit")){
                return ResponseMap.response(status.getSuccessCode(), credit + " " + status.getRegDesc(), "");
            } else {
                return ResponseMap.response(status.getSuccessCode(), debit + " " + status.getRegDesc(), "");
            }

        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to create tariff");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> reconcileDebt(UUID debitCreditAdjustmentId, String amount) {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();

        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");

            UserModel um = handleUserValidation();

            DebitCreditAdjust debitCreditAdjust = mapper.getDebitAdjustmentById(debitCreditAdjustmentId, um.getOrgId());
            if (debitCreditAdjust == null) {
                throw new GlobalExceptionHandler.NotFoundException("Debit Adjustment not found");
            }


            // Convert strings to BigDecimal for precise monetary calculation
            BigDecimal currentBalance = debitCreditAdjust.getBalance();
            BigDecimal paymentAmount = new BigDecimal(amount);

            if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new GlobalExceptionHandler.NotFoundException("Payment amount must be greater than zero");
            }

            // Ensure we don't allow overpayment
            if (paymentAmount.compareTo(currentBalance) > 0) {
                throw new GlobalExceptionHandler.NotFoundException("Payment exceeds current balance");
            }

            // 1. Save payment record
            DebitCreditPayment payment = new DebitCreditPayment();
            payment.setCreditDebitAdjId(debitCreditAdjustmentId);
            payment.setCredit(paymentAmount);
            payment.setOrgId(um.getOrgId());
            mapper.insertDebtCreditPayment(payment);

            // 2. Update penalty balance
            BigDecimal newBalance = currentBalance.subtract(paymentAmount);
            String newStatus = newBalance.compareTo(BigDecimal.ZERO) == 0 ? "paid" : "partially_paid";

            // Persist the updated values
            mapper.updateReconciledDebt(debitCreditAdjustmentId, newBalance, newStatus);

            DebitCreditAdjust debitAdjustment = mapper.getDebitAdjustmentById(debitCreditAdjustmentId, um.getOrgId());
            String desc = capitalizeFirstLetter(debitAdjustment.getLiabilityCause().getName())+" debt reconcile "+newStatus;
            um.setPassword("");
            handleAddCache(debitAdjustment);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setType("debit-credit");
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setDebitCreditAdjust(debitAdjustment);
            auditRepository.save(auditNotificationDTO);
            // Optionally, you can log the payment in a separate table (e.g. payment log)
            return ResponseMap.response(status.getSuccessCode(), "Payment reconciliation successful", "");

        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to create band");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> getMeterAndLiabilityCause(String meterNumber, String accountNumber) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            Meter meter = null;
            UserModel um = handleUserValidation();

            if (meterNumber == null && accountNumber == null) {
                throw new GlobalExceptionHandler.NotFoundException("At least one of meterId, meterNumber, or accountNumber must be provided.");
            }

//            Object cachedUser = meterCache.get(meterId.toString()+"_"+um.getOrgId());
//
//            if (cachedUser != null) {
//                return ResponseMap.response(status.getSuccessCode(), "Cached Meter" + " " + status.getDesc(), cachedUser);
//            }

            if(meterNumber != null){
                meter = mapper.getMeterNumber(um.getOrgId(), meterNumber);
            }

            if(accountNumber != null){
                meter = mapper.getAccountNumber(um.getOrgId(), accountNumber);
            }

            List<LiabilityCause> liabilityCause = mapper.getLiabilityCause(um.getOrgId());

            if(liabilityCause == null){
                throw new GlobalExceptionHandler.NotFoundException("Liability cause not found");
            }

            MeterAndLiabilityCause meterAndLiabilityCause = new MeterAndLiabilityCause();
            meterAndLiabilityCause.setMeter(meter);
            meterAndLiabilityCause.setLiabilityCause(liabilityCause);


//            if(meterId != null){
//                meter = meterMapper.getMeter(um.getOrgId(), meterId);
//            }

//            handleAddCache(meter);

            return ResponseMap.response(status.getSuccessCode(),  "Meter " + status.getDesc(), meterAndLiabilityCause);
        } catch (Exception exception) {
            log.error("Error occurred while fetching feeder lines [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetch transformer");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> getDebitAdjustments(
            int page, int size, String customerId, String accountNumber,
            String customerName, String meterNumber, BigDecimal balance, String type) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            UserModel um = handleUserValidation();

            // Build a unique cache key
            StringBuilder cacheKeyBuilder = new StringBuilder("debitCreditAdjustments_"+um.getOrgId());
            if (customerId != null && !customerId.isEmpty()) cacheKeyBuilder.append("_customerId_").append(customerId);
            if (accountNumber != null && !accountNumber.isEmpty()) cacheKeyBuilder.append("_accountNumber_").append(accountNumber);
            if (customerName != null && !customerName.isEmpty()) cacheKeyBuilder.append("_customerName_").append(customerName);
            if (meterNumber != null && !meterNumber.isEmpty()) cacheKeyBuilder.append("_meterNumber_").append(meterNumber);
            if (type != null && !type.isEmpty()) cacheKeyBuilder.append("_type_").append(type);
            if (balance != null) cacheKeyBuilder.append("_balance_").append(balance);
            cacheKeyBuilder.append("_page_").append(page);
            cacheKeyBuilder.append("_size_").append(size);

            String cacheKey = cacheKeyBuilder.toString();

            // Return from cache if available
            Object cachedDebitCreditAdjustment = debitCreditCache.get(cacheKey);
            if (cachedDebitCreditAdjustment != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached tariffs " + status.getDesc(), cachedDebitCreditAdjustment);
            }

            List<DebitCreditAdjust> allDebitCreditAdjustment;
            // Ideally, this should be a dynamic query in the mapper layer

            allDebitCreditAdjustment = mapper.GetDebitCreditAdjustment(um.getOrgId(), type);

            List<DebitCreditAdjust> filteredDebitCreditAdjustment = allDebitCreditAdjustment.stream()
                    .filter(t -> customerId == null || customerId.isEmpty() ||
                            t.getMeter().stream().anyMatch(m -> m.getCustomerId().equalsIgnoreCase(customerId)))
                    .filter(t -> accountNumber == null || accountNumber.isEmpty() ||
                            t.getMeter().stream().anyMatch(m -> m.getAccountNumber().equalsIgnoreCase(accountNumber)))
                    .filter(t -> customerName == null || customerName.isEmpty() ||
                            t.getMeter().stream().anyMatch(m -> m.getCustomer().getFirstname().equalsIgnoreCase(customerName)))
                    .filter(t -> meterNumber == null || meterNumber.isEmpty() ||
                            t.getMeter().stream().anyMatch(m -> m.getMeterNumber().equalsIgnoreCase(meterNumber)))
                    .filter(t -> balance == null || t.getBalance().equals(balance))
                    .collect(Collectors.toList());


            // Pagination logic
            int totalDebitCreditAdjustment = filteredDebitCreditAdjustment.size();
            List<DebitCreditAdjust> paginatedDebitCreditAdjustment;
            if (size == 0) {
                paginatedDebitCreditAdjustment = filteredDebitCreditAdjustment; // Return all users
            } else {
                int fromIndex = Math.min(page * size, totalDebitCreditAdjustment);
                int toIndex = Math.min(fromIndex + size, totalDebitCreditAdjustment);
                paginatedDebitCreditAdjustment = filteredDebitCreditAdjustment.subList(fromIndex, toIndex);
            }

            // Prepare response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedDebitCreditAdjustment);
            response.put("totalData", totalDebitCreditAdjustment);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", (int) Math.ceil((double) paginatedDebitCreditAdjustment.size() / size));

            debitCreditCache.put(cacheKey, response);
            assert type != null;
            if(type.equalsIgnoreCase("credit")){
                return ResponseMap.response(status.getSuccessCode(),  credit + " "+status.getDesc(), response);
            } else {
                return ResponseMap.response(status.getSuccessCode(),  debit + " "+status.getDesc(), response);
            }


        } catch (Exception exception) {
            log.error("Error occurred while filtering tariffs: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to filter tariffs");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> getDebitAdjustment(UUID meterId, String type) {
        try {
            UserModel um = handleUserValidation();
            Object cachedMeterId = null;

//            if(meterId != null) {
//                cachedMeterId = debitCreditCache.get(meterId.toString());
//            }
//
//            if (cachedMeterId != null) {
//                return ResponseMap.response(status.getSuccessCode(), "Cached " + debit + " " + status.getDesc(), cachedMeterId);
//            }
            List<DebitCreditAdjust> result = mapper.getDebitAdjustmentByMeterId(meterId, um.getOrgId(), type);

            if(result == null) {
                throw new GlobalExceptionHandler.NotFoundException(debit + " " + status.getNotFoundDesc());
            }

//            handleAddCache(result);

            return ResponseMap.response(status.getSuccessCode(), debit + " " + status.getDesc(), result);
        } catch (Exception exception) {
            ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to create band");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    private void handleAddCache(DebitCreditAdjust debitCreditAdjustment) {
        debitCreditCache.remove(debitCreditAdjustment.getId().toString()+"_"+debitCreditAdjustment.getOrgId());
        for (String key : auditCache.keySet()) {
            if (key.startsWith("grid_flex_audit_log_page_")) {
                auditCache.remove(key);
            }
        }
        for (String key : debitCreditCache.keySet()) {
            if (key.startsWith("debitCreditAdjustments_"+debitCreditAdjustment.getOrgId())) {
                debitCreditCache.remove(key);
            }
        }
        debitCreditCache.put(debitCreditAdjustment.getId().toString()+"_"+debitCreditAdjustment.getOrgId(), debitCreditAdjustment);  // Cache updated or deleted entity
    }

}
