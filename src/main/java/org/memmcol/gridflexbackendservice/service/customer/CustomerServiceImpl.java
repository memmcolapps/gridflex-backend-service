package org.memmcol.gridflexbackendservice.service.customer;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

@Transactional
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
//            // check if customer exist
//            Customer isCustomer = customerMapper.findByAccountNo(request.getAccountNumber());
//            if (isCustomer != null){
//                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(customerName + " " + status.getExistDesc());
//            }

            // Generate unique customer ID using 13-digit timestamp
            String uniqueCustomerId = "C" + Instant.now().toEpochMilli();
            request.setCustomerId(uniqueCustomerId);

            request.setOrgId(um.getOrgId());
            // Insert into customer
            customerMapper.insertCustomer(request);

            UUID id = request.getId();
            System.out.println("id: " + id);

            Customer customer = customerMapper.findById(id, um.getOrgId());
            handleAddCache(customer);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription("Created Customer [" + customer.getEmail() + "]");
            auditNotificationDTO.setType("customer");
            auditNotificationDTO.setCreatedCustomer(customer);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(), customerName + " " + status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while creating customer [ACTION]: {}", exception.getMessage(), exception);
            exception.printStackTrace();
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
//            System.out.println("Updating Customer [" + request.getAccountNumber() + "]");
//            // check if customer exist
//            Customer isCustomer = customerMapper.findByAccountNo(request.getAccountNumber());
//            if (isCustomer == null){
//                throw new GlobalExceptionHandler.NotFoundException(customerName + " " + status.getNotFoundDesc());
//            }

            request.setOrgId(um.getOrgId());
            // Insert into customer
            customerMapper.updateCustomer(request);
