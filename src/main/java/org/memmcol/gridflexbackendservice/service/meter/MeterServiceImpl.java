package org.memmcol.gridflexbackendservice.service.meter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.mapper.CustomerMapper;
import org.memmcol.gridflexbackendservice.mapper.MeterMapper;
import org.memmcol.gridflexbackendservice.mapper.NodeMapper;
import org.memmcol.gridflexbackendservice.mapper.TariffMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditAdjustVersion;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.meter.*;
import org.memmcol.gridflexbackendservice.model.node.RegionBhubServiceCenter;
import org.memmcol.gridflexbackendservice.model.node.SubStationTransformerFeederLine;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.model.vend.MeterView;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.service.customer.CustomerServiceImpl;
import org.memmcol.gridflexbackendservice.util.GenericResp;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.memmcol.gridflexbackendservice.components.GenericHandler.capitalizeFirstLetter;
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
    private TariffMapper tariffMapper;

    @Autowired
    private GenericHandler genericHandler;

    @Autowired
    private HttpServletRequest httpServletRequest;

    private String meterName = "Meter";

    private final IMap<String, Object> meterCache;

    private final IMap<String, Object> auditCache;

    @Autowired
    private CustomerMapper customerMapper;


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
            AuditLog auditLog = buildAuditLog(user, "Meter created", meterName, newMeter, metadata, "");
            auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(), meterName + " " + status.getRegDesc(), "");

        } catch (Exception ex) {
            log.error("Error creating meter: {}", ex.getMessage(), ex);
            genericHandler.logIncidentReport("Creating meter service failed");
            genericHandler.logAndSaveException(ex, "creating meter");
            throw ex;
        }
    }

    private void validateMeterRequest(Meter request, UserModel user) {
        Manufacturer manufacturer = meterMapper.getMeterManufacturer(request.getMeterManufacturer());
        if (manufacturer == null) {
            throw new GlobalExceptionHandler.NotFoundException("Meter manufacturer not found");
        }

        Meter existing = meterMapper.getMeter(user.getOrgId(), null, request.getMeterNumber().trim(), null, null, request.getSimNumber());
        if (existing != null) {
            throw new GlobalExceptionHandler.NotFoundException("Meter Number ("+existing.getMeterNumber()+" or Sim Number "+existing.getSimNumber()+") "+status.getExistDesc());
        }
//        if (existing.getSimNumber().equalsIgnoreCase(request.getSimNumber())){
//            throw new GlobalExceptionHandler.NotFoundException("Sim Number "+status.getExistDesc());
//        }

        String clazz = request.getMeterClass();
        if (!clazz.equalsIgnoreCase("md") &&
                !clazz.equalsIgnoreCase("single-phase") &&
                !clazz.equalsIgnoreCase("three-phase")) {
            throw new GlobalExceptionHandler.NotFoundException(
                    "Meter class must be one of: MD, single-phase, or three-phase");
        }

        // Default states
        request.setStatus("Active");
        request.setMeterStage("Pending-created");
        request.setOrgId(user.getOrgId());
        request.setType("NON-VIRTUAL");
        request.setDescription(capitalizeFirstLetter("Newly Added"));
        request.setCreatedBy(user.getId());
    }

    private void insertMDMeterInfo(Meter request, UserModel user) {
        request.getMdMeterInfo().setMeterId(request.getId());
        request.getMdMeterInfo().setOrgId(user.getOrgId());
        request.getMdMeterInfo().setCreatedBy(user.getId());
        request.getMdMeterInfo().setMeterStage("Pending-created");
        request.getMdMeterInfo().setDescription("Newly Added");

        int inserted = meterMapper.insertMDMeterInfoVersion(request.getMdMeterInfo());
        if (inserted == 0) {
            throw new GlobalExceptionHandler.NotFoundException(meterName + " MD data " + status.getRegFailureDesc());
        }
    }

    private void insertSmartMeterInfo(Meter request, UserModel user) {
        request.getSmartMeterInfo().setMeterId(request.getId());
        request.getSmartMeterInfo().setOrgId(user.getOrgId());
        request.getSmartMeterInfo().setCreatedBy(user.getId());
        request.getSmartMeterInfo().setMeterStage("Pending-created");
        request.getSmartMeterInfo().setDescription("Newly Added");
//        request.getSmartMeterInfo().setPassword(passwordEncoder.encode(request.getSmartMeterInfo().getPassword()));

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

            String MDDesc = "";
            String SmartDesc = "";
            String MeterDesc = "";
            String meterStage = "Pending-edited";

            // Prepare meter update data
            request.setType("NON-VIRTUAL");
            request.setStatus("Active");
            request.setMeterStage(meterStage);
            MeterDesc = buildChangeDescription(existingMeter, request);
            request.setDescription("Meter edited");
            request.setCreatedBy(user.getId());

            request.setNodeId(existingMeter.getNodeId());
            request.setDss(existingMeter.getDss());
            request.setCin(existingMeter.getCin());
            request.setAccountNumber(existingMeter.getAccountNumber());
            request.setOrgId(user.getOrgId());
            request.setMeterId(existingMeter.getMeterId());
            request.setCreatedBy(user.getId());
            request.setCustomerId(existingMeter.getCustomerId());
            request.setTariff(existingMeter.getTariff());
            request.setMeterId(existingMeter.getId());

            // Insert or update meter version
            int result;
            if (existingMeter.getMeterStage().contains("Pending") || existingMeter.getStatus().contains("Pending")) {
                throw new GlobalExceptionHandler.NotFoundException(existingMeter.getMeterNumber()+ " have a pending state that needs to be cleared");
            }
            if(existingMeter.getStatus().equalsIgnoreCase("Deactivated")){
                throw new GlobalExceptionHandler.NotFoundException(existingMeter.getMeterNumber()+ " is deactivated and cannot be edited");
            }

            int res = meterMapper.updateMeter(meterStage, request.getId(), request.getUpdatedAt(), request.getStatus());
            result = meterMapper.insertMeterVersion(request);
            if (result == 0 || res == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());

            // Handle MD meter-specific logic
            if (request.getMdMeterInfo() != null) {
                UUID meterId = request.getId();
                request.getMdMeterInfo().setMeterId(meterId);
                request.getMdMeterInfo().setOrgId(user.getOrgId());
                request.getMdMeterInfo().setMeterStage(meterStage);
                request.getMdMeterInfo().setCreatedBy(user.getId());
                MDDesc = buildMDMeterInfoChangeDescription(existingMeter.getMdMeterInfo(), request.getMdMeterInfo());
                request.getMdMeterInfo().setDescription("Pending edited");

                int mdResult2 = meterMapper.insertMDMeterInfoVersion(request.getMdMeterInfo());
                    if (mdResult2 == 0) {
                        throw new GlobalExceptionHandler.NotFoundException(meterName + " MD data " + status.getUpdateFailureDesc());
                    }
            }

            // Handle smart meter-specific logic
            if (request.getSmartMeterInfo() != null) {
                UUID meterId = request.getId();
                request.getSmartMeterInfo().setMeterId(meterId);
                request.getSmartMeterInfo().setOrgId(user.getOrgId());
                request.getSmartMeterInfo().setMeterStage(meterStage);
                request.getSmartMeterInfo().setCreatedBy(user.getId());
                SmartDesc = buildSmartMeterInfoChangeDescription(existingMeter.getSmartMeterInfo(), request.getSmartMeterInfo());
                request.getSmartMeterInfo().setDescription("Pending edited");
                int mdResult2 = meterMapper.insertSmartMeterInfoVersion(request.getSmartMeterInfo());
                if (mdResult2 == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(meterName + " MD data " + status.getUpdateFailureDesc());
                }
            }

            String desc = MeterDesc + "," + MDDesc + ","+ SmartDesc;

            // Fetch updated meter and log audit
            Meter updatedMeter = meterMapper.findByIdVersion(request.getId(), user.getOrgId());
            AuditLog auditLog = buildAuditLog(user, desc, meterName, updatedMeter, metadata, "");
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
            String meterClass, String category, String state, String createdAt, String customerId, String type) {
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
            if (type.trim().equalsIgnoreCase("pending-state")) {
                meters = meterMapper.getMetersVersion(um.getOrgId(), page, size);
            } else if (type.trim().equalsIgnoreCase("inventory")) {
                meters = meterMapper.getInventoryMeters(um.getOrgId(), page, size);
            } else if (type.trim().equalsIgnoreCase("allocated")) {
                meters = meterMapper.getAllocatedMeters(um.getOrgId(), page, size);
            } else if (type.trim().equalsIgnoreCase("assigned")) {
                meters = meterMapper.getAssignedMeters(um.getOrgId(),  page, size);
            } else if (type.trim().equalsIgnoreCase("virtual")) {
                meters = meterMapper.getAssignedVirtualMeters(um.getOrgId(), page, size);
            } else {
                meters = meterMapper.getMeters(um.getOrgId(), page, size);
            }

//            System.out.println(">>>>>>>>>>::: here: "+meters.get(0).getMeterNumber());
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
//                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .isBefore(date);
                });
            }

            List<Meter> filteredMeters = meterStream.toList();

            // Pagination logic
            int totalMeters = filteredMeters.size();
            List<Meter> paginatedMeters;

//             if (size <= 0) {
//                 paginatedMeters = filteredMeters; // Return all users
//                 page = 0;
//             } else {
//                 int fromIndex = Math.min(page * size, totalMeters);
//                 int toIndex = Math.min(fromIndex + size, totalMeters);
//                 paginatedMeters = filteredMeters.subList(fromIndex, toIndex);
//             }

                paginatedMeters = filteredMeters; // Return all users

            int totalPages = size <= 0 ? 1 : (int) Math.ceil((double) totalMeters / size);

            // Prepare response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedMeters);
            response.put("totalData", totalMeters);
            response.put("page", page);
            response.put("size", size);

//             response.put("totalPages", totalPages);

            response.put("totalPages", (int) Math.ceil((double) totalMeters / size));
  

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
    public Map<String, Object> getSingleMeter(UUID meterId, String meterNumber, String accountNumber, UUID meterVersionId, String versionMeterNumber, String cin) {
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
                meter = meterMapper.getMeter(um.getOrgId(), meterId, meterNumber, accountNumber, cin, "");
            }

            if(accountNumber != null){
                meter = meterMapper.getMeter(um.getOrgId(), meterId, meterNumber, accountNumber, cin, "");
            }

            if(meterId != null){
                meter = meterMapper.getMeter(um.getOrgId(), meterId, meterNumber, accountNumber, cin, "");
            }

            if(cin != null){
                meter = meterMapper.getMeter(um.getOrgId(), meterId, meterNumber, accountNumber, cin, "");
            }

            if(versionMeterNumber != null){
                meter = meterMapper.getVersionMeter(um.getOrgId(), meterVersionId, versionMeterNumber, cin);
            }

            if(meterVersionId != null){
                meter = meterMapper.getVersionMeter(um.getOrgId(), meterVersionId, versionMeterNumber, cin);
            }

