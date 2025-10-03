package org.memmcol.gridflexbackendservice.service.meter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.mapper.MeterMapper;
import org.memmcol.gridflexbackendservice.mapper.NodeMapper;
import org.memmcol.gridflexbackendservice.mapper.TariffMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.meter.*;
import org.memmcol.gridflexbackendservice.model.node.RegionBhubServiceCenter;
import org.memmcol.gridflexbackendservice.model.node.SubStationTransformerFeederLine;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
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

        Meter existing = meterMapper.getMeter(user.getOrgId(), null, request.getMeterNumber().trim(), null, null);
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

//            Meter meter = meterMapper.findByMeterNumber(request.getMeterNumber(), user.getOrgId());
//            if (meter != null) {
//                throw new GlobalExceptionHandler.NotFoundException("Meter not found");
//            }

            String MDDesc = "";
            String SmartDesc = "";
            String MeterDesc = "";
            String meterStage = "Pending-edited";

            // Prepare meter update data
            request.setType("NON-VIRTUAL");
            request.setStatus("Active");
            request.setMeterStage(meterStage);
            MeterDesc = buildChangeDescription(existingMeter, request);
            request.setDescription("Pending edited");
            request.setCreatedBy(user.getId());

            request.setNodeId(existingMeter.getNodeId());
            request.setDss(existingMeter.getDss());
            request.setCin(existingMeter.getCin());
            request.setAccountNumber(existingMeter.getAccountNumber());
            request.setOrgId(existingMeter.getOrgId());
            request.setMeterId(existingMeter.getMeterId());
            request.setCreatedBy(user.getId());
            request.setCustomerId(existingMeter.getCustomerId());
            request.setTariff(existingMeter.getTariff());
            request.setMeterId(existingMeter.getId());
//            request.setMeterModel(existingMeter.getMeterModel());

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

//                int resp = meterMapper.updateMeter(meterStage, request.getId(), request.getUpdatedAt(), request.getStatus());
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
//                request.getSmartMeterInfo().setMeterModel(user.getId());
                SmartDesc = buildSmartMeterInfoChangeDescription(existingMeter.getSmartMeterInfo(), request.getSmartMeterInfo());
                request.getSmartMeterInfo().setDescription("Pending edited");
//                int response = meterMapper.updateMeter(meterStage, request.getId(), request.getUpdatedAt(), request.getStatus());
                int mdResult2 = meterMapper.insertSmartMeterInfoVersion(request.getSmartMeterInfo());
                if (mdResult2 == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(meterName + " MD data " + status.getUpdateFailureDesc());
                }
            }

