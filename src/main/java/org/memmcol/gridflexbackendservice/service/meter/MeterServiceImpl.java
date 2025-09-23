package org.memmcol.gridflexbackendservice.service.meter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.mapper.MeterMapper;
import org.memmcol.gridflexbackendservice.mapper.NodeMapper;
import org.memmcol.gridflexbackendservice.mapper.TariffMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.meter.*;
import org.memmcol.gridflexbackendservice.model.node.RegionBhubServiceCenter;
import org.memmcol.gridflexbackendservice.model.node.SubStationTransformerFeederLine;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.service.customer.CustomerServiceImpl;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

import static org.memmcol.gridflexbackendservice.components.GenericHandler.capitalizeFirstLetter;
import static org.memmcol.gridflexbackendservice.components.GenericHandler.getClientIp;
import static org.memmcol.gridflexbackendservice.components.handleValidUser.handleUserValidation;

@Service
public class MeterServiceImpl implements MeterService {
    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

    @Autowired
    private ResponseProperties status;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private NodeMapper nodeMapper;

    @Autowired
    private MeterMapper meterMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TariffMapper tariffMapper;

    @Autowired
    private GenericHandler genericHandler;

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;

    @Autowired
    private HttpServletRequest httpServletRequest;

    private String meterName = "Meter";

    private final IMap<String, Object> meterCache;

    private final IMap<String, Object> auditCache;


    public MeterServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.meterCache = hazelcastInstance.getMap("meterCache");
        this.auditCache = hazelcastInstance.getMap("auditCache");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Map<String, Object> createMeter(Meter request) {
        try {
            // --- Step 1: Context & Validation ---
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel user = handleUserValidation();

            validateMeterRequest(request, user);

            // --- Step 2: Insert Meter + Versions ---
            int result1 = meterMapper.insertMeter(request);
            request.setMeterId(request.getId());
            int result2 = meterMapper.insertMeterVersion(request);
            if (result1 == 0 || result2 == 0) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getRegFailureDesc());
            }
            if ("md".trim().equalsIgnoreCase(request.getMeterClass())) {
                insertMDMeterInfo(request, user);
            }
            if (Boolean.TRUE.equals(request.getSmartStatus())) {
                insertSmartMeterInfo(request, user);
            }

            // --- Step 3: Fetch created meter & Audit ---
            Meter newMeter = meterMapper.findByIdVersion(request.getId(), request.getOrgId());
            AuditLog auditLog = buildAuditLog(user, "Meter created", meterName, newMeter, metadata);
            auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(), meterName + " " + status.getRegDesc(), "");

        } catch (Exception ex) {
            log.error("Error creating meter: {}", ex.getMessage(), ex);
//            genericHandler.logIncidentReport("Creating meter service failed");
            genericHandler.logAndSaveException(ex, "creating meter");
            throw ex;
        }
    }

    // ---------------- Helper Methods ----------------

    private void validateMeterRequest(Meter request, UserModel user) {
        Manufacturer manufacturer = meterMapper.getMeterManufacturer(request.getMeterManufacturer());
        if (manufacturer == null) {
            throw new GlobalExceptionHandler.NotFoundException("Meter manufacturer not found");
        }

        Meter existing = meterMapper.getMeter(user.getOrgId(), null, request.getMeterNumber().trim(), null);
        if (existing != null) {
            throw new GlobalExceptionHandler.NotFoundException("Meter already exists");
        }

        String clazz = request.getMeterClass();
        if (!clazz.equalsIgnoreCase("md") &&
                !clazz.equalsIgnoreCase("single-phase") &&
                !clazz.equalsIgnoreCase("three-phase")) {
            throw new GlobalExceptionHandler.NotFoundException(
                    "Meter class must be one of: MD, single-phase, or three-phase");
        }

        // Default states
        request.setStatus("Inactive");
        request.setMeterStage("Pending-created");
        request.setOrgId(user.getOrgId());
        request.setType("NON-VIRTUAL");
        request.setDescription(capitalizeFirstLetter("Meter created"));
        request.setCreatedBy(user.getId());
    }

