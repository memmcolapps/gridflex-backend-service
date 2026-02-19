package org.memmcol.gridflexbackendservice.service.debit_credit_adjustment;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditAdjust;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditPayment;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.MeterAndLiabilityCause;
import org.memmcol.gridflexbackendservice.model.debt_setting.LiabilityCause;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.service.audit.SafeAuditService;
import org.memmcol.gridflexbackendservice.service.tariff.TariffServiceImpl;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.memmcol.gridflexbackendservice.components.GenericHandler.capitalizeFirstLetter;
import static org.memmcol.gridflexbackendservice.components.HandleValidUser.handleUserValidation;

@Service
public class DebitCreditAdjustmentServiceImpl implements DebitCreditAdjustmentService {
    private static final Logger log = LoggerFactory.getLogger(TariffServiceImpl.class);

    @Autowired
    private GenericHandler genericHandler;

    @Autowired
    private DebitCreditAdjustmentMapper mapper;

    @Autowired
    private ResponseProperties status;

//    @Autowired
//    private AuditRepository auditRepository;

    @Autowired
    private SafeAuditService safeAuditService;

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
        this.debitCreditCache = hazelcastInstance.getMap("debitCreditCache");
        this.auditCache = hazelcastInstance.getMap("auditCache");
    }

    @Transactional
    @Override
    public Map<String, Object> createDebitAdjustment(DebitCreditAdjust request) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            int result;

            UserModel um = handleUserValidation();

            if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0){
                throw new GlobalExceptionHandler.NotFoundException("Amount must be greater than zero");
            }

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

            LiabilityCause liabilityCause = mapper.getLiabilityCauseById(request.getLiabilityCauseId(), um.getOrgId());
            if (liabilityCause == null) {
                throw new GlobalExceptionHandler.NotFoundException("Liability cause not found");
            }

//            DebitCreditAdjust liabilityResult = mapper.getDebitAdjustmentByMeterIdAndLiabilityCause(request.getMeterId(),um.getOrgId(),
//                    request.getLiabilityCauseId(),request.getType());
//            if (liabilityResult != null) {
//
//                int rows = mapper.addCreditDebitAdjustment(liabilityResult.getId(), request.getAmount(), request.getAmount());
//                if (rows == 0) {
//                    throw new GlobalExceptionHandler.NotFoundException(request.getType()+" adjustment failed");
//                }
//            } else {
            request.setOrgId(um.getOrgId());
            request.setStatus("UNPAID");
            result = mapper.createDebitAdjustment(request);

            if(result == 0){
                throw new GlobalExceptionHandler.NotFoundException(debit + " " + status.getRegFailureDesc());
            }


            // 1. Save payment record
            DebitCreditPayment payment = new DebitCreditPayment();
            payment.setCreditDebitAdjId(request.getId());
            payment.setCredit(BigDecimal.ZERO);
            payment.setBalance(request.getAmount());
            payment.setDebt(request.getAmount());
            payment.setOrgId(um.getOrgId());

            int res = mapper.insertDebtCreditPayment(payment);
            if(res == 0){
                throw new GlobalExceptionHandler.NotFoundException("Debt Reconciliation" + status.getRegFailureDesc());
            }
//            }

            DebitCreditAdjust debitAdjustment = mapper.getDebitAdjustmentById(request.getId(), um.getOrgId());
            um.setPassword("");