//            List<String> desc = new ArrayList<>();
////            int tariff = tariffMapper.getTariffBandById(bandId, um.getOrgId());
//            desc.add(MDDesc);
//            desc.add(SmartDesc);
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
            assert meterStage != null;
            if(type.trim().equalsIgnoreCase("pending-state")) {
                System.out.println("---------------->>>>>>>>>>>"+um.getOrgId());
                meters = meterMapper.getMetersVersion(um.getOrgId());
            } else {
                meters = meterMapper.getMeters(um.getOrgId());
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
                meter = meterMapper.getMeter(um.getOrgId(), meterId, meterNumber, accountNumber, cin);
            }

            if(accountNumber != null){
                meter = meterMapper.getMeter(um.getOrgId(), meterId, meterNumber, accountNumber, cin);
            }

            if(meterId != null){
                meter = meterMapper.getMeter(um.getOrgId(), meterId, meterNumber, accountNumber, cin);
            }

            if(cin != null){
                meter = meterMapper.getMeter(um.getOrgId(), meterId, meterNumber, accountNumber, cin);
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
            UserModel um = handleUserValidation();

            Meter meterById = meterMapper.findById(meterId, um.getOrgId());
            if(meterById == null) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getNotFoundDesc());
            }

            if(state){
                Tariff tariff = tariffMapper.getApproveTariff(meterById.getTariff());
                if(tariff == null){
                    throw new GlobalExceptionHandler.NotFoundException("Tariff is either not found, not approved or deactivated" );
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
            meterById.setCreatedBy(um.getId());
            meterById.setMeterId(meterById.getId());

            String changeDescription = buildChangeStatusDescription(meterById, state);
            meterById.setDescription(state ? "Meter Activated" : "Meter Deactivated");


            result = meterMapper.insertMeterVersion(meterById);
            if(result == 0){
                throw new GlobalExceptionHandler.NotFoundException("Meter " + status.getUpdateDesc());
            }

            int u = meterMapper.updateMeter(meterById.getMeterStage(), meterById.getId(), meterById.getUpdatedAt(), "Pending");
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException("Meter" + (state ? " activated " : " deactivated ")+ "failed");
            Meter meter = meterMapper.getMeter(um.getOrgId(), meterById.getMeterId(), null, null, null);
            um.setPassword("");
//            handleAddCache(newTariff);
            AuditLog auditLog = buildAuditLog(um, changeDescription, meterName, meter, metadata, reason);
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

//            Meter mainMeter = null;
            if(request.getMeterCategory() == null || !request.getMeterCategory().equalsIgnoreCase("Non-MD")){

                // Validate main meter record
                Meter mainMeter = meterMapper.getMeter(user.getOrgId(), null, request.getNewMeterNumber(), null, null);
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
                    throw new GlobalExceptionHandler.NotFoundException(request.getNewMeterNumber() + " meter has not been allocated");
                }

                request.setOldKrn(mainMeter.getOldKrn());
                request.setNewKrn(mainMeter.getNewKrn());
                request.setOldSgc(mainMeter.getOldSgc());
                request.setNewSgc(mainMeter.getNewSgc());
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
                request.setMeterModel(mainMeter.getMeterModel());
            }

            Meter m = meterMapper.getMeter(user.getOrgId(), null, null, null, request.getCin());
            if(m != null ) {
                return ResponseMap.response(status.getSuccessCode(), "Existing meter attached to the cin provided fetch successfully", m);
            }

            request.setNodeId(feederLine.getNodeId());
            request.setDss(dss.getNodeId());
            request.setOrgId(user.getOrgId());
            request.setCreatedBy(user.getId());

            handleMeterAssign(request);

            Meter meter = meterMapper.getVersionMeter(user.getOrgId(), null, request.getNewMeterNumber(), null);
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
        // Assign meter to customer
        request.setDescription("Meter Assigned");
        request.setMeterStage("Pending-assigned");
        request.setStatus("Active");
        int customerAssignResult;
        int customerAssignResult1;
        if(request.getMeterCategory().equalsIgnoreCase("Non-MD")){
            request.setType("VIRTUAL");
            request.setMeterCategory("Postpaid");
            request.setMeterClass("Non-MD");
            request.setSmartStatus(false);
            request.setSimNumber("VIRTUAL");
            request.setMeterModel("VIRTUAL");
            request.setMeterType("Electricity");
            customerAssignResult = meterMapper.insertVirtualVersionMeterToCustomer(request);
            request.setMeterId(request.getId());
            customerAssignResult1 = meterMapper.assignedVirtualVersionMeterToCustomer(request);
            if(customerAssignResult == 0 || customerAssignResult1 == 0)
                throw new GlobalExceptionHandler.NotFoundException("Assigning virtual meter to customer failed");
        } else {
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
        }

        request.setMeterId(request.getMeterId());
        int locationAssignResult = meterMapper.assignVersionMeterToLocation(request);

        if (locationAssignResult == 0) {
            throw new GlobalExceptionHandler.NotFoundException("Meter assignment to location failed");
        }
    }

    @Override
    public Map<String, Object> continueAssignMeter(AssignMeterToCustomer request) {
        try{
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel um = handleUserValidation();

            Meter meterById = meterMapper.findById(request.getMeterId(), um.getOrgId());
            if(meterById == null) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getNotFoundDesc());
            }

            if(request.getActivateStatus()){
                int result = meterMapper.changeState(request.getMeterId(), "Pending", um.getOrgId());
            }