//    private void insertMeterVersion(Meter request) {
//        int result1 = meterMapper.insertMeter(request);
//        request.setMeterId(request.getId());
//        int result2 = meterMapper.insertMeterVersion(request);
//        if (result1 == 0 || result2 == 0) {
//            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getRegFailureDesc());
//        }
//    }

    private void insertMDMeterInfo(Meter request, UserModel user) {
        request.getMdMeterInfo().setMeterId(request.getId());
        request.getMdMeterInfo().setOrgId(user.getOrgId());
        request.getMdMeterInfo().setCreatedBy(user.getId());
        request.getMdMeterInfo().setMeterStage("Pending-created");
        request.getMdMeterInfo().setDescription(capitalizeFirstLetter("MD Meter Info created"));

        int inserted = meterMapper.insertMDMeterInfoVersion(request.getMdMeterInfo());
        if (inserted == 0) {
            throw new GlobalExceptionHandler.NotFoundException(meterName + " MD data " + status.getRegFailureDesc());
        }
    }

    private void insertSmartMeterInfo(Meter request, UserModel user) {
        System.out.println(">>>>>>>>>>3:: "+request.getId());
        request.getSmartMeterInfo().setMeterId(request.getId());
        request.getSmartMeterInfo().setOrgId(user.getOrgId());
        request.getSmartMeterInfo().setCreatedBy(user.getId());
        request.getSmartMeterInfo().setMeterStage("Pending-created");
        request.getSmartMeterInfo().setDescription(capitalizeFirstLetter("Smart Meter Info created"));
        request.getSmartMeterInfo().setPassword(passwordEncoder.encode(request.getSmartMeterInfo().getPassword()));

        int inserted = meterMapper.insertSmartMeterInfoVersion(request.getSmartMeterInfo());
        if (inserted == 0) {
            throw new GlobalExceptionHandler.NotFoundException(meterName + " Smart data " + status.getRegFailureDesc());
        }
    }


    @Transactional
    @Override
    public Map<String, Object> updateMeter(Meter request) {
        try {
            // Gather client metadata
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

            // Validate user and set organization ID
            UserModel user = handleUserValidation();
            request.setOrgId(user.getOrgId());

            // Fetch existing meter and version
            Meter existingMeter = meterMapper.findById(request.getId(), user.getOrgId());
            if (existingMeter == null) {
                throw new GlobalExceptionHandler.NotFoundException("Meter not found");
            }

            Meter versionedMeter = meterMapper.findByIdVersion(request.getId(), user.getOrgId());

            // Prepare meter update data
            request.setType("NON-VIRTUAL");
            request.setStatus("Active");
            request.setMeterStage("Pending-edited");
            request.setDescription(buildChangeDescription(existingMeter, request));
            request.setCreatedBy(user.getId());

            // Insert or update meter version
            int result;
            if (versionedMeter != null) {
                throw new GlobalExceptionHandler.NotFoundException(versionedMeter.getMeterNumber()+ " have a pending status needs to be cleared");
            } else {
                int res = meterMapper.updateMeter("Pending-edited", request.getMeterNumber(), request.getUpdatedAt(), request.getStatus());
                result = meterMapper.insertMeterVersion(request);
                if (result == 0 || res == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());
            }

            if (result == 0) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());
            }

            // Handle MD meter-specific logic
            if (request.getMdMeterInfo() != null) {
                UUID meterId = request.getId();
                request.getMdMeterInfo().setMeterId(meterId);
                request.getMdMeterInfo().setOrgId(user.getOrgId());
                request.getMdMeterInfo().setMeterStage("Pending-edited");
                request.getMdMeterInfo().setDescription(buildMDMeterInfoChangeDescription(existingMeter.getMdMeterInfo(), request.getMdMeterInfo()));

                int res = meterMapper.updateMeter("Pending-edited", request.getMeterNumber(), request.getUpdatedAt(), request.getStatus());
                int mdResult2 = meterMapper.insertMDMeterInfoVersion(request.getMdMeterInfo());
                    if (res == 0 || mdResult2 == 0) {
                        throw new GlobalExceptionHandler.NotFoundException(meterName + " MD data " + status.getUpdateFailureDesc());
                    }
            }

            // Handle smart meter-specific logic
            if (request.getSmartMeterInfo() != null) {
                UUID meterId = request.getId();
                request.getSmartMeterInfo().setMeterId(meterId);
                request.getSmartMeterInfo().setOrgId(user.getOrgId());
                request.getSmartMeterInfo().setMeterStage("Pending-edited");
                request.getSmartMeterInfo().setDescription(buildSmartMeterInfoChangeDescription(existingMeter.getSmartMeterInfo(), request.getSmartMeterInfo()));
                int res = meterMapper.updateMeter("Pending-edited", request.getMeterNumber(), request.getUpdatedAt(), request.getStatus());
                int mdResult2 = meterMapper.insertSmartMeterInfoVersion(request.getSmartMeterInfo());
                if (res == 0 || mdResult2 == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(meterName + " MD data " + status.getUpdateFailureDesc());
                }
            }

            // Fetch updated meter and log audit
            Meter updatedMeter = meterMapper.findByIdVersion(request.getId(), user.getOrgId());
            AuditLog auditLog = buildAuditLog(user, "Meter edited", meterName, updatedMeter, metadata);
            auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(), meterName + " " + status.getUpdateDesc(), "");
        } catch (Exception ex) {
            log.error("Error updating meter: {}", ex.getMessage(), ex);
            genericHandler.logIncidentReport("Editing meter service failed");
            genericHandler.logAndSaveException(ex, "editing meter");
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getAllMeters(
            int page, int size, String meterNumber, String simNo, String manufacturer, String meterStage,
            String meterClass, String category, String state, String createdAt, String customerId) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {

            UserModel um = handleUserValidation();

            // Build a unique cache key
            StringBuilder cacheKeyBuilder = new StringBuilder("users_"+um.getOrgId());
            if (meterNumber != null && !meterNumber.isEmpty()) cacheKeyBuilder.append("_meterNumber_").append(meterNumber);
            if (simNo != null && !simNo.isEmpty()) cacheKeyBuilder.append("_simNo_").append(simNo);
            if (meterStage != null && !meterStage.isEmpty()) cacheKeyBuilder.append("_meterStage_").append(meterStage);
            if (manufacturer != null && !manufacturer.isEmpty()) cacheKeyBuilder.append("_manufacturer_").append(manufacturer);
            if (meterClass != null && !meterClass.isEmpty()) cacheKeyBuilder.append("_meterClass_").append(meterClass);
            if (category != null && !category.isEmpty()) cacheKeyBuilder.append("_category_").append(category);
            if (state != null && !state.isEmpty()) cacheKeyBuilder.append("_state_").append(state);
            if (createdAt != null && !createdAt.isEmpty()) cacheKeyBuilder.append("_createdAt_").append(createdAt);
            if (customerId != null && !customerId.isEmpty()) cacheKeyBuilder.append("_customerId_").append(customerId);
            cacheKeyBuilder.append("_page_").append(page);
            cacheKeyBuilder.append("_size_").append(size);

            String cacheKey = cacheKeyBuilder.toString();

            // Return from cache if available
            Object cachedUser = meterCache.get(cacheKey);
            if (cachedUser != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached Meters " + status.getDesc(), cachedUser);
            }

            List<Meter> meters;
             // Fetch all users
            if(meterStage.trim().equalsIgnoreCase("pending-state")) {
                meters = meterMapper.getMetersVersion(um.getOrgId());
            } else {
                meters = meterMapper.getMeters(um.getOrgId());
            }

            // Apply filtering
            Stream<Meter> meterStream = meters.stream();

            if (meterNumber != null && !meterNumber.isEmpty()) {
                meterStream = meterStream.filter(u -> u.getMeterNumber() != null && u.getMeterNumber().equalsIgnoreCase(meterNumber));
            }

            if (simNo != null && !simNo.isEmpty()) {
                meterStream = meterStream.filter(u -> u.getSimNumber() != null && u.getSimNumber().equalsIgnoreCase(simNo));
            }

            if (meterStage != null && !meterStage.isEmpty()) {
                meterStream = meterStream.filter(u -> u.getMeterStage() != null && u.getMeterStage().equalsIgnoreCase(meterStage));
            }

            if (meterClass != null && !meterClass.isEmpty()) {
                meterStream = meterStream.filter(u -> u.getMeterClass() != null && u.getMeterClass().equalsIgnoreCase(meterClass));
            }

            if (category != null && !category.isEmpty()) {
                meterStream = meterStream.filter(u -> u.getMeterCategory() != null && u.getMeterCategory().equalsIgnoreCase(category));
            }

            if (customerId != null && !customerId.isEmpty()) {
                meterStream = meterStream.filter(u -> u.getCustomerId() != null && u.getCustomerId().equalsIgnoreCase(customerId));
            }

            if (createdAt != null && !createdAt.isEmpty()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate date = LocalDate.parse(createdAt, formatter);
                meterStream = meterStream.filter(u -> {
                    if (u.getCreatedAt() == null) return false;
                    return !u.getCreatedAt()
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .isBefore(date);
                });
            }

            List<Meter> filteredMeters = meterStream.toList();

            // Pagination logic
            int totalMeters = filteredMeters.size();
            List<Meter> paginatedMeters;
            if (size == 0) {
                paginatedMeters = filteredMeters; // Return all users
            } else {
                int fromIndex = Math.min(page * size, totalMeters);
                int toIndex = Math.min(fromIndex + size, totalMeters);
                paginatedMeters = filteredMeters.subList(fromIndex, toIndex);
            }

            // Prepare response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedMeters);
            response.put("totalData", totalMeters);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", (int) Math.ceil((double) paginatedMeters.size() / size));

//            userCache.put(cacheKey, response);

            return ResponseMap.response(status.getSuccessCode(), meterName + "s " + status.getDesc(), response);

        } catch (Exception exception) {
            log.error("Error filtering / fetching users: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Fetching meter service failed");
            genericHandler.logAndSaveException(exception, "fetching meter");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getSingleMeter(UUID meterId, String meterNumber, String accountNumber, UUID meterVersionId, String versionMeterNumber) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            Meter meter = null;
            UserModel um = handleUserValidation();

            if (meterId == null && meterNumber == null && accountNumber == null && meterVersionId == null && versionMeterNumber == null) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException("At least one of meterId, meterNumber, or accountNumber must be provided.");
            }

//            Object cachedUser = meterCache.get(meterId.toString()+"_"+um.getOrgId());

//            if (cachedUser != null) {
//                return ResponseMap.response(status.getSuccessCode(), "Cached " + meterName + " " + status.getDesc(), cachedUser);
//            }

            if(meterNumber != null){
                meter = meterMapper.getMeter(um.getOrgId(), meterId, meterNumber, accountNumber);
            }

            if(accountNumber != null){
                meter = meterMapper.getMeter(um.getOrgId(), meterId, meterNumber, accountNumber);
            }

            if(meterId != null){
                meter = meterMapper.getMeter(um.getOrgId(), meterId, meterNumber, accountNumber);
            }

            if(versionMeterNumber != null){
                meter = meterMapper.getVersionMeter(um.getOrgId(), meterVersionId, versionMeterNumber);
            }

            if(meterVersionId != null){
                meter = meterMapper.getVersionMeter(um.getOrgId(), meterVersionId, versionMeterNumber);
            }

//            handleAddCache(meter);

            return ResponseMap.response(status.getSuccessCode(),  meterName + " " + status.getDesc(), meter);
        } catch (Exception exception) {
            log.error("Error occurred while fetching feeder lines [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetch transformer");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> changeStatus(UUID meterId, String state, String reason) throws MissingServletRequestParameterException {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        int result;
        String desc = "";
        String resp = "";
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            UserModel um = handleUserValidation();

            Meter meterById = meterMapper.findById(meterId, um.getOrgId());
            if(meterById == null) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getNotFoundDesc());
            }
            if(state != null && (state.equalsIgnoreCase("in-stock")
                    || state.equalsIgnoreCase("deactivated"))) {
                result = meterMapper.changeState(meterId, state, um.getOrgId());
                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(meterName + " " + state + " " + status.getUpdateFailureDesc());
                }
                desc = meterById.getMeterNumber() + state;
            }
            else {
                assert state != null;
                throw new MissingServletRequestParameterException("Required request parameter '%s' is not present", state);
            }

            Meter meter = meterMapper.findById(meterId, um.getOrgId());

            if(meter.getStatus().equals(false)) {
                resp = "Meter ("+ meter.getMeterNumber() + ") in-stock successfully";
            }
