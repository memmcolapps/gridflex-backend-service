package org.memmcol.gridflexbackendservice.service.meter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.MeterMapper;
import org.memmcol.gridflexbackendservice.mapper.NodeMapper;
import org.memmcol.gridflexbackendservice.mapper.TariffMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.meter.*;
import org.memmcol.gridflexbackendservice.model.node.Node;
import org.memmcol.gridflexbackendservice.model.node.RegionBhubServiceCenter;
import org.memmcol.gridflexbackendservice.model.node.SubStationTransformerFeederLine;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.service.customer.CustomerServiceImpl;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.util.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

import static org.memmcol.gridflexbackendservice.util.GenericHandler.capitalizeFirstLetter;
import static org.memmcol.gridflexbackendservice.util.GenericHandler.getClientIp;
import static org.memmcol.gridflexbackendservice.util.handleValidUser.handleUserValidation;

@Transactional
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
    private ExceptionAuditRepository exceptionAuditRepository;

    @Autowired
    private HttpServletRequest httpServletRequest;

    private String meterName = "Meter";

    private final IMap<String, Object> meterCache;

    private final IMap<String, Object> auditCache;


    public MeterServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.meterCache = hazelcastInstance.getMap("meter-Cache");
        this.auditCache = hazelcastInstance.getMap("audit-Cache");
    }

    @Override
    public Map<String, Object> createMeter(Meter request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            UserModel um = handleUserValidation();
            String desc = capitalizeFirstLetter("Meter created");
            Manufacturer manufacturer = meterMapper.getMeterManufacturer(request.getMeterManufacturer());
            if(manufacturer == null) {
                throw new GlobalExceptionHandler.NotFoundException("Meter manufacturer not found");
            }
            Meter  meter = meterMapper.getMeterNumber(um.getOrgId(), request.getMeterNumber());
            if(meter != null) {
                throw new GlobalExceptionHandler.NotFoundException("Meter not found");
            }

            if(!request.getMeterClass().equalsIgnoreCase("md")
                    && !request.getMeterClass().equalsIgnoreCase("single-phase")
                    && !request.getMeterClass().equalsIgnoreCase("three-phase")){
                throw new GlobalExceptionHandler.NotFoundException("Meter class can only allow one of three value; MD, single-phase or three-phase");
            }

            // set default state to be In-stock
            request.setStatus("In-stock");
            request.setOrgId(um.getOrgId());
            request.setType("NON VIRTUAL");
            request.setApproveStatus("pending");
            request.setDescription(desc);
            request.setCreatedBy(um.getId());

            int result;
            int result1;
            int result2;
//            if(meter.getApproveStatus().equalsIgnoreCase("rejected")){
//                result = meterMapper.updateMeter(request);
//                if(result == 0){
//                    throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());
//                }
//            } else {
                //insert meter to databases
                result1 = meterMapper.insertMeter(request);
//            }
            request.setMeterId(request.getId());
            result2 = meterMapper.insertMeterVersion(request);
            if(result1 == 0 || result2 == 0){
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getRegFailureDesc());
            }

            if(request.getMeterClass().equalsIgnoreCase("md")) {
                int mdResult1;
                int mdResult2;
                request.getMdMeterInfo().setMeterId(request.getId());
                request.getMdMeterInfo().setOrgId(um.getOrgId());
                request.getMdMeterInfo().setCreatedBy(um.getId());
                String description = capitalizeFirstLetter("MD Meter Info created");
                request.getMdMeterInfo().setDescription(description);
//                if(meter.getApproveStatus().equalsIgnoreCase("rejected")){
//                    result = meterMapper.insertMDMeterInfoVersion(request.getMdMeterInfo());
//                    if(result == 0){
//                        throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());
//                    }
//                } else {
                    mdResult1 = meterMapper.insertMDMeterInfo(request.getMdMeterInfo());   
//                }
                mdResult2 = meterMapper.insertMDMeterInfoVersion(request.getMdMeterInfo());
                if (mdResult1 == 0 || mdResult2 == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getRegFailureDesc());
                }
            }

            UUID meterId = request.getId();

            //fetch meter from the database
            Meter newMeter = meterMapper.findById(meterId, request.getOrgId());

            //call cache method