//            if(request.getActivateStatus() != null && (state.equalsIgnoreCase("in-stock")
//                    || state.equalsIgnoreCase("deactivated"))) {
//                result = meterMapper.changeState(meterId, state, um.getOrgId());
//                if (result == 0) {
//                    throw new GlobalExceptionHandler.NotFoundException(meterName + " " + state + " " + status.getUpdateFailureDesc());
//                }
//                desc = meterById.getMeterNumber() + state;
//            }
//            else {
//                assert state != null;
//                throw new MissingServletRequestParameterException("Required request parameter '%s' is not present", state);
//            }

//            Meter meter = meterMapper.findById(meterId, um.getOrgId());
//
//            if(meter.getStatus().equals(false)) {
//                resp = "Meter ("+ meter.getMeterNumber() + ") in-stock successfully";
//            }
////            else if (meter.getStatus().equalsIgnoreCase("assigned")) {
////                resp = "Meter ("+ meter.getMeterNumber() + ") assigned successfully";
////            }
//            else if (meter.getMeterStage().equals("Deactivated")) {
//                resp = "Meter ("+ meter.getMeterNumber() + ") deactivated successfully";
//            } else {
//                throw new MissingServletRequestParameterException("Required request parameter '%s' is not present", state);
//            }

//            handleMeterAssign(assignMeterToCustomer);

//            handleAddCache(meter);

//            AuditLog auditLog = buildAuditLog(um, desc, meterName, meter, metadata, reason);
//            auditRepository.save(auditLog);
//
//            return ResponseMap.response(status.getSuccessCode(), resp, "");
            return Map.of();

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
                        "Meter has a pending record that needs cleared"
                );
            }

            if(meterById.getMeterStage().equalsIgnoreCase("Deactivated")
                    || meterById.getType().equalsIgnoreCase("virtual")
                    || meterById.getCustomerId() == null) {
                throw new GlobalExceptionHandler.NotFoundException("Meters migration failed because meter is either unassigned, deactivated and virtual");
            }

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

            }

            // migrate to postpaid
            if(request.getMigrationFrom().equalsIgnoreCase("prepaid") && meterById.getMeterCategory().equalsIgnoreCase("prepaid")){
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
                System.out.println(">>>>>>>>>>>>>>success>>>>>>>>>>>>>>>");
            }

            // get recent meter record
            Meter meter = meterMapper.findById(request.getMeterId(), um.getOrgId());

//            handleAddCache(meter);
            AuditLog auditLog = buildAuditLog(um, desc, meterName, meter, metadata, "");
            auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(),
                    "Meter migrated successfully",
                    "");

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
                throw new MissingServletRequestParameterException("meterStage", "not found");
            }

            // --- Step 3: Audit log ---
            Meter updatedMeter = meterMapper.findById(meter.getId(), user.getOrgId());
            user.setPassword(null); // hide password in logs
            AuditLog auditLog = buildAuditLog(user, "Meter approve", meterName, updatedMeter, metadata, "");
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
            if (meterById.getMeterStage().contains("Pending") || meterById.getStatus().contains("Pending")) {
                throw new GlobalExceptionHandler.NotFoundException("Meter has a pending record that needs to be cleared");
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
            meterById.setStatus("Inactive");
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
        // String st = meter.getMeterStage();
        // meter.setApproveBy(user.getId());

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
            meter.setMeterStage("Active");
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
                meter.setStatus("Activated");

            // === Default fallback ===
            } else {
                meter.getPaymentMode().setMeterStage("Active");
                meter.setMeterStage("Active");
                meter.setStatus("Active");
            }