//            else if (meter.getStatus().equalsIgnoreCase("assigned")) {
//                resp = "Meter ("+ meter.getMeterNumber() + ") assigned successfully";
//            }
            else if (meter.getMeterStage().equals("Deactivated")) {
                resp = "Meter ("+ meter.getMeterNumber() + ") deactivated successfully";
            } else {
                throw new MissingServletRequestParameterException("Required request parameter '%s' is not present", state);
            }

//            handleAddCache(meter);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setReason(reason);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setType(meterName);
            auditNotificationDTO.setCreatedMeter(meter);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(), resp, "");

        } catch (Exception exception) {
            log.error("Error occurred while changing user status [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetching user");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getManufacturers() {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {

            UserModel um = handleUserValidation();

            // Get all manufacturers
            List<Manufacturer> manufacturers = meterMapper.getManufacturers(um.getOrgId());

            return ResponseMap.response(status.getSuccessCode(),  status.getDesc(), manufacturers);
        } catch (Exception exception) {
            log.error("Error occurred while fetching feeder lines [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetch transformer");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> singleCustomer(String customerId) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {

            UserModel um = handleUserValidation();
            String virtualMeterNo = handleGetVirtualMeter();
            String accountNumber = handleGetAccountNumber();

            // check if customer exist
            Customer isCustomer = meterMapper.findByCustomerId(customerId, um.getOrgId());
            if (isCustomer == null) {
                throw new GlobalExceptionHandler.NotFoundException("Customer " + status.getNotFoundDesc());
            }

            List<Tariff> allTariffs = tariffMapper.GetTariffs(um.getOrgId());

            Map<String, Object> response = new HashMap<>();
            response.put("customer", isCustomer);
            response.put("GeneratedAccountNumber", accountNumber);
            response.put("GeneratedVirtualMeterNo", virtualMeterNo);
            response.put("tariffs", allTariffs);

            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), response);
        } catch (Exception exception) {
            log.error("Error occurred while fetching customer [ACTION]: {}", exception.getMessage(), exception);
            exception.printStackTrace();
            exceptionErrorLogs.setDescription("Error occurred while trying to fetching customer");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> assignMeterToCustomer(AssignMeterToCustomer request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditLog = new AuditLog();
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            UserModel user = handleUserValidation();
            request.setOrgId(user.getOrgId());
            request.setCreatedBy(user.getId());

            // Validate DSS
            SubStationTransformerFeederLine dss = meterMapper.verifyDss(request.getDssAssetId(), user.getOrgId());
            if (dss == null) {
                throw new GlobalExceptionHandler.NotFoundException("DSS " + status.getNotFoundDesc());
            }

            // Validate feeder line
            SubStationTransformerFeederLine feederLine = meterMapper.verifyFeederLine(dss.getParentId(), user.getOrgId());
            if (feederLine == null) {
                throw new GlobalExceptionHandler.NotFoundException("Feeder line " + status.getNotFoundDesc());
            }

            // Validate meter version (must exist and be approved)
            Meter pendingMeter = meterMapper.getVersionMeter(user.getOrgId(), null, request.getNewMeterNumber());
//                    getVersionMeterNumber(user.getOrgId(), request.getNewMeterNumber());
            if (pendingMeter == null) {
                throw new GlobalExceptionHandler.NotFoundException(
                        request.getNewMeterNumber() + " meter has a pending record that needs approval"
                );
            }

            // Validate main meter record
            Meter mainMeter = meterMapper.getMeter(user.getOrgId(), null, request.getNewMeterNumber(), null);
            if (mainMeter == null) {
                throw new GlobalExceptionHandler.NotFoundException("Meter " + status.getNotFoundDesc());
            }

            // Validate node assignment
            if (mainMeter.getNodeId() == null) {
                throw new GlobalExceptionHandler.NotFoundException(
                        request.getNewMeterNumber() + " meter has not been allocated"
                );
            }
            String description = "Meter assigned to customer " + request.getCustomerId();

            // Assign meter to customer
            request.setDssAssetId(dss.getAssetId());
            request.setDescription(description);
            int customerAssignResult;
            if(request.getNewMeterNumber().contains("V") || request.getNewMeterNumber().contains("v")){
                customerAssignResult = meterMapper.assignedVirtualVersionMeterToCustomer(mainMeter, request);
            } else {
                customerAssignResult = meterMapper.assignedVersionMeterToCustomer(mainMeter, request);
            }

            request.setMeterId(mainMeter.getId());
            int locationAssignResult = meterMapper.assignVersionMeterToLocation(request);

            if (customerAssignResult == 0 || locationAssignResult == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Meter assignment to customer or location failed");
            }

            // Handle prepaid meter assignment
            if ("prepaid".equalsIgnoreCase(request.getMeterCategory())) {
                request.setDescription("Payment mode assigned to " + request.getNewMeterNumber());
                int paymentModeResult = meterMapper.assignPaymentModeVersion(request);

                if (paymentModeResult == 0) {
                    throw new GlobalExceptionHandler.NotFoundException("Payment mode assignment failed");
                }
            }
            Meter meter = meterMapper.getVersionMeter(user.getOrgId(), null, request.getNewMeterNumber());
            user.setPassword("");

            auditLog.setCreator(user);
            auditLog.setDescription(description);
            auditLog.setType(meterName);
            auditLog.setUserAgent(userAgent);
            auditLog.setIpAddress(ipAddress);
            auditLog.setCreatedMeter(meter);
            auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(), "meter assigned successfully", "");

        } catch (Exception e) {
            log.error("Error occurred during meter assignment: {}", e.getMessage(), e);

            exceptionErrorLogs.setDescription("Error occurred while assigning meter to customer");
            exceptionErrorLogs.setError_message(e.getMessage().trim());
            exceptionErrorLogs.setError(e.toString().trim());

            exceptionAuditRepository.save(exceptionErrorLogs);
            throw e;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> migrate(PaymentMode request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        int result;
        String desc = "";
        String resp = "";
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            UserModel um = handleUserValidation();

            // verify if meter exist
            Meter meterById = meterMapper.findById(request.getMeterId(), um.getOrgId());
            if(meterById == null) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getNotFoundDesc());
            }

            // prevent MD meter from migrating
            if(meterById.getMeterClass().equalsIgnoreCase("MD")){
                throw new GlobalExceptionHandler.NotFoundException("MD meter can not be migrated" );
            }

            //migrate to prepaid
            if(request.getMigrationFrom().equalsIgnoreCase("postpaid")){
                meterMapper.updateMeterCategory("prepaid", um.getOrgId(), request.getMeterId());
                request.setMeterCategory("prepaid");

                // insert payment method
                meterMapper.assignPaymentModeWhenMigrationToPrepaid(request);
            }

            // migrate to postpaid
            if(request.getMigrationFrom().equalsIgnoreCase("prepaid")){
                meterMapper.updateMeterCategory("postpaid", um.getOrgId(), request.getMeterId());
            }

            // get recent meter record
            Meter meter = meterMapper.findById(request.getMeterId(), um.getOrgId());


            handleAddCache(meter);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
//            auditNotificationDTO.setReason(reason);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setType(meterName);
            auditNotificationDTO.setCreatedMeter(meter);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(), resp, "");

        } catch (Exception exception) {
            log.error("Error occurred while changing user status [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetching user");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Map<String, Object> approve(UUID meterVersionId, String approveStatus)
            throws MissingServletRequestParameterException {

        try {
            // --- Step 1: Validate request ---
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel user = handleUserValidation();

            Meter meter = meterMapper.findByIdVersion(meterVersionId, user.getOrgId());
            if (meter == null) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getNotFoundDesc());
            }

            prepareMeterForApproval(meter, user, meterVersionId);

            // --- Step 2: Handle approval / rejection ---
            if (isApprove(approveStatus)) {
                handleApproval(meter, user, approveStatus);
            } else if (isReject(approveStatus)) {
                handleRejection(meter, approveStatus, user);
            } else {
                throw new MissingServletRequestParameterException("meterStage", "not found");
            }

            // --- Step 3: Audit log ---
            Meter updatedMeter = meterMapper.findById(meter.getId(), user.getOrgId());
            user.setPassword(null); // hide password in logs
            AuditLog auditLog = buildAuditLog(user, "Meter approve", meterName, updatedMeter, metadata);
            auditRepository.save(auditLog);

            return ResponseMap.response(
                    status.getSuccessCode(),
                    meter.getMeterNumber() + " " + meterName + " " + capitalizeFirstLetter(approveStatus) + " Successfully",
                    ""
            );

        } catch (Exception ex) {
            log.error("Error occurred while approving/rejecting meter: {}", ex.getMessage(), ex);
            genericHandler.logIncidentReport("approving meter service failed");
            genericHandler.logAndSaveException(ex, "approving meter");
            throw ex;
        }
    }