//            handleAddCache(meter);

            //save to audit (mongodb)
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setType(meterName);
            auditNotificationDTO.setCreatedMeter(newMeter);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(), meterName + " " + status.getRegDesc(), "");
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
    public Map<String, Object> updateMeter(Meter request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditLog = new AuditLog();

        try {
            // Gather client metadata
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");

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
            request.setType("NON VIRTUAL");
            request.setApproveStatus("Pending");
            request.setDescription(buildChangeDescription(existingMeter, request));
            request.setCreatedBy(user.getId());

            // Insert or update meter version
            int result;
            if (versionedMeter != null && "pending".equalsIgnoreCase(versionedMeter.getApproveStatus())) {
                result = meterMapper.updateMeterVersion(request);
//                meterMapper.updateMeter(request);
            } else {
                result = meterMapper.insertMeterVersion(request);
            }

            if (result == 0) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());
            }

            // Handle MD meter-specific logic
            if ("md".equalsIgnoreCase(request.getMeterClass())) {
                UUID meterId = request.getId();
                request.getMdMeterInfo().setMeterId(meterId);
                request.getMdMeterInfo().setOrgId(user.getOrgId());
                request.getMdMeterInfo().setDescription(buildMDMeterInfoChangeDescription(existingMeter.getMdMeterInfo(), request.getMdMeterInfo()));
                if (versionedMeter != null &&
                        "pending".equalsIgnoreCase(versionedMeter.getMdMeterInfo().getApproveStatus())) {

//                    int mdResult1 = meterMapper.updateMDMeterInfo(request.getMdMeterInfo());
                    int mdResult = meterMapper.updateMDMeterInfoVersion(request.getMdMeterInfo());

                    if (mdResult == 0) {
                        throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());
                    }
                }
            }

            // Fetch updated meter and log audit
            Meter updatedMeter = meterMapper.findById(request.getId(), user.getOrgId());

            auditLog.setCreator(user);
            auditLog.setDescription("Meter edited");
            auditLog.setUserAgent(userAgent);
            auditLog.setIpAddress(ipAddress);
            auditLog.setType(meterName);
            auditLog.setCreatedMeter(updatedMeter);
            auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(), meterName + " " + status.getUpdateDesc(), "");
        } catch (Exception e) {
            log.error("Error updating meter: {}", e.getMessage(), e);
            e.printStackTrace();

            exceptionErrorLogs.setDescription("Error occurred while updating meter");
            exceptionErrorLogs.setError_message(e.getMessage());
            exceptionErrorLogs.setError(e.toString());
            exceptionAuditRepository.save(exceptionErrorLogs);

            throw e;
        }
    }

    @Override
    public Map<String, Object> getAllMeters(
            int page, int size, String meterNumber, String simNo, String manufacturer, String type,
            String meterClass, String category, String state, String createdAt, String customerId) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {

            UserModel um = handleUserValidation();

            // Build a unique cache key
            StringBuilder cacheKeyBuilder = new StringBuilder("users_"+um.getOrgId());
            if (meterNumber != null && !meterNumber.isEmpty()) cacheKeyBuilder.append("_meterNumber_").append(meterNumber);
            if (simNo != null && !simNo.isEmpty()) cacheKeyBuilder.append("_simNo_").append(simNo);
            if (type != null && !type.isEmpty()) cacheKeyBuilder.append("_type_").append(type);
            if (manufacturer != null && !manufacturer.isEmpty()) cacheKeyBuilder.append("_manufacturer_").append(manufacturer);
            if (meterClass != null && !meterClass.isEmpty()) cacheKeyBuilder.append("_meterClass_").append(meterClass);
            if (category != null && !category.isEmpty()) cacheKeyBuilder.append("_category_").append(category);
            if (state != null && !state.isEmpty()) cacheKeyBuilder.append("_approvedStatus_").append(state);
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
            if(type.equalsIgnoreCase("pending")){
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

            if (type != null && !type.isEmpty()) {
                meterStream = meterStream.filter(u -> u.getType() != null && u.getType().equalsIgnoreCase(type));
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

            if (state != null && !state.isEmpty()) {
                meterStream = meterStream.filter(u -> u.getStatus() != null && u.getStatus().equalsIgnoreCase(state));
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
            exceptionErrorLogs.setDescription("Error occurred while filtering users");
            exceptionErrorLogs.setError_message(exception.getMessage());
            exceptionErrorLogs.setError(exception.toString());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> getSingleMeter(UUID meterId, String meterNumber, String accountNumber, UUID meterVersionId, String versionMeterNumber) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            Meter meter = null;
            UserModel um = handleUserValidation();

            if (meterId == null && meterNumber == null && accountNumber == null && meterVersionId == null && versionMeterNumber == null) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException("At least one of meterId, meterNumber, or accountNumber must be provided.");
            }

            Object cachedUser = meterCache.get(meterId.toString()+"_"+um.getOrgId());

            if (cachedUser != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached " + meterName + " " + status.getDesc(), cachedUser);
            }

            if(meterNumber != null){
                meter = meterMapper.getMeterNumber(um.getOrgId(), meterNumber);
            }

            if(accountNumber != null){
                meter = meterMapper.getAccountNumber(um.getOrgId(), accountNumber);
            }

            if(meterId != null){
                meter = meterMapper.getMeter(um.getOrgId(), meterId);
            }

            if(versionMeterNumber != null){
                meter = meterMapper.getVersionMeterNumber(um.getOrgId(), versionMeterNumber);
            }

            if(meterVersionId != null){
                meter = meterMapper.getVersionMeter(um.getOrgId(), meterVersionId);
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

            Meter meterById = meterMapper.getMeter(um.getOrgId(), meterId);
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

            if(meter.getStatus().equalsIgnoreCase("in-stock")) {
                resp = "Meter ("+ meter.getMeterNumber() + ") in-stock successfully";
            }
//            else if (meter.getStatus().equalsIgnoreCase("assigned")) {
//                resp = "Meter ("+ meter.getMeterNumber() + ") assigned successfully";
//            }
            else if (meter.getStatus().equalsIgnoreCase("deactivated")) {
                resp = "Meter ("+ meter.getMeterNumber() + ") deactivated successfully";
            } else {
                throw new MissingServletRequestParameterException("Required request parameter '%s' is not present", state);
            }

            handleAddCache(meter);
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

    @Override
    public Map<String, Object> assignMeterToCustomer(AssignMeterToCustomer request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            int result;
            UserModel um = handleUserValidation();

            request.setOrgId(um.getOrgId());

//            if(request.getOldMeterId() != null){
////                result = meterMapper.changeState(request.getOldMeterId(), "Inactive", um.getOrgId());
//                result = meterMapper.changeStateVersion(request.getOldMeterId(), "Inactive", um.getOrgId());
//                if(result == 0){
//                    throw new GlobalExceptionHandler.NotFoundException("Meter " + status.getNotFoundDesc());
//                }
//            }

//            Customer customer = meterMapper.getByCustomerId(request.getCustomerId());
//            if(customer == null){
//                throw new GlobalExceptionHandler.NotFoundException("Customer " + status.getNotFoundDesc());
//            }

            // verify dss
            SubStationTransformerFeederLine verifyDss = meterMapper.verifyDss(request.getDssAssetId(), um.getOrgId());
            if(verifyDss == null){
                throw new GlobalExceptionHandler.NotFoundException("DSS " + status.getNotFoundDesc());
            }

            // verify feeder line
            SubStationTransformerFeederLine verifyFeederLine = meterMapper.verifyFeederLine(verifyDss.getParentId(), um.getOrgId());
            if(verifyFeederLine == null){
                throw new GlobalExceptionHandler.NotFoundException("Feeder line " + status.getNotFoundDesc());
            }

            Meter verifyMeter2 = meterMapper.findByIdVersion(request.getMeterId(), um.getOrgId());
            if(verifyMeter2 == null){
                throw new GlobalExceptionHandler.NotFoundException(request.getNewMeterNumber() + "meter have a pending record that needs approval");
            }

            Meter verifyMeter1 = meterMapper.findById(request.getMeterId(), um.getOrgId());
            if(verifyMeter1 == null){
                throw new GlobalExceptionHandler.NotFoundException("Meter " + status.getNotFoundDesc());
            }
            if(verifyMeter1.getNodeId() == null){
                throw new GlobalExceptionHandler.NotFoundException(request.getNewMeterNumber() + "meter have not been allocated");
            }

            if(verifyMeter2.getMeterAssignLocation() == null){
                throw new GlobalExceptionHandler.NotFoundException(request.getNewMeterNumber() + "meter assign location have a pending record that needs approval");
            }

            // set dss value obtain after verification
            request.setDssAssetId(verifyDss.getAssetId());
            request.setCreatedBy(um.getId());
            request.setOrgId(um.getOrgId());

            // Assign meter to customer
            request.setDescription("Meter assigned to "+request.getCustomerId() );
            int result1 = meterMapper.assignedVersionMeterToCustomer(verifyMeter1, request);
//            int result1 = meterMapper.assignedVersionMeterToCustomer(request);


            // Assign meter to customer location
            request.setDescription(request.getNewMeterNumber() + " meter assigned to location" );
            int result2 = meterMapper.assignVersionMeterToLocation(request);

            if(result1 == 0 || result2 == 0){
                throw new GlobalExceptionHandler.NotFoundException("Meter assigned to customer or location failed");
            }

            if(request.getMeterCategory().equalsIgnoreCase("prepaid")){
                if(verifyMeter2.getPaymentMode() == null){
                    throw new GlobalExceptionHandler.NotFoundException(request.getNewMeterNumber() + "meter payment mode have a pending record that needs approval");
                }
                int result4 = meterMapper.assignPaymentModeVersion(request);
                request.setDescription("Payment mode assigned to" + request.getNewMeterNumber());
                if(result4 == 0){
                    throw new GlobalExceptionHandler.NotFoundException("Meter " + status.getNotFoundDesc());
                }
            }

//            meterMapper.assignedMeterToCustomer(request);
            // Assign meter to customer location
//            meterMapper.assignMeterToLocation(request);
//                meterMapper.assignPaymentMode(request);
            // assign payment mode



//            handleAddCache(isCustomer);

            return ResponseMap.response(status.getSuccessCode(), "Meter assigned successfully", "");
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
            Meter meterById = meterMapper.getMeter(um.getOrgId(), request.getMeterId());
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

    @Override
    public Map<String, Object> approve(UUID meterVersionId, String approveStatus) throws MissingServletRequestParameterException {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        int result;
        String desc = "";
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            UserModel um = handleUserValidation();

            Meter meter = meterMapper.findByIdVersion(meterVersionId, um.getOrgId());
            if(meter == null) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getNotFoundDesc());
            }


            meter.setOrgId(um.getOrgId());
            meter.setApproveBy(um.getId());

            if (meter.getCustomerId() != null) {
                meter.setStatus("Assigned");
            } else if (meter.getNodeId() == null){
                meter.setStatus("In-stock");
            } else {
                meter.setStatus("Unassigned");
            }

            if(approveStatus != null && approveStatus.contains("approve")) {
                meter.setApproveStatus("Approved");
//                meter.setStatus("Unassigned");
                result = meterMapper.approvedMeterVersion(meter);
                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(meterName +" "+ approveStatus + "d "+ status.getUpdateFailureDesc());
                }
                result = meterMapper.updateMeter(meter);
//                        meterMapper.approveMeter(meter);
                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(meterName +" "+ approveStatus + "d "+ status.getUpdateFailureDesc());
                }
                desc = capitalizeFirstLetter(meter.getMeterNumber()) + " meter " + approveStatus;
            }
            else if (approveStatus != null && approveStatus.contains("reject")){
                meter.setApproveStatus("Rejected");
//                meter.setStatus(false);
                result = meterMapper.rejectedMeterVersion(meter);
                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(meterName +" "+ approveStatus + "ed "+ status.getUpdateFailureDesc());
                }
                desc = capitalizeFirstLetter(meter.getMeterNumber()) + " meter " + approveStatus;
            }
            else {
                assert approveStatus != null;
                throw new MissingServletRequestParameterException("Required request parameter '%s' is not present", approveStatus);
            }

            Meter newMeter = meterMapper.findById(meter.getId(), um.getOrgId());
//            handleAddCache(tariff);
            um.setPassword("");
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setType(meterName);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setCreatedMeter(newMeter);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(), meter.getMeterNumber() + meterName + (capitalizeFirstLetter(approveStatus) +" Successfully"), "");

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
    public Map<String, Object> allocateMeter(String meterNumber, String regionId) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");

            UserModel um = handleUserValidation();

            Meter verifyMeter2 = meterMapper.getVersionMeterNumber(um.getOrgId(), meterNumber);
            if(verifyMeter2 == null){
                throw new GlobalExceptionHandler.NotFoundException(meterNumber + "meter have a pending record that needs approval");
            }

            Meter verifyMeter = meterMapper.getMeterNumber(um.getOrgId(), meterNumber);
            if(verifyMeter == null){
                throw new GlobalExceptionHandler.NotFoundException("Meter " + status.getNotFoundDesc());
            }

            // verify if node (organization id) exist
            RegionBhubServiceCenter node = nodeMapper.verifyNode(regionId);
            if(node == null){
                throw new GlobalExceptionHandler.NotFoundException("Node " + status.getNotFoundDesc());
            }

            String desc = meterNumber + "meter allocated to" + regionId;

            //Allocate meter
            int result1 = meterMapper.allocateMeterVersion(verifyMeter, node.getNodeId(), um.getId(), desc);
            if(result1 == 1){
                throw new GlobalExceptionHandler.NotFoundException("Meter allocation failed");
            }

//             Allocate meter
//            meterMapper.allocateMeter(meterNumber, node.getNodeId(), um.getOrgId());


            //fetch meter from the database
            Meter meter = meterMapper.getMeterNumber(um.getOrgId(), meterNumber);
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

//    UserModel handleUserValidation() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String username = "Unknown";
//
//        if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal) {
//            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
//            username = principal.getUsername();  // or principal.getEmail() if you named it that way
//        }
//
//        UserModel isOperatorExist = operatorMapper.findAuthByUserEmail(username);
//
//        if (!Boolean.TRUE.equals(isOperatorExist.getStatus())) {
//            throw new LockedException("User is disabled");
//        }
//
//        return isOperatorExist;
//    }

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

    private String handleGetVirtualMeter(){
        String accountNumber;
        accountNumber = String.valueOf(Instant.now().getEpochSecond());
        return accountNumber;
    }

    private String handleGetAccountNumber(){
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

}
