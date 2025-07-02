package org.memmcol.gridflexbackendservice.service.debit_credit_adjustment;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditAdjustment;
import org.memmcol.gridflexbackendservice.model.debt_setting.LiabilityCause;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
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
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public Map<String, Object> createDebitAdjustment(DebitCreditAdjustment request) {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            String ipAddress = httpServletRequest.getRemoteAddr();
            String userAgent = httpServletRequest.getHeader("User-Agent");
            int result;
            String desc = "Debit adjustment newly created";
            UserModel um = handleUserValidation();

            if (!request.getType().equalsIgnoreCase("credit") || !request.getType().equalsIgnoreCase("debit") ) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException("Parameter type must be 'credit' or 'debit'");
            }

            Meter meter = mapper.getMeterByAccountNumber(request.getAccountNumber());
            if (meter == null) {
                throw new GlobalExceptionHandler.NotFoundException("Meter not found");
            }

            LiabilityCause liabilityCause = mapper.getLiabilityCauseById(request.getLiabilityCauseId());
            if (liabilityCause == null) {
                throw new GlobalExceptionHandler.NotFoundException("Liability cause not found");
            }

            request.setOrgId(um.getOrgId());
            result = mapper.createDebitAdjustment(request);

            if(result == 0){
                throw new GlobalExceptionHandler.NotFoundException(debit + " " + status.getNotFoundDesc());
            }

            DebitCreditAdjustment debitAdjustment = mapper.getDebitAdjustmentById(request.getId(), um.getOrgId());
            um.setPassword("");
            handleAddCache(debitAdjustment);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setType("debit-credit");
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setDebitAdjustment(debitAdjustment);
            auditRepository.save(auditNotificationDTO);
            return ResponseMap.response(status.getSuccessCode(), debit + " " + status.getRegDesc(), "");
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
        try {
            UserModel um = handleUserValidation();

            return Map.of();

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

            List<DebitCreditAdjustment> allDebitCreditAdjustment;
            // Ideally, this should be a dynamic query in the mapper layer

            allDebitCreditAdjustment = mapper.GetDebitCreditAdjustment(um.getOrgId(), type);

            List<DebitCreditAdjustment> filteredDebitCreditAdjustment = allDebitCreditAdjustment.stream()
                    .filter(t -> customerId == null || customerId.isEmpty() ||
                            t.getMeter().stream().anyMatch(m -> m.getCustomerId().equalsIgnoreCase(customerId)))
                    .filter(t -> accountNumber == null || accountNumber.isEmpty() ||
                            t.getAccountNumber().equalsIgnoreCase(accountNumber))
                    .filter(t -> customerName == null || customerName.isEmpty() ||
                            t.getMeter().stream().anyMatch(m -> m.getCustomer().getFirstname().equalsIgnoreCase(customerName)))
                    .filter(t -> meterNumber == null || meterNumber.isEmpty() ||
                            t.getMeter().stream().anyMatch(m -> m.getMeterNumber().equalsIgnoreCase(meterNumber)))
                    .filter(t -> balance == null || t.getBalance().equals(balance))
                    .collect(Collectors.toList());


            // Pagination logic
            int totalDebitCreditAdjustment = filteredDebitCreditAdjustment.size();
            List<DebitCreditAdjustment> paginatedDebitCreditAdjustment;
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
            if(type.equalsIgnoreCase("credit")){
                return ResponseMap.response(status.getSuccessCode(),  debit + " "+status.getDesc(), response);
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
    public Map<String, Object> getDebitAdjustment(String accountNumber) {
        try {
            UserModel um = handleUserValidation();
            Object cachedAccountNumber = null;
            if(accountNumber != null) {
                cachedAccountNumber = debitCreditCache.get(accountNumber);
            }

            if (cachedAccountNumber != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached " + debit + " " + status.getDesc(), cachedAccountNumber);
            }
            List<DebitCreditAdjustment> result = mapper.getDebitAdjustmentByAccountNumber(accountNumber, um.getOrgId());

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

    private void handleAddCache(DebitCreditAdjustment debitCreditAdjustment) {
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

    UserModel handleUserValidation() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = "Unknown";

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal) {
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            username = principal.getUsername();  // or principal.getEmail() if you named it that way
        }

        UserModel isOperatorExist = operatorMapper.findAuthByUserEmail(username);

        if (!Boolean.TRUE.equals(isOperatorExist.getStatus())) {
            throw new LockedException("User is disable");
        }

        return isOperatorExist;
    }

    public static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }
}