//        if(meter.getMeterStage().trim().equalsIgnoreCase("Pending-created")
//                && (meter.getMdMeterInfo() != null || meter.getSmartMeterInfo() != null)){
//            System.out.println("meterStage>> "+meter.getMeterStage());
//            System.out.println("getMdMeterInfo>> "+meter.getMdMeterInfo().getMeterStage());
//            System.out.println("getSmartMeterInfo>> "+meter.getSmartMeterInfo().getMeterStage());
//            meter.setMeterStage("Created");
//            meter.setStatus("Active");
//            meter.getMdMeterInfo().setMeterStage("Created");
//            meter.getSmartMeterInfo().setMeterStage("Created");
//        }
//        if(meter.getMeterStage().trim().equalsIgnoreCase("Pending-created")
//                && (meter.getMdMeterInfo() == null || meter.getSmartMeterInfo() != null)){
//            meter.setMeterStage("Created");
//            meter.setStatus("Active");
//            meter.getSmartMeterInfo().setMeterStage("Created");
//        }
//        if(meter.getMeterStage().trim().equalsIgnoreCase("Pending-created")
//                && (meter.getMdMeterInfo() != null || meter.getSmartMeterInfo() == null)){
//            meter.setMeterStage("Created");
//            meter.setStatus("Active");
//            meter.getMdMeterInfo().setMeterStage("Created");
//        }
//        if(meter.getMeterStage().trim().equalsIgnoreCase("Pending-created")
//                && meter.getMdMeterInfo() == null && meter.getSmartMeterInfo() == null){
//            meter.setMeterStage("Created");
//            meter.setStatus("Active");
//        }
//        else if(meter.getMeterStage().trim().equalsIgnoreCase("Pending-assigned")){
//            meter.setMeterStage("Assigned");
//            meter.setStatus("Active");
//        } else if(meter.getMeterStage().trim().equalsIgnoreCase("Pending-allocated")){
//            meter.setMeterStage("Unassigned");
//            meter.setStatus("Active");
//        } else if(meter.getMeterStage().trim().equalsIgnoreCase("Pending-detached")) {
//            meter.setMeterStage("Unassigned");
//            meter.setStatus("Deactivated");
//        }
//        else if(meter.getMeterStage().trim().equalsIgnoreCase("Pending-migrated") && meter.getSmartMeterInfo() != null){
//            meter.getSmartMeterInfo().setMeterStage("Active");
//            meter.getPaymentMode().setMeterStage("Active");
//            meter.setMeterStage("Active");
//            meter.setStatus("Active");
//        } else if((meter.getMeterStage().trim().equalsIgnoreCase("Pending-edited"))
//                && meter.getCustomerId() == null && meter.getNodeId() != null) {
//            meter.setMeterStage("Unassigned");
//            meter.setStatus("Active");
//        } else if((meter.getMeterStage().trim().equalsIgnoreCase("Pending-edited"))
//                && meter.getCustomerId() == null && meter.getNodeId() == null) {
//            meter.setMeterStage("Created");
//            meter.setStatus("Active");
////            handleMeterInfoRejection(meter, user);
//        } else if(meter.getMeterStage().trim().equalsIgnoreCase("Pending-edited")
//                && meter.getCustomerId() != null && meter.getNodeId() != null) {
//            meter.setMeterStage("Assigned");
//            meter.setStatus("Active");
//        } else if(meter.getStatus().trim().equalsIgnoreCase("Pending-deactivated")) {
//            meter.setStatus("Deactivated");
//        } else if(meter.getStatus().trim().equalsIgnoreCase("Pending-activated")) {
//            meter.setStatus("Activated");
//        } else {
//            //pending-detached & pending-migrated
////            meter.getMdMeterInfo().setMeterStage("Active");
//            meter.getPaymentMode().setMeterStage("Active");
//            meter.setMeterStage("Active");
//            meter.setStatus("Active");
//        }

        int approved = meterMapper.approvedMeterVersion(meter.getMeterStage(), meter.getStatus(), meter.getApproveBy(), meter.getUpdatedAt(), meter.getMeterNumber());
        if (approved == 0) {
            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "d " + status.getUpdateFailureDesc());
        }

        if(!"Pending-detached".equalsIgnoreCase(stage)){
            //Approve meter in meters table
            if (meterMapper.approveMeter(meter) == 0) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "d " + status.getUpdateFailureDesc());
            }

            //approve meter location
            if(meter.getMeterAssignLocation() != null ){
                meter.getMeterAssignLocation().setApproveBy(user.getId());
                approveMeterAssignLocation(meter);
            }

            //approve MD meter Information