//            handleAddCache(meter);

            return ResponseMap.response(status.getSuccessCode(),  meterName + " " + status.getDesc(), meter);
        } catch (Exception exception) {
            log.error("Error occurred while fetching feeder lines [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Editing meter service failed");
            genericHandler.logAndSaveException(exception, "fetching meter");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> changeStatus(UUID meterId, Boolean state, String reason) throws MissingServletRequestParameterException {
        int result;
        try {
            // Gather client metadata
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel user = handleUserValidation();

            Meter meterById = meterMapper.findById(meterId, user.getOrgId());

            if(meterById == null) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getNotFoundDesc());
            }

            if(state && meterById.getMeterStage().equalsIgnoreCase("Assigned")){
                Tariff tariff = tariffMapper.getApproveTariff(meterById.getTariff());
                if(tariff == null){
                    throw new GlobalExceptionHandler.NotFoundException("Tariff is either not found, not approved or deactivated");
                }
            }

            if(state){
                Meter m = meterMapper.getMeterCin(user.getOrgId(), meterById.getAccountNumber(), meterById.getCin());
                if(m != null){
                    throw new GlobalExceptionHandler.NotFoundException(
                            "Activation failed because "+m.getMeterNumber()+
                                    " meter is active with the same CIN or account number");
                }
            }

            if(meterById.getMeterStage().contains("Pending") || meterById.getStatus().contains("Pending")){
                throw new GlobalExceptionHandler.NotFoundException("Meter have a pending state that needs to be cleared");
            }
            if(meterById.getStatus().contains("Deactivated") && !state){
                throw new GlobalExceptionHandler.NotFoundException("Meter already deactivated");
            }
            if(meterById.getStatus().contains("Active") && state){
                throw new GlobalExceptionHandler.NotFoundException("Meter already activated");
            }

            meterById.setStatus("Pending-"+(state ? "activated" : "deactivated"));
            meterById.setCreatedBy(user.getId());
            meterById.setMeterId(meterById.getId());
            meterById.setReason(reason);

            String changeDescription = buildChangeStatusDescription(meterById, state);
            meterById.setDescription(state ? "Meter Activated" : "Meter Deactivated");


            result = meterMapper.insertMeterVersion(meterById);
            if(result == 0){
                throw new GlobalExceptionHandler.NotFoundException("Meter " + status.getUpdateDesc());
            }

            int u = meterMapper.updateMeter(meterById.getMeterStage(), meterById.getId(), meterById.getUpdatedAt(), meterById.getStatus());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException("Meter" + (state ? " activated " : " deactivated ")+ "failed");
            Meter meter = meterMapper.getMeter(user.getOrgId(), meterById.getMeterId(), null, null, null, "");
            user.setPassword("");
//            handleAddCache(newTariff);
            AuditLog auditLog = buildAuditLog(user, changeDescription, meterName, meter, metadata, reason);
            auditRepository.save(auditLog);
            return ResponseMap.response(status.getSuccessCode(), meterName + (state ? " activated ": " deactivated ")+"successfully", "");

        } catch (Exception exception) {
            log.error("Error occurred while changing user status [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Editing meter service failed");
            genericHandler.logAndSaveException(exception, "changing meter state");
            throw exception;
        }
    }

    private String buildChangeStatusDescription(Meter oldMeter, Boolean status) {
        StringBuilder changes = new StringBuilder("Edited meter ");
        String oldState = oldMeter.getStatus().trim().equalsIgnoreCase("Active") ? "activated" : "deactivated";
        String newState = status ? "activated" : "deactivated";
        if (!Objects.equals(oldMeter.getStatus(), newState)) {
            changes.append(String.format("status: '%s' → '%s' ", oldState, newState));
        }

        return changes.toString();
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getManufacturers() {
        try {

            UserModel um = handleUserValidation();

            // Get all manufacturers
            List<Manufacturer> manufacturers = meterMapper.getManufacturers(um.getOrgId());

            return ResponseMap.response(status.getSuccessCode(),  status.getDesc(), manufacturers);
        } catch (Exception exception) {
            log.error("Error occurred while fetching feeder lines [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("fetching manufacturer service failed");
            genericHandler.logAndSaveException(exception, "fetching manufacturers");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> singleCustomer(String customerId) {
        try {

            UserModel um = handleUserValidation();
            String virtualMeterNo = handleGetVirtualMeter();
            String accountNumber = handleGetAccountNumber();

            // check if customer exist
            Customer isCustomer = meterMapper.findByCustomerId(customerId, um.getOrgId());
            if (isCustomer == null) {
                throw new GlobalExceptionHandler.NotFoundException("Customer is either not found");
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
            genericHandler.logIncidentReport("Fetching customer in meter service failed");
            genericHandler.logAndSaveException(exception, "fetching customer in meter");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> assignMeterToCustomer(AssignMeterToCustomer request) {
        try {
            // Gather client metadata
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel user = handleUserValidation();

            boolean meterStatus = meterMapper.hasAssignedMeter(user.getOrgId(), request.getMeterNumber());
            if(meterStatus) throw new GlobalExceptionHandler.NotFoundException("Meter ("+ request.getMeterNumber() +") already assigned to a customer");

            // Validate DSS
            SubStationTransformerFeederLine dss = meterMapper.verifyDssFeeder(request.getDssAssetId(), user.getOrgId());
            if (dss == null) {
                throw new GlobalExceptionHandler.NotFoundException("DSS " + status.getNotFoundDesc());
            }

            // Validate feeder line
            SubStationTransformerFeederLine feederLine = meterMapper.verifyDssFeeder(request.getFeederAssetId(), user.getOrgId());
            if (feederLine == null) {
                throw new GlobalExceptionHandler.NotFoundException("Feeder line " + status.getNotFoundDesc());
            }

            if(!dss.getParentId().equals(feederLine.getNodeId())){
                throw new GlobalExceptionHandler.NotFoundException("DSS ("+ request.getDssAssetId() +") " +
                        "provided does not belong to the feeder line ("+request.getFeederAssetId()+")");
            }

            Tariff tariff = tariffMapper.getApproveTariff(request.getTariffId());
            if(tariff == null){
                throw new GlobalExceptionHandler.NotFoundException("Tariff is either not found, not approved or deactivated");
            }

            Customer customer = meterMapper.getByCustomerId(request.getCustomerId());
            if(customer == null) throw new GlobalExceptionHandler.NotFoundException("Customer not found");

            request.setOrgId(user.getOrgId());
            request.setCreatedBy(user.getId());

            if(request.getMeterClass() == null) {

                // Validate main meter record
                Meter mainMeter = meterMapper.getMeter(user.getOrgId(), null, request.getMeterNumber(), null, null, request.getSimNumber());
                if (mainMeter == null) {
                    throw new GlobalExceptionHandler.NotFoundException("Meter " + status.getNotFoundDesc());
                }

                if (mainMeter.getMeterStage().contains("Pending") || mainMeter.getStatus().contains("Pending")) {
                    throw new GlobalExceptionHandler.NotFoundException("Meter has a pending record that needs to be cleared");
                }

                if (mainMeter.getStatus().contains("Deactivated")) {
                    throw new GlobalExceptionHandler.NotFoundException("Deactivated meter can not be assign");
                }

                // Validate node assignment
                if (mainMeter.getNodeId() == null) {
                    throw new GlobalExceptionHandler.NotFoundException(request.getMeterNumber() + " meter has not been allocated");
                }

                request.setOldKrn(mainMeter.getOldKrn());
                request.setNewKrn(mainMeter.getNewKrn());
                request.setOldSgc(mainMeter.getOldSgc());
                request.setNewSgc(mainMeter.getNewSgc());
                request.setType("NON-VIRTUAL");
                request.setOldTariffIndex(mainMeter.getOldTariffIndex());
                request.setNewTariffIndex(mainMeter.getNewTariffIndex());
                request.setMeterType(mainMeter.getMeterType());
                request.setMeterClass(mainMeter.getMeterClass());
                request.setMeterCategory(mainMeter.getMeterCategory());
                request.setSmartStatus(mainMeter.getSmartStatus());
                request.setMeterManufacturer(mainMeter.getMeterManufacturer());
                request.setMeterType(mainMeter.getMeterType());
                request.setMeterId(mainMeter.getId());
                request.setSimNumber(mainMeter.getSimNumber());
//                request.setMeterModel(mainMeter.getMeterModel());
            } else {
                request.setType("VIRTUAL");
            }

            Meter m = meterMapper.getMeterDuplicateCin(user.getOrgId(), request.getAccountNumber(), request.getCin());
            if(m != null ) {
                Map<String, Object> result = new HashMap<>();
                result.put("meter", m);
                throw new GlobalExceptionHandler.PartialFailureException(
                        "Meter already assigned to cin or account number",
                        result
                );
            }

            request.setNodeId(feederLine.getNodeId());
            request.setDss(dss.getNodeId());
            request.setOrgId(user.getOrgId());
            request.setCreatedBy(user.getId());

            handleMeterAssign(request);

            Meter meter = meterMapper.getVersionMeter(user.getOrgId(), null, request.getMeterNumber(), null);
            String description = "Meter assigned to customer " + request.getCustomerId();

            AuditLog auditLog = buildAuditLog(user, description, meterName, meter, metadata, "");
            auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(), "Meter assigned successfully", "");

        } catch (Exception exception) {
            log.error("Error occurred during meter assignment: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Assigning meter service failed");
            genericHandler.logAndSaveException(exception, "assigning meter");
            throw exception;
        }
    }

    private void handleMeterAssign(AssignMeterToCustomer request){
        UserModel user = handleUserValidation();
        // Assign meter to customer
        request.setDescription("Meter Assigned");
        request.setMeterStage("Pending-assigned");
        request.setStatus("Active");
        int customerAssignResult;
        int customerAssignResult1;
        if(request.getType().equalsIgnoreCase("NON-VIRTUAL")){
            request.setType("NON-VIRTUAL");
            customerAssignResult = meterMapper.assignedMeterToCustomer(request.getMeterStage(), request.getStatus(), request.getMeterId(), request.getUpdatedAt());
            customerAssignResult1 = meterMapper.assignedVersionMeterToCustomer(request);
            if(customerAssignResult == 0 || customerAssignResult1 == 0)
                throw new GlobalExceptionHandler.NotFoundException("Assigning meter to customer failed");

            // Handle prepaid meter assignment
            if ("prepaid".equalsIgnoreCase(request.getMeterCategory())) {
                request.setDescription("Payment mode assigned");
                int paymentModeResult = meterMapper.assignPaymentModeVersion(request);

                if (paymentModeResult == 0) {
                    throw new GlobalExceptionHandler.NotFoundException("Payment mode assignment failed");
                }
            }
        } else {
            request.setType("VIRTUAL");
            request.setMeterCategory("Postpaid");
            request.setSmartStatus(false);
            request.setSimNumber("VIRTUAL");
            request.setMeterType("Electricity");
            customerAssignResult = meterMapper.insertVirtualVersionMeterToCustomer(request);
            request.setMeterId(request.getId());
            customerAssignResult1 = meterMapper.assignedVirtualVersionMeterToCustomer(request);

            if(customerAssignResult == 0 || customerAssignResult1 == 0)
                throw new GlobalExceptionHandler.NotFoundException("Assigning virtual meter to customer failed");
        }

        if(request.getDebitCreditAdjust() != null){

            DebitCreditAdjustVersion debitCreditAdjustVersion =
                    meterMapper.getDebitAdjustmentByOldVersion(request.getDebitCreditAdjust().get(0).getMeterId());

            if(debitCreditAdjustVersion != null){
                throw new GlobalExceptionHandler.NotFoundException("Meter have a pending state that needs to be cleared");
            }

            int res = meterMapper.insertDebitCreditAdjVersion(request.getDebitCreditAdjust().get(0).getMeterId(), request.getMeterId(), request.getOrgId(), request.getCreatedAt(), true);
            if (res == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Debit credit adjustment update failed");
            }
        }

        request.setMeterId(request.getMeterId());
        int locationAssignResult = meterMapper.assignVersionMeterToLocation(request);

        if (locationAssignResult == 0) {
            throw new GlobalExceptionHandler.NotFoundException("Meter assignment to location failed");
        }

    }

    @Override
    public Map<String, Object> continueAssignMeter(AssignMeterToCustomer request) {
        int result;
        boolean state = false;
        try {
            // Gather client metadata
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel user = handleUserValidation();

            Meter meterById = meterMapper.findById(request.getMeterId(), user.getOrgId());
            if(meterById == null) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getNotFoundDesc());
            }

            if(meterById.getMeterStage().contains("Pending") || meterById.getStatus().contains("Pending")) {
                throw new GlobalExceptionHandler.NotFoundException("Meter have a pending state that needs to be cleared");
            }

            meterById.setStatus("Deactivated");
            meterById.setCreatedBy(user.getId());
            meterById.setMeterId(meterById.getId());
            meterById.setReason("Meter replacement");

            String changeDescription = buildChangeStatusDescription(meterById, state);
            meterById.setDescription("Meter Deactivated");

            // Deactivate old meter
            int u = meterMapper.updateMeter(meterById.getMeterStage(), meterById.getId(), meterById.getUpdatedAt(), meterById.getStatus());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException("Meter deactivated failed");
            Meter meter = meterMapper.getMeter(user.getOrgId(), meterById.getMeterId(), null, null, null, "");
            user.setPassword("");
//            handleAddCache(newTariff);
            AuditLog auditLog = buildAuditLog(user, changeDescription, meterName, meter, metadata, "Meter deactivated by replacement");
            auditRepository.save(auditLog);

            //Assign the new meter
            // Validate DSS
            SubStationTransformerFeederLine dss = meterMapper.verifyDssFeeder(request.getDssAssetId(), user.getOrgId());
            if (dss == null) {
                throw new GlobalExceptionHandler.NotFoundException("DSS " + status.getNotFoundDesc());
            }

            // Validate feeder line
            SubStationTransformerFeederLine feederLine = meterMapper.verifyDssFeeder(request.getFeederAssetId(), user.getOrgId());
            if (feederLine == null) {
                throw new GlobalExceptionHandler.NotFoundException("Feeder line " + status.getNotFoundDesc());
            }

            if(!dss.getParentId().equals(feederLine.getNodeId())){
                throw new GlobalExceptionHandler.NotFoundException("DSS ("+ request.getDssAssetId() +") " +
                        "provided does not belong to the feeder line ("+request.getFeederAssetId()+")");
            }

            Tariff tariff = tariffMapper.getApproveTariff(request.getTariffId());
            if(tariff == null){
                throw new GlobalExceptionHandler.NotFoundException("Tariff is either not found, not approved or deactivated" );
            }

            Customer customer = meterMapper.getByCustomerId(request.getCustomerId());
            if(customer == null) throw new GlobalExceptionHandler.NotFoundException("Customer not found");

            request.setOrgId(user.getOrgId());
            request.setCreatedBy(user.getId());

            if(meter.getDebitCreditAdjustInfo() != null){
                request.setDebitCreditAdjust(meter.getDebitCreditAdjustInfo());
            }

            if(request.getMeterClass() == null) {

                // Validate main meter record
                Meter mainMeter = meterMapper.getMeter(user.getOrgId(), null, request.getMeterNumber(), null, null, request.getSimNumber());
                if (mainMeter == null) {
                    throw new GlobalExceptionHandler.NotFoundException("Meter " + status.getNotFoundDesc());
                }

                if (mainMeter.getMeterStage().contains("Pending") || mainMeter.getStatus().contains("Pending")) {
                    throw new GlobalExceptionHandler.NotFoundException("Meter has a pending record that needs to be cleared");
                }

                if (mainMeter.getStatus().contains("Deactivated")) {
                    throw new GlobalExceptionHandler.NotFoundException("Deactivated meter can not be assign");
                }

                // Validate node assignment
                if (mainMeter.getNodeId() == null) {
                    throw new GlobalExceptionHandler.NotFoundException(request.getMeterNumber() + " meter has not been allocated");
                }

                request.setOldKrn(mainMeter.getOldKrn());
                request.setNewKrn(mainMeter.getNewKrn());
                request.setOldSgc(mainMeter.getOldSgc());
                request.setNewSgc(mainMeter.getNewSgc());
                request.setType("NON-VIRTUAL");
                request.setOldTariffIndex(mainMeter.getOldTariffIndex());
                request.setNewTariffIndex(mainMeter.getNewTariffIndex());
                request.setMeterType(mainMeter.getMeterType());
                request.setMeterClass(mainMeter.getMeterClass());
                request.setMeterCategory(mainMeter.getMeterCategory());
                request.setSmartStatus(mainMeter.getSmartStatus());
                request.setMeterManufacturer(mainMeter.getMeterManufacturer());
                request.setMeterType(mainMeter.getMeterType());
                request.setMeterId(mainMeter.getId());
                request.setSimNumber(mainMeter.getSimNumber());

            } else {
                request.setType("VIRTUAL");
            }

            request.setNodeId(feederLine.getNodeId());
            request.setDss(dss.getNodeId());
            request.setOrgId(user.getOrgId());
            request.setCreatedBy(user.getId());

            handleMeterAssign(request);

            Meter m = meterMapper.getVersionMeter(user.getOrgId(), null, request.getMeterNumber(), null);
            String description = "Meter assigned to customer " + request.getCustomerId();

            AuditLog audit = buildAuditLog(user, description, meterName, m, metadata, "Meter replacement");
            auditRepository.save(audit);

            return ResponseMap.response(status.getSuccessCode(), "Meter assigned successfully", "");

        } catch (Exception exception) {
            log.error("Error occurred while changing user status [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Assigning meter with an existing cin failed");
            genericHandler.logAndSaveException(exception, "continue assign meter");
            throw exception;
        }

    }

    @Transactional
    @Override
    public Map<String, Object> detachMeter(UUID meterId, String reason) {
        try{
            // Gather client metadata
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel um = handleUserValidation();

            // verify if meter exist
            Meter meterById = meterMapper.findById(meterId, um.getOrgId());
            if(meterById == null) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getNotFoundDesc());
            }

            boolean hasUnpaid = meterById.getDebitCreditAdjustInfo().stream()
                    .anyMatch(m ->
                            m.getStatus().equalsIgnoreCase("UNPAID") ||
                                    m.getStatus().equalsIgnoreCase("PARTIALLY_PAID")
                    );

            if (hasUnpaid) {
                throw new GlobalExceptionHandler.NotFoundException(
                        meterName + " (" + meterById.getMeterNumber() + ") have unpaid credit or debit adjustment"
                );
            }


            if (meterById.getMeterStage().contains("Pending") || meterById.getStatus().contains("Pending")) {
                throw new GlobalExceptionHandler.NotFoundException("Meter have a pending record that needs to be cleared");
            }

            if(meterById.getMeterStage().equalsIgnoreCase("Deactivated")
                    || meterById.getType().equalsIgnoreCase("virtual")
                    || meterById.getCustomerId() == null) {
                throw new GlobalExceptionHandler.NotFoundException("Meters detaching failed because meter is either unassigned, deactivated and virtual");
            }

            // Validate feeder line
            UUID parentNode = meterMapper.getFeederParentNode(meterById.getNodeId());
            if (parentNode == null) {
                throw new GlobalExceptionHandler.NotFoundException("Feeder line " + status.getNotFoundDesc());
            }

            //set meter Id
            meterById.setMeterId(meterById.getId());
            meterById.setCreatedBy(um.getId());
            meterById.setDescription("Meter detached");
            meterById.setMeterStage("Pending-detached");
            meterMapper.updateMeterCategory(um.getOrgId(), meterId, "Pending-detached", meterById.getUpdatedAt());

            meterById.setDss(null);
            meterById.setNodeId(parentNode);
            meterById.setCustomerId(null);
            meterById.setAccountNumber(null);
            meterById.setTariff(null);
            meterById.setCin(null);
            meterById.setStatus("Active");
            meterById.setReason(reason);
            int m = meterMapper.insertMeterVersion(meterById);
            if(m == 0) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getRegFailureDesc());
            }

            // get recent meter record
            Meter meter =  meterMapper.findById(meterId, um.getOrgId());

            AuditLog auditLog = buildAuditLog(um, "Meter detached", meterName, meter, metadata, reason);
            auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(), "Meter detached successfully", "");
        } catch (Exception exception) {
            log.error("Error occurred while changing user status [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Migrating meter service failed");
            genericHandler.logAndSaveException(exception, "migrating meter");
            throw exception;
        }

    }

    @Transactional
    @Override
    public Map<String, Object> migrate(PaymentMode request) {
        String desc = "";
        try {
            // Gather client metadata
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel um = handleUserValidation();

            // verify if meter exist
            Meter meterById = meterMapper.findById(request.getMeterId(), um.getOrgId());
            if(meterById == null) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getNotFoundDesc());
            }
            if (meterById.getMeterStage().contains("Pending") || meterById.getStatus().contains("Pending")) {
                throw new GlobalExceptionHandler.NotFoundException(
                        "Meter have a pending record that needs cleared"
                );
            }

            if(meterById.getMeterStage().equalsIgnoreCase("Deactivated")
                    || meterById.getType().equalsIgnoreCase("virtual")
                    || meterById.getCustomerId() == null) {
                throw new GlobalExceptionHandler.NotFoundException("Meters migration failed because meter is either unassigned, deactivated and virtual");
            }

//            if(request.getMigrationFrom().equalsIgnoreCase("postpaid") && meterById.getMeterCategory().equalsIgnoreCase("prepaid")){
//                throw new GlobalExceptionHandler.NotFoundException("Meter is a prepaid meter");
//            }

            // prevent MD meter from migrating
            if(meterById.getMeterClass().equalsIgnoreCase("MD")){
                throw new GlobalExceptionHandler.NotFoundException("MD meter can not be migrated" );
            }
            String meterStage = "Pending-migrated";
            String description = "Meter Migrated";
            meterById.setMeterId(request.getMeterId());
            meterById.setCreatedBy(um.getId());
            meterById.setMeterStage(meterStage);
            meterById.setDescription(description);
            request.setOrgId(meterById.getOrgId());
            request.setCreatedBy(um.getId());
            request.setDescription(description);
            request.setMeterStage(meterStage);


            //migrate to prepaid
            if(request.getMigrationFrom().equalsIgnoreCase("postpaid") && meterById.getMeterCategory().equalsIgnoreCase("postpaid")){
                desc = "Meter migration from postpaid to prepaid";

                meterMapper.updateMeterCategory(um.getOrgId(), request.getMeterId(), meterStage, meterById.getUpdatedAt());

                meterById.setMeterCategory("Prepaid");

                int m = meterMapper.insertMeterVersion(meterById);
                if(m == 0) throw new GlobalExceptionHandler.NotFoundException(meterName+ " Migration " +status.getRegFailureDesc());
                request.setStatus(true);

                // insert payment method
                int migrate = meterMapper.assignPaymentModeWhenMigrationToPrepaid(request);
                if(migrate == 0) throw new GlobalExceptionHandler.NotFoundException(meterName+ " migration failed");

            } else if(request.getMigrationFrom().equalsIgnoreCase("prepaid") && meterById.getMeterCategory().equalsIgnoreCase("prepaid")){
                desc = "Meter migration from prepaid to postpaid";

                request.setCreditPaymentMode(meterById.getPaymentMode().getCreditPaymentMode());
                request.setCreditPaymentPlan(meterById.getPaymentMode().getCreditPaymentPlan());
                request.setDebitPaymentMode(meterById.getPaymentMode().getDebitPaymentMode());
                request.setDebitPaymentPlan(meterById.getPaymentMode().getDebitPaymentPlan());

                meterMapper.updateMeterCategory(um.getOrgId(), request.getMeterId(), meterStage, meterById.getUpdatedAt());

                meterById.setMeterCategory("Postpaid");

                int m = meterMapper.insertMeterVersion(meterById);
                if(m == 0) throw new GlobalExceptionHandler.NotFoundException(meterName+ " Migration " +status.getRegFailureDesc());

                request.setStatus(false);

                // insert payment method
                int migrate = meterMapper.assignPaymentModeWhenMigrationToPrepaid(request);
                if(migrate == 0) throw new GlobalExceptionHandler.NotFoundException(meterName+ " migration failed");
            } else {
                throw new GlobalExceptionHandler.NotFoundException("Migration not allowed because meter is already "+meterById.getMeterCategory());
            }

            // get recent meter record
            Meter meter = meterMapper.findById(request.getMeterId(), um.getOrgId());

