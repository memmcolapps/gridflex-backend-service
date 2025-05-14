package org.memmcol.gridflexbackendservice.service.customer;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.CustomerMapper;
import org.memmcol.gridflexbackendservice.mapper.UserMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.user.CustomUserPrincipal;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class CustomerServiceImpl implements CustomerService {


    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private ResponseProperties status;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private AuthMapper operatorMapper;

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;

    private String customerName = "Customer";

    private final IMap<String, Object> customerCache;

    private final IMap<String, Object> auditCache;

    public CustomerServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.customerCache = hazelcastInstance.getMap("customer-Cache");
        this.auditCache = hazelcastInstance.getMap("audit-Cache");
    }

    @Override
    public Map<String, Object> createCustomer(Customer request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            UserModel um = handleUserValidation();

            if (!Boolean.TRUE.equals(um.getStatus())) {
                throw new LockedException("User is disabled");
            }
            // check if customer exist
            Customer isCustomer = customerMapper.findByAccountNo(request.getAccountNumber());
            if (isCustomer != null){
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(customerName + " " + status.getExistDesc());
            }

            // Insert into customer
            customerMapper.insertCustomer(request);

            Customer customer = customerMapper.findByAccountNo(request.getAccountNumber());
            handleAddCache(customer);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription("Created Customer [" + customer.getEmail() + "]");
            auditNotificationDTO.setType("customer");
            auditNotificationDTO.setCreatedCustomer(customer);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(), customerName + " " + status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while creating customer [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to creating customer");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> updateCustomer(Customer request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            UserModel um = handleUserValidation();

            if (!Boolean.TRUE.equals(um.getStatus())) {
                throw new LockedException("User is disabled");
            }
            System.out.println("Updating Customer [" + request.getAccountNumber() + "]");
            // check if customer exist
            Customer isCustomer = customerMapper.findByAccountNo(request.getAccountNumber());
            if (isCustomer == null){
                throw new GlobalExceptionHandler.NotFoundException(customerName + " " + status.getNotFoundDesc());
            }

            // Insert into customer
            customerMapper.updateCustomer(request);

            Customer customer = customerMapper.findByAccountNo(request.getAccountNumber());

            handleAddCache(customer);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription("Updated Customer [" + customer.getEmail() + "]");
            auditNotificationDTO.setType("customer");
            auditNotificationDTO.setCreatedCustomer(customer);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(), customerName + " " + status.getUpdateDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while creating customer [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to creating customer");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> allCustomers(int page, int size, String firstname, String lastname, String meterNumber, String accountNumber, Boolean meterAssigned) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {

            UserModel um = handleUserValidation();

            if (!Boolean.TRUE.equals(um.getStatus())) {
                throw new LockedException("User is disable");
            }

            // Build a unique cache key
            StringBuilder cacheKeyBuilder = new StringBuilder("customers");
            if (firstname != null && !firstname.isEmpty()) cacheKeyBuilder.append("_firstname_").append(firstname);
            if (lastname != null && !lastname.isEmpty()) cacheKeyBuilder.append("_lastname_").append(lastname);
            if (meterNumber != null && !meterNumber.isEmpty()) cacheKeyBuilder.append("_meterNumber_").append(meterNumber);
            if (accountNumber != null && !accountNumber.isEmpty()) cacheKeyBuilder.append("_accountNumber_").append(accountNumber);
//            if (address != null && !address.isEmpty()) cacheKeyBuilder.append("_address_").append(address);
//            if (state != null && !state.isEmpty()) cacheKeyBuilder.append("_state_").append(state);
            if (meterAssigned != null) cacheKeyBuilder.append("_st_").append(meterAssigned);
            cacheKeyBuilder.append("_page_").append(page);
            cacheKeyBuilder.append("_size_").append(size);

            String cacheKey = cacheKeyBuilder.toString();

            // Return from cache if available
            Object cachedCustomer = customerCache.get(cacheKey);
            if (cachedCustomer != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached Customers " + status.getDesc(), cachedCustomer);
            }

            List<Customer> customers = customerMapper.findAllCustomers();

            // Apply filtering
            Stream<Customer> userStream = customers.stream();

            if (firstname != null && !firstname.isEmpty()) {
                userStream = userStream.filter(u -> u.getFirstname() != null && u.getFirstname().equalsIgnoreCase(firstname));
            }

            if (lastname != null && !lastname.isEmpty()) {
                userStream = userStream.filter(u -> u.getLastname() != null && u.getLastname().equalsIgnoreCase(lastname));
            }

            if (meterNumber != null && !meterNumber.isEmpty()) {
                userStream = userStream.filter(u -> u.getMeterNumber() != null && u.getMeterNumber().equalsIgnoreCase(meterNumber));
            }

            if (accountNumber != null && !accountNumber.isEmpty()) {
                userStream = userStream.filter(u -> u.getAccountNumber() != null && u.getAccountNumber().equalsIgnoreCase(accountNumber));
            }

            if (meterAssigned != null) {
                userStream = userStream.filter(u -> u.getMeterAssigned() != null);
            }


            List<Customer> filteredCustomers = userStream.toList();

            // Pagination logic
            int totalCustomers = filteredCustomers.size();
            List<Customer> paginatedCustomers;
            if (size == 0) {
                paginatedCustomers = filteredCustomers; // Return all users
            } else {
                int fromIndex = Math.min(page * size, totalCustomers);
                int toIndex = Math.min(fromIndex + size, totalCustomers);
                paginatedCustomers = filteredCustomers.subList(fromIndex, toIndex);
            }

            // Prepare response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedCustomers);
            response.put("totalData", totalCustomers);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", (int) Math.ceil((double) paginatedCustomers.size() / size));

            customerCache.put(cacheKey, response);

            return ResponseMap.response(status.getSuccessCode(), customerName + "s " + status.getDesc(), response);

        } catch (Exception exception) {
            log.error("Error filtering / fetching users: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while filtering users");
            exceptionErrorLogs.setError_message(exception.getMessage());
            exceptionErrorLogs.setError(exception.toString());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> singleCustomer(String accountNumber) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {

            UserModel um = handleUserValidation();

            if (!Boolean.TRUE.equals(um.getStatus())) {
                throw new LockedException("User is disabled");
            }

            Object cachedUser = customerCache.get(accountNumber);

            if (cachedUser != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached " + customerName + " " + status.getDesc(), cachedUser);
            }
            // check if customer exist
            Customer isCustomer = customerMapper.findByAccountNo(accountNumber);
            if (isCustomer == null){
                throw new GlobalExceptionHandler.NotFoundException(customerName + " " + status.getExistDesc());
            }

            handleAddCache(isCustomer);

            return ResponseMap.response(status.getSuccessCode(), customerName + " " + status.getRegDesc(), isCustomer);
        } catch (Exception exception) {
            log.error("Error occurred while creating customer [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to creating customer");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> changeState(String accountNumber, Boolean state, String reason) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            UserModel um = handleUserValidation();

            if (!Boolean.TRUE.equals(um.getStatus())) {
                throw new LockedException("User is disabled");
            }

            // check if customer exist
            Customer isCustomer = customerMapper.findByAccountNo(accountNumber);
            if (isCustomer == null){
                throw new GlobalExceptionHandler.NotFoundException(customerName + " " + status.getExistDesc());
            }

            int isStatus = customerMapper.changeStatus(accountNumber, state);
            if (isStatus != 1) {
                throw new GlobalExceptionHandler.NotFoundException(customerName + " " + status.getUpdateFailureDesc());
            }


            String desc = state ? "Activated" : "Deactivated" + " User [" + isCustomer.getEmail() + "]";


//            Map<String, Object> reasonMap = new HashMap<>();
//            Map<String, Object> map = new HashMap<>();
//            map.put("desc", desc);
//            map.put("reason", reason);
//            reasonMap.put("status", map);

            Customer customer = customerMapper.findByAccountNo(accountNumber);

            handleAddCache(customer);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setReason(reason);
            auditNotificationDTO.setType("customer");
            auditNotificationDTO.setCreatedCustomer(customer);
            auditRepository.save(auditNotificationDTO);
            return ResponseMap.response(status.getSuccessCode(), state ? " User Activated Successfully" : "User Deactivated Successfully", "");
        } catch (Exception exception) {
            log.error("Error occurred while changing user status [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetching user");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    UserModel handleUserValidation() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = "Unknown";

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal) {
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            username = principal.getUsername();  // or principal.getEmail() if you named it that way
        }

        UserModel isOperatorExist = operatorMapper.findAuthByUserEmail(username);

        return isOperatorExist;
    }

    private void handleAddCache(Customer customer) {
        customerCache.remove(customer.getAccountNumber());
        for (String key : auditCache.keySet()) {
            if (key.startsWith("grid_flex_audit_log_page_")) {
                auditCache.remove(key);
            }
        }
        for (String key : customerCache.keySet()) {
            if (key.startsWith("customers")) {
                customerCache.remove(key);
            }
        }
        customerCache.put(customer.getAccountNumber(), customer);  // Cache updated or deleted entity
    }
}