// ---------------- Helper Methods ----------------

    private void prepareMeterForApproval(Meter meter, UserModel user, UUID meterVersionId) {
        meter.setOrgId(user.getOrgId());
        meter.setApproveBy(user.getId());

        if (meter.getMdMeterInfo() != null) {
            meter.getMdMeterInfo().setMeterId(meterVersionId);
            meter.getMdMeterInfo().setOrgId(user.getOrgId());
            meter.getMdMeterInfo().setApproveBy(user.getId());
        }

        if (meter.getSmartMeterInfo() != null) {
            meter.getSmartMeterInfo().setMeterId(meterVersionId);
            meter.getSmartMeterInfo().setOrgId(user.getOrgId());
            meter.getSmartMeterInfo().setApproveBy(user.getId());
        }

//        boolean hasCustomer = meter.getCustomerId() != null;
//        meter.setStatus(hasCustomer);
//        meter.setActivateStatus(hasCustomer);
    }
//    String meterStage = meter.getMeterStage().equalsIgnoreCase("Pending-created") ? "Created" : approveStatus;
    private void handleApproval(Meter meter, UserModel user, String approveStatus) {
        meter.setApproveBy(user.getId());
        if(meter.getMeterStage().trim().equalsIgnoreCase("Pending-created")){
            meter.setMeterStage("Created");
            meter.setStatus("Active");
        } else if(meter.getMeterStage().trim().equalsIgnoreCase("Pending-assigned")){
            meter.setMeterStage("Assigned");
            meter.setStatus("Active");
        } else if(meter.getMeterStage().trim().equalsIgnoreCase("Pending-allocated")){
            meter.setMeterStage("Pending-assigned");
            meter.setStatus("Active");
        } else if(meter.getStatus().trim().equalsIgnoreCase("Deactivated")) {
//            meter.setMeterStage("");
            meter.setStatus("Deactivated");
        } else {
            //pending-detached & pending-migrated
            meter.setMeterStage("Active");
            meter.setStatus("Active");
        }

        int approved = meterMapper.approvedMeterVersion(meter);
        if (approved == 0) {
            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "d " + status.getUpdateFailureDesc());
        }
        //Approve meter in actual meters table
        if (meterMapper.approveMeter(meter) == 0) {
            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "d " + status.getUpdateFailureDesc());
        }

        if ("md".equalsIgnoreCase(meter.getMeterClass())) {
            approveMDMeterInfo(meter);
        }

        if ("prepaid".equalsIgnoreCase(meter.getMeterCategory())) {
            approvePrepaidMeterInfo(meter, approveStatus);
        }

    }

    private void approveMDMeterInfo(Meter meter) {
        int mdInfoApproval = meterMapper.approveMDMeterInfoVersion(meter.getMdMeterInfo());
        int updateMDInfoApproval = meterMapper.updateMDMeterInfo(meter.getMdMeterInfo());
        if (mdInfoApproval == 0 || updateMDInfoApproval == 0) {
            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());
        }
    }

    private void approvePrepaidMeterInfo(Meter meter, String approveStatus) {
        int prepaidApproval = meterMapper.approvePrepaidMeterVersion(meter.getPaymentMode());
        int updatePrepaidApproval = meterMapper.updatePrepaidMeterVersion(meter.getPaymentMode());

        if (prepaidApproval == 0 || updatePrepaidApproval == 0) {
            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "d " + status.getUpdateFailureDesc());
        }
    }

    private void handleRejection(Meter meter, String approveStatus, UserModel user) {

        if (meterMapper.rejectedMeterVersion("Rejected", meter.getMeterNumber(), meter.getUpdatedAt(), user.getId()) == 0) {
            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "ed failed");
        }

        if(meter.getMeterStage().trim().equalsIgnoreCase("Pending-created")){

            int res = meterMapper.removeMeter(meter.getMeterNumber());

            if ("md".equalsIgnoreCase(meter.getMeterClass())) {
                 res = meterMapper.removeMDMeterInfo(meter.getMeterId());
            }

            if ("prepaid".equalsIgnoreCase(meter.getMeterCategory())) {
                 res = meterMapper.removeSmartMeterInfo(meter.getMeterId());
            }

            if(res == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " failed to delete");

        } else if(meter.getMeterStage().trim().equalsIgnoreCase("Pending-edited")
                || meter.getMeterStage().trim().equalsIgnoreCase("Pending-allocated")){
            meter.setMeterStage("Created");
            meter.setStatus("Active");
            int u = meterMapper.updateMeter(meter.getMeterStage(), meter.getMeterNumber(), meter.getUpdatedAt(), meter.getStatus());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " deactivation failed");
        }
        else if(meter.getMeterStage().trim().equalsIgnoreCase("Pending-migrated")
                || meter.getMeterStage().trim().equalsIgnoreCase("Pending-detached")){
            meter.setMeterStage("Active");
            meter.setStatus("Active");
            int u = meterMapper.updateMeter(meter.getMeterStage(), meter.getMeterNumber(), meter.getUpdatedAt(), meter.getStatus());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " deactivation failed");
        }
        else if(meter.getMeterStage().trim().equalsIgnoreCase("Pending-assigned")){

            meter.setMeterStage("Unassigned");
            meter.setStatus("Active");
            int u = meterMapper.updateMeter(meter.getMeterStage(), meter.getMeterNumber(), meter.getUpdatedAt(), meter.getStatus());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " deactivation failed");

        } else {
            meter.setMeterStage("Active");
            meter.setStatus("Active");
            int u = meterMapper.updateMeter(meter.getMeterStage(), meter.getMeterNumber(), meter.getUpdatedAt(), meter.getStatus());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " update failed");
        }

    }

    private AuditLog buildAuditLog(UserModel creator, String description, String type, Meter createdEntity, Map<String, String> metadata) {
        AuditLog log = new AuditLog();
        log.setCreator(creator);
        log.setDescription(description);
        log.setType(type);
        log.setCreatedMeter(createdEntity);
        log.setIpAddress(metadata.get("ipAddress"));
        log.setUserAgent(metadata.get("userAgent"));
        log.setEndpoint(metadata.get("endpoint"));
        log.setHttpMethod(metadata.get("httpMethod"));
        return log;
    }

    private void saveAuditLog(AuditLog auditLog, UserModel user, String ip, String userAgent, Meter updatedMeter, Meter meter, String approveStatus) {
        String description = capitalizeFirstLetter(meter.getMeterNumber()) + " meter " + approveStatus;
        auditLog.setCreator(user);
        auditLog.setDescription(description);
        auditLog.setType(meterName);
        auditLog.setUserAgent(userAgent);
        auditLog.setIpAddress(ip);
        auditLog.setCreatedMeter(updatedMeter);
        auditRepository.save(auditLog);
    }