//            if ("md".equalsIgnoreCase(meter.getMeterClass()) && meter.getMdMeterInfo() != null && (stage.equalsIgnoreCase("Pending-created")
//                    || stage.equalsIgnoreCase("Pending-edited"))) {
//                meter.getMdMeterInfo().setApproveBy(user.getId());
//                approveMDMeterInfo(meter);
//            }

            if (meter.getMdMeterInfo() != null) {
                meter.getMdMeterInfo().setApproveBy(user.getId());
                approveMDMeterInfo(meter);
            }

            //approve smart meter Information
//            if (Boolean.TRUE.equals(meter.getSmartStatus()) && (stage.equalsIgnoreCase("Pending-created")
//                    || stage.equalsIgnoreCase("Pending-edited"))) {
//                approveSmartMeterInfo(meter);
//            }

            if (meter.getMdMeterInfo() != null) {
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
            int result = meterMapper.updateMeterAssignedLocation("Rejected", meter.getMeterAssignLocation().getMeterId(), meter.getOrgId(), meter.getUpdatedAt(), user.getId());
            if(result == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " assigned location failed");
        }

        if(meter.getPaymentMode() != null){
            //Update smart meter Info, mater-stage to rejected in payment_mode_version table
            int result = meterMapper.removePaymentModeInfo("Rejected", meter.getPaymentMode().getMeterId(), meter.getOrgId(), user.getId());
            if(result == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " assign payment mode failed");
        }


        if(meter.getMeterStage().trim().equalsIgnoreCase("Pending-created")){

            //Delete meter record in meters table
           int res = meterMapper.removeMeter(meter.getMeterNumber(), meter.getOrgId());

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
            int u = meterMapper.removeMeter(meter.getMeterNumber(), meter.getOrgId());
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
        } else {
//            meter.setMeterStage("Active");
            meter.setStatus(s);
            int u = meterMapper.updateMeter(meter.getMeterStage(), meter.getMeterId(), meter.getUpdatedAt(), meter.getStatus());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " update failed");
        }
    }

//    void handlePendingAssignedRejection(Meter meter, UserModel user) {
//        if(meter.getMdMeterInfo() != null){
//            int v = meterMapper.updateMeterAssignedLocation(meter.getMeterStage(), meter.getMeterId(), meter.getUpdatedAt(), meter.getStatus(), user.getId());
//            if(v == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " editing failed");
//        }
//        if(meter.getSmartMeterInfo() != null){
//            int v = meterMapper.updateSmartMeter(meter.getMeterStage(), meter.getMeterId(), meter.getUpdatedAt(), meter.getStatus(), user.getId());
//            if(v == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " editing failed");
//        }
//        int m = meterMapper.updateMeter(meter.getMeterStage(), meter.getMeterId(), meter.getUpdatedAt(), meter.getStatus());
//        if(m == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " assign failed");
//    }

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

            Meter verifyMeter = meterMapper.getMeter(um.getOrgId(), null, meterNumber, null, null);
            if(verifyMeter == null){
                throw new GlobalExceptionHandler.NotFoundException("Meter " + status.getNotFoundDesc());
            }

            if (verifyMeter.getMeterStage().contains("Pending") || verifyMeter.getStatus().contains("Pending")) {
                throw new GlobalExceptionHandler.NotFoundException("Meter has a pending record that needs to be cleared");
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
            log.error("Error filtering / fetching users: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Allocating meter service failed");
            genericHandler.logAndSaveException(exception, "allocating meter");
            throw exception;
        }
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

        System.out.println(">>>>>>>>>>>>>>:: "+oldMeter.getMeterModel());

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