//            UUID id = request.getId();
//            System.out.println("id: " + id);
            Customer customer = customerMapper.findById(request.getId(), um.getOrgId());

            handleAddCache(customer);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription("Updated Customer [" + customer.getEmail() + "]");
            auditNotificationDTO.setType("customer");
            auditNotificationDTO.setCreatedCustomer(customer);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(), customerName + " " + status.getUpdateDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while creating customer [ACTION]: {}", exception.getMessage(), exception);
            exception.printStackTrace();
            exceptionErrorLogs.setDescription("Error occurred while trying to creating customer");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> allCustomers(
            int page, int size, String firstname, String lastname, String meterNumber,
            String customerId, Boolean meterAssigned) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {

            UserModel um = handleUserValidation();

            if (!Boolean.TRUE.equals(um.getStatus())) {
                throw new LockedException("User is disable");
            }

            // Build a unique cache key
            StringBuilder cacheKeyBuilder = new StringBuilder("customers_"+um.getOrgId());
            if (firstname != null && !firstname.isEmpty()) cacheKeyBuilder.append("_firstname_").append(firstname);
            if (lastname != null && !lastname.isEmpty()) cacheKeyBuilder.append("_lastname_").append(lastname);
            if (meterNumber != null && !meterNumber.isEmpty()) cacheKeyBuilder.append("_meterNumber_").append(meterNumber);
            if (customerId != null && !customerId.isEmpty()) cacheKeyBuilder.append("_customerId_").append(customerId);
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

            List<Customer> customers = customerMapper.findAllCustomers(um.getOrgId());

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

            if (customerId != null && !customerId.isEmpty()) {
                userStream = userStream.filter(u -> u.getCustomerId() != null && u.getCustomerId().equalsIgnoreCase(customerId));
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
            exception.printStackTrace();
            exceptionErrorLogs.setDescription("Error occurred while filtering users");
            exceptionErrorLogs.setError_message(exception.getMessage());
            exceptionErrorLogs.setError(exception.toString());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> singleCustomer(UUID customerId) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {

            UserModel um = handleUserValidation();

            if (!Boolean.TRUE.equals(um.getStatus())) {
                throw new LockedException("User is disabled");
            }

            Object cachedUser = customerCache.get(customerId.toString()+"_"+um.getOrgId());

            if (cachedUser != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached " + customerName + " " + status.getDesc(), cachedUser);
            }
            // check if customer exist
            Customer isCustomer = customerMapper.findById(customerId, um.getOrgId());
            if (isCustomer == null){
                throw new GlobalExceptionHandler.NotFoundException(customerName + " " + status.getExistDesc());
            }

            handleAddCache(isCustomer);

            return ResponseMap.response(status.getSuccessCode(), customerName + " " + status.getRegDesc(), isCustomer);
        } catch (Exception exception) {
            log.error("Error occurred while creating customer [ACTION]: {}", exception.getMessage(), exception);
            exception.printStackTrace();
            exceptionErrorLogs.setDescription("Error occurred while trying to creating customer");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> changeState(UUID customerId, Boolean state, String reason) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            UserModel um = handleUserValidation();

            if (!Boolean.TRUE.equals(um.getStatus())) {
                throw new LockedException("User is disabled");
            }

            // check if customer exist
            Customer isCustomer = customerMapper.findById(customerId, um.getOrgId());
            if (isCustomer == null){
                throw new GlobalExceptionHandler.NotFoundException(customerName + " " + status.getExistDesc());
            }

            int isStatus = customerMapper.changeStatus(customerId, state, um.getOrgId());
            if (isStatus != 1) {
                throw new GlobalExceptionHandler.NotFoundException(customerName + " " + status.getUpdateFailureDesc());
            }


            String desc = state ? "Activated" : "Deactivated" + " User [" + isCustomer.getEmail() + "]";


//            Map<String, Object> reasonMap = new HashMap<>();
//            Map<String, Object> map = new HashMap<>();
//            map.put("desc", desc);
//            map.put("reason", reason);
//            reasonMap.put("status", map);

            Customer customer = customerMapper.findById(customerId, um.getOrgId());

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

    @Override
    public Map<String, Object> bulkUpload(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        List<Customer> customers;
        assert filename != null;
        if (filename.endsWith(".csv")) {
            customers = parseCSV(file);
        } else if (filename.endsWith(".xlsx")) {
            customers = parseExcel(file);
        } else {
            throw new IllegalArgumentException("Unsupported file type");
        }
        System.out.println("Uploading " + customers.size() + " customers");
        customers.forEach(System.out::println);



        List<String> failedRecords = new ArrayList<>();
        List<String> fullErrors = new ArrayList<>();
        int successCount = 0;

        for (Customer customer : customers) {
            try {

                customerMapper.insertCustomer(customer);
                successCount++;
            }
            catch (Exception exception) {
                // Build a full identifier string from unique keys
                String identifier = String.format("Acct#: %s | Email: %s | NIN: %s | Meter#: %s",
                        customer.getCustomerId(),
                        customer.getEmail(),
                        customer.getNin(),
                        customer.getMeterNumber());

                // Add to failed summary
                failedRecords.add(identifier + " - " + exception.getMessage());

                // Add full exception for audit
                fullErrors.add(identifier + " - " + exception);

                exception.printStackTrace();

                // Log in system
                log.error("Failed to upload customer [DATA]: {} | [ERROR]: {}", identifier, exception.getMessage(), exception);
//                // Log a meaningful identifier of the customer
//                String identifier = customer.getAccountNumber() != null
//                        ? customer.getAccountNumber()
//                        : customer.getFirstname() + " " + customer.getLastname();
//                failedRecords.add(identifier + " - " + exception.getMessage());
//
//                exception.printStackTrace(); // Optional: keep logs for debugging
//
//                e.add(exception.toString());
//                log.error("Error occurred while upload customer record [ACTION]: {}", exception.getMessage(), exception);
            }
        }
        if (!failedRecords.isEmpty()) {
            exceptionErrorLogs.setDescription("Error occurred while trying to upload customer record");
            exceptionErrorLogs.setError_message(String.join("; ", failedRecords));
            exceptionErrorLogs.setError(fullErrors.toString());
            exceptionAuditRepository.save(exceptionErrorLogs);
        }
        return ResponseMap.response(
                status.getSuccessCode(),
                successCount + " of " + customers.size() + " " + customerName + "s " + status.getRegDesc(),
                failedRecords.isEmpty() ? "" : "Some records failed to upload. See error logs.");


    }

    //        try {
//            for (Customer customer : customers) {
//                customerMapper.insertCustomer(customer);
//            }
//            return ResponseMap.response(status.getSuccessCode(), customers.size() + " " + customerName + "s " + status.getRegDesc(), "");
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException("Failed to insert customers: " + e.getMessage());
//        }
    private List<Customer> parseCSV(MultipartFile file) throws IOException {
        List<Customer> customers = new ArrayList<>();
        Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(reader);

        for (CSVRecord record : records) {
            customers.add(buildCustomer(UUID.fromString(record.get("orgId")), record.get("firstname"),
                    record.get("lastname"), record.get("customerId"), record.get("nin"),
                    record.get("phoneNumber"), record.get("email"), record.get("state"),
                    record.get("city"), record.get("houseNo"), record.get("streetName")));
        }

        return customers;
    }

    private List<Customer> parseExcel(MultipartFile file) throws IOException {
        List<Customer> customers = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        Iterator<Row> rowIterator = sheet.iterator();
        if (rowIterator.hasNext()) rowIterator.next(); // Skip header

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();

            customers.add(buildCustomer(
                    UUID.fromString(getCellValue(row.getCell(0))),
                    getCellValue(row.getCell(1)),
                    getCellValue(row.getCell(2)),
                    getCellValue(row.getCell(3)),
                    getCellValue(row.getCell(4)),
                    getCellValue(row.getCell(5)),
                    getCellValue(row.getCell(6)),
                    getCellValue(row.getCell(7)),
                    getCellValue(row.getCell(8)),
                    getCellValue(row.getCell(9)),
                    getCellValue(row.getCell(10))
            ));
        }

        workbook.close();
        return customers;
    }

    private Customer buildCustomer(UUID orgId, String firstname, String lastname,
                                   String customerId, String nin ,String phoneNumber, String email,
                                   String state,  String city, String houseNo, String streetName) {
        Customer c = new Customer();
        c.setOrgId(orgId);
        c.setFirstname(firstname);
        c.setLastname(lastname);
        c.setCustomerId(customerId);
        c.setNin(nin);
        c.setPhoneNumber(phoneNumber);
        c.setEmail(email);
        c.setState(state);
        c.setCity(city);
        c.setHouseNo(houseNo);
        c.setStreetName(streetName);
        return c;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> "";
        };
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
        customerCache.remove(customer.getId().toString()+"_"+customer.getOrgId());
        for (String key : auditCache.keySet()) {
            if (key.startsWith("grid_flex_audit_log_page_")) {
                auditCache.remove(key);
            }
        }
        for (String key : customerCache.keySet()) {
            if (key.startsWith("customers_"+customer.getOrgId())) {
                customerCache.remove(key);
            }
        }
        customerCache.put(customer.getId().toString()+"_"+customer.getOrgId(), customer);  // Cache updated or deleted entity
    }
}