//            handleAddCache(meter);
            AuditLog auditLog = buildAuditLog(um, desc, meterName, meter, metadata, "");
            auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(), "Meter migrated successfully", "");

        } catch (Exception exception) {
            log.error("Error occurred while changing user status [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Migrating meter service failed");
            genericHandler.logAndSaveException(exception, "migrating meter");
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
                throw new MissingServletRequestParameterException("approveStatus", "not found");
            }

            // --- Step 3: Audit log ---
            Meter updatedMeter = meterMapper.findById(meter.getId(), user.getOrgId());
            user.setPassword(null); // hide password in logs
            AuditLog auditLog = buildAuditLog(user, "Meter "+ approveStatus+"ed", meterName, updatedMeter, metadata, "");
            auditRepository.save(auditLog);

            return ResponseMap.response(
                    status.getSuccessCode(),
                     meterName + " ("+meter.getMeterNumber() +") " + capitalizeFirstLetter(approveStatus) + "ed Successfully",
                    ""
            );

        } catch (Exception ex) {
            log.error("Error occurred while approving/rejecting meter: {}", ex.getMessage(), ex);
            genericHandler.logIncidentReport("approving meter service failed");
            genericHandler.logAndSaveException(ex, "approving meter");
            throw ex;
        }
    }


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
    }

    private void handleApproval(Meter meter, UserModel user, String approveStatus) {

        meter.setApproveBy(user.getId());

        String stage = meter.getMeterStage() != null ? meter.getMeterStage().trim() : "";
        String stat = meter.getStatus() != null ? meter.getStatus().trim() : "";

        // === Handle Pending-created cases ===
        if (stage.equalsIgnoreCase("Pending-created")) {

            if (meter.getMdMeterInfo() != null && meter.getSmartMeterInfo() != null) {
                System.out.println("Case: both mdMeterInfo and smartMeterInfo present");
                meter.setMeterStage("Created");
                meter.setStatus("Active");
                meter.getMdMeterInfo().setMeterStage("Created");
                meter.getSmartMeterInfo().setMeterStage("Created");

            } else if (meter.getMdMeterInfo() == null && meter.getSmartMeterInfo() != null) {
                System.out.println("Case: only smartMeterInfo present");
                meter.setMeterStage("Created");
                meter.setStatus("Active");
                meter.getSmartMeterInfo().setMeterStage("Created");

            } else if (meter.getMdMeterInfo() != null && meter.getSmartMeterInfo() == null) {
                System.out.println("Case: only mdMeterInfo present");
                meter.setMeterStage("Created");
                meter.setStatus("Active");
                meter.getMdMeterInfo().setMeterStage("Created");

            } else {
                System.out.println("Case: neither mdMeterInfo nor smartMeterInfo present");
                meter.setMeterStage("Created");
                meter.setStatus("Active");
            }

        // === Handle Pending-assigned ===
        } else if (stage.equalsIgnoreCase("Pending-assigned")) {
            meter.setMeterStage("Assigned");
            meter.setStatus("Active");

        // === Handle Pending-allocated ===
        } else if (stage.equalsIgnoreCase("Pending-allocated")) {
            meter.setMeterStage("Unassigned");
            meter.setStatus("Active");

        // === Handle Pending-detached ===
        } else if (stage.equalsIgnoreCase("Pending-detached")) {
            meter.setMeterStage("Unassigned");
            meter.setStatus("Deactivated");

        // === Handle Pending-migrated ===
        } else if (stage.equalsIgnoreCase("Pending-migrated") && meter.getSmartMeterInfo() != null) {
            meter.getSmartMeterInfo().setMeterStage("Active");
            meter.getPaymentMode().setMeterStage("Active");
            meter.setMeterStage("Assigned");
            meter.setStatus("Active");

        // === Handle Pending-edited ===
        } else if (stage.equalsIgnoreCase("Pending-edited")) {
            if (meter.getCustomerId() == null && meter.getNodeId() != null) {
                meter.setMeterStage("Unassigned");
                meter.setStatus("Active");
            } else if (meter.getCustomerId() == null && meter.getNodeId() == null) {
                meter.setMeterStage("Created");
                meter.setStatus("Active");
            } else if (meter.getCustomerId() != null && meter.getNodeId() != null) {
                meter.setMeterStage("Assigned");
                meter.setStatus("Active");
            }

            // === Handle Pending-deactivated ===
            } else if (stat.equalsIgnoreCase("Pending-deactivated")) {
                meter.setStatus("Deactivated");

            // === Handle Pending-activated ===
            } else if (stat.equalsIgnoreCase("Pending-activated")) {
                meter.setStatus("Active");

            // === Default fallback ===
            } else {
                meter.getPaymentMode().setMeterStage("Active");
                meter.setMeterStage("Assigned");
                meter.setStatus("Active");
            }

        int approved = meterMapper.approvedMeterVersion(meter.getMeterStage(), meter.getStatus(), meter.getApproveBy(), meter.getUpdatedAt(), meter.getMeterNumber());
        if (approved == 0) {
            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "d " + status.getUpdateFailureDesc());
        }

        int c = customerMapper.totalCustomer(meter.getCustomerId());

        if(!"Pending-detached".equalsIgnoreCase(stage)){

            if("Pending-assigned".equalsIgnoreCase(stage)){
                if (meterMapper.approvePendingMeter(meter) == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "d " + status.getUpdateFailureDesc());
                }
                //Change customer status to Active
                int customerStatus = customerMapper.changeStatusCustomer(meter.getCustomerId(), "Active",user.getOrgId());
                if (customerStatus == 0) {
                    throw new GlobalExceptionHandler.NotFoundException("Customer status update failed");
                }
            } else {
                //Approve meter in meters table
                if (meterMapper.approveMeter(meter) == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "d " + status.getUpdateFailureDesc());
                }
                //Change customer status to Active
                if(c == 1) {
                    int customerStatus = customerMapper.changeStatusCustomer(meter.getCustomerId(), "Inactive",user.getOrgId());
                    if (customerStatus == 0) {
                        throw new GlobalExceptionHandler.NotFoundException("Customer status update failed");
                    }
                }

            }

            //approve meter location
            if(meter.getMeterAssignLocation() != null ){
                meter.getMeterAssignLocation().setApproveBy(user.getId());
                approveMeterAssignLocation(meter);
            }

            if (meter.getMdMeterInfo() != null) {
                meter.getMdMeterInfo().setApproveBy(user.getId());
                approveMDMeterInfo(meter);
            }

            if (meter.getSmartMeterInfo() != null) {
                meter.getSmartMeterInfo().setApproveBy(user.getId());
                approveSmartMeterInfo(meter);
            }

            //approve payment mode for prepaid meter Information
            if(meter.getPaymentMode() != null){
                meter.getPaymentMode().setApproveBy(user.getId());
                approvePrepaidMeterInfo(meter, approveStatus);
            }
        }

        if("Pending-detached".equalsIgnoreCase(stage)){
            if (meterMapper.meterApproval(meter) == 0) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "d " + status.getUpdateFailureDesc());
            }

            if(meterMapper.removeAssignedLocation(meter.getMeterId()) == 0){
                throw new GlobalExceptionHandler.NotFoundException("Unassigned location failed");
            }

            if(meterMapper.removePaymentMode(meter.getMeterId()) == 0){
                throw new GlobalExceptionHandler.NotFoundException("Unassigned payment mode failed");
            }

            if(c == 1) {
                int customerStatus = customerMapper.changeStatusCustomer(meter.getCustomerId(), "Inactive",user.getOrgId());
                if (customerStatus == 0) {
                    throw new GlobalExceptionHandler.NotFoundException("Customer status update failed");
                }
            }
        }

        if(meter.getDebitCreditAdjustVersionInfo() != null){
            int res1 = meterMapper.updateDebitCreditAdj(
                    meter.getDebitCreditAdjustVersionInfo().getOldMeterId(),
                    meter.getDebitCreditAdjustVersionInfo().getNewMeterId(), user.getOrgId());

            int res2 = meterMapper.updateDebitCreditAdjVersion(
                    meter.getDebitCreditAdjustVersionInfo().getOldMeterId(),
                    meter.getDebitCreditAdjustVersionInfo().getNewMeterId(),
                    false, user.getOrgId());

            if (res1 == 0 || res2 == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Debit credit adjustment replacement failed");
            }
        }


    }

    private void approveMeterAssignLocation(Meter meter) {
        int updateMeterLocationApproval,meterLocationApproval;
        meterLocationApproval = meterMapper.approveMeterAssignLocationVersion(meter.getMeterAssignLocation());

        MeterAssignLocation check = meterMapper.getMeterAssignLocation(meter.getMeterId());
        if (check == null) {
            updateMeterLocationApproval = meterMapper.insertMeterLocation(meter.getMeterAssignLocation());
        } else {
            updateMeterLocationApproval = meterMapper.updateMeterLocation(meter.getMeterAssignLocation());
        }
        if (updateMeterLocationApproval == 0 || meterLocationApproval == 0) {
            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());
        }
    }

    private void approveMDMeterInfo(Meter meter) {
        int updateMDInfoApproval, mdInfoApproval;
        meter.getMdMeterInfo().setMeterStage(meter.getMeterStage());
        mdInfoApproval = meterMapper.approveMDMeterInfoVersion(meter.getMdMeterInfo());
        MDMeterInfo check = meterMapper.getMDMeterInfo(meter.getMeterId());
        if (check == null) {
            updateMDInfoApproval = meterMapper.insertMDMeterInfo(meter.getMdMeterInfo());
        } else if(meter.getMdMeterInfo() != null){
            updateMDInfoApproval = meterMapper.updateMDMeterInfo(meter.getMdMeterInfo());
        } else {
            return;
        }
        if (updateMDInfoApproval == 0 || mdInfoApproval == 0) {
            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());
        }
    }

    private void approveSmartMeterInfo(Meter meter) {
        int updateMDInfoApproval, mdInfoApproval;
        meter.getSmartMeterInfo().setMeterStage(meter.getMeterStage());
        mdInfoApproval = meterMapper.approveSmartMeterInfoVersion(meter.getSmartMeterInfo());
        SmartMeterInfo check = meterMapper.getSmartMeter(meter.getMeterId());
        if (check == null) {
            updateMDInfoApproval = meterMapper.insertSmartMeterInfo(meter.getSmartMeterInfo());
        } else if(meter.getSmartMeterInfo() != null){
            updateMDInfoApproval = meterMapper.updateSmartMeterInfo(meter.getSmartMeterInfo());
        }
        else {
           return;
        }
        if (updateMDInfoApproval == 0 || mdInfoApproval == 0) {
            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());
        }
    }

    private void approvePrepaidMeterInfo(Meter meter, String approveStatus) {
        int updateMDInfoApproval, mdInfoApproval;
        mdInfoApproval = meterMapper.approvePrepaidMeterVersion(meter.getPaymentMode());
        if (mdInfoApproval == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());

        PaymentMode check = meterMapper.getPaymentMode(meter.getMeterId());
        if (check == null) {
            updateMDInfoApproval = meterMapper.insertPrepaidMeterVersion(meter.getPaymentMode());
            if (updateMDInfoApproval == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());

        } else if(meter.getPaymentMode() != null){
            updateMDInfoApproval = meterMapper.updatePrepaidMeterVersion(meter.getPaymentMode());
            if (updateMDInfoApproval == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());

        }
        else {
            return;
        }
//        if (updateMDInfoApproval == 0 || mdInfoApproval == 0) {
//            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());
//        }
    }

    private void handleRejection(Meter meter, String approveStatus, UserModel user) {
//        if(meter.getDebitCreditAdjustVersionInfo() != null) {
//            throw new GlobalExceptionHandler.NotFoundException(meter.getDebitCreditAdjustVersionInfo().getDescription());
//        }

        String st = meter.getMeterStage();
        String status = meter.getStatus();
        String s = status.equalsIgnoreCase("Pending-deactivated") ? "Active" : status.equalsIgnoreCase("Active") ? "Active" : "Deactivated";
        int reject;


        //Update meter meter-stage status in meters_version table to rejected
         reject = meterMapper.rejectedMeterVersion("Rejected", meter.getMeterNumber(), meter.getUpdatedAt(), user.getId(), s);
        if (reject == 0) {
            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "ed failed");
        }

        //Update assigned location approve status to rejected in meter_assign_locations_version table
        if(meter.getMeterAssignLocation() != null) {
            int result = meterMapper.updateMeterAssignedLocation("Rejected", meter.getMeterAssignLocation().getMeterId(), user.getOrgId(), meter.getUpdatedAt(), user.getId());
            if(result == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " assigned location failed");
        }

        if(meter.getPaymentMode() != null){
            //Update smart meter Info, mater-stage to rejected in payment_mode_version table
            int result = meterMapper.removePaymentModeInfo("Rejected", meter.getPaymentMode().getMeterId(), user.getOrgId(), user.getId());
            if(result == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " assign payment mode failed");
        }

        if(meter.getMeterStage().trim().equalsIgnoreCase("Pending-created")){

            //Delete meter record in meters table
           int res = meterMapper.removeMeter(meter.getMeterNumber(), user.getOrgId());

            //Update MD meter Info, mater-stage to rejected in md_meters_info_version table
            if ("md".equalsIgnoreCase(meter.getMeterClass()) && !st.equalsIgnoreCase("Pending-allocated")) {
                res = meterMapper.updateMDMeterInfoVersion("Rejected", meter.getMdMeterInfo().getMeterId(), user.getOrgId(), user.getId());
            }

            //Update smart meter Info, mater-stage to rejected in smart_meter_info_version table
            if (Boolean.TRUE.equals(meter.getSmartStatus() && !st.equalsIgnoreCase("Pending-allocated"))) {
                res = meterMapper.updateSmartMeterInfoVersion("Rejected", meter.getSmartMeterInfo().getMeterId(), user.getOrgId(), user.getId());
            }

            if(res == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " failed to delete");

        }
        else if (meter.getMeterStage().trim().equalsIgnoreCase("Pending-assigned")
                && meter.getType().equalsIgnoreCase("virtual")) {
            int u = meterMapper.removeMeter(meter.getMeterNumber(), user.getOrgId());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "ed failed");
        }
        else if((meter.getMeterStage().trim().equalsIgnoreCase("Pending-allocated"))
                && meter.getCustomerId() == null && meter.getNodeId() != null) {
            meter.setMeterStage("Created");
            meter.setStatus("Active");
            int u = meterMapper.updateMeter(meter.getMeterStage(), meter.getMeterId(), meter.getUpdatedAt(), meter.getStatus());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " deactivation failed");
        } else if((meter.getMeterStage().trim().equalsIgnoreCase("Pending-edited"))
                && meter.getCustomerId() == null && meter.getNodeId() != null) {
            meter.setMeterStage("Unassigned");
            meter.setStatus("Active");
            handleMeterInfoRejection(meter, user);
        } else if((meter.getMeterStage().trim().equalsIgnoreCase("Pending-edited"))
                && meter.getCustomerId() == null && meter.getNodeId() == null) {
            meter.setMeterStage("Created");
            meter.setStatus("Active");
            handleMeterInfoRejection(meter, user);
        } else if((meter.getMeterStage().trim().equalsIgnoreCase("Pending-assigned"))
                && meter.getCustomerId() != null && meter.getNodeId() != null && meter.getType().equalsIgnoreCase("non-virtual")) {
            meter.setMeterStage("Unassigned");
            meter.setStatus("Active");
//            handlePendingAssignedRejection(meter, user);
            int u = meterMapper.updateMeter(meter.getMeterStage(), meter.getMeterId(), meter.getUpdatedAt(), meter.getStatus());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " deactivation failed");
        } else if(meter.getMeterStage().trim().equalsIgnoreCase("Pending-edited")
                && meter.getCustomerId() != null && meter.getNodeId() != null) {
            meter.setMeterStage("Assigned");
            meter.setStatus("Active");
            handleMeterInfoRejection(meter, user);
        }  else if((meter.getMeterStage().trim().equalsIgnoreCase("Pending-detached")
                || meter.getMeterStage().trim().equalsIgnoreCase("Pending-migrated"))) {
            meter.setMeterStage("Assigned");
            meter.setStatus("Active");
            int u = meterMapper.updateMeter(meter.getMeterStage(), meter.getMeterId(), meter.getUpdatedAt(), meter.getStatus());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " migration failed");
        }
        else {
            meter.setStatus(s);
            int u = meterMapper.updateMeter(meter.getMeterStage(), meter.getMeterId(), meter.getUpdatedAt(), meter.getStatus());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " update failed");
        }

        if(meter.getDebitCreditAdjustVersionInfo() != null){
            int res = meterMapper.updateDebitCreditAdjVersion(
                    meter.getDebitCreditAdjustVersionInfo().getOldMeterId(),
                    meter.getDebitCreditAdjustVersionInfo().getNewMeterId(),
                    false, user.getOrgId());
            if (res == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Debit credit adjustment replacement rejection failed");
            }
        }