// ---------------- Utility ----------------

    private boolean isApprove(String status) {
        return status != null && status.toLowerCase().contains("approve");
    }

    private boolean isReject(String status) {
        return status != null && status.toLowerCase().contains("reject");
    }

    //    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveExceptionLog(ExceptionErrorLogs errorLog, Exception ex) {
        errorLog.setDescription("Error occurred while creating meter");
        errorLog.setError_message(ex.getMessage());
        errorLog.setError(ex.toString());
        exceptionAuditRepository.save(errorLog);
    }

    @Transactional
    @Override
    public Map<String, Object> allocateMeter(String meterNumber, String regionId) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");

            UserModel um = handleUserValidation();

            Meter verifyMeter2 = meterMapper.getVersionMeter(um.getOrgId(), null, meterNumber);
//                    .getVersionMeterNumber(um.getOrgId(), meterNumber);
            if(verifyMeter2 != null) {
                throw new GlobalExceptionHandler.NotFoundException(meterNumber + " meter have a pending record that needs approval");
            }

            Meter verifyMeter = meterMapper.getMeter(um.getOrgId(), null, meterNumber, null);
            if(verifyMeter == null){
                throw new GlobalExceptionHandler.NotFoundException("Meter " + status.getNotFoundDesc());
            }

            // verify if node (organization id) exist
            RegionBhubServiceCenter node = nodeMapper.verifyNode(regionId);
            if(node == null){
                throw new GlobalExceptionHandler.NotFoundException("Node " + status.getNotFoundDesc());
            }

            verifyMeter.setCreatedAt(new Date());
            verifyMeter.setUpdatedAt(new Date());

            String desc = meterNumber + " meter allocated to " + regionId;

            //Allocate meter
            int result1 = meterMapper.allocateMeterVersion(verifyMeter, node.getNodeId(), um.getId(), desc);
            if(result1 == 0){
                throw new GlobalExceptionHandler.NotFoundException("Meter allocation failed");
            }

