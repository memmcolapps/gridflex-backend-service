package org.memmcol.gridflexbackendservice.service.customer;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.CustomerMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
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
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.memmcol.gridflexbackendservice.components.GenericHandler.capitalizeFirstLetter;
import static org.memmcol.gridflexbackendservice.components.handleValidUser.handleUserValidation;

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
    private GenericHandler genericHandler;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;

    private String customerName = "Customer";

    private final IMap<String, Object> customerCache;

    private final IMap<String, Object> auditCache;

    public CustomerServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.customerCache = hazelcastInstance.getMap("customerCache");
        this.auditCache = hazelcastInstance.getMap("auditCache");
    }

    @Transactional
    @Override
    public Map<String, Object> createCustomer(Customer request) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            String desc = "Customer newly created";
            UserModel um = handleUserValidation();

            Customer email = customerMapper.findByEmail(request.getEmail(),um.getOrgId());
            if (email != null && email.getEmail().equalsIgnoreCase(request.getEmail())) {
                throw new GlobalExceptionHandler.NotFoundException("Email already used by a Customer");
            }

//            String uniqueCustomerId = "C" + Instant.now().toEpochMilli();
            String uniqueCustomerId = "C" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            request.setCustomerId(uniqueCustomerId);

            request.setOrgId(um.getOrgId());
            request.setStatus("Inactive");

            capitalizeFirstLetter(request.getVat());

            // Insert into customer
            customerMapper.insertCustomer(request);

            UUID id = request.getId();

            Customer customer = customerMapper.findById(id, um.getOrgId());