//            handleAddCache(debitAdjustment);
            AuditLog auditLog = buildAuditLog(um, desc, "debit-credit", debitAdjustment, metadata);
            safeAuditService.saveAudit(auditLog);

            if(request.getType().equalsIgnoreCase("credit")){
                return ResponseMap.response(status.getSuccessCode(), credit + " " + status.getRegDesc(), "");
            } else {
                return ResponseMap.response(status.getSuccessCode(), debit + " " + status.getRegDesc(), "");
            }

        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Creating debit adjustment service failed");
            genericHandler.logAndSaveException(exception, "creating debit-credit adjustment");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> reconcileDebt(UUID debitCreditAdjustmentId, String amount) {

        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

            UserModel um = handleUserValidation();

            DebitCreditAdjust debitCreditAdjust = mapper.getDebitAdjustmentById(debitCreditAdjustmentId, um.getOrgId());
            if (debitCreditAdjust == null) {
                throw new GlobalExceptionHandler.NotFoundException("Debit Adjustment not found");
            }

            if (debitCreditAdjust.getStatus().equalsIgnoreCase("PAID")) {
                throw new GlobalExceptionHandler.NotFoundException("Customer have no debt");
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

            BigDecimal returnBalance = debitCreditAdjust.getStatus().equalsIgnoreCase( "UNPAID")
                    ? currentBalance.subtract(paymentAmount) : currentBalance;

            DebitCreditPayment debitCreditPayment = mapper.getPaymentById(debitCreditAdjustmentId, um.getOrgId());

            if (debitCreditPayment == null) {
                throw new GlobalExceptionHandler.NotFoundException("Debit adjustment payment not found");
            }

            // 1. Save payment record
            DebitCreditPayment payment = new DebitCreditPayment();
            payment.setParentId(debitCreditPayment.getId());
            payment.setCreditDebitAdjId(debitCreditAdjustmentId);
            payment.setCredit(paymentAmount);
            payment.setBalance(returnBalance);
            payment.setDebt(BigDecimal.ZERO);
            payment.setOrgId(um.getOrgId());

            int res = mapper.insertDebtCreditPayment(payment);
            if(res == 0){
                throw new GlobalExceptionHandler.NotFoundException("Debt Reconciliation" + status.getRegFailureDesc());
            }
            // 2. Update penalty balance
            BigDecimal newBalance = currentBalance.subtract(paymentAmount);
            String newStatus = newBalance.compareTo(BigDecimal.ZERO) == 0 ? "PAID" : "PARTIALLY_PAID";

            // Persist the updated values
            int resp = mapper.updateReconciledDebt(debitCreditAdjustmentId, newBalance, newStatus);
            if(resp == 0){
                throw new GlobalExceptionHandler.NotFoundException("Balance" + status.getUpdateFailureDesc());
            }

            DebitCreditAdjust debitAdjustment = mapper.getDebitAdjustmentById(debitCreditAdjustmentId, um.getOrgId());
            String desc = capitalizeFirstLetter(debitAdjustment.getLiabilityCause().getName())+" debt reconcile "+newStatus;
            um.setPassword("");
//            handleAddCache(debitAdjustment);
            AuditLog auditLog = buildAuditLog(um, desc, "debit-credit", debitAdjustment, metadata);
            safeAuditService.saveAudit(auditLog);
            return ResponseMap.response(status.getSuccessCode(), "Payment reconciliation successful", "");

        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Reconciliation dept service failed");
            genericHandler.logAndSaveException(exception, "reconcile dept");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getMeterAndLiabilityCause(String meterNumber, String accountNumber) {
        try {
            Meter meter = null;
            UserModel um = handleUserValidation();

            if (meterNumber == null && accountNumber == null) {
                throw new GlobalExceptionHandler.NotFoundException("At least one of meterNumber or accountNumber must be provided.");
            }

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

            return ResponseMap.response(status.getSuccessCode(),  "Meter " + status.getDesc(), meterAndLiabilityCause);
        } catch (Exception exception) {
            log.error("Error occurred while fetching feeder lines [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Fetching meter & liability causes service failed");
            genericHandler.logAndSaveException(exception, "fetch meter liability cause");
            throw exception;
        }
    }

//    @Override
//    public Map<String, Object> getDebitAdjustmentPaymentHistory(
//            UUID meterId, UUID liabilityCauseId, String type) {
//        try {
//
//            UserModel um = handleUserValidation();
//
//            List<DebitCreditPayment> response;
//            // Ideally, this should be a dynamic query in the mapper layer
//            if(type.equalsIgnoreCase("debit") || type.equalsIgnoreCase("credit")) {
//                response = mapper.FetchDebitCreditPaymentHistory(meterId, liabilityCauseId, type, um.getOrgId());
//            } else {
//                throw new GlobalExceptionHandler.NotFoundException("Type parameter (" + type + ") not supported");
//            }
//
//            Map<String, Object> responseData = new HashMap<>();
//
//
//            if(type.equalsIgnoreCase("credit")){
//                return ResponseMap.response(status.getSuccessCode(),  credit + " "+status.getDesc(), response);
//            } else {
//                return ResponseMap.response(status.getSuccessCode(),  debit + " "+status.getDesc(), response);
//            }
//
//        } catch (Exception exception) {
//            log.error("Error occurred while filtering tariffs: {}", exception.getMessage().trim(), exception);
//            genericHandler.logIncidentReport("Fetching debit adjustments service failed");
//            genericHandler.logAndSaveException(exception, "fetch debit adjustments");
//            throw exception;
//        }
//    }

    @Override
    public Map<String, Object> getDebitAdjustmentPaymentHistory(
            UUID meterId, UUID liabilityCauseId, String type) {

        try {

            UserModel um = handleUserValidation();

            if (!type.equalsIgnoreCase("debit") &&
                    !type.equalsIgnoreCase("credit")) {

                throw new GlobalExceptionHandler.NotFoundException(
                        "Type parameter (" + type + ") not supported"
                );
            }

            List<DebitCreditPayment> response =
                    mapper.FetchDebitCreditPaymentHistory(
                            meterId,
                            liabilityCauseId,
                            type,
                            um.getOrgId()
                    );

            // 🔥 Group root + children correctly
            Map<UUID, List<DebitCreditPayment>> grouped = new LinkedHashMap<>();

            for (DebitCreditPayment payment : response) {

                UUID rootId;

                // Case 1: Root record (parentId is null OR equals its own id)
                if (payment.getParentId() == null ||
                        payment.getParentId().equals(payment.getId())) {

                    rootId = payment.getId();
                }
                // Case 2: Child record
                else {
                    rootId = payment.getParentId();
                }

                grouped
                        .computeIfAbsent(rootId, k -> new ArrayList<>())
                        .add(payment);
            }

            List<List<DebitCreditPayment>> groupedList =
                    new ArrayList<>(grouped.values());

            return ResponseMap.response(
                    status.getSuccessCode(),
                    type + " " + status.getDesc(),
                    groupedList
            );

        } catch (Exception exception) {

            log.error("Error occurred while fetching debit/credit payment history: {}",
                    exception.getMessage().trim(), exception);

            genericHandler.logIncidentReport("Fetching debit/credit payment history failed");
            genericHandler.logAndSaveException(exception, "fetch debit/credit payment history");

            throw exception;
        }
    }


    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getDebitAdjustments(
            int page, int size, String customerId, String accountNumber,
            String customerName, String meterNumber, BigDecimal balance, String type) {
        try {
            String db;
            if("credit".equals(type) ){
                db = credit;
            } else if("debit".equalsIgnoreCase(type)){
                db = debit;
            } else {
                throw new GlobalExceptionHandler.NotFoundException("Type parameter not found, use credit or debit instead");
            }
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
//            Object cachedDebitCreditAdjustment = debitCreditCache.get(cacheKey);
//            if (cachedDebitCreditAdjustment != null) {
//                return ResponseMap.response(status.getSuccessCode(), "Cached " + db + status.getDesc(), cachedDebitCreditAdjustment);
//            }

            List<Meter> allDebitCreditAdjustment;
            // Ideally, this should be a dynamic query in the mapper layer
            if(type.equalsIgnoreCase("debit")) {
                allDebitCreditAdjustment = mapper.GetDebitAdjustment(um.getOrgId(), page,size);
            } else if (type.equalsIgnoreCase("credit")){
                allDebitCreditAdjustment = mapper.GetCreditAdjustment(um.getOrgId(), page,size);
            } else {
                throw new GlobalExceptionHandler.NotFoundException("Type parameter (" + type + ") not supported");
            }


            List<Meter> filteredDebitCreditAdjustment =
                    allDebitCreditAdjustment.stream().toList();
//                            .filter(u ->
//                                    // customerId filter (mandatory)
//                                    u.getCustomerId() != null
//                                            && customerId != null
//                                            && u.getCustomerId().equalsIgnoreCase(customerId)
//                            )
//                            .filter(u ->
//                                    // accountNumber filter (optional)
//                                    accountNumber == null || accountNumber.isBlank()
//                                            || (u.getAccountNumber() != null
//                                            && u.getAccountNumber().equalsIgnoreCase(accountNumber))
//                            )
//                            .filter(u ->
//                                    // accountNumber filter (optional)
//                                    customerName == null || customerName.isBlank()
//                                            || (u.getCustomer().getFirstname() != null
//                                            && u.getCustomer().getFirstname().equalsIgnoreCase(customerName))
//                            )
//                            .collect(Collectors.toList());



            // Pagination logic
            int totalDebitCreditAdjustment = filteredDebitCreditAdjustment.size();
            List<Meter> paginatedDebitCreditAdjustment;

            if (size <= 0) {
                paginatedDebitCreditAdjustment = filteredDebitCreditAdjustment; // Return all users
                page = 0;
            } else {
                int fromIndex = Math.min(page * size, totalDebitCreditAdjustment);
                int toIndex = Math.min(fromIndex + size, totalDebitCreditAdjustment);
                paginatedDebitCreditAdjustment = filteredDebitCreditAdjustment.subList(fromIndex, toIndex);
            }

            int totalPages = size <= 0 ? 1 : (int) Math.ceil((double) totalDebitCreditAdjustment / size);

            // Prepare response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedDebitCreditAdjustment);
            response.put("totalData", totalDebitCreditAdjustment);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", totalPages);

//            debitCreditCache.put(cacheKey, response);
            assert type != null;
            if(type.equalsIgnoreCase("credit")){
                return ResponseMap.response(status.getSuccessCode(),  credit + " "+status.getDesc(), response);
            } else {
                return ResponseMap.response(status.getSuccessCode(),  debit + " "+status.getDesc(), response);
            }

        } catch (Exception exception) {
            log.error("Error occurred while filtering tariffs: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Fetching debit adjustments service failed");
            genericHandler.logAndSaveException(exception, "fetch debit adjustments");
            throw exception;
        }
    }


//    @Transactional(readOnly = true)
//    @Override
//    public Map<String, Object> getDebitAdjustments(
//            int page, int size, String customerId, String accountNumber,
//            String customerName, String meterNumber, BigDecimal balance, String type) {
//        try {
//            String db;
//            if("credit".equals(type) ){
//                db = credit;
//            } else if("debit".equalsIgnoreCase(type)){
//                db = debit;
//            } else {
//                throw new GlobalExceptionHandler.NotFoundException("Type parameter not found, use credit or debit instead");
//            }
//            UserModel um = handleUserValidation();
//
//            // Build a unique cache key
//            StringBuilder cacheKeyBuilder = new StringBuilder("debitCreditAdjustments_"+um.getOrgId());
//            if (customerId != null && !customerId.isEmpty()) cacheKeyBuilder.append("_customerId_").append(customerId);
//            if (accountNumber != null && !accountNumber.isEmpty()) cacheKeyBuilder.append("_accountNumber_").append(accountNumber);
//            if (customerName != null && !customerName.isEmpty()) cacheKeyBuilder.append("_customerName_").append(customerName);
//            if (meterNumber != null && !meterNumber.isEmpty()) cacheKeyBuilder.append("_meterNumber_").append(meterNumber);
//            if (type != null && !type.isEmpty()) cacheKeyBuilder.append("_type_").append(type);
//            if (balance != null) cacheKeyBuilder.append("_balance_").append(balance);
//            cacheKeyBuilder.append("_page_").append(page);
//            cacheKeyBuilder.append("_size_").append(size);
//
//            String cacheKey = cacheKeyBuilder.toString();
//
//            // Return from cache if available
////            Object cachedDebitCreditAdjustment = debitCreditCache.get(cacheKey);
////            if (cachedDebitCreditAdjustment != null) {
////                return ResponseMap.response(status.getSuccessCode(), "Cached " + db + status.getDesc(), cachedDebitCreditAdjustment);
////            }
//
//            List<DebitCreditAdjust> allDebitCreditAdjustment;
//            // Ideally, this should be a dynamic query in the mapper layer
//
//            allDebitCreditAdjustment = mapper.GetDebitCreditAdjustment(um.getOrgId(), type, page,size);
//
//            List<DebitCreditAdjust> filteredDebitCreditAdjustment = allDebitCreditAdjustment.stream()
//                    .filter(t -> customerId == null || customerId.isEmpty() ||
//                            t.getMeter().stream().anyMatch(m -> m.getCustomerId().equalsIgnoreCase(customerId)))
//                    .filter(t -> accountNumber == null || accountNumber.isEmpty() ||
//                            t.getMeter().stream().anyMatch(m -> m.getAccountNumber().equalsIgnoreCase(accountNumber)))
//                    .filter(t -> customerName == null || customerName.isEmpty() ||
//                            t.getMeter().stream().anyMatch(m -> m.getCustomer().getFirstname().equalsIgnoreCase(customerName)))
//                    .filter(t -> meterNumber == null || meterNumber.isEmpty() ||
//                            t.getMeter().stream().anyMatch(m -> m.getMeterNumber().equalsIgnoreCase(meterNumber)))
//                    .filter(t -> balance == null || t.getBalance().equals(balance))
//                    .collect(Collectors.toList());
//
//
//            // Pagination logic
//            int totalDebitCreditAdjustment = filteredDebitCreditAdjustment.size();
//            List<DebitCreditAdjust> paginatedDebitCreditAdjustment;
//
//            if (size <= 0) {
//                paginatedDebitCreditAdjustment = filteredDebitCreditAdjustment; // Return all users
//                page = 0;
//            } else {
//                int fromIndex = Math.min(page * size, totalDebitCreditAdjustment);
//                int toIndex = Math.min(fromIndex + size, totalDebitCreditAdjustment);
//                paginatedDebitCreditAdjustment = filteredDebitCreditAdjustment.subList(fromIndex, toIndex);
//            }
//
//            int totalPages = size <= 0 ? 1 : (int) Math.ceil((double) totalDebitCreditAdjustment / size);
//
//            // Prepare response with pagination metadata
//            Map<String, Object> response = new HashMap<>();
//            response.put("data", paginatedDebitCreditAdjustment);
//            response.put("totalData", totalDebitCreditAdjustment);
//            response.put("page", page);
//            response.put("size", size);
//            response.put("totalPages", totalPages);
//
////            debitCreditCache.put(cacheKey, response);
//            assert type != null;
//            if(type.equalsIgnoreCase("credit")){
//                return ResponseMap.response(status.getSuccessCode(),  credit + " "+status.getDesc(), response);
//            } else {
//                return ResponseMap.response(status.getSuccessCode(),  debit + " "+status.getDesc(), response);
//            }
//
//        } catch (Exception exception) {
//            log.error("Error occurred while filtering tariffs: {}", exception.getMessage().trim(), exception);
//            genericHandler.logIncidentReport("Fetching debit adjustments service failed");
//            genericHandler.logAndSaveException(exception, "fetch debit adjustments");
//            throw exception;
//        }
//    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getDebitAdjustment(UUID meterId, String type) {
        try {
            UserModel um = handleUserValidation();

            List<DebitCreditAdjust> result = mapper.getDebitAdjustmentByMeterId(meterId, um.getOrgId(), type);

            if(result == null) {
                throw new GlobalExceptionHandler.NotFoundException(debit + " " + status.getNotFoundDesc());
            }

            return ResponseMap.response(status.getSuccessCode(), debit + " " + status.getDesc(), result);
        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Fetching debit adjustment service failed");
            genericHandler.logAndSaveException(exception, "fetch debit adjustment");
            throw exception;
        }
    }

    private AuditLog buildAuditLog(UserModel creator, String description, String type, Object createdEntity, Map<String, String> metadata) {
        AuditLog log = new AuditLog();
        log.setCreator(creator);
        log.setDescription(description);
        log.setType(type);
        log.setDebitCreditAdjust(createdEntity instanceof DebitCreditAdjust ? (DebitCreditAdjust) createdEntity : null);
        log.setIpAddress(metadata.get("ipAddress"));
        log.setUserAgent(metadata.get("userAgent"));
        log.setEndpoint(metadata.get("endpoint"));
        log.setHttpMethod(metadata.get("httpMethod"));
        return log;
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