//             Allocate meter
//            meterMapper.allocateMeter(meterNumber, node.getNodeId(), um.getOrgId());


            //fetch meter from the database
            Meter meter = meterMapper.getVersionMeter(um.getOrgId(), null, meterNumber);
            um.setPassword("");
//            String desc = capitalizeFirstLetter(meter.getMeterNumber() + " allocated " + node.getName());
            //save to audit (mongodb)
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setType(meterName);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setCreatedMeter(meter);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(), meterName + " allocated successfully" , "");

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

    private void handleAddCache(Meter meter) {
        meterCache.remove(meter.getId().toString()+"_"+meter.getOrgId());
        for (String key : auditCache.keySet()) {
            if (key.startsWith("grid_flex_audit_log_page_")) {
                auditCache.remove(key);
            }
        }
        for (String key : meterCache.keySet()) {
            if (key.startsWith("meters_"+meter.getOrgId())) {
                meterCache.remove(key);
            }
        }
        meterCache.put(meter.getId().toString()+"_"+meter.getOrgId(), meter);  // Cache updated or deleted entity
    }

    private String handleGetAccountNumber(){
        String accountNumber;
        accountNumber = String.valueOf(Instant.now().getEpochSecond());
        return accountNumber;
    }

    private String handleGetVirtualMeter(){
        String virtualMeterNo;
        long timePart = Instant.now().toEpochMilli(); // 13 digits
        int randomPart = new Random().nextInt(90) + 10; // 2-digit number
        virtualMeterNo = "V" + String.valueOf(timePart).substring(0, 11) + randomPart;
        return virtualMeterNo;
    }

    private String buildChangeDescription(Meter oldMeter, Meter newMeter) {
        StringBuilder changes = new StringBuilder("Edited meter ");

        if (!Objects.equals(oldMeter.getMeterNumber(), newMeter.getMeterNumber())) {
            changes.append(String.format("number: '%s' → '%s' ", oldMeter.getMeterNumber(), newMeter.getMeterNumber()));
        }

        if (!Objects.equals(oldMeter.getSimNumber(), newMeter.getSimNumber())) {
            changes.append(String.format("sim number: '%s' → '%s' ", oldMeter.getSimNumber(), newMeter.getSimNumber()));
        }

        if (!Objects.equals(oldMeter.getMeterCategory(), newMeter.getMeterCategory())) {
            changes.append(String.format("category: '%s' → '%s' ", oldMeter.getMeterCategory(), newMeter.getMeterCategory()));
        }

        if (!Objects.equals(oldMeter.getMeterClass(), newMeter.getMeterClass())) {
            changes.append(String.format("class: '%s' → '%s' ", oldMeter.getMeterClass(), newMeter.getMeterClass()));
        }

        if (!Objects.equals(oldMeter.getMeterType(), newMeter.getMeterType())) {
            changes.append(String.format("type: '%s' → '%s' ", oldMeter.getMeterType(), newMeter.getMeterType()));
        }

        if (!Objects.equals(oldMeter.getOldSgc(), newMeter.getOldSgc())) {
            changes.append(String.format("old sgc: '%s' → '%s' ", oldMeter.getOldSgc(), newMeter.getOldSgc()));
        }

        if (!Objects.equals(oldMeter.getNewSgc(), newMeter.getNewSgc())) {
            changes.append(String.format("new sgc: '%s' → '%s' ", oldMeter.getNewSgc(), newMeter.getNewSgc()));
        }

        if (!Objects.equals(oldMeter.getOldKrn(), newMeter.getOldKrn())) {
            changes.append(String.format("old krn: '%s' → '%s' ", oldMeter.getOldKrn(), newMeter.getOldKrn()));
        }

        if (!Objects.equals(oldMeter.getNewKrn(), newMeter.getNewKrn())) {
            changes.append(String.format("new krn: '%s' → '%s' ", oldMeter.getNewKrn(), newMeter.getNewKrn()));
        }

        if (!Objects.equals(oldMeter.getOldTariffIndex(), newMeter.getOldTariffIndex())) {
            changes.append(String.format("old tariff index: '%s' → '%s' ", oldMeter.getOldTariffIndex(), newMeter.getOldTariffIndex()));
        }

        if (!Objects.equals(oldMeter.getNewTariffIndex(), newMeter.getNewTariffIndex())) {
            changes.append(String.format("new tariff index: '%s' → '%s' ", oldMeter.getNewTariffIndex(), newMeter.getNewTariffIndex()));
        }
        return changes.toString();
    }

    private String buildMDMeterInfoChangeDescription(MDMeterInfo oldMeter, MDMeterInfo newMeter) {
        StringBuilder changes = new StringBuilder("Edited MD meter ");

        if (!Objects.equals(oldMeter.getCtRatioNum(), newMeter.getCtRatioNum())) {
            changes.append(String.format("ct ratio num: '%s' → '%s' ", oldMeter.getCtRatioNum(), newMeter.getCtRatioNum()));
        }

        if (!Objects.equals(oldMeter.getCtRatioDenom(), newMeter.getCtRatioDenom())) {
            changes.append(String.format("ct ratio denom: '%s' → '%s' ", oldMeter.getCtRatioDenom(), newMeter.getCtRatioDenom()));
        }

        if (!Objects.equals(oldMeter.getVoltRatioNum(), newMeter.getVoltRatioNum())) {
            changes.append(String.format("volt ratio num: '%s' → '%s' ", oldMeter.getVoltRatioNum(), newMeter.getVoltRatioNum()));
        }

        if (!Objects.equals(oldMeter.getVoltRatioDenom(), newMeter.getVoltRatioDenom())) {
            changes.append(String.format("volt ratio denom: '%s' → '%s' ", oldMeter.getVoltRatioDenom(), newMeter.getVoltRatioDenom()));
        }

        if (!Objects.equals(oldMeter.getMultiplier(), newMeter.getMultiplier())) {
            changes.append(String.format("multiplier: '%s' → '%s' ", oldMeter.getMultiplier(), newMeter.getMultiplier()));
        }

        if (!Objects.equals(oldMeter.getMeterRating(), newMeter.getMeterRating())) {
            changes.append(String.format("reading: '%s' → '%s' ", oldMeter.getMeterRating(), newMeter.getMeterRating()));
        }

        if (!Objects.equals(oldMeter.getInitialReading(), newMeter.getInitialReading())) {
            changes.append(String.format("initial reading: '%s' → '%s' ", oldMeter.getInitialReading(), newMeter.getInitialReading()));
        }

        if (!Objects.equals(oldMeter.getDial(), newMeter.getDial())) {
            changes.append(String.format("dial: '%s' → '%s' ", oldMeter.getDial(), newMeter.getDial()));
        }

        if (!Objects.equals(oldMeter.getLatitude(), newMeter.getLatitude())) {
            changes.append(String.format("latitude: '%s' → '%s' ", oldMeter.getLatitude(), newMeter.getLatitude()));
        }

        if (!Objects.equals(oldMeter.getLongitude(), newMeter.getLongitude())) {
            changes.append(String.format("longitude: '%s' → '%s' ", oldMeter.getLongitude(), newMeter.getLongitude()));
        }

        return changes.toString();
    }

    private String buildSmartMeterInfoChangeDescription(SmartMeterInfo oldMeter, SmartMeterInfo newMeter) {
        StringBuilder changes = new StringBuilder("Edited smart meter ");

        if (!Objects.equals(oldMeter.getMeterModel(), newMeter.getMeterModel())) {
            changes.append(String.format("model: '%s' → '%s' ", oldMeter.getMeterModel(), newMeter.getMeterModel()));
        }

        if (!Objects.equals(oldMeter.getAuthentication(), newMeter.getAuthentication())) {
            changes.append(String.format("authentication: '%s' → '%s' ", oldMeter.getAuthentication(), newMeter.getAuthentication()));
        }

        if (!Objects.equals(oldMeter.getPassword(), newMeter.getPassword())) {
            changes.append(String.format("password: '%s' → '%s' ", oldMeter.getPassword(), newMeter.getPassword()));
        }

        if (!Objects.equals(oldMeter.getProtocol(), newMeter.getProtocol())) {
            changes.append(String.format("protocol: '%s' → '%s' ", oldMeter.getProtocol(), newMeter.getProtocol()));
        }

        return changes.toString();
    }
}