//        if(meter.getDebitCreditAdjustVersionInfo() != null){
//            int res = meterMapper.updateMeter(
//                    "Assigned",
//                    meter.getDebitCreditAdjustVersionInfo().getOldMeterId(),
//                    meter.getUpdatedAt(),
//                    "Active");
//            if (res == 0) {
//                throw new GlobalExceptionHandler.NotFoundException("Debit credit adjustment replacement rejection failed");
//            }
//        }
    }

    void handleMeterInfoRejection(Meter meter, UserModel user) {
        if(meter.getMdMeterInfo() != null){
            int v = meterMapper.updateMDMeter(meter.getMeterStage(), meter.getMeterId(), meter.getUpdatedAt(), meter.getStatus(), user.getId());
            if(v == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " editing failed");
        }
        if(meter.getSmartMeterInfo() != null){
            int v = meterMapper.updateSmartMeter(meter.getMeterStage(), meter.getMeterId(), meter.getUpdatedAt(), meter.getStatus(), user.getId());
            if(v == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " editing failed");
        }
        int m = meterMapper.updateMeter(meter.getMeterStage(), meter.getMeterId(), meter.getUpdatedAt(), meter.getStatus());
        if(m == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " editing failed");
    }

    private boolean isApprove(String status) {
        return status != null && status.toLowerCase().contains("approve");
    }

    private boolean isReject(String status) {
        return status != null && status.toLowerCase().contains("reject");
    }

    @Transactional
    @Override
    public Map<String, Object> allocateMeter(String meterNumber, String regionId) {
        try {
            // Gather client metadata
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

            UserModel um = handleUserValidation();

            Meter verifyMeter = meterMapper.getMeter(um.getOrgId(), null, meterNumber, null, null, "");
            if(verifyMeter == null){
                throw new GlobalExceptionHandler.NotFoundException("Meter " + status.getNotFoundDesc());
            }

            if (verifyMeter.getMeterStage().contains("Pending") || verifyMeter.getStatus().contains("Pending")) {
                throw new GlobalExceptionHandler.NotFoundException("Meter has a pending record that needs to be cleared");
            }

            // verify if node (organization id) exist
            RegionBhubServiceCenter node = nodeMapper.verifyNode(regionId, um.getOrgId());
            if(node == null){
                throw new GlobalExceptionHandler.NotFoundException("Node " + status.getNotFoundDesc());
            }

            verifyMeter.setCreatedAt(LocalDateTime.now());
            verifyMeter.setUpdatedAt(LocalDateTime.now());

            String desc = meterNumber + " meter allocated to " + regionId;

            //Allocate meter
            int result;
            result = meterMapper.allocateMeterVersion(verifyMeter, node.getNodeId(), um.getId(), "Meter Allocated");
            if(result == 0){
                throw new GlobalExceptionHandler.NotFoundException("Meter allocation failed");
            }

            result = meterMapper.updateMeter("Pending-allocated", verifyMeter.getId(), verifyMeter.getUpdatedAt(), verifyMeter.getStatus());
            if(result == 0){
                throw new GlobalExceptionHandler.NotFoundException("Meter allocation failed");
            }

            //fetch meter from the database
            Meter meter = meterMapper.getVersionMeter(um.getOrgId(), null, meterNumber, null);
//            String desc = capitalizeFirstLetter(meter.getMeterNumber() + " allocated " + node.getName());
            //save to audit (mongodb)
            AuditLog auditLog = buildAuditLog(um, desc, meterName, meter, metadata, "");
            auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(), meterName + " allocated successfully" , "");

        } catch (Exception exception) {
            log.error("Error filtering / fetching meters: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Allocating meter service failed");
            genericHandler.logAndSaveException(exception, "allocating meter");
            throw exception;
        }
    }

    @Async("bulkUploadExecutor")
    public CompletableFuture<Integer> insertSingleAsync(
            Meter meter, UserModel user, List<GenericResp> failedRecords) {
        try {
            insertSingleTransactional(meter, user);
            return CompletableFuture.completedFuture(1);
        } catch (Exception e) {
            String reason = extractErrorMessage(e);
            GenericResp resp = new GenericResp();
            resp.setId(meter.getMeterId().toString());
            resp.setMessage("Meter Allocate failed: "+reason);
            resp.setData(meter.getMeterNumber());

            failedRecords.add(resp);
//            failedRecords.add(meter.getMeterNumber() + " (" + reason + ")");
            log.warn("Async single insert failed for {}: {}", meter.getMeterNumber(), reason);
            return CompletableFuture.completedFuture(0);
        }
    }

    @Override
    public Map<String, Object> bulkUpload(MultipartFile file) throws IOException {
        try {
            UserModel user = handleUserValidation();

            // Determine file type
            String filename = Optional.ofNullable(file.getOriginalFilename())
                    .orElseThrow(() -> new IOException("File has no name"));

            List<Meter> meters;
            if (filename.endsWith(".csv")) {
                meters = processCsv(file.getInputStream(), user);
            } else if (filename.endsWith(".xlsx")) {
                meters = processExcel(file.getInputStream(), user);
            } else {
                throw new IOException("Unsupported file format. Only .csv or .xlsx allowed.");
            }

            Map<String, Object> result = bulkInsertMeters(meters, user);
            return result;

        } catch (Exception e) {
            log.error("Error in bulk upload: {}", e.getMessage(), e);
            genericHandler.logIncidentReport("Bulk upload service failed");
            genericHandler.logAndSaveException(e, "Bulk upload meter");
            throw new IOException("Bulk upload failed: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> bulkAllocate(MultipartFile file) throws IOException {
        try {
            UserModel user = handleUserValidation();

            // Determine file type
            String filename = Optional.ofNullable(file.getOriginalFilename())
                    .orElseThrow(() -> new IOException("File has no name"));

            List<MeterRequest> meters;
            if (filename.endsWith(".csv")) {
                meters = processAllocateCsv(file.getInputStream());
            } else if (filename.endsWith(".xlsx")) {
                meters = processAllocateExcel(file.getInputStream());
            } else {
                throw new IOException("Unsupported file format. Only .csv or .xlsx allowed.");
            }
            Map<String, Object> result = bulkAllocateMeters(meters, user);
            return result;

        } catch (Exception e) {
            log.error("Error in bulk allocate upload: {}", e.getMessage(), e);
            genericHandler.logIncidentReport("Bulk allocate service failed");
            genericHandler.logAndSaveException(e, "Bulk allocate meter");
            throw new IOException("Bulk allocate failed: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> bulkAllocateMeters(List<MeterRequest> allocations, UserModel user) {
        Map<String, Object> result = new HashMap<>();
        List<GenericResp> failedRecords = new ArrayList<>();
        int successCount = 0;

        if (allocations == null || allocations.isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("No records found in uploaded file");
        }

        final int BATCH_SIZE = 500;

        for (int i = 0; i < allocations.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, allocations.size());
            List<MeterRequest> subBatch = allocations.subList(i, end);

            // Extract meter numbers and region IDs
            List<String> meterNumbers = subBatch.stream()
                    .map(MeterRequest::getMeterNumber)
                    .filter(num -> num != null && !num.trim().isEmpty())
                    .map(String::trim)
                    .toList();

            List<String> regionIds = subBatch.stream()
                    .map(MeterRequest::getRegionId)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

//            if (meterNumbers.isEmpty() || regionIds.isEmpty()) {
//                GenericResp resp = new GenericResp();
//                resp.setId(meter.getMeterId().toString());
//                resp.setMessage("Meter Approve failed: "+reason);
//                resp.setData(meter.getMeterNumber());
//
//                failedRecords.add(resp);
//                subBatch.forEach(req -> failedRecords.add(
//                        String.format("%s [Region: %s] (Invalid meter/region)",
//                                req.getMeterNumber(), req.getRegionId())
//                ));
//                continue;
//            }

            if (meterNumbers.isEmpty()  || regionIds.isEmpty()) {
                subBatch.forEach(req -> {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Missing meter number or region id");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
                });

                continue;
            }
            // Fetch meters
            List<Meter> meters = meterMapper.getMetersByMeterNumbers(meterNumbers, user.getOrgId());
            Map<String, Meter> meterMap = meters.stream()
                    .collect(Collectors.toMap(Meter::getMeterNumber, m -> m));

            // Fetch region → business-hub mappings
            List<RegionBhubServiceCenter> regionHubs = meterMapper.getRegionBhubMappings(regionIds, user.getOrgId());
            Map<String, UUID> regionNodeIdMap = regionHubs.stream()
                    .collect(Collectors.toMap(RegionBhubServiceCenter::getRegionId, RegionBhubServiceCenter::getNodeId));

            List<Meter> validAllocations = new ArrayList<>();

            for (MeterRequest req : subBatch) {
                Meter meter = meterMap.get(req.getMeterNumber());
                UUID nodeId = regionNodeIdMap.get(req.getRegionId());

                if (meter == null) {
                    GenericResp resp = new GenericResp();
                    resp.setId(meter.getMeterId().toString());
                    resp.setMessage("Meter Not found");
                    resp.setData(meter.getMeterNumber());

                    failedRecords.add(resp);
                    continue;
                }

                if (nodeId == null) {
                    GenericResp resp = new GenericResp();
                    resp.setId(meter.getMeterId().toString());
                    resp.setMessage("Meter Allocate failed: region not found in business hub");
                    resp.setData(meter.getMeterNumber());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format("%s [Region: %s] (Region not found in business hub)", req.getMeterNumber(), req.getRegionId()));
                    continue;
                }

                meter.setNodeId(nodeId);
//                meter.getNodeInfo().setRegionId(req.getRegionId());
                meter.setOrgId(user.getOrgId());
                meter.setMeterStage("Pending-allocated");
                meter.setCreatedBy(user.getId());
                meter.setDescription("Meter Allocated");
                validAllocations.add(meter);
            }

            if (validAllocations.isEmpty()) continue;

            // Try allocating
            try {
                log.info("Processing batch {} - {} ({} records)", i, end - 1, subBatch.size());
                int allocated = allocateBatchTransactional(validAllocations, user);
                successCount += allocated;
            } catch (Exception e) {
                log.warn("Batch {} failed — retrying smaller sub-batches: {}", (i / BATCH_SIZE) + 1, e.getMessage());
                successCount += allocateSubBatchTransactional(validAllocations, user, failedRecords);
            }
        }

        int total = successCount + failedRecords.size();

        result.put("totalRecords", total);
        result.put("successCount", successCount);
        result.put("failedCount", failedRecords.size());
        result.put("failedRecords", failedRecords);

        if (!failedRecords.isEmpty()) {
            throw new GlobalExceptionHandler.PartialFailureException(
                    failedRecords.size() + " of " + total + " Meters allocate failed",
                    result
            );
        }

        return ResponseMap.response(
                status.getSuccessCode(),
                String.format("%d of %d meters allocated successfully", successCount, total),
                result
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int allocateBatchTransactional(List<Meter> batch, UserModel user) {
        if (batch.isEmpty()) return 0;

        try {
            // Update main meter table
            meterMapper.updateBatchMeterAllocation(batch);

            // Update version table (node_id + meter_stage)
            meterMapper.insertMeterVersions(batch);

            // Audit allocations
            auditBatch(batch, user, "Meter Allocated");

            log.info("Allocated {} meters successfully", batch.size());
            return batch.size();

        } catch (Exception e) {
            log.error("Transaction failed during allocation, rolling back batch of size {}: {}", batch.size(), e.getMessage());
            genericHandler.logIncidentReport("Bulk allocate batch service failed");
            genericHandler.logAndSaveException(e, "Bulk allocate batch meter");
            throw new RuntimeException("Batch allocation transaction failed. Rolled back.", e);
        }
    }

    public int allocateSubBatchTransactional(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        try {
            int successCount = 0;
            int subBatchSize = 100;

            for (int i = 0; i < batch.size(); i += subBatchSize) {
                int end = Math.min(i + subBatchSize, batch.size());
                List<Meter> subBatch = batch.subList(i, end);

                try {
                    successCount += allocateBatchTransactional(subBatch, user);
                } catch (Exception e) {
                    log.warn("Sub-batch allocation failed (size={}): {}", subBatch.size(), e.getMessage());

                    if (subBatch.size() > 50) {
                        successCount += allocateSinglesFallbackAsync(subBatch, user, failedRecords);
                    } else {
                        successCount += allocateSinglesFallback(subBatch, user, failedRecords);
                    }
                }
            }

            return successCount;
        } catch (Exception e) {
            genericHandler.logIncidentReport("Bulk allocate sub batch service failed");
            genericHandler.logAndSaveException(e, "Bulk allocate sub batch meter");
            throw new RuntimeException("Sub Batch allocation transaction failed. Rolled back.", e);
        }

    }

    public int allocateSinglesFallbackAsync(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        for (Meter meter : batch) {
            futures.add(allocateSingleAsync(meter, user, failedRecords));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream().mapToInt(CompletableFuture::join).sum();
    }

    public int allocateSinglesFallback(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        int successCount = 0;

        for (Meter meter : batch) {
            try {
                log.debug("Fallback single allocation for meter: {}", meter.getMeterNumber());
                allocateSingleTransactional(meter, user);
                successCount++;
            } catch (Exception e) {
                String reason = extractErrorMessage(e);
                GenericResp resp = new GenericResp();
                resp.setId(meter.getMeterId().toString());
                resp.setMessage("Meter single allocate failed: "+reason);
                resp.setData(meter.getMeterNumber());

                failedRecords.add(resp);
//                failedRecords.add(String.format(
//                        "%s [Region: %s] (Allocation failed: %s)",
//                        meter.getMeterNumber(),
////                        meter.getNodeInfo().getRegionId(),
//                        reason
//                ));
                log.warn("Meter {} failed individually: {}", meter.getMeterNumber(), reason);
            }
        }

        return successCount;
    }

    @Async
    public CompletableFuture<Integer> allocateSingleAsync(Meter meter, UserModel user, List<GenericResp> failedRecords) {
        try {
            allocateSingleTransactional(meter, user);
            return CompletableFuture.completedFuture(1);
        } catch (Exception e) {
            String reason = extractErrorMessage(e);
            GenericResp resp = new GenericResp();
            resp.setId(meter.getMeterId().toString());
            resp.setMessage("Meter single allocation failed: "+reason);
            resp.setData(meter.getMeterNumber());

            failedRecords.add(resp);
//            failedRecords.add(String.format(
//                    "%s [Region: %s] (Allocation failed: %s)",
//                    meter.getMeterNumber(),
////                    meter.getNodeInfo().getRegionId(),
//                    reason
//            ));
            log.warn("Async allocation failed for meter {}: {}", meter.getMeterNumber(), reason);
            return CompletableFuture.completedFuture(0);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void allocateSingleTransactional(Meter meter, UserModel user) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

        // --- Step 1: Prepare core meter entity ---
//        meter.setOrgId(user.getOrgId());
//        meter.setCreatedBy(user.getId());
//        meter.setStatus("Active");
//        meter.setMeterStage("Pending-allocated");
//        meter.setType("NON-VIRTUAL");
//        meter.setDescription("Meter Allocated");
//        String desc = meter.getMeterNumber() + " meter allocated to " + .;
//        String desc = meter.getMeterNumber() + " meter allocated to " + meter.getNodeInfo().getRegionId();

        // --- Step 2: Insert into main + version tables ---
        meterMapper.allocateMeterVersion(meter, meter.getNodeId(), meter.getId(), "Pending Allocated");
//        if(result == 0){
//            throw new GlobalExceptionHandler.NotFoundException("Meter allocation failed");
//        }

        meterMapper.updateMeter(meter.getDescription(), meter.getId(), meter.getUpdatedAt(), meter.getStatus());
//        if(result == 0){
//            throw new GlobalExceptionHandler.NotFoundException("Meter allocation failed");
//        }

        //fetch meter from the database
        Meter m = meterMapper.getVersionMeter(user.getOrgId(), null, meter.getMeterNumber(), null);
//            String desc = capitalizeFirstLetter(meter.getMeterNumber() + " allocated " + node.getName());
        //save to audit (mongodb)
        AuditLog auditLog = buildAuditLog(user, "Pending Allocated", meterName, m, metadata, "");
        auditRepository.save(auditLog);

    }

    @Override
    public Map<String, Object> bulkApproval(List<MeterRequest> meters) {

        UserModel user = handleUserValidation();
        Map<String, Object> result = new HashMap<>();
        List<GenericResp> failedRecords = new ArrayList<>();
        int successCount = 0;

        if (meters == null || meters.isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("No records found in file");
        }

        final int BATCH_SIZE = 500; // Tune as needed for performance


        for (int i = 0; i < meters.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, meters.size());
            List<MeterRequest> batch = meters.subList(i, end);

            // Collect all meter numbers in this subBatch
            List<String> meterNumbers = batch.stream()
                    .map(m -> m.getMeterNumber().trim())
                    .filter(num -> !num.isEmpty())
                    .toList();

            if (meterNumbers.isEmpty()) {
                batch.forEach(req -> {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Missing meter number");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
                });

                continue;
            }

            // fetch found meters
            List<Meter> versionBatch = meterMapper.getMetersByVersionMeterNumbers(meterNumbers, user.getOrgId());

            System.out.println("versionBatch: "+versionBatch.size());

            Set<String> foundNames = versionBatch.stream()
                    .map(Meter::getMeterNumber)
                    .map(String::trim)
                    .collect(Collectors.toSet());

            List<String> missingNames = meterNumbers.stream()
                    .filter(name -> !foundNames.contains(name.trim()))
                    .toList();

            // Record missing/invalid tariffs
            for (String name : missingNames) {
                GenericResp resp = new GenericResp();
                resp.setId("");
                resp.setMessage("Not found or not in pending state");
                resp.setData(name);
                failedRecords.add(resp);
            }

            try {
                prepareUpdateMeters(versionBatch, user, failedRecords);

                int updatedCount = updateBatchTransactional(versionBatch, user, failedRecords);
                successCount += updatedCount;

            } catch (Exception e) {
                log.warn("Batch {} failed — retrying smaller sub-batches: {}", (i / BATCH_SIZE) + 1, e.getMessage());
                int retrySuccess = updateSubBatchTransactional(versionBatch, user, failedRecords);
                successCount += retrySuccess;
            }
        }

        int total = meters.size();

        result.put("totalRecords", total);
        result.put("successCount", successCount);
        result.put("failedCount", failedRecords.size());
        result.put("failedRecords", failedRecords);

        // If any failed → throw browser error
        if (!failedRecords.isEmpty()) {
            throw new GlobalExceptionHandler.PartialFailureException(
                    failedRecords.size() + " of " + total + " meters approval failed",
                    result
            );
        }

        return ResponseMap.response(
                status.getSuccessCode(),
                successCount + " of " + total + " meters approved successfully",
                result
        );
    }

    /** Validate and enrich meters before DB update. */
    private void prepareUpdateMeters(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        Iterator<Meter> iterator = batch.iterator();
        while (iterator.hasNext()) {
            Meter meter = iterator.next();
            if (meter.getMeterNumber() == null || meter.getMeterNumber().trim().isEmpty()) {
                GenericResp resp = new GenericResp();
                resp.setId(meter.getMeterId().toString());
                resp.setMessage("Missing meter number");
                resp.setData(meter.getMeterNumber());

                failedRecords.add(resp);
                iterator.remove();
                continue;
            }

            meter.setOrgId(user.getOrgId());
            meter.setApproveBy(user.getId());
            meter.setId(meter.getMeterId());

            if("Pending-created".equalsIgnoreCase(meter.getMeterStage())){
                meter.setStatus("Active");
            } else if ("Pending-allocated".equalsIgnoreCase(meter.getMeterStage())){
                meter.setStatus("Active");
            } else if ("Pending-assigned".equalsIgnoreCase(meter.getMeterStage())) {
                meter.setStatus("Active");
            } else if ("Pending-migrated".equalsIgnoreCase(meter.getMeterStage())) {
                meter.setStatus("Active");
            } else if ("Pending-detached".equalsIgnoreCase(meter.getMeterStage())) {
                meter.setStatus("Active");
            } else if ("Pending-edited".equalsIgnoreCase(meter.getMeterStage()) && meter.getCustomerId() != null) {
//                meter.setMeterStage("Assigned");
                meter.setStatus("Active");
            } else if ("Pending-edited".equalsIgnoreCase(meter.getMeterStage()) && meter.getNodeId() != null && meter.getCustomerId() == null) {
//                meter.setMeterStage("Unassigned");
                meter.setStatus("Active");
            } else if ("Pending-edited".equalsIgnoreCase(meter.getMeterStage()) && meter.getNodeId() == null && meter.getCustomerId() == null) {
//                meter.setMeterStage("Created");
                meter.setStatus("Active");
            }
            else if ("Pending-activated".equalsIgnoreCase(meter.getStatus())) {
//                meter.setMeterStage("Created");
                meter.setStatus("Pending-activated");
            }else if ("Pending-deactivated".equalsIgnoreCase(meter.getStatus())) {
                System.out.println("------------> here <---------------------");
                meter.setStatus("Pending-deactivated");
            }
            else {
                GenericResp resp = new GenericResp();
                resp.setId(meter.getMeterId().toString());
                resp.setMessage("Meter is not in a pending state");
                resp.setData(meter.getMeterNumber());

                failedRecords.add(resp);
            }

            if (meter.getMdMeterInfo() != null) {
                meter.getMdMeterInfo().setApproveBy(user.getId());
                meter.getMdMeterInfo().setMeterId(meter.getMeterId());
                meter.getMdMeterInfo().setOrgId(user.getOrgId());
            }

            if (meter.getSmartMeterInfo() != null) {
                meter.getSmartMeterInfo().setApproveBy(user.getId());
                meter.getSmartMeterInfo().setMeterId(meter.getMeterId());
                meter.getSmartMeterInfo().setOrgId(user.getOrgId());
            }
        }
    }

    /** Transactionally update main + version + children + audit */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int updateBatchTransactional(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        String desc = "";
        if (batch.isEmpty()) return 0;

        try {

//            for (Meter meter : batch) {
//                if (meter.getDebitCreditAdjustVersionInfo() != null) {
//                    int res1 = meterMapper.updateBatchDebitCreditAdj(
//                            meter.getDebitCreditAdjustVersionInfo().getOldMeterId(),
//                            meter.getDebitCreditAdjustVersionInfo().getNewMeterId(),
//                            user.getOrgId()
//                    );
//
//                    int res2 = meterMapper.updateBatchDebitCreditAdjVersion(
//                            meter.getDebitCreditAdjustVersionInfo().getOldMeterId(),
//                            meter.getDebitCreditAdjustVersionInfo().getNewMeterId(),
//                            false,
//                            user.getOrgId()
//                    );
//
//                    if (res1 == 0 || res2 == 0) {
//                        throw new GlobalExceptionHandler.NotFoundException("Debit credit adjustment replacement failed");
//                    }
//                }
//            }


            List<Meter> approvedCreatedMeters = batch.stream()
                    .filter(m -> "Pending-created".equalsIgnoreCase(m.getMeterStage()))
                    .peek(m -> m.setMeterStage("Created"))
                    .peek(m -> m.setStatus("Active"))
                    .toList();

            List<Meter> approvedAllocatedMeters = batch.stream()
                    .filter(m -> "Pending-allocated".equalsIgnoreCase(m.getMeterStage()))
                    .peek(m -> m.setMeterStage("Unassigned"))
                    .peek(m -> m.setStatus("Active"))
                    .toList();

            List<Meter> approvedAssignedMeters = batch.stream()
                    .filter(m -> "Pending-assigned".equalsIgnoreCase(m.getMeterStage()))
                    .peek(m -> m.setMeterStage("Assigned"))
                    .peek(m -> m.setStatus("Active"))
                    .toList();

            List<Meter> approvedMigratedMeters = batch.stream()
                    .filter(m -> "Pending-migrated".equalsIgnoreCase(m.getMeterStage()))
                    .peek(m -> m.setMeterStage("Assigned"))
                    .peek(m -> m.setStatus("Active"))
                    .toList();

            List<Meter> approvedDetachedMeters = batch.stream()
                    .filter(m -> "Pending-detached".equalsIgnoreCase(m.getMeterStage()))
                    .peek(m -> m.setMeterStage("Unassigned"))
                    .peek(m -> m.setStatus("Active"))
                    .toList();

            List<Meter> approvedDeactivatedMetersStatus = batch.stream()
                    .filter(m -> "Pending-deactivated".equalsIgnoreCase(m.getStatus()))
                    .peek(m -> m.setStatus("Active"))
                    .toList();

            List<Meter> approvedActiveMetersStatus = batch.stream()
                    .filter(m -> "Pending-activated".equalsIgnoreCase(m.getStatus()))
                    .peek(m -> m.setStatus("Active"))
                    .toList();


            // Handle "Pending-edited" dynamically
            List<Meter> approvedEditedMeters = batch.stream()
                    .filter(m -> "Pending-edited".equalsIgnoreCase(m.getMeterStage()))
                    .peek(m -> {
                        if (m.getCustomerId() != null) {
                            m.setMeterStage("Assigned");
                        } else if (m.getNodeId() != null && m.getCustomerId() == null) {
                            m.setMeterStage("Unassigned");
                        } else {
                            m.setMeterStage("Created");
                        }
                        m.setStatus("Active");
                    })
                    .toList();

            // Combine all for main update
            List<Meter> toUpdate = Stream.of(
                            approvedCreatedMeters,
                            approvedAllocatedMeters,
                            approvedAssignedMeters,
                            approvedMigratedMeters,
                            approvedEditedMeters,
                            approvedDeactivatedMetersStatus,
                            approvedActiveMetersStatus)
                    .flatMap(Collection::stream)
                    .toList();
            if (!toUpdate.isEmpty()) {
                meterMapper.updateBatchMeters(toUpdate);
                meterMapper.updateBatchVersionMeters(toUpdate);
            }

            // Combine all for main update
            List<Meter> detach = Stream.of(approvedDetachedMeters)
                    .flatMap(Collection::stream)
                    .toList();

            if (!detach.isEmpty()) {
                meterMapper.updateDetachBatchMeters(detach, user.getOrgId());
                meterMapper.updateBatchVersionMeters(detach);
                meterMapper.removeBulkAssignedLocations(detach);
                meterMapper.removeBulkPaymentModes(detach);
            }

            List<DebitCreditAdjustVersion> adjustmentList = batch.stream()
                    .filter(m -> m.getDebitCreditAdjustVersionInfo() != null)
                    .map(m -> {
                        var info = m.getDebitCreditAdjustVersionInfo();
                        info.setStatus(false);              // New status
                        return info;
                    }).toList();

            if (!adjustmentList.isEmpty()) {
                meterMapper.updateBatchDebitCreditAdj(adjustmentList);
                meterMapper.updateBatchDebitCreditAdjVersion(adjustmentList);
            }

            // --- Migration ---
            if (!approvedMigratedMeters.isEmpty()) {
                desc = "Meter migration approved";
                handleMigration(approvedMigratedMeters, user);
            }

            // --- Assigned ---
            if (!approvedAssignedMeters.isEmpty()) {
                desc = "Meter assigned approved";
                handleAssignment(approvedAssignedMeters, user);
            }

            // --- Edited (can behave similar to assigned) ---
            if (!approvedEditedMeters.isEmpty()) {
                desc = "Meter edit approved";
                handleEditedMeters(approvedEditedMeters, user);
            }

            // --- Created ---
            if (!approvedCreatedMeters.isEmpty()) {
                desc = "Meter created approved";
                updateChildMeterData(batch, user);
            }

            auditApproveBatch(batch, user, desc);

            log.info("Batch updated successfully: {}", batch.size());
            return batch.size();

        } catch (Exception e) {
            log.error("Transaction failed, rolling back batch of size {}: {}", batch.size(), e.getMessage());
//            failedRecords.add("Sub-batch failed: " + e.getMessage());
            genericHandler.logIncidentReport("Bulk approve sub batch service failed");
            genericHandler.logAndSaveException(e, "Bulk approve sub batch meter");
            throw new RuntimeException("Batch transaction failed. Rolled back.", e);
        }
    }

    private void handleEditedMeters(List<Meter> editedMeters, UserModel user) {
        // Example: treat edited like assigned
        meterMapper.editAssignLocationFromVersion(editedMeters, user.getOrgId());
        meterMapper.updateAssignLocationVersion(editedMeters);

        List<Meter> prepaidMeters = editedMeters.stream()
                .filter(m -> "Prepaid".equalsIgnoreCase(m.getMeterCategory()))
                .toList();

        if (!prepaidMeters.isEmpty()) {
            meterMapper.updatePaymentModeFromVersion(prepaidMeters, user.getOrgId());
            meterMapper.updatePaymentModeVersion(prepaidMeters);
        }
    }

    private void handleAssignment(List<Meter> assignedMeters, UserModel user) {
        // Copy approved locations from version → actual table
        meterMapper.copyAssignLocationFromVersion(assignedMeters, user.getOrgId());

        // Create a list of prepaid meters
        List<Meter> prepaidMeters = assignedMeters.stream()
                .filter(m -> "Prepaid".equalsIgnoreCase(m.getMeterCategory()))
                .toList();

        if (!prepaidMeters.isEmpty()) {
            // Copy approved payment modes from version → actual table (for prepaid)
            meterMapper.copyPaymentModeFromVersion(prepaidMeters, user.getOrgId());

            //   Update the version tables to mark as approved
            meterMapper.updatePaymentModeVersion(prepaidMeters);
        }

        // Clean up location version table
        meterMapper.updateAssignLocationVersion(assignedMeters);

        // Update customer record (status = active)
        customerMapper.changeStatusBulkCustomer(assignedMeters, user.getOrgId());
    }

    private void handleMigration(List<Meter> migratedMeters, UserModel user) {

            // Create a list of prepaid meters
            List<Meter> prepaidMeters = migratedMeters.stream()
                    .filter(m -> "Prepaid".equalsIgnoreCase(m.getMeterCategory()))
                    .toList();

            List<Meter> postpaidMeters = migratedMeters.stream()
                    .filter(m -> "Postpaid".equalsIgnoreCase(m.getMeterCategory()))
                    .toList();

            if (!prepaidMeters.isEmpty()) {
                // Copy approved payment modes from version → actual table (for prepaid)
                meterMapper.copyPaymentModeFromVersion(prepaidMeters, user.getOrgId());

                // Update the version tables to mark as approved
                meterMapper.updatePaymentModeVersion(prepaidMeters);
            }

            if (!postpaidMeters.isEmpty()) {

                // Copy approved payment modes from version → actual table (for prepaid)
                meterMapper.deletePaymentModeFromVersion(prepaidMeters, user.getOrgId());

                //   Update the version tables to mark as approved
                meterMapper.updatePaymentModeVersion(prepaidMeters);
            }
    }

    private List<Meter> getMetersByStage(List<Meter> batch, String stage, String newStage, String status) {
        System.out.println("meter_stage2: "+batch.get(0).getMeterStage());
        List<Meter> ms;
        ms = batch.stream()
                .filter(m -> stage.equalsIgnoreCase(m.getMeterStage()))
                .peek(m -> m.setMeterStage(newStage))
                .peek(m -> m.setStatus(status))
                .toList();

        return ms;
    }

    private List<Meter> getMetersByStatus(List<Meter> batch, String status, String newStatus) {
        System.out.println("getMetersByStatus: "+batch.get(0).getStatus());
        System.out.println("batch: "+batch.size());
        List<Meter> ms;
        ms = batch.stream()
                .filter(m -> status.equalsIgnoreCase(m.getStatus()))
                .peek(m -> m.setStatus(newStatus)).toList();
        System.out.println("ms "+ms.size());
        return ms;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleRejectionBatch(List<Meter> rejectList, UserModel user) {

        // Separate PENDING-CREATED and others
        List<Meter> pendingCreatedMeters = rejectList.stream()
                .filter(m -> "Pending-created".equalsIgnoreCase(m.getMeterStage()))
                .toList();

        List<Meter> otherRejections = rejectList.stream()
                .filter(m -> !"Pending-created".equalsIgnoreCase(m.getMeterStage()))
                .toList();

        // Extract IDs for each group
        List<UUID> pendingMeterIds = pendingCreatedMeters.stream()
                .map(Meter::getMeterId)
                .toList();

        List<UUID> otherMeterIds = otherRejections.stream()
                .map(Meter::getMeterId)
                .toList();

        // Handle Pending-created Meters (DELETE + REJECT)
        if (!pendingCreatedMeters.isEmpty()) {
            log.info("Deleting meters: {}", pendingCreatedMeters.stream()
                    .map(Meter::getMeterNumber)
                    .toList());

            // --- SmartMeterInfo (only if not null)
            List<UUID> smartMeterIds = pendingCreatedMeters.stream()
                    .filter(m -> m.getSmartMeterInfo() != null)
                    .map(Meter::getMeterId)
                    .toList();

            if (!smartMeterIds.isEmpty()) {
                meterMapper.rejectSmartMeterInfoVersion(smartMeterIds, user.getOrgId(), user.getId(), "Rejected");
            }

            // --- MDMeterInfo (only if not null)
            List<UUID> mdMeterIds = pendingCreatedMeters.stream()
                    .filter(m -> m.getMdMeterInfo() != null)
                    .map(Meter::getMeterId)
                    .toList();

            if (!mdMeterIds.isEmpty()) {
                meterMapper.rejectMDMeterInfoVersion(mdMeterIds, user.getOrgId(), user.getId(), "Rejected");
            }

            // --- Update version table
            meterMapper.rejectVersionMeters(pendingMeterIds, user.getOrgId(), user.getId(), "Rejected");

            // --- Finally, delete from main meter table
            meterMapper.deleteMetersByMeterIds(pendingMeterIds);
        }

        // Handle Other Rejections (Only Mark Rejected, No Delete)
        if (!otherRejections.isEmpty()) {
            log.info("Marking other meters as rejected: {}", otherRejections.stream()
                    .map(Meter::getMeterNumber)
                    .toList());

            // --- SmartMeterInfo (only if not null)
            List<UUID> smartMeterIds = otherRejections.stream()
                    .filter(m -> m.getSmartMeterInfo() != null)
                    .map(Meter::getMeterId)
                    .toList();

            if (!smartMeterIds.isEmpty()) {
                meterMapper.rejectSmartMeterInfoVersion(smartMeterIds, user.getOrgId(), user.getId(), "Rejected");
            }

            // --- MDMeterInfo (only if not null)
            List<UUID> mdMeterIds = otherRejections.stream()
                    .filter(m -> m.getMdMeterInfo() != null)
                    .map(Meter::getMeterId)
                    .toList();

            if (!mdMeterIds.isEmpty()) {
                meterMapper.rejectMDMeterInfoVersion(mdMeterIds, user.getOrgId(), user.getId(), "Rejected");
            }

            // --- Update version meter record
            meterMapper.rejectVersionMeters(otherMeterIds, user.getOrgId(), user.getId(), "Rejected");
        }

        // Audit
        auditRejectBatch(rejectList, user);
    }

    /** Retry mechanism for smaller sub-batches */
    public int updateSubBatchTransactional(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        int success = 0;
        int subSize = 100;

        for (int i = 0; i < batch.size(); i += subSize) {
            int end = Math.min(i + subSize, batch.size());
            List<Meter> subList = batch.subList(i, end);
            try {
                success += updateBatchTransactional(subList, user, failedRecords);
            } catch (Exception e) {
                log.error("Sub-batch {} failed: {}", (i / subSize) + 1, e.getMessage());
//                subList.forEach(m -> failedRecords.add(m.getMeterNumber() + " - " + e.getMessage()));
                if (batch.size() > 50) {
                    success += approveSinglesFallbackAsync(batch, user, failedRecords);
                } else {
                    success += approveSinglesFallback(batch, user, failedRecords);
                }
            }
        }
        return success;
    }

    public int approveSinglesFallbackAsync(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        for (Meter meter : batch) {
            futures.add(approveSingleAsync(meter, user, failedRecords));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream().mapToInt(CompletableFuture::join).sum();
    }

    public int approveSinglesFallback(List<Meter> meters, UserModel user, List<GenericResp> failedRecords) {
        int successCount = 0;

        for (Meter meter : meters) {
            try {
                log.debug("Fallback single allocation for meter: {}", meter.getMeterNumber());
                approveSingleTransactional(meter, user);
                successCount++;
            } catch (Exception e) {
                String reason = extractErrorMessage(e);
                GenericResp resp = new GenericResp();
                resp.setId(meter.getMeterId().toString());
                resp.setMessage("Meter Approve failed: "+reason);
                resp.setData(meter.getMeterNumber());

                failedRecords.add(resp);

                log.warn("Meter {} failed individually: {}",meter.getMeterNumber(), reason);
            }
        }

        return successCount;
    }

    @Async
    public CompletableFuture<Integer> approveSingleAsync(Meter meter, UserModel user, List<GenericResp> failedRecords) {
        try {
            approveSingleTransactional(meter, user);
            return CompletableFuture.completedFuture(1);
        } catch (Exception e) {
            String reason = extractErrorMessage(e);
            GenericResp resp = new GenericResp();
            resp.setId(meter.getMeterId().toString());
            resp.setMessage("Meter Approve failed: "+reason);
            resp.setData(meter.getMeterNumber());

            failedRecords.add(resp);
//            failedRecords.add(String.format(
//                    "%s [Region: %s] (Allocation failed: %s)",
//                    meter.getMeterNumber(),
////                    meter.getNodeInfo().getRegionId(),
//                    reason
//            ));
            log.warn("Async allocation failed for meter {}: {}",  meter.getMeterNumber(), reason);
            return CompletableFuture.completedFuture(0);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void approveSingleTransactional(Meter meter, UserModel user) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

        // --- Step 2: Insert into main + version tables ---
        meterMapper.approvedMeterVersion(meter.getMeterStage(), meter.getStatus(), meter.getApproveBy(), meter.getUpdatedAt(), meter.getMeterNumber());

        meterMapper.approveMeter(meter);

        //fetch meter from the database
        Meter m = meterMapper.findById(meter.getMeterId(), user.getOrgId());
        //save to audit (mongodb)
        AuditLog auditLog = buildAuditLog(user, "Meter approved", meterName, m, metadata, "");
        auditRepository.save(auditLog);

    }


    /** Update or insert approved child meter data */
    private void updateChildMeterData(List<Meter> batch, UserModel user) {
        List<MDMeterInfo> mdList = new ArrayList<>();
        List<SmartMeterInfo> smartList = new ArrayList<>();
        List<PaymentMode> prepaidList = new ArrayList<>();
        List<MDMeterInfo> newMDMeters = new ArrayList<>();
        List<SmartMeterInfo> newSmartMeters = new ArrayList<>();
        System.out.println(">>>>>>>>>>>> updateChildMeterData");
        for (Meter meter : batch) {
            if (meter.getMdMeterInfo() != null) {
                if ("Pending-created".equalsIgnoreCase(meter.getMdMeterInfo().getMeterStage())) {
                    meter.getMdMeterInfo().setMeterStage("Created");
                    newMDMeters.add(meter.getMdMeterInfo());
                } else {
                    mdList.add(meter.getMdMeterInfo());
                }
            }

            if (meter.getSmartMeterInfo() != null) {
                if ("Pending-created".equalsIgnoreCase(meter.getSmartMeterInfo().getMeterStage())) {
                    meter.getSmartMeterInfo().setMeterStage("Created");
                    newSmartMeters.add(meter.getSmartMeterInfo());
                } else {
                    smartList.add(meter.getSmartMeterInfo());
                }
            }
        }

        // Approve existing version data
        if (!mdList.isEmpty()) meterMapper.batchApproveMDMeterInfo(mdList);
        if (!smartList.isEmpty()) meterMapper.batchApproveSmartMeterInfo(smartList);

        // Insert new ones into main tables
        if (!newMDMeters.isEmpty()) {
            meterMapper.batchApproveMDMeterInfo(newMDMeters);
            meterMapper.insertBatchApproveMDMeterInfo(newMDMeters);
        }
        if (!newSmartMeters.isEmpty()) {
            meterMapper.batchApproveSmartMeterInfo(newSmartMeters);
            meterMapper.insertBatchApproveSmartMeterInfo(newSmartMeters);
        }
    }

    /**
     * Record audit logs for each approved meter.
     */
    private void auditApproveBatch(List<Meter> batch, UserModel user, String desc) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
        for (Meter m : batch) {
            AuditLog auditLog = buildAuditLog(user, desc, "Bulk Meter", m, metadata, "");
            auditRepository.save(auditLog);
        }
    }

    private void auditRejectBatch(List<Meter> batch, UserModel user) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
        for (Meter m : batch) {
            AuditLog auditLog = buildAuditLog(user, "Meter reject", "Bulk Meter", m, metadata, "");
            auditRepository.save(auditLog);
        }
    }


    // ---------------------------
    // Simple helper to extract human-friendly message from exceptions
    // ---------------------------
    private String extractErrorMessage1(Exception ex) {
        if (ex == null) return "Unknown error";
        String m = ex.getMessage();
        return m == null ? ex.getClass().getSimpleName() : m;
    }


    private List<MeterRequest> processAllocateExcel(InputStream inputStream) throws IOException {
        List<MeterRequest> meters = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip header row safely
            if (rows.hasNext()) {
                rows.next();
            }

            while (rows.hasNext()) {
                Row row = rows.next();
                MeterRequest meter = new MeterRequest();

                meter.setMeterNumber(getStringCellValue(row.getCell(0)));
                meter.setRegionId(getStringCellValue(row.getCell(1)));

                meters.add(meter);
            }
        }
        return meters;
    }

    private List<MeterRequest> processAllocateCsv(InputStream inputStream) throws IOException {
        List<MeterRequest> meters = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            for (CSVRecord record : csvParser) {
                MeterRequest meter = new MeterRequest();
                meter.setMeterNumber(record.get("meter number"));
                meter.setRegionId(record.get("business hub"));

                meters.add(meter);
            }
        }
        return meters;
    }

    public Map<String, Object> bulkInsertMeters(List<Meter> meters, UserModel user) {
        Map<String, Object> result = new HashMap<>();
        List<GenericResp> failedRecords = new ArrayList<>();

        List<Manufacturer> manufacturers = meterMapper.getManufacturers(user.getOrgId());
        Map<String, UUID> manufacturerNameToId = manufacturers.stream()
                .collect(Collectors.toMap(
                        m -> m.getName().trim().toLowerCase(),
                        Manufacturer::getId
                ));


        int successCount = 0;

        int batchSize = 500; // try 500–1000 for optimal JDBC performance

        for (int i = 0; i < meters.size(); i += batchSize) {
            int end = Math.min(i + batchSize, meters.size());
            List<Meter> batch = meters.subList(i, end);

            try {
                insertBatchTransactional(batch, user, manufacturerNameToId, failedRecords);
                successCount += batch.size();
            } catch (Exception e) {
                log.warn("Batch {} failed — retrying sub batch upload", (i / batchSize) + 1);
                // Attempt smaller sub-batches to isolate failure
                successCount += insertSubBatchTransactional(batch, user, manufacturerNameToId, failedRecords);
            }
        }

        result.put("totalRecords", meters.size());
        result.put("successCount", successCount);
        result.put("failedCount", failedRecords.size());
        result.put("failedRecords", failedRecords);

        if (!failedRecords.isEmpty()) {
            throw new GlobalExceptionHandler.PartialFailureException(
                    failedRecords.size() + " of " + meters.size() + " Meters upload failed",
                    result
            );
        }

        return ResponseMap.response(
                status.getSuccessCode(),
                successCount + " of " + meters.size() + " Meters uploaded successfully",
                result
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void insertBatchTransactional(List<Meter> batch, UserModel user,  Map<String, UUID> manufacturerNameToId, List<GenericResp> failedRecords) {
        prepareMeters(batch, user, manufacturerNameToId, failedRecords);

        // Step 1: Insert main meters
        meterMapper.insertMeters(batch);

        // Step 2: Map 'id' → 'meterId' before inserting version records
        for (Meter meter : batch) {
            meter.setMeterId(meter.getId()); // Copy generated ID
        }
        // Insert into meter_versions (replica)
        meterMapper.insertMeterVersions(batch);

        // Insert related info
        insertChildMeterData(batch, user);
        // Audit logs
        auditBatch(batch, user, "Meter created");
    }

    public int insertSubBatchTransactional(List<Meter> batch, UserModel user,  Map<String, UUID> manufacturerNameToId, List<GenericResp> failedRecords) {
        int successCount = 0;
        int subBatchSize = 100;


        for (int i = 0; i < batch.size(); i += subBatchSize) {
            int end = Math.min(i + subBatchSize, batch.size());
            List<Meter> subBatch = batch.subList(i, end);

            try {
                insertBatchTransactional(subBatch, user, manufacturerNameToId, failedRecords);
                successCount += subBatch.size();
            } catch (Exception e) {
                log.warn("Sub-batch failed (size={}): {}", subBatch.size(), e.getMessage());

                if (subBatch.size() > 50) {
                    successCount += insertSinglesFallbackAsync(subBatch, user, failedRecords);
                } else {
                    successCount += insertSinglesFallback(subBatch, user, failedRecords);
                }

//                successCount += insertSinglesFallback(subBatch, user, failedRecords);
            }
        }
        return successCount;
    }

    public int insertSinglesFallbackAsync(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        for (Meter meter : batch) {
            futures.add(insertSingleAsync(meter, user, failedRecords));
        }

        // Wait for all tasks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Sum successful inserts
        return futures.stream().mapToInt(f -> f.join()).sum();
    }

    public int insertSinglesFallback(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        int successCount = 0;

        for (Meter meter : batch) {
            try {
                log.debug("Fallback single upload for meter: {}", meter.getMeterNumber());
                insertSingleTransactional(meter, user);
                successCount++;
            } catch (Exception e) {
                String reason = extractErrorMessage(e);
                GenericResp resp = new GenericResp();
                resp.setId(meter.getMeterId().toString());
                resp.setMessage("Meter single save failed: "+reason);
                resp.setData(meter.getMeterNumber());

                failedRecords.add(resp);
//                failedRecords.add(meter.getMeterNumber() + " (" + reason + ")");
                log.warn("Meter {} failed individually: {}", meter.getMeterNumber(), reason);
            }
        }

        return successCount;
    }

    private void prepareMeters(
            List<Meter> batch,
            UserModel user,
            Map<String, UUID> manufacturerNameToId,
            List<GenericResp> failedRecords
    ) {
        Iterator<Meter> iterator = batch.iterator();

        while (iterator.hasNext()) {
            Meter meter = iterator.next();

            // --- Validate and set Manufacturer ID ---
            String manuName = meter.getMeterManufacturerName();
            if (manuName == null || manuName.trim().isEmpty()) {
                GenericResp resp = new GenericResp();
                resp.setId(meter.getMeterId().toString());
                resp.setMessage("Missing manufacturer name");
                resp.setData(meter.getMeterNumber());

                failedRecords.add(resp);
//                failedRecords.add(meter.getMeterNumber() + " (Missing manufacturer name)");
                iterator.remove();
                continue;
            }

            UUID manuId = manufacturerNameToId.get(manuName.trim().toLowerCase());
            if (manuId == null) {
                GenericResp resp = new GenericResp();
                resp.setId(meter.getMeterId().toString());
                resp.setMessage("Invalid manufacturer: "+manuName);
                resp.setData(meter.getMeterNumber());

                failedRecords.add(resp);
//                failedRecords.add(meter.getMeterNumber() + " (Invalid manufacturer: " + manuName + ")");
                iterator.remove();
                continue;
            }

            meter.setMeterManufacturer(manuId);

            // --- Default Meter Fields ---
            meter.setOrgId(user.getOrgId());
            meter.setCreatedBy(user.getId());
            meter.setStatus("Active");
            meter.setMeterStage("Pending-created");
            meter.setType("NON-VIRTUAL");
            meter.setDescription("Newly Added");
        }
    }

    private void insertChildMeterData(List<Meter> batch, UserModel user) {
        List<SmartMeterInfo> smartInfos = new ArrayList<>();
        List<MDMeterInfo> mdInfos = new ArrayList<>();

        for (Meter m : batch) {
            if (m.getSmartMeterInfo() != null) {
                m.getSmartMeterInfo().setMeterId(m.getId());
                m.getSmartMeterInfo().setCreatedBy(user.getId());
                m.getSmartMeterInfo().setOrgId(user.getOrgId());
                m.getSmartMeterInfo().setMeterStage("Pending-created");
                m.getSmartMeterInfo().setDescription("Newly Added");
                smartInfos.add(m.getSmartMeterInfo());
            }
            if (m.getMdMeterInfo() != null) {
                m.getMdMeterInfo().setMeterId(m.getId());
                m.getMdMeterInfo().setCreatedBy(user.getId());
                m.getMdMeterInfo().setOrgId(user.getOrgId());
                m.getMdMeterInfo().setMeterStage("Pending-created");
                m.getMdMeterInfo().setDescription("Newly Added");
                mdInfos.add(m.getMdMeterInfo());
            }
        }

        if (!smartInfos.isEmpty()) meterMapper.insertBatchSmartMeterInfoVersion(smartInfos);
        if (!mdInfos.isEmpty()) meterMapper.insertBatchMDMeterInfoVersion(mdInfos);
    }

    private void auditBatch(List<Meter> batch, UserModel user, String desc) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
        for (Meter m : batch) {
            AuditLog auditLog = buildAuditLog(user, desc, "Meter", m, metadata, "");
            auditRepository.save(auditLog);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void insertSingleTransactional(Meter meter, UserModel user) {
//        try {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

        // --- Step 1: Prepare core meter entity ---
        meter.setOrgId(user.getOrgId());
        meter.setCreatedBy(user.getId());
        meter.setStatus("Active");
        meter.setMeterStage("Pending-created");
        meter.setType("NON-VIRTUAL");
        meter.setDescription("Newly Added");

        // --- Step 2: Insert into main + version tables ---
        meterMapper.insertSingleBatchMeter(meter);

        // Link version table by setting meterId = generated meter.id
        meter.setMeterId(meter.getId());
        meterMapper.insertSingleBatchMeterVersion(meter);

        // --- Step 3: Child entities ---
        if ("md".equalsIgnoreCase(meter.getMeterClass())) {
            insertMDMeterInfo(meter, user);
        }
        if (Boolean.TRUE.equals(meter.getSmartStatus())) {
            insertSmartMeterInfo(meter, user);
        }

        // --- Step 4: Audit logging ---
        Meter newMeter = meterMapper.findByIdVersion(meter.getId(), user.getOrgId());
        AuditLog auditLog = buildAuditLog(user, "Meter created", meterName, newMeter, metadata, "");
        auditRepository.save(auditLog);

//        } catch (Exception e) {
//            log.error("Failed to insert meter {}: {}", meter.getMeterNumber(), e.getMessage(), e);
//            throw e; // rethrow so parent caller can track failure count
//        }
    }

    // Parse CSV file into a list of Meter objects
    public static List<Meter> processCsv(InputStream inputStream, UserModel user) throws IOException {
        List<Meter> meters = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            for (CSVRecord record : csvParser) {
                Meter meter = new Meter();
                meter.setMeterNumber(record.get("meterNumber"));
                meter.setSimNumber(record.get("simNumber"));
                meter.setMeterCategory(record.get("meterCategory"));
                meter.setMeterClass(record.get("meterClass"));
                meter.setMeterManufacturerName(record.get("meterManufacturerName"));
                meter.setMeterType(record.get("meterType"));
                meter.setOldSgc(record.get("oldSgc"));
                meter.setNewSgc(record.get("newSgc"));
                meter.setOldKrn(record.get("oldKrn"));
                meter.setNewKrn(record.get("newKrn"));
                meter.setOldTariffIndex(Long.parseLong(record.get("oldTariffIndex")));
                meter.setNewTariffIndex(Long.parseLong(record.get("newTariffIndex")));

                boolean isSmart = Boolean.parseBoolean(record.get("smartStatus"));
                meter.setSmartStatus(isSmart);

                // Handle smart meter info
                if (isSmart) {
                    if (meter.getSmartMeterInfo() == null) {
                        meter.setSmartMeterInfo(new SmartMeterInfo());
                    }
                    meter.getSmartMeterInfo().setMeterModel(record.get("meterModel"));
                    meter.getSmartMeterInfo().setProtocol(record.get("protocol"));
                    meter.getSmartMeterInfo().setAuthentication(record.get("authentication"));
                    meter.getSmartMeterInfo().setPassword(record.get("password"));
                }

                // Handle MD meter info (only if class matches certain type)
                String meterClass = record.get("meterClass");
                if ("MD".equalsIgnoreCase(meterClass)) { // or whatever condition applies
                    if (meter.getMdMeterInfo() == null) {
                        meter.setMdMeterInfo(new MDMeterInfo());
                    }
                    meter.getMdMeterInfo().setCtRatioNum(parseLongSafe(record.get("ctRatioNum")));
                    meter.getMdMeterInfo().setCtRatioDenom(parseLongSafe(record.get("ctRatioDenom")));
                    meter.getMdMeterInfo().setVoltRatioNum(parseLongSafe(record.get("voltRatioNum")));
                    meter.getMdMeterInfo().setVoltRatioDenom(parseLongSafe(record.get("voltRatioDenom")));
                    meter.getMdMeterInfo().setMultiplier(parseLongSafe(record.get("multiplier")));
                    meter.getMdMeterInfo().setMeterRating(parseLongSafe(record.get("meterRating")));
                    meter.getMdMeterInfo().setInitialReading(parseLongSafe(record.get("initialReading")));
                    meter.getMdMeterInfo().setDial(parseLongSafe(record.get("dial")));
                    meter.getMdMeterInfo().setLatitude(record.get("latitude"));
                    meter.getMdMeterInfo().setLongitude(record.get("longitude"));
                }

                meters.add(meter);
            }
        }
        return meters;
    }

    // Parse Excel (.xlsx) file into a list of Meter objects
    public static List<Meter> processExcel(InputStream inputStream, UserModel user) throws IOException {
        List<Meter> meters = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip header row safely
            if (rows.hasNext()) {
                rows.next();
            }

            while (rows.hasNext()) {
                Row row = rows.next();
                Meter meter = new Meter();

                meter.setMeterNumber(getStringCellValue(row.getCell(0)));
                meter.setSimNumber(getStringCellValue(row.getCell(1)));
                meter.setMeterCategory(getStringCellValue(row.getCell(2)));
                meter.setMeterClass(getStringCellValue(row.getCell(3)));
                meter.setMeterManufacturerName(getStringCellValue(row.getCell(4)));
                meter.setMeterType(getStringCellValue(row.getCell(5)));

                meter.setOldSgc(getStringCellValue(row.getCell(7)));
                meter.setNewSgc(getStringCellValue(row.getCell(8)));
                meter.setOldKrn(getStringCellValue(row.getCell(9)));
                meter.setNewKrn(getStringCellValue(row.getCell(10)));

                meter.setOldTariffIndex(parseLongSafe(getStringCellValue(row.getCell(11))));
                meter.setNewTariffIndex(parseLongSafe(getStringCellValue(row.getCell(12))));

                boolean isSmart = Boolean.parseBoolean(getStringCellValue(row.getCell(6)));
                meter.setSmartStatus(isSmart);

                // Smart meter info
                if (isSmart) {
                    if (meter.getSmartMeterInfo() == null) {
                        meter.setSmartMeterInfo(new SmartMeterInfo());
                    }
                    meter.getSmartMeterInfo().setMeterModel(getStringCellValue(row.getCell(13)));
                    meter.getSmartMeterInfo().setProtocol(getStringCellValue(row.getCell(14)));
                    meter.getSmartMeterInfo().setAuthentication(getStringCellValue(row.getCell(15)));
                    meter.getSmartMeterInfo().setPassword(getStringCellValue(row.getCell(16)));
                }

                // MD meter info
                String meterClass = meter.getMeterClass();
                if ("MD".equalsIgnoreCase(meterClass)) {
                    if (meter.getMdMeterInfo() == null) {
                        meter.setMdMeterInfo(new MDMeterInfo());
                    }
                    meter.getMdMeterInfo().setCtRatioNum(parseLongSafe(getStringCellValue(row.getCell(17))));
                    meter.getMdMeterInfo().setCtRatioDenom(parseLongSafe(getStringCellValue(row.getCell(18))));
                    meter.getMdMeterInfo().setVoltRatioNum(parseLongSafe(getStringCellValue(row.getCell(19))));
                    meter.getMdMeterInfo().setVoltRatioDenom(parseLongSafe(getStringCellValue(row.getCell(20))));
                    meter.getMdMeterInfo().setMultiplier(parseLongSafe(getStringCellValue(row.getCell(21))));
                    meter.getMdMeterInfo().setMeterRating(parseLongSafe(getStringCellValue(row.getCell(22))));
                    meter.getMdMeterInfo().setInitialReading(parseLongSafe(getStringCellValue(row.getCell(23))));
                    meter.getMdMeterInfo().setDial(parseLongSafe(getStringCellValue(row.getCell(24))));
                    meter.getMdMeterInfo().setLatitude(getStringCellValue(row.getCell(25)));
                    meter.getMdMeterInfo().setLongitude(getStringCellValue(row.getCell(26)));
                }

                meters.add(meter);
            }
        }
        return meters;
    }

    @Override
    public ByteArrayInputStream exportActualMeter() {

        UserModel user = handleUserValidation();

        List<Meter> allMeters = meterMapper.getAllMeters(user.getOrgId(), "NON-VIRTUAL");

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Meter Report");

            // Create header
            String[] headers = {
                    "S/N", "Meter Number", "SIM No", "Old SGC",
                    "New SGC", "Manufacturer", "Class", "Category", "Meter Stage", "Activation Status", "Feeder", "DSS", "Tariff"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // Data rows
            for (int i = 0; i < allMeters.size(); i++) {
                Meter meter = allMeters.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(meter.getMeterNumber());
                row.createCell(2).setCellValue(meter.getSimNumber());
                row.createCell(3).setCellValue(meter.getOldSgc());
                row.createCell(4).setCellValue(meter.getNewSgc());
                row.createCell(5).setCellValue(meter.getManufacturer().getName());
                row.createCell(6).setCellValue(meter.getMeterClass());
                row.createCell(7).setCellValue(meter.getMeterCategory());
                row.createCell(8).setCellValue(meter.getMeterStage());
                row.createCell(9).setCellValue(meter.getStatus());
                row.createCell(10).setCellValue(meter.getFeederInfo() == null ? "" : meter.getFeederInfo().getName());
                row.createCell(11).setCellValue(meter.getDssInfo() == null ? "" : meter.getDssInfo().getName());
                row.createCell(12).setCellValue(meter.getTariffInfo() == null ? "" : meter.getTariffInfo().getName());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Error exporting meter data", e);
        }
    }

    @Override
    public ByteArrayInputStream exportVirtualMeter() {

        UserModel user = handleUserValidation();

        List<Meter> allMeters = meterMapper.getAllMeters(user.getOrgId(), "VIRTUAL");

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Meter Report");

            // Create header
            String[] headers = {
                    "S/N", "Customer ID", "Meter Number", "Account Number",
                        "Feeder", "DSS", "CIN", "Tariff", "Status"
                };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // Data rows
            for (int i = 0; i < allMeters.size(); i++) {
                Meter meter = allMeters.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(meter.getCustomerId());
                row.createCell(2).setCellValue(meter.getMeterNumber());
                row.createCell(3).setCellValue(meter.getAccountNumber());
                row.createCell(4).setCellValue(meter.getFeederInfo().getName());
                row.createCell(5).setCellValue(meter.getDssInfo().getName());
                row.createCell(6).setCellValue(meter.getCin());
                row.createCell(7).setCellValue(meter.getTariffInfo().getName());
                row.createCell(8).setCellValue(meter.getStatus());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Error exporting meter data", e);
        }
    }

    @Override
    public Map<String, Object> bulkAssign(MultipartFile file) throws IOException {
        try {
            UserModel user = handleUserValidation();

            // Determine file type
            String filename = Optional.ofNullable(file.getOriginalFilename())
                    .orElseThrow(() -> new IOException("File has no name"));

            List<AssignMeterToCustomer> meters;
            if (filename.endsWith(".csv")) {
                meters = processAssignCsv(file.getInputStream());
            } else if (filename.endsWith(".xlsx")) {
                meters = processAssignExcel(file.getInputStream());
            } else {
                throw new IOException("Unsupported file format. Only .csv or .xlsx allowed.");
            }
            Map<String, Object> result = bulkAssignMeters(meters, user);
            return result;

        } catch (Exception e) {
            log.error("Error in bulk assign upload: {}", e.getMessage(), e);
            genericHandler.logIncidentReport("Bulk assign service failed");
            genericHandler.logAndSaveException(e, "Bulk assign meter");
            throw new IOException("Bulk allocate failed: " + e.getMessage());
        }
    }


    @Override
    public Map<String, Object> bulkVirtualAssign(MultipartFile file) throws IOException {
        try {
            UserModel user = handleUserValidation();

            // Determine file type
            String filename = Optional.ofNullable(file.getOriginalFilename())
                    .orElseThrow(() -> new IOException("File has no name"));

            List<AssignMeterToCustomer> meters;
            if (filename.endsWith(".csv")) {
                meters = processVirtualAssignCsv(file.getInputStream());
            } else if (filename.endsWith(".xlsx")) {
                meters = processVirtualAssignExcel(file.getInputStream());
            } else {
                throw new IOException("Unsupported file format. Only .csv or .xlsx allowed.");
            }
            Map<String, Object> result = bulkAssignVirtualMeters(meters, user);
            return result;

        } catch (Exception e) {
            log.error("Error in bulk assign upload: {}", e.getMessage(), e);
            genericHandler.logIncidentReport("Bulk virtual assign service failed");
            genericHandler.logAndSaveException(e, "Bulk virtual assign meter");
            throw new IOException("Bulk virtual assign failed: " + e.getMessage());
        }
    }

//    @Override
    public Map<String, Object> bulkAssignMeters(List<AssignMeterToCustomer> assign, UserModel user) {
        Map<String, Object> result = new HashMap<>();
        List<GenericResp> failedRecords = new ArrayList<>();
        int successCount = 0;

        if (assign == null || assign.isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("No records found in uploaded file");
        }

        final int BATCH_SIZE = 500;

        for (int i = 0; i < assign.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, assign.size());
            List<AssignMeterToCustomer> subBatch = assign.subList(i, end);

            // Extract required lists
            List<String> meterNumbers = subBatch.stream()
                    .map(AssignMeterToCustomer::getMeterNumber)
                    .filter(num -> num != null && !num.trim().isEmpty())
                    .map(String::trim)
                    .toList();

            List<String> tariffNames = subBatch.stream()
                    .map(AssignMeterToCustomer::getTariffName)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> customerIds = subBatch.stream()
                    .map(AssignMeterToCustomer::getCustomerId)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> dssIds = subBatch.stream()
                    .map(AssignMeterToCustomer::getDssAssetId)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> feederIds = subBatch.stream()
                    .map(AssignMeterToCustomer::getFeederAssetId)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> cins = subBatch.stream()
                    .map(AssignMeterToCustomer::getCin)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> state = subBatch.stream()
                    .map(AssignMeterToCustomer::getState)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> city = subBatch.stream()
                    .map(AssignMeterToCustomer::getCity)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> houseNo = subBatch.stream()
                    .map(AssignMeterToCustomer::getHouseNo)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();
            List<String> streetName = subBatch.stream()
                    .map(AssignMeterToCustomer::getStreetName)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

//            if (meterNumbers.isEmpty() || tariffNames.isEmpty() || customerIds.isEmpty() ||
//                    dssIds.isEmpty() || feederIds.isEmpty() || cins.isEmpty()) {
//                subBatch.forEach(req -> failedRecords.add(
//                        String.format("%s [TariffName: %s, customerId: %s, dssAssetId: %s, feederAssetId: %s, cin: %s] (Invalid or missing data)",
//                                req.getMeterNumber(), req.getTariffName(), req.getCustomerId(), req.getDssAssetId(), req.getFeederAssetId(), req.getCin())
//                ));
//                continue;
//            }

            if (meterNumbers.isEmpty() || tariffNames.isEmpty() || customerIds.isEmpty()
                    || dssIds.isEmpty() || feederIds.isEmpty() || cins.isEmpty()) {
                subBatch.forEach(req -> {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Missing meter number, tariff name, customer id, dss asset id, feeder asset id or cin");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
                });

                continue;
            }

            if (state.isEmpty() || city.isEmpty() || houseNo.isEmpty() || streetName.isEmpty()) {
                subBatch.forEach(req -> {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Missing state, city, customer id, houseNo, or streetName");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
                });

                continue;
            }

//            if (state.isEmpty() || city.isEmpty() || houseNo.isEmpty() || streetName.isEmpty()) {
//                subBatch.forEach(req -> failedRecords.add(
//                        String.format("%s [State: %s, city: %s, houseNo: %s, streetName: %s] (Invalid or missing data)",
//                                req.getMeterNumber(), req.getState(), req.getCity(), req.getHouseNo(), req.getStreetName())
//                ));
//                continue;
//            }

            // Fetch from DB
            List<Meter> meters = meterMapper.getUnassignMetersByMeterNumbers(meterNumbers, user.getOrgId());
            Map<String, Meter> meterMap = meters.stream()
                    .collect(Collectors.toMap(Meter::getMeterNumber, m -> m));

            List<Meter> cin = meterMapper.getMetersByCins(cins, user.getOrgId());
            Map<String, Meter> cinMap = meters.stream()
                    .collect(Collectors.toMap(Meter::getCin, m -> m));

            List<Tariff> tariff = meterMapper.getTariffByNames(tariffNames, user.getOrgId());
            Map<String, UUID> tariffMap = tariff.stream()
                    .collect(Collectors.toMap(Tariff::getName, Tariff::getId));

            List<Customer> cId = meterMapper.getByCustomerIds(customerIds, user.getOrgId());
            Map<String, String> customerIdMap = cId.stream()
                    .collect(Collectors.toMap(Customer::getCustomerId, Customer::getCustomerId));

            List<SubStationTransformerFeederLine> dssAssetId = meterMapper.getDss(dssIds, user.getOrgId());
            Map<String, UUID> dssIdMap = dssAssetId.stream()
                    .collect(Collectors.toMap(SubStationTransformerFeederLine::getAssetId, SubStationTransformerFeederLine::getNodeId));

            List<SubStationTransformerFeederLine> feederAssetId = meterMapper.getFeeder(feederIds, user.getOrgId());
            Map<String, UUID> feederIdMap = feederAssetId.stream()
                    .collect(Collectors.toMap(SubStationTransformerFeederLine::getAssetId, SubStationTransformerFeederLine::getNodeId));

            List<Meter> validAssign = new ArrayList<>();

            List<MeterAssignLocation> validAssignLocation = new ArrayList<>();

            List<PaymentMode> validAssignPayment = new ArrayList<>();

            for (AssignMeterToCustomer req : subBatch) {
                Meter meter = meterMap.get(req.getMeterNumber());
                Meter c = cinMap.get(req.getCin());
                UUID tariffId = tariffMap.get(req.getTariffName());
                String customerId = customerIdMap.get(req.getCustomerId());
                UUID dssId = dssIdMap.get(req.getDssAssetId());
                UUID feederId = feederIdMap.get(req.getFeederAssetId());

                if (meter == null) {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Meter not found, deactivated or in a pending state");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format(
//                            "%s (Meter not found, deactivated or in a pending state)",
//                            req.getMeterNumber()
//                    ));
                    continue;
                }

                if (cin != null) {
                    GenericResp resp = new GenericResp();
                    resp.setId(meter.getMeterId().toString());
                    resp.setMessage("CIN already exist");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format("%s (CIN already exist)", req.getCin()));
                    continue; }

                if (tariffId == null) {
                    GenericResp resp = new GenericResp();
                    resp.setId(meter.getMeterId().toString());
                    resp.setMessage("Tariff not found, deactivated or have a pending state");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format("%s [Tariff: %s] (Tariff not found, deactivated or have a pending state)", req.getMeterNumber(), req.getTariffName()));
                    continue;
                }

                if (customerId == null) {
                    GenericResp resp = new GenericResp();
                    resp.setId(meter.getMeterId().toString());
                    resp.setMessage("Customer not found or blocked");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format("%s [Customer: %s] (Customer not found or blocked)", req.getMeterNumber(), req.getCustomerId()));
                    continue;
                }

                if (dssId == null) {
                    GenericResp resp = new GenericResp();
                    resp.setId(meter.getMeterId().toString());
                    resp.setMessage("Dss not found");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format("%s [DssAssetId: %s] (Dss not found)", req.getMeterNumber(), req.getDssAssetId()));
                    continue;
                }

                if (feederId == null) {
                    GenericResp resp = new GenericResp();
                    resp.setId(meter.getMeterId().toString());
                    resp.setMessage("Feeder not found");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format("%s [FeederAssetId: %s] (Feeder not found)", req.getMeterNumber(), req.getFeederAssetId()));
                    continue;
                }

                // Auto-generate unique account number
                String generatedAccountNumber = handleGetAccountNumber();

                meter.setOrgId(user.getOrgId());
                meter.setCin(req.getCin());
                meter.setAccountNumber(generatedAccountNumber);
                meter.setNodeId(feederId);
                meter.setDss(dssId);
                meter.setCustomerId(customerId);
                meter.setTariff(tariffId);
                meter.setOrgId(user.getOrgId());
                meter.setMeterStage("Pending-assigned");
                meter.setCreatedBy(user.getId());
                meter.setDescription("Meter Assigned");

//                System.out.println(">>>>meterId: "+meter.getId());

                // === New fields ===
                if (meter.getMeterAssignLocation() == null) {
                    meter.setMeterAssignLocation(new MeterAssignLocation());
                }
                if (meter.getPaymentMode() == null) {
                    meter.setPaymentMode(new PaymentMode());
                }

                MeterAssignLocation location = meter.getMeterAssignLocation();
                location.setOrgId(user.getOrgId());
                location.setCreatedBy(user.getId());
                location.setMeterStage("Pending-assigned");
                location.setDescription("Location assigned");
                location.setMeterId(meter.getId());
                location.setState(req.getState());
                location.setCity(req.getCity());
                location.setHouseNo(req.getHouseNo());
                location.setStreetName(req.getStreetName());


                // === Payment info only for PREPAID meters ===
                if ("PREPAID".equalsIgnoreCase(meter.getMeterCategory())) {
                    PaymentMode payment = new PaymentMode();
                    payment.setOrgId(user.getOrgId());
                    payment.setMeterId(meter.getId());
                    payment.setCreatedBy(user.getId());
                    payment.setDescription("Payment mode assigned");
                    payment.setMeterStage("Pending-assigned");
                    payment.setCreditPaymentMode(req.getCreditPaymentMode());
                    payment.setDebitPaymentMode(req.getDebitPaymentMode());
                    payment.setCreditPaymentPlan(req.getCreditPaymentPlan());
                    payment.setDebitPaymentPlan(req.getDebitPaymentPlan());
                    validAssignPayment.add(payment);
                }
                validAssign.add(meter);

                validAssignLocation.add(location);
            }

            if (validAssign.isEmpty()) continue;

            try {
                log.info("Processing batch {} - {} ({} records)", i, end - 1, subBatch.size());
                int assigned = assignBatchTransactional(validAssign, user, validAssignLocation,validAssignPayment);
                successCount += assigned;
            } catch (Exception e) {
                log.warn("Batch {} failed — retrying smaller sub-batches: {}", (i / BATCH_SIZE) + 1, e.getMessage());
                successCount += assignSubBatchTransactional(validAssign, user, failedRecords, validAssignLocation, validAssignPayment);
            }
        }

        int total = successCount + failedRecords.size();

        result.put("totalRecords", total);
        result.put("successCount", successCount);
        result.put("failedCount", failedRecords.size());
        result.put("failedRecords", failedRecords);

        if (!failedRecords.isEmpty()) {
            throw new GlobalExceptionHandler.PartialFailureException(
                    failedRecords.size() + " of " + total + " Meters assigned failed",
                    result
            );
        }

        return ResponseMap.response(
                status.getSuccessCode(),
                String.format("%d of %d meters assigned successfully", successCount, total),
                result
        );
    }

    public Map<String, Object> bulkAssignVirtualMeters(List<AssignMeterToCustomer> assign, UserModel user) {
        Map<String, Object> result = new HashMap<>();
        List<GenericResp> failedRecords = new ArrayList<>();
        int successCount = 0;

        if (assign == null || assign.isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("No records found in uploaded file");
        }

        final int BATCH_SIZE = 500;

        for (int i = 0; i < assign.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, assign.size());
            List<AssignMeterToCustomer> subBatch = assign.subList(i, end);

//            // Extract required lists
            List<String> tariffNames = subBatch.stream()
                    .map(AssignMeterToCustomer::getTariffName)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> meterClass = subBatch.stream()
                    .map(AssignMeterToCustomer::getMeterClass)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> customerIds = subBatch.stream()
                    .map(AssignMeterToCustomer::getCustomerId)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> dssIds = subBatch.stream()
                    .map(AssignMeterToCustomer::getDssAssetId)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> feederIds = subBatch.stream()
                    .map(AssignMeterToCustomer::getFeederAssetId)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> cins = subBatch.stream()
                    .map(AssignMeterToCustomer::getCin)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> state = subBatch.stream()
                    .map(AssignMeterToCustomer::getState)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> city = subBatch.stream()
                    .map(AssignMeterToCustomer::getCity)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> houseNo = subBatch.stream()
                    .map(AssignMeterToCustomer::getHouseNo)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();
            List<String> streetName = subBatch.stream()
                    .map(AssignMeterToCustomer::getStreetName)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            if (tariffNames.isEmpty() || customerIds.isEmpty()
                    || dssIds.isEmpty() || feederIds.isEmpty() || cins.isEmpty()) {
                subBatch.forEach(req -> {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Missing tariff name, customer id, dss asset id, feeder asset id or cin");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
                });

                continue;
            }

            if (state.isEmpty() || city.isEmpty() || houseNo.isEmpty() || streetName.isEmpty()) {
                subBatch.forEach(req -> {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Missing state, city, customer id, houseNo, or streetName");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
                });

                continue;
            }

//            if (tariffNames.isEmpty() || customerIds.isEmpty() || meterClass.isEmpty() ||
//                    dssIds.isEmpty() || feederIds.isEmpty() || cins.isEmpty()) {
//                subBatch.forEach(req -> failedRecords.add(
//                        String.format("[TariffName: %s, customerId: %s, dssAssetId: %s, feederAssetId: %s, cin: %s, meterClass: %s] (Invalid or missing data)",
//                                req.getTariffName(), req.getCustomerId(), req.getDssAssetId(), req.getFeederAssetId(), req.getCin(), req.getMeterClass())
//                ));
//                continue;
//            }
//
//
//            if (state.isEmpty() || city.isEmpty() || houseNo.isEmpty() || streetName.isEmpty()) {
//                subBatch.forEach(req -> failedRecords.add(
//                        String.format("[State: %s, city: %s, houseNo: %s, streetName: %s] (Invalid or missing data)",
//                                req.getState(), req.getCity(), req.getHouseNo(), req.getStreetName())
//                ));
//                continue;
//            }

            // Fetch from DB
            List<Meter> meters = meterMapper.getMetersByCins(cins, user.getOrgId());
            Map<String, Meter> meterMap = meters.stream()
                    .collect(Collectors.toMap(Meter::getCin, m -> m));

            // Fetch from DB
            List<Tariff> tariff = meterMapper.getTariffByNames(tariffNames, user.getOrgId());
            Map<String, UUID> tariffMap = tariff.stream()
                    .collect(Collectors.toMap(Tariff::getName, Tariff::getId));

            List<Customer> cId = meterMapper.getByCustomerIds(customerIds, user.getOrgId());
            Map<String, String> customerIdMap = cId.stream()
                    .collect(Collectors.toMap(Customer::getCustomerId, Customer::getCustomerId));

            List<SubStationTransformerFeederLine> dssAssetId = meterMapper.getDss(dssIds, user.getOrgId());
            Map<String, UUID> dssIdMap = dssAssetId.stream()
                    .collect(Collectors.toMap(SubStationTransformerFeederLine::getAssetId, SubStationTransformerFeederLine::getNodeId));

            List<SubStationTransformerFeederLine> feederAssetId = meterMapper.getFeeder(feederIds, user.getOrgId());
            Map<String, UUID> feederIdMap = feederAssetId.stream()
                    .collect(Collectors.toMap(SubStationTransformerFeederLine::getAssetId, SubStationTransformerFeederLine::getNodeId));

            List<Meter> validAssign = new ArrayList<>();

            List<MeterAssignLocation> validAssignLocation = new ArrayList<>();


            for (AssignMeterToCustomer req : subBatch) {
                Meter ci = meterMap.get(req.getCin());
                UUID tariffId = tariffMap.get(req.getTariffName());
                String customerId = customerIdMap.get(req.getCustomerId());
                UUID dssId = dssIdMap.get(req.getDssAssetId());
                UUID feederId = feederIdMap.get(req.getFeederAssetId());

                if (ci != null) {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("CIN already exist");
                    resp.setData(req.getCin());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format("%s (CIN already exist)", req.getCin()));
                    continue;
                }

                if (tariffId == null) {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Tariff not found, deactivated or have a pending state");
                    resp.setData(req.getTariffName());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format("%s [Tariff: %s] (Tariff not found, deactivated or have a pending state)", req.getCustomerId(), req.getTariffName()));
                    continue;
                }

                if (customerId == null) {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Customer not found or blocked");
                    resp.setData(req.getTariffName());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format("%s (Customer not found or blocked)", req.getCustomerId()));
                    continue;
                }

                if (dssId == null) {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Dss not found");
                    resp.setData(req.getDssAssetId());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format("%s [DssAssetId: %s] (Dss not found)", req.getCustomerId(), req.getDssAssetId()));
                    continue;
                }

                if (feederId == null) {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Feeder not found");
                    resp.setData(req.getFeederAssetId());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format("%s [FeederAssetId: %s] (Feeder not found)", req.getCustomerId(), req.getFeederAssetId()));
                    continue;
                }



                // Auto-generate unique account number
                String generatedAccountNumber = handleGetAccountNumber();

                // Auto-generate unique meter number
                String generateMeterNumber = handleGetVirtualMeter();

                Meter meter = new Meter();

                meter.setMeterNumber(generateMeterNumber);
                meter.setOrgId(user.getOrgId());
                meter.setCin(req.getCin());
                meter.setAccountNumber(generatedAccountNumber);
                meter.setType("VIRTUAL");
                meter.setSimNumber("VIRTUAL");
                meter.setMeterClass(req.getMeterClass());
                meter.setMeterType("Electricity");
                meter.setOldSgc("0");
                meter.setNewSgc("0");
                meter.setOldKrn("0");
                meter.setNewKrn("0");
                meter.setOldTariffIndex(1L);
                meter.setNewTariffIndex(1L);
                meter.setNodeId(feederId);
                meter.setSmartStatus(false);
                meter.setDss(dssId);
                meter.setCustomerId(customerId);
                meter.setTariff(tariffId);
                meter.setStatus("Active");
                meter.setOrgId(user.getOrgId());
                meter.setMeterStage("Pending-assigned");
                meter.setCreatedBy(user.getId());
                meter.setDescription("Meter Assigned");
                meter.setFixedEnergy(req.getFixedEnergy());
                meter.setMeterCategory("Postpaid");

                // === New fields ===
                if (meter.getMeterAssignLocation() == null) {
                    meter.setMeterAssignLocation(new MeterAssignLocation());
                }

                MeterAssignLocation location = meter.getMeterAssignLocation();
                location.setOrgId(user.getOrgId());
                location.setCreatedBy(user.getId());
                location.setMeterStage("Pending-assigned");
                location.setDescription("Location assigned");
                location.setState(req.getState());
                location.setCity(req.getCity());
                location.setHouseNo(req.getHouseNo());
                location.setStreetName(req.getStreetName());

                validAssign.add(meter);

                validAssignLocation.add(location);
            }

            if (validAssign.isEmpty()) continue;

            try {
                log.info("Processing batch {} - {} ({} records)", i, end - 1, subBatch.size());
                int assigned = assignVirtualBatchTransactional(validAssign, user, validAssignLocation);
                successCount += assigned;
            } catch (Exception e) {
                log.warn("Batch {} failed — retrying smaller sub-batches: {}", (i / BATCH_SIZE) + 1, e.getMessage());
                successCount += assignVirtualSubBatchTransactional(validAssign, user, failedRecords, validAssignLocation);
            }
        }

        int total = successCount + failedRecords.size();

        result.put("totalRecords", total);
        result.put("successCount", successCount);
        result.put("failedCount", failedRecords.size());
        result.put("failedRecords", failedRecords);

        if (!failedRecords.isEmpty()) {
            throw new GlobalExceptionHandler.PartialFailureException(
                    failedRecords.size() + " of " + total + " Meters assigned failed",
                    result
            );
        }

        return ResponseMap.response(
                status.getSuccessCode(),
                String.format("%d of %d virtual meters assigned successfully", successCount, total),
                result
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int assignVirtualBatchTransactional(List<Meter> batch, UserModel user, List<MeterAssignLocation> locations) {
        if (batch.isEmpty()) return 0;

        try {

            // === Step 1: Update main meter table ===
            meterMapper.insertMeters(batch);

            // Step 2: Map 'id' → 'meterId' before inserting version records
            for (Meter meter : batch) {
                meter.setMeterId(meter.getId()); // Copy generated ID
                meter.getMeterAssignLocation().setMeterId(meter.getId());
            }

            // === Step 3: Insert meter version records ===
            meterMapper.insertMeterVersions(batch);

            // === Step 4: Bulk insert location assignments ===
            meterMapper.insertAssignLocation(locations);

            // Audit allocations
            auditBatch(batch, user, "Virtual Meter Assigned");

            log.info("Assign virtual {} meters successfully", batch.size());
            return batch.size();

        } catch (Exception e) {
            log.error("Transaction failed during assign, rolling back batch of size {}: {}", batch.size(), e.getMessage());
            genericHandler.logIncidentReport("Bulk virtual assign batch service failed");
            genericHandler.logAndSaveException(e, "Bulk virtual assign batch meter");
            throw new RuntimeException("Batch virtual assign transaction failed. Rolled back.", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int assignBatchTransactional(List<Meter> batch, UserModel user, List<MeterAssignLocation> locations, List<PaymentMode> paymentModes) {
        if (batch.isEmpty()) return 0;

        try {
            // === Step 1: Update main meter table ===
            meterMapper.updateBatchMeterAssign(batch);

            // === Step 2: Insert meter version records ===
            meterMapper.insertMeterVersions(batch);

            // === Step 3: Bulk insert location assignments ===
            meterMapper.insertAssignLocation(locations);

            // Audit allocations
            auditBatch(batch, user, "Virtual Meter Assigned");

            log.info("Assign {} meters successfully", batch.size());
            return batch.size();

        } catch (Exception e) {
            log.error("Transaction failed during assign, rolling back batch of size {}: {}", batch.size(), e.getMessage());
            genericHandler.logIncidentReport("Bulk assign batch service failed");
            genericHandler.logAndSaveException(e, "Bulk assign batch meter");
            throw new RuntimeException("Batch virtual assign transaction failed. Rolled back.", e);
        }
    }

    public int assignSubBatchTransactional(List<Meter> batch, UserModel user, List<GenericResp> failedRecords,  List<MeterAssignLocation> locations, List<PaymentMode> paymentModes) {
        try {
            int successCount = 0;
            int subBatchSize = 100;

            for (int i = 0; i < batch.size(); i += subBatchSize) {
                int end = Math.min(i + subBatchSize, batch.size());
                List<Meter> subBatch = batch.subList(i, end);

                try {
                    successCount += assignBatchTransactional(subBatch, user,  locations, paymentModes);
                } catch (Exception e) {
                    log.warn("Sub-batch assign failed (size={}): {}", subBatch.size(), e.getMessage());

                    if (subBatch.size() > 50) {
                        successCount += assignSinglesFallbackAsync(subBatch, user, failedRecords);
                    } else {
                        successCount += assignSinglesFallback(subBatch, user, failedRecords);
                    }
                }
            }

            return successCount;
        } catch (Exception e) {
            genericHandler.logIncidentReport("Bulk assign sub batch service failed");
            genericHandler.logAndSaveException(e, "Bulk assign sub batch meter");
            throw new RuntimeException("Sub Batch allocation transaction failed. Rolled back.", e);
        }

    }

    public int assignVirtualSubBatchTransactional(List<Meter> batch, UserModel user, List<GenericResp> failedRecords,  List<MeterAssignLocation> locations) {
        try {
            int successCount = 0;
            int subBatchSize = 100;

            for (int i = 0; i < batch.size(); i += subBatchSize) {
                int end = Math.min(i + subBatchSize, batch.size());
                List<Meter> subBatch = batch.subList(i, end);

                try {
                    successCount += assignVirtualBatchTransactional(subBatch, user,  locations);
                } catch (Exception e) {
                    log.warn("Sub-batch allocation failed (size={}): {}", subBatch.size(), e.getMessage());

                    if (subBatch.size() > 50) {
                        successCount += assignVirtualSinglesFallbackAsync(subBatch, user, failedRecords);
                    } else {
                        successCount += assignVirtualSinglesFallback(subBatch, user, failedRecords);
                    }
                }
            }

            return successCount;
        } catch (Exception e) {
            genericHandler.logIncidentReport("Bulk virtual assign sub batch service failed");
            genericHandler.logAndSaveException(e, "Bulk virtual assign sub batch meter");
            throw new RuntimeException("Sub Batch allocation transaction failed. Rolled back.", e);
        }

    }

    public int assignSinglesFallbackAsync(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        for (Meter meter : batch) {
            futures.add(assignSingleAsync(meter, user, failedRecords));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream().mapToInt(CompletableFuture::join).sum();
    }

    public int assignVirtualSinglesFallbackAsync(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        for (Meter meter : batch) {
            futures.add(assignVirtualSingleAsync(meter, user, failedRecords));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream().mapToInt(CompletableFuture::join).sum();
    }

    public int assignSinglesFallback(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        int successCount = 0;

        for (Meter meter : batch) {
            try {
                log.debug("Fallback single assign for meter: {}", meter.getMeterNumber());
                assignSingleTransactional(meter, user);
                successCount++;
            } catch (Exception e) {
                String reason = extractErrorMessage(e);
                GenericResp resp = new GenericResp();
                resp.setId(meter.getMeterId().toString());
                resp.setMessage("Meter assign failed: "+reason);
                resp.setData(meter.getMeterNumber());

                failedRecords.add(resp);
//                failedRecords.add(String.format(
//                        "%s [Region: %s] (Allocation failed: %s)",
//                        meter.getMeterNumber(),
////                        meter.getNodeInfo().getRegionId(),
//                        reason
//                ));
                log.warn("Meter {} failed individually: {}", meter.getMeterNumber(), reason);
            }
        }

        return successCount;
    }

    public int assignVirtualSinglesFallback(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        int successCount = 0;

        for (Meter meter : batch) {
            try {
                log.debug("Fallback single assign for meter: {}", meter.getMeterNumber());
                assignVirtualSingleTransactional(meter, user);
                successCount++;
            } catch (Exception e) {
                String reason = extractErrorMessage(e);
                GenericResp resp = new GenericResp();
                resp.setId(meter.getMeterId().toString());
                resp.setMessage("Virtual meter assign failed: "+reason);
                resp.setData(meter.getMeterNumber());

                failedRecords.add(resp);
//                failedRecords.add(String.format(
//                        "%s Assigned failed: %s",
//                        meter.getCin(),
//                        reason
//                ));
                log.warn("Meter {} failed individually: {}", meter.getCin(), reason);
            }
        }

        return successCount;
    }

    @Async
    public CompletableFuture<Integer> assignSingleAsync(Meter meter, UserModel user, List<GenericResp> failedRecords) {
        try {
            assignSingleTransactional(meter, user);
            return CompletableFuture.completedFuture(1);
        } catch (Exception e) {
            String reason = extractErrorMessage(e);

            GenericResp resp = new GenericResp();
            resp.setId(meter.getMeterId().toString());
            resp.setMessage("Meter assign failed: "+reason);
            resp.setData(meter.getMeterNumber());

            failedRecords.add(resp);
//            failedRecords.add(String.format(
//                    "%s [Cin: %s] (Assign failed: %s)",
//                    meter.getMeterNumber(),
//                    meter.getCin(),
//                    reason
//            ));
            log.warn("Async assign failed for meter {}: {}", meter.getMeterNumber(), reason);
            return CompletableFuture.completedFuture(0);
        }
    }

    @Async
    public CompletableFuture<Integer> assignVirtualSingleAsync(Meter meter, UserModel user, List<GenericResp> failedRecords) {
        try {
            assignVirtualSingleTransactional(meter, user);
            return CompletableFuture.completedFuture(1);
        } catch (Exception e) {
            String reason = extractErrorMessage(e);
            GenericResp resp = new GenericResp();
            resp.setId(meter.getMeterId().toString());
            resp.setMessage("Virtual meter assign failed: "+reason);
            resp.setData(meter.getMeterNumber());

            failedRecords.add(resp);
//            failedRecords.add(String.format(
//                    "%s Cin assign failed (%s)",
//                    meter.getCin(),
//                    reason
//            ));
            log.warn("Async assign failed for meter {}: {}", meter.getMeterNumber(), reason);
            return CompletableFuture.completedFuture(0);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void assignSingleTransactional(Meter meter, UserModel user) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

        meterMapper.updateMeter(meter.getDescription(), meter.getId(), meter.getUpdatedAt(), meter.getStatus());
        meter.setMeterId(meter.getId());
        meterMapper.assignMeterVersion(meter, meter.getNodeId(), meter.getId(), "Pending Assigned");

        meterMapper.assignVerMeterToLocation(meter.getMeterAssignLocation());

        //fetch meter from the database
        Meter m = meterMapper.getVersionMeter(user.getOrgId(), null, meter.getMeterNumber(), null);

        //save to audit (mongodb)
        AuditLog auditLog = buildAuditLog(user, "Pending Assigned", meterName, m, metadata, "");
        auditRepository.save(auditLog);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void assignVirtualSingleTransactional(Meter meter, UserModel user) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

        meterMapper.insertMeter(meter);
        meter.setMeterId(meter.getId());
        meterMapper.assignMeterVersion(meter, meter.getNodeId(), meter.getId(), "Pending Assigned");

        meterMapper.assignVerMeterToLocation(meter.getMeterAssignLocation());

        //fetch meter from the database
        Meter m = meterMapper.getVersionMeter(user.getOrgId(), null, meter.getMeterNumber(), null);

        //save to audit (mongodb)
        AuditLog auditLog = buildAuditLog(user, "Pending Assigned", meterName, m, metadata, "");
        auditRepository.save(auditLog);
    }

    private List<AssignMeterToCustomer> processAssignExcel(InputStream inputStream) throws IOException {
        List<AssignMeterToCustomer> meters = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip header row safely
            if (rows.hasNext()) {
                rows.next();
            }

            while (rows.hasNext()) {
                Row row = rows.next();
                AssignMeterToCustomer meter = new AssignMeterToCustomer();

                meter.setMeterNumber(getStringCellValue(row.getCell(0)));
                meter.setCustomerId(getStringCellValue(row.getCell(1)));
                meter.setTariffName(getStringCellValue(row.getCell(2)));
                meter.setDssAssetId(getStringCellValue(row.getCell(3)));
                meter.setFeederAssetId(getStringCellValue(row.getCell(4)));
                meter.setCin(getStringCellValue(row.getCell(5)));
                meter.setState(getStringCellValue(row.getCell(6)));
                meter.setCity(getStringCellValue(row.getCell(7)));
                meter.setHouseNo(getStringCellValue(row.getCell(8)));
                meter.setStreetName(getStringCellValue(row.getCell(9)));
                meter.setCreditPaymentMode(getStringCellValue(row.getCell(10)));
                meter.setCreditPaymentPlan(getStringCellValue(row.getCell(11)));
                meter.setDebitPaymentMode(getStringCellValue(row.getCell(12)));
                meter.setDebitPaymentPlan(getStringCellValue(row.getCell(13)));

                meters.add(meter);
            }
        }
        return meters;
    }

    private List<AssignMeterToCustomer> processAssignCsv(InputStream inputStream) throws IOException {
        List<AssignMeterToCustomer> meters = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            for (CSVRecord record : csvParser) {
                AssignMeterToCustomer meter = new AssignMeterToCustomer();
                meter.setMeterNumber(record.get("meter number"));
                meter.setCustomerId(record.get("customer id"));
                meter.setTariffName(record.get("tariff name"));
                meter.setDssAssetId(record.get("dss asset id"));

                meter.setFeederAssetId(record.get("feeder asset id"));
                meter.setCin(record.get("cin"));
                meter.setState(record.get("state"));

                meter.setCity(record.get("city"));
                meter.setHouseNo(record.get("house number"));
                meter.setStreetName(record.get("street name"));
                meter.setCreditPaymentMode(record.get("credit payment mode"));
                meter.setCreditPaymentPlan(record.get("credit payment plan"));
                meter.setDebitPaymentMode(record.get("debit payment mode"));
                meter.setDebitPaymentPlan(record.get("debit payment plan"));

                meters.add(meter);
            }
        }
        return meters;
    }

    private List<AssignMeterToCustomer> processVirtualAssignExcel(InputStream inputStream) throws IOException {
        List<AssignMeterToCustomer> meters = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip header row safely
            if (rows.hasNext()) {
                rows.next();
            }

            while (rows.hasNext()) {
                Row row = rows.next();
                AssignMeterToCustomer meter = new AssignMeterToCustomer();

                meter.setCustomerId(getStringCellValue(row.getCell(0)));
                meter.setTariffName(getStringCellValue(row.getCell(1)));
                meter.setDssAssetId(getStringCellValue(row.getCell(2)));
                meter.setFeederAssetId(getStringCellValue(row.getCell(3)));
                meter.setCin(getStringCellValue(row.getCell(4)));
                meter.setMeterClass(getStringCellValue(row.getCell(5)));
                meter.setState(getStringCellValue(row.getCell(6)));
                meter.setCity(getStringCellValue(row.getCell(7)));
                meter.setHouseNo(getStringCellValue(row.getCell(8)));
                meter.setStreetName(getStringCellValue(row.getCell(9)));
                meter.setFixedEnergy(getStringCellValue(row.getCell(10)));

                meters.add(meter);
            }
        }
        return meters;
    }

    private List<AssignMeterToCustomer> processVirtualAssignCsv(InputStream inputStream) throws IOException {
        List<AssignMeterToCustomer> meters = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            for (CSVRecord record : csvParser) {
                AssignMeterToCustomer meter = new AssignMeterToCustomer();
                meter.setCustomerId(record.get("customer id"));
                meter.setTariffName(record.get("tariff name"));
                meter.setDssAssetId(record.get("dss asset id"));

                meter.setFeederAssetId(record.get("feeder asset id"));
                meter.setCin(record.get("cin"));
                meter.setMeterClass(record.get("meter class"));
                meter.setState(record.get("state"));

                meter.setCity(record.get("city"));
                meter.setHouseNo(record.get("house number"));
                meter.setStreetName(record.get("street name"));
                meter.setFixedEnergy(record.get("fixed energy"));

                meters.add(meter);
            }
        }
        return meters;
    }

    // Helper method to avoid NumberFormatException
    private static Long parseLongSafe(String value) {
        try {
            return (value == null || value.isEmpty()) ? null : Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String getStringCellValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }


    private String extractErrorMessage(Exception e) {
        String message = e.getMessage();

        if (message == null) return "Unknown error";

        if (message.contains("duplicate key value")) {
            return "Duplicate record — Meter already exists.";
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

    private AuditLog buildAuditLog(UserModel creator, String description, String type, Meter createdEntity, Map<String, String> metadata, String reason) {
        AuditLog log = new AuditLog();
        log.setCreator(creator);
        log.setDescription(description);
        log.setType(type);
        log.setCreatedMeter(createdEntity);
        log.setReason(reason);
        log.setIpAddress(metadata.get("ipAddress"));
        log.setUserAgent(metadata.get("userAgent"));
        log.setEndpoint(metadata.get("endpoint"));
        log.setHttpMethod(metadata.get("httpMethod"));
        return log;
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

//    private String handleGetAccountNumber(){
//        String accountNumber;
//        accountNumber = String.valueOf(Instant.now().getEpochSecond());
//        return accountNumber;
//    }

    private String handleGetAccountNumber() {
        String accountNumber;
        long millis = System.currentTimeMillis(); // e.g. 1730667129123
        String base = String.valueOf(millis);

        // Take last 7 digits of current milliseconds + 3 random digits = 10 total
        int random = ThreadLocalRandom.current().nextInt(100, 999);
        accountNumber = base.substring(base.length() - 7) + random;

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