//            handleAddCache(customer);
            AuditLog auditLog = buildAuditLog(um, desc, "", customerName, customer, metadata);
            auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(), customerName + " " + status.getRegDesc(), "");

        } catch (Exception exception) {
            log.error("Error occurred while creating customer [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Creating customer Service failed");
            genericHandler.logAndSaveException(exception, "creating customer");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> updateCustomer(Customer request) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

            UserModel um = handleUserValidation();

            Customer email = customerMapper.findByEmail(request.getEmail(),um.getOrgId());
            if (email != null && email.getEmail().equalsIgnoreCase(request.getEmail())) {
                throw new GlobalExceptionHandler.NotFoundException("Email ("+request.getEmail()+") "+ status.getExistDesc()+" for a Customer");
            }

            request.setOrgId(um.getOrgId());

            // Insert into customer
            customerMapper.updateCustomer(request);
            Customer customer = customerMapper.findById(request.getId(), um.getOrgId());

//            handleAddCache(customer);
            AuditLog auditLog = buildAuditLog(um, "Edited customer", "", customerName, customer, metadata);
            auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(), customerName + " " + status.getUpdateDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while creating customer [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Editing customer Service failed");
            genericHandler.logAndSaveException(exception, "updating customer");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> allCustomers(
            int page, int size, String firstname, String lastname,
            String accountNumber, String assignedStatus, String customerId) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {

            UserModel um = handleUserValidation();

            // Build a unique cache key
            StringBuilder cacheKeyBuilder = new StringBuilder("customers_"+um.getOrgId());
            if (firstname != null && !firstname.isEmpty()) cacheKeyBuilder.append("_firstname_").append(firstname);
            if (lastname != null && !lastname.isEmpty()) cacheKeyBuilder.append("_lastname_").append(lastname);
//            if (meterNumber != null && !meterNumber.isEmpty()) cacheKeyBuilder.append("_meterNumber_").append(meterNumber);
            if (customerId != null && !customerId.isEmpty()) cacheKeyBuilder.append("_customerId_").append(customerId);
            if (assignedStatus != null && !assignedStatus.isEmpty()) cacheKeyBuilder.append("_status_").append(assignedStatus);
//            if (address != null && !address.isEmpty()) cacheKeyBuilder.append("_address_").append(address);
//            if (state != null && !state.isEmpty()) cacheKeyBuilder.append("_state_").append(state);
//            if (meterAssigned != null) cacheKeyBuilder.append("_st_").append(meterAssigned);
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

            if (assignedStatus != null && !assignedStatus.isEmpty()) {
                userStream = userStream.filter(u -> u.getStatus() != null && u.getStatus().equalsIgnoreCase(lastname));
            }

//            if (meterNumber != null && !meterNumber.isEmpty()) {
//                userStream = userStream.filter(u -> u.getMeterNumber() != null && u.getMeterNumber().equalsIgnoreCase(meterNumber));
//            }

            if (customerId != null && !customerId.isEmpty()) {
                userStream = userStream.filter(u -> u.getCustomerId() != null && u.getCustomerId().equalsIgnoreCase(customerId));
            }

            List<Customer> filteredCustomers = userStream.toList();

            // Pagination logic
            int totalCustomers = filteredCustomers.size();
            List<Customer> paginatedCustomers;
            if (size == 0) {
                paginatedCustomers = filteredCustomers;
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
            genericHandler.logIncidentReport("Fetching customers Service failed");
            genericHandler.logAndSaveException(exception, "fetch customers");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> singleCustomer(UUID id) {
        try {

            UserModel um = handleUserValidation();

            Object cachedUser = customerCache.get(id.toString()+"_"+um.getOrgId());

            if (cachedUser != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached " + customerName + " " + status.getDesc(), cachedUser);
            }
            // check if customer exist
            Customer isCustomer = customerMapper.findById(id, um.getOrgId());
            if (isCustomer == null){
                throw new GlobalExceptionHandler.NotFoundException(customerName + " " + status.getNotFoundDesc());
            }

//            handleAddCache(isCustomer);

            return ResponseMap.response(status.getSuccessCode(), customerName + " " + status.getRegDesc(), isCustomer);
        } catch (Exception exception) {
            log.error("Error occurred while creating customer [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Fetching customer Service failed");
            genericHandler.logAndSaveException(exception, "fetch customers");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> changeState(UUID customerId, String state, String reason) throws MissingServletRequestParameterException {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            String desc;
            UserModel um = handleUserValidation();

            // check if customer exist
            Customer isCustomer = customerMapper.findById(customerId, um.getOrgId());
            if (isCustomer == null){
                throw new GlobalExceptionHandler.NotFoundException(customerName + " " + status.getExistDesc());
            }

            if(state.equalsIgnoreCase("active") || state.equalsIgnoreCase("inactive") || state.equalsIgnoreCase("block")){
                int isStatus = customerMapper.changeStatus(customerId, capitalizeFirstLetter(state.toLowerCase()), um.getOrgId());
                if (isStatus != 1) {
                    throw new GlobalExceptionHandler.NotFoundException(customerName + " " + status.getUpdateFailureDesc());
                }
                desc = state.equals("active") ? "User activated" : state.equals("inactive") ? "User inactive": "User blocked" ;
            } else {
                throw new MissingServletRequestParameterException("Required request parameter '%s' is not present", state);
            }

            Customer customer = customerMapper.findById(customerId, um.getOrgId());

//            handleAddCache(customer);
            AuditLog auditLog = buildAuditLog(um, desc, reason, customerName, customer, metadata);
            auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(), "Customer " + state + " successfully", "");
            
        } catch (Exception exception) {
            log.error("Error occurred while changing user status [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("changing customer status Service failed");
            genericHandler.logAndSaveException(exception, "changing customer state");
            throw exception;
        }
    }

    @Override
    public Map<String, Object> bulkUpload(MultipartFile file) throws IOException {
        UserModel currentUser = handleUserValidation();
        String filename = file.getOriginalFilename();

        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Uploaded file must have a valid name.");
        }

        final int MAX_UPLOAD_LIMIT = 5000;
        final int BATCH_SIZE = 200;
        final List<Customer> batch = new ArrayList<>();
        final List<String> failedRecords = new ArrayList<>();
        final int[] totalCount = {0};
        final int[] successCount = {0};
        ExceptionErrorLogs errorLogs = new ExceptionErrorLogs();

        Consumer<Customer> customerConsumer = customer -> {
//            if (totalCount[0] >= MAX_UPLOAD_LIMIT) {
//                return; // Ignore further records after hitting the limit
//            }
            if (totalCount[0] >= MAX_UPLOAD_LIMIT) {
                throw new IllegalArgumentException("Maximum upload limit of " + MAX_UPLOAD_LIMIT + " records exceeded.");
            }

            totalCount[0]++;
            customer.setCustomerId("C" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            customer.setOrgId(currentUser.getOrgId());
            customer.setStatus("Inactive");
            batch.add(customer);

            if (batch.size() >= BATCH_SIZE) {
                processBatch(batch, successCount, failedRecords);
            }
        };

        // Parse with streaming logic
        if (filename.endsWith(".csv")) {
            parseCSV(file, customerConsumer);
        } else if (filename.endsWith(".xlsx")) {
            parseExcel(file, customerConsumer);
        } else {
            throw new IllegalArgumentException("Unsupported file type");
        }

        // Final insert for any remaining customers
        if (!batch.isEmpty()) {
            processBatch(batch, successCount, failedRecords);
        }

        if(successCount[0] == 0){
            throw new GlobalExceptionHandler.NotFoundException("Customer record upload failed");
        }
        return ResponseMap.response(
                status.getSuccessCode(),
                String.format("%d of %d %ss %s", successCount[0], totalCount[0], customerName, status.getRegDesc()),
                Map.of(
                        "successful", successCount[0],
                        "failed", failedRecords.size(),
                        "failedDetails", failedRecords
                )
        );
    }

    private void processBatch(List<Customer> batch, int[] successCount, List<String> failedRecords) {
        try {
            customerMapper.bulkInsertCustomers(batch);
            successCount[0] += batch.size();
            log.warn("Batch insert");
        } catch (Exception batchEx) {

            log.warn("Batch insert failed. Falling back to single inserts");
            for (Customer customer : batch) {
                try {
                    customerMapper.insertCustomer(customer);
                    successCount[0]++;
                } catch (Exception e) {
                    String errorMessage = extractErrorMessage(e);

                    String failedId = String.format(
                            "Email: %s | Phone number: %s | Reason: %s",
                            customer.getEmail(),
                            customer.getPhoneNumber(),
                            errorMessage
                    );

                    failedRecords.add(failedId);
                    log.error("Single insert failed for {}: {}", failedId, e.getMessage(), e);
                }
            }
        }
        batch.clear();
    }

    private void parseCSV(MultipartFile file, Consumer<Customer> customerConsumer) throws IOException {
        try (
                Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
                CSVParser parser = CSVFormat.DEFAULT
                        .withFirstRecordAsHeader()
                        .parse(reader)
        ) {
            for (CSVRecord record : parser) {
                Customer customer = buildCustomer(
                        record.get("firstname"),
                        record.get("lastname"),
                        record.get("nin"),
                        record.get("phoneNumber"),
                        record.get("email"),
                        record.get("state"),
                        record.get("city"),
                        record.get("houseNo"),
                        record.get("streetName"),
                        record.get("vat")
                );
                customerConsumer.accept(customer);
            }
        }
    }

    public void parseExcel(MultipartFile file, Consumer<Customer> customerConsumer) throws IOException {
        try {
            OPCPackage pkg = OPCPackage.open(file.getInputStream());
            XSSFReader reader = new XSSFReader(pkg);
            InputStream sheetStream = reader.getSheetsData().next(); // Only first sheet

            XMLReader parser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            parser.setContentHandler(new ExcelSheetHandler(customerConsumer));

            parser.parse(new InputSource(sheetStream));

            sheetStream.close();
            pkg.close();
        } catch (Exception e) {
            throw new IOException("Error while parsing Excel file", e);
        }
    }


    private String extractErrorMessage(Exception e) {
        String message = e.getMessage();

        if (message == null) return "Unknown error";

        if (message.contains("duplicate key value")) {
            return "Duplicate record — The email, or phone number already exists.";
        }
        if (message.contains("violates not-null constraint")) {
            return "Missing required field — one or more mandatory columns are empty.";
        }
        if (message.contains("foreign key constraint")) {
            return "Invalid reference — linked data does not exist.";
        }
        if (message.contains("invalid input syntax")) {
            return "Invalid data type — check number or date format.";
        }

        // default fallback
        return message.split("\n")[0];
    }


    public class ExcelSheetHandler extends DefaultHandler {
        private final Consumer<Customer> customerConsumer;

        private StringBuilder value = new StringBuilder();
        private String[] rowData = new String[10];
        private int colIndex = -1;
        private boolean insideValue = false;
        private boolean isHeaderRow = true;

        public ExcelSheetHandler(Consumer<Customer> customerConsumer) {
            this.customerConsumer = customerConsumer;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if ("row".equals(qName)) {
                colIndex = -1;
                rowData = new String[10]; // Reset row
            }
            if ("c".equals(qName)) {
                colIndex++;
            }
            if ("v".equals(qName)) {
                insideValue = true;
                value.setLength(0);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (insideValue) {
                value.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if ("v".equals(qName)) {
                rowData[colIndex] = value.toString();
                insideValue = false;
            } else if ("row".equals(qName)) {
                if (isHeaderRow) {
                    isHeaderRow = false; // skip header
                } else {
                    Customer customer = buildCustomer(
                            safeGet(rowData, 0),
                            safeGet(rowData, 1),
                            safeGet(rowData, 2),
                            safeGet(rowData, 3),
                            safeGet(rowData, 4),
                            safeGet(rowData, 5),
                            safeGet(rowData, 6),
                            safeGet(rowData, 7),
                            safeGet(rowData, 8),
                            safeGet(rowData, 9)
                    );
                    customerConsumer.accept(customer);
                }
            }
        }

        private String safeGet(String[] array, int index) {
            return (index >= 0 && index < array.length) ? array[index] : "";
        }
    }


//    // Save all errors once
//        if (!failedRecords.isEmpty()) {
//        errorLogs.setDescription("Error occurred while trying to upload customer records");
//        errorLogs.setError_message(String.join("; ", failedRecords));
//        exceptionAuditRepository.save(errorLogs);
//    }

        ///-----------------------

//    private void parseExcel(MultipartFile file, Consumer<Customer> customerConsumer) throws IOException {
//        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
//            Sheet sheet = workbook.getSheetAt(0);
//            Iterator<Row> rowIterator = sheet.iterator();
//
//            if (rowIterator.hasNext()) rowIterator.next(); // Skip header row
//
//            while (rowIterator.hasNext()) {
//                Row row = rowIterator.next();
//                Customer customer = buildCustomer(
//                        getCellValue(row.getCell(0)),
//                        getCellValue(row.getCell(1)),
//                        getCellValue(row.getCell(2)),
//                        getCellValue(row.getCell(3)),
//                        getCellValue(row.getCell(4)),
//                        getCellValue(row.getCell(5)),
//                        getCellValue(row.getCell(6)),
//                        getCellValue(row.getCell(7)),
//                        getCellValue(row.getCell(8)),
//                        getCellValue(row.getCell(9))
//                );
//                customerConsumer.accept(customer);
//            }
//        }
//    }

    private AuditLog buildAuditLog(UserModel creator, String description, String reason, String type, Object createdEntity, Map<String, String> metadata) {
        AuditLog log = new AuditLog();
        log.setCreator(creator);
        log.setDescription(description);
        log.setReason(reason);
        log.setType(type);
        log.setCreatedCustomer(createdEntity instanceof Customer ? (Customer) createdEntity : null);
        log.setIpAddress(metadata.get("ipAddress"));
        log.setUserAgent(metadata.get("userAgent"));
        log.setEndpoint(metadata.get("endpoint"));
        log.setHttpMethod(metadata.get("httpMethod"));
        return log;
    }

    private Customer buildCustomer(String firstname, String lastname, String nin ,String phoneNumber, String email,
                                   String state, String city, String houseNo, String streetName, String vat) {
        Customer c = new Customer();
        c.setFirstname(firstname);
        c.setLastname(lastname);
        c.setNin(nin);
        c.setPhoneNumber(phoneNumber);
        c.setEmail(email);
        c.setState(state);
        c.setCity(city);
        c.setHouseNo(houseNo);
        c.setStreetName(streetName);
        c.setVat(vat);
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