//    @Transactional(rollbackFor = Exception.class)
//    @Override
//    public Map<String, Object> approve(UUID meterVersionId, String approveStatus) throws MissingServletRequestParameterException {
//        AuditLog auditLog = new AuditLog();
//        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
//
//        try {
//            // Get client metadata
//            String ipAddress = getClientIp(httpServletRequest);
//            String userAgent = httpServletRequest.getHeader("User-Agent");
//            UserModel user = handleUserValidation();
//
//            // Fetch meter version
//            Meter meter = meterMapper.findByIdVersion(meterVersionId, user.getOrgId());
//            if (meter == null) {
//                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getNotFoundDesc());
//            }
//
//            // Set common properties
//            meter.setOrgId(user.getOrgId());
//            meter.setApproveBy(user.getId());
//
//            if (meter.getMdMeterInfo() != null) {
//                meter.getMdMeterInfo().setMeterId(meterVersionId);
//                meter.getMdMeterInfo().setOrgId(user.getOrgId());
//                meter.getMdMeterInfo().setApproveBy(user.getId());
//            }
//
//            // Set meter status based on assignment
//            if (meter.getCustomerId() != null) {
//                meter.setStatus(true);
//                meter.setActivateStatus(true);
//            } else {
//                meter.setStatus(false);
//                meter.setActivateStatus(false);
//            }
//
//            // Approval logic
//            if (approveStatus != null && approveStatus.contains("approve")) {
//                meter.setApproveStatus("approved");
//
//                int approvedMeter = meterMapper.approvedMeterVersion(meter);
//                if (approvedMeter == 0) {
//                    throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "d " + status.getUpdateFailureDesc());
//                }
//                Meter resp = meterMapper.findByMeterNumber(meter.getMeterNumber(), user.getOrgId());
//                if (resp != null) {
//                    int updatedMeter = meterMapper.approveMeter(meter);
//                    if ( updatedMeter == 0) {
//                        throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "d " + status.getUpdateFailureDesc());
//                    }
//
//                    // Approve MD meter info if applicable
//                    if ("md".equalsIgnoreCase(meter.getMeterClass()) && meter.getMdMeterInfo() != null) {
//                        meter.getMdMeterInfo().setApproveStatus("approved");
//                        int mdApproval = meterMapper.approveMDMeterInfoVersion(meter.getMdMeterInfo());
////                        int updateMdApproval = meterMapper.insertMDMeterInfo(meter.getMdMeterInfo());
//                        if (mdApproval == 0) {
//                            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());
//                        }
//                    }
//
//                    // Approve prepaid meter info if applicable
//                    if ("prepaid".equalsIgnoreCase(meter.getMeterCategory()) && meter.getPaymentMode() != null) {
//                        meter.getPaymentMode().setApproveStatus("approved");
//                        int prepaidApproval = meterMapper.approvePrepaidMeterVersion(meter.getPaymentMode());
//                        int updatePrepaidApproval = meterMapper.updatePrepaidMeterVersion(meter.getPaymentMode());
//                        if (prepaidApproval == 0 || updatePrepaidApproval == 0) {
//                            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "d " + status.getUpdateFailureDesc());
//                        }
//                    }
//                } else {
//                    int result1 = meterMapper.insertMeter(meter);
//                    if ( result1 == 0) {
//                        throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "d " + status.getFailDesc());
//                    }
//                    if ("md".equalsIgnoreCase(meter.getMeterClass()) && meter.getMdMeterInfo() != null) {
//                        int updateMdApproval = meterMapper.insertMDMeterInfo(meter.getMdMeterInfo());
//                        if (updateMdApproval == 0) {
//                            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getFailDesc());
//                        }
//                    }
//                    // Approve prepaid meter info if applicable
//                    if ("prepaid".equalsIgnoreCase(meter.getMeterCategory()) && meter.getPaymentMode() != null) {
//                        meter.getPaymentMode().setApproveStatus("approved");
////                        int prepaidApproval = meterMapper.approvePrepaidMeterVersion(meter.getPaymentMode());
//                        int updatePrepaidApproval = meterMapper.insertPrepaidMeterVersion(meter.getPaymentMode());
//                        if (updatePrepaidApproval == 0) {
//                            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "d " + status.getFailDesc());
//                        }
//                    }
//                }
//
//
//
//            } else if (approveStatus != null && approveStatus.contains("reject")) {
//                meter.setApproveStatus("rejected");
//
//                meterMapper.removeMeter(meter);
//                int rejectedMeter = meterMapper.rejectedMeterVersion(meter);
//                if (rejectedMeter == 0) {
//                    throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "ed failed");
//                }
//
//                // Reject MD meter info if applicable
//                if ("md".equalsIgnoreCase(meter.getMeterClass()) && meter.getMdMeterInfo() != null) {
//                    meter.getMdMeterInfo().setApproveStatus("rejected");
//                    int mdRejection = meterMapper.rejectedMDMeterInfoVersion(meter.getMdMeterInfo());
//                    if (mdRejection == 0) {
//                        throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());
//                    }
//                }
//
//                // Reject prepaid meter info if applicable
//                if ("prepaid".equalsIgnoreCase(meter.getMeterCategory()) && meter.getPaymentMode() != null) {
//                    meter.getPaymentMode().setApproveStatus("rejected");
//                    int prepaidRejection = meterMapper.rejectPrepaidMeterVersion(meter.getPaymentMode());
//                    if (prepaidRejection == 0) {
//                        throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "d failed");
//                    }
//                }
//
//            } else {
//                throw new MissingServletRequestParameterException("approveStatus", "String");
//            }
//            // Prepare audit log
//            Meter updatedMeter = meterMapper.findById(meter.getId(), user.getOrgId());
//            user.setPassword(null); // remove password from audit
//
//            String description = capitalizeFirstLetter(meter.getMeterNumber()) + " meter " + approveStatus;
//
//            auditLog.setCreator(user);
//            auditLog.setDescription(description);
//            auditLog.setType(meterName);
//            auditLog.setUserAgent(userAgent);
//            auditLog.setIpAddress(ipAddress);
//            auditLog.setCreatedMeter(updatedMeter);
//            auditRepository.save(auditLog);
//
//            return ResponseMap.response(
//                    status.getSuccessCode(),
//                    meter.getMeterNumber() + " " + meterName + " " + capitalizeFirstLetter(approveStatus) + " Successfully",
//                    ""
//            );
//
//        } catch (Exception ex) {
//            log.error("Error occurred while approving/rejecting meter: {}", ex.getMessage(), ex);
//            errorLog.setDescription("Error occurred while processing meter approval");
//            errorLog.setError_message(ex.getMessage().trim());
//            errorLog.setError(ex.toString().trim());
//            exceptionAuditRepository.save(errorLog);
//            throw ex;
//        }
//    }
///---------------------------

//    @Transactional
//    @Override
//    public Map<String, Object> createMeter(Meter request) {
//        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
//        AuditLog auditNotificationDTO = new AuditLog();
//        try {
//            String ipAddress = getClientIp(httpServletRequest);
//            String userAgent = httpServletRequest.getHeader("User-Agent");
//            UserModel um = handleUserValidation();
//            String desc = capitalizeFirstLetter("Meter created");
//            Manufacturer manufacturer = meterMapper.getMeterManufacturer(request.getMeterManufacturer());
//            if(manufacturer == null) {
//                throw new GlobalExceptionHandler.NotFoundException("Meter manufacturer not found");
//            }//.getMeter()
//            Meter  meter = meterMapper.getMeter(um.getOrgId(), null, request.getMeterNumber(), null);
////                    getMeterNumber(um.getOrgId(), request.getMeterNumber());
//            if(meter != null) {
//                throw new GlobalExceptionHandler.NotFoundException("Meter already exist");
//            }
//
//            if(!request.getMeterClass().equalsIgnoreCase("md")
//                    && !request.getMeterClass().equalsIgnoreCase("single-phase")
//                    && !request.getMeterClass().equalsIgnoreCase("three-phase")){
//                throw new GlobalExceptionHandler.NotFoundException("Meter class can only allow one of three value; MD, single-phase or three-phase");
//            }
//
//            // set default state to be In-stock
//            request.setStatus(false);
//            request.setOrgId(um.getOrgId());
//            request.setType("NON VIRTUAL");
//            request.setApproveStatus("pending");
//            request.setDescription(desc);
//            request.setCreatedBy(um.getId());
//            request.setActivateStatus(false);
//
////            int result1;
//            int result2;
////            //insert meter to databases
////            result1 = meterMapper.insertMeter(request);
////            request.setMeterId(request.getId());
//            result2 = meterMapper.insertMeterVersion(request);
////            request.setMeterId(request.getId());
//            if(result2 == 0) {
//                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getRegFailureDesc());
//            }
//
//            // insert MD meter information
//            if(request.getMeterClass().equalsIgnoreCase("md")) {
//                request.getMdMeterInfo().setMeterId(request.getId());
//                request.getMdMeterInfo().setOrgId(um.getOrgId());
//                request.getMdMeterInfo().setCreatedBy(um.getId());
//                String description = capitalizeFirstLetter("MD Meter Info created");
//                request.getMdMeterInfo().setDescription(description);
//                int mdResult2 = meterMapper.insertMDMeterInfoVersion(request.getMdMeterInfo());
//                if (mdResult2 == 0) {
//                    throw new GlobalExceptionHandler.NotFoundException(meterName + " MD data " + status.getRegFailureDesc());
//                }
//            }
//
//            if(request.getSmartStatus()){
//                request.getSmartMeter().setMeterId(request.getId());
//                request.getSmartMeter().setOrgId(um.getOrgId());
//                request.getSmartMeter().setCreatedBy(um.getId());
//                String description = capitalizeFirstLetter("Smart Meter Info created");
//                request.getSmartMeter().setPassword(passwordEncoder.encode(request.getSmartMeter().getPassword()));
//                request.getMdMeterInfo().setDescription(description);
//                int result = meterMapper.insertSmartMeterInfoVersion(request.getSmartMeter());
//                if (result == 0) {
//                    throw new GlobalExceptionHandler.NotFoundException(meterName + " Smart data " + status.getRegFailureDesc());
//                }
//            }
//
//            UUID meterId = request.getId();
//
//            //fetch meter from the database
//            Meter newMeter = meterMapper.findByIdVersion(meterId, request.getOrgId());
//
//            //call cache method
////            handleAddCache(meter);
//
//            //save to audit (mongodb)
//            auditNotificationDTO.setCreator(um);
//            auditNotificationDTO.setDescription(desc);
//            auditNotificationDTO.setUserAgent(userAgent);
//            auditNotificationDTO.setIpAddress(ipAddress);
//            auditNotificationDTO.setType(meterName);
//            auditNotificationDTO.setCreatedMeter(newMeter);
//            auditRepository.save(auditNotificationDTO);
//
//            return ResponseMap.response(status.getSuccessCode(), meterName + " " + status.getRegDesc(), "");
//        } catch (Exception exception) {
//            log.error("Error filtering / fetching users: {}", exception.getMessage(), exception);
//            exception.printStackTrace();
//            exceptionErrorLogs.setDescription("Error occurred while filtering users");
//            exceptionErrorLogs.setError_message(exception.getMessage());
//            exceptionErrorLogs.setError(exception.toString());
//            exceptionAuditRepository.save(exceptionErrorLogs);
//            throw exception;
//        }
//
//    }