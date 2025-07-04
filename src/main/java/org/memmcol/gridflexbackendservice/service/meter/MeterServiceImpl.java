package org.memmcol.gridflexbackendservice.service.meter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.MeterMapper;
import org.memmcol.gridflexbackendservice.mapper.TariffMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.meter.BulkApproveMeter;
import org.memmcol.gridflexbackendservice.model.meter.FeederTransformer;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.node.SubStationTransformerFeederLine;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.memmcol.gridflexbackendservice.model.user.CustomUserPrincipal;
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
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.MissingServletRequestParameterException;

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
    private AuthMapper operatorMapper;

    @Autowired
    private MeterMapper mapperMapper;

    @Autowired
    private TariffMapper tariffMapper;

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;

    @Autowired
    private HttpServletRequest httpServletRequest;

    private String meterName = "Meter";

    private final IMap<String, Object> meterCache;

    private final IMap<String, Object> auditCache;
    @Autowired
    private MeterMapper meterMapper;

    public MeterServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.meterCache = hazelcastInstance.getMap("meter-Cache");
        this.auditCache = hazelcastInstance.getMap("audit-Cache");
    }

    @Override
    public Map<String, Object> createMeter(Meter request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {
            String desc = capitalizeFirstLetter(request.getMeterClass()) + "newly created";
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            UserModel um = handleUserValidation();

            if(request.getManufacturer().isEmpty() || request.getManufacturer() == null) {
                throw new GlobalExceptionHandler.NotFoundException("Manufacturer not found");
            }

            // set default state to be pending
            request.setStatus("in-stock");

            request.setOrgId(um.getOrgId());

            //insert meter to database
            mapperMapper.insertMeter(request);

            UUID meterId = request.getId();

            //fetch meter from the database
            Meter meter = meterMapper.findById(meterId, request.getOrgId());

            //call cache method
            handleAddCache(meter);

            //save to audit (mongodb)
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setType(meterName);
            auditNotificationDTO.setCreatedMeter(meter);
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
        AuditLog auditNotificationDTO = new AuditLog();
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            UserModel um = handleUserValidation();
            request.setOrgId(um.getOrgId());
            mapperMapper.updateMeter(request);

//            UUID meterId = request.getId();
//            System.out.println("id: " + meterId);

            Meter meter = meterMapper.findById(request.getId(), um.getOrgId());
//            handleAddCache(meter);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription("Updated Meter [" + meter.getMeterNumber() + "]");
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setType(meterName);
            auditNotificationDTO.setCreatedMeter(meter);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(), meterName + " " + status.getUpdateDesc(), "");
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
    public Map<String, Object> getAllMeters(
            int page, int size, String meterNumber, String simNo, String manufacturer,
            String meterClass, String category, String state, String createdAt) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {

            UserModel um = handleUserValidation();

            // Build a unique cache key
            StringBuilder cacheKeyBuilder = new StringBuilder("users_"+um.getOrgId());
            if (meterNumber != null && !meterNumber.isEmpty()) cacheKeyBuilder.append("_meterNumber_").append(meterNumber);
            if (simNo != null && !simNo.isEmpty()) cacheKeyBuilder.append("_simNo_").append(simNo);
            if (manufacturer != null && !manufacturer.isEmpty()) cacheKeyBuilder.append("_manufacturer_").append(manufacturer);
            if (meterClass != null && !meterClass.isEmpty()) cacheKeyBuilder.append("_meterClass_").append(meterClass);
            if (category != null && !category.isEmpty()) cacheKeyBuilder.append("_category_").append(category);
            if (state != null && !state.isEmpty()) cacheKeyBuilder.append("_approvedStatus_").append(state);
            if (createdAt != null && !createdAt.isEmpty()) cacheKeyBuilder.append("_createdAt_").append(createdAt);
            cacheKeyBuilder.append("_page_").append(page);
            cacheKeyBuilder.append("_size_").append(size);

            String cacheKey = cacheKeyBuilder.toString();

            // Return from cache if available
            Object cachedUser = meterCache.get(cacheKey);
            if (cachedUser != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached Users " + status.getDesc(), cachedUser);
            }

             // Fetch all users
            List<Meter> meters = meterMapper.getMeters(um.getOrgId());

            // Apply filtering
            Stream<Meter> meterStream = meters.stream();

            if (meterNumber != null && !meterNumber.isEmpty()) {
                meterStream = meterStream.filter(u -> u.getMeterNumber() != null && u.getMeterNumber().equalsIgnoreCase(meterNumber));
            }

            if (simNo != null && !simNo.isEmpty()) {
                meterStream = meterStream.filter(u -> u.getSimNumber() != null && u.getSimNumber().equalsIgnoreCase(simNo));
            }

            if (manufacturer != null && !manufacturer.isEmpty()) {
                meterStream = meterStream.filter(u -> u.getManufacturer() != null && u.getManufacturer().equalsIgnoreCase(manufacturer));
            }


            if (meterClass != null && !meterClass.isEmpty()) {
                meterStream = meterStream.filter(u -> u.getMeterClass() != null && u.getMeterClass().equalsIgnoreCase(meterClass));
            }

            if (category != null && !category.isEmpty()) {
                meterStream = meterStream.filter(u -> u.getMeterCategory() != null && u.getMeterCategory().equalsIgnoreCase(category));
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
    public Map<String, Object> getSingleMeter(UUID meterId, String meterNumber, String accountNumber) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            Meter meter = null;
            UserModel um = handleUserValidation();

            if (meterId == null && meterNumber == null && accountNumber == null) {
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

            handleAddCache(meter);

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
                    || state.equalsIgnoreCase("assigned")
                    || state.equalsIgnoreCase("deactivated"))) {
                result = meterMapper.changeState(meterId, state, um.getOrgId());
                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(meterName +" "+ state + " "+ status.getUpdateFailureDesc());
                }
                desc = meterById.getMeterNumber() + state; //capitalizeFirstLetter(state) +" Meter [" + meterById.getMeterNumber() + "]";
            }
//            else if (assigned != null) {
//                result = meterMapper.assignMeter(meterId, assigned, um.getOrgId());
//                if (result == 0) {
//                    return ResponseMap.response(status.getUpdateCode(), meterName +" Activated or Deactivated "+ status.getUpdateFailureDesc(), "");
//                }
//                desc = assigned ? "Assigned" : "In-Stock" + " Meter [" + meterById.getMeterNumber() + "]";
//            }
//            else if (state != null){
//                result = meterMapper.activateMeter(meterId, state,um.getOrgId());
//                if (result == 0) {
//                    return ResponseMap.response(status.getUpdateCode(), meterName +" Activated or Deactivated "+ status.getUpdateFailureDesc(), "");
//                }
//                desc = state ? "Activated" : "Deactivated" + " Meter [" + meterById.getMeterNumber() + "]";
//            }
            else {
                assert state != null;
                throw new MissingServletRequestParameterException("Required request parameter '%s' is not present", state);
            }

            Meter meter = meterMapper.findById(meterId, um.getOrgId());

            if(meter.getStatus().equalsIgnoreCase("in-stock")) {
                resp = "Meter ("+ meter.getMeterNumber() + ") in-stock successfully";
            } else if (meter.getStatus().equalsIgnoreCase("assigned")) {
                resp = "Meter ("+ meter.getMeterNumber() + ") assigned successfully";
            } else if (meter.getStatus().equalsIgnoreCase("deactivated")) {
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

    @Override
    public Map<String, Object> getManufacturers() {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {

            UserModel um = handleUserValidation();

//            String fl = "feeder line";
//
//            List<SubStationTransformerFeederLine> subStationTransformerFeederLine = meterMapper.getSubStationTransformerFeederLine(um.getOrgId());
            List<Manufacturer> manufacturers = meterMapper.getManufacturers(um.getOrgId());

//            Map<String, Object> response = new HashMap<>();
//            response.put("subStationTransformerFeederLines", subStationTransformerFeederLine);
//            response.put("manufacturers", manufacturers);

//            handleAddCache(meter);

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

//            Object cachedUser = customerCache.get(id.toString()+"_"+um.getOrgId());
//
//            if (cachedUser != null) {
//                return ResponseMap.response(status.getSuccessCode(), "Cached " + customerName + " " + status.getDesc(), cachedUser);
//            }
            // check if customer exist
            Customer isCustomer = meterMapper.findByCustomerId(customerId, um.getOrgId());
            if (isCustomer == null){
                throw new GlobalExceptionHandler.NotFoundException("Customer " + status.getNotFoundDesc());
            }

            List<Tariff> allTariffs = tariffMapper.GetTariffs(um.getOrgId());

            String feederLine = "feeder line";

            List<FeederTransformer> feederTransformers = meterMapper.getTransformerFeederLine(um.getOrgId(), feederLine);

            Map<String, Object> response = new HashMap<>();
            response.put("customer", isCustomer);
            response.put("feederTransformer", feederTransformers);
            response.put("tariffs", allTariffs);

//            handleAddCache(isCustomer);

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
    public Map<String, Object> assignMeterToCustomer(
            String accountNumber, String tariff, String customerId, UUID meterId, UUID cId, String feederLine, String transformer, String substation) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {

            UserModel um = handleUserValidation();

            meterMapper.assignedMeterToCustomer(accountNumber, um.getOrgId(), meterId, customerId, feederLine, transformer, substation);

            Boolean meterAssigned = true;
            meterMapper.updateCustomer(meterAssigned, tariff, cId);

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
    public Map<String, Object> allocateMeter(UUID meterId, UUID nodeId) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");

            UserModel um = handleUserValidation();

            meterMapper.allocateMeter(meterId, nodeId, um.getOrgId());

            //fetch meter from the database
            Meter meter = meterMapper.findById(meterId, um.getOrgId());
            String desc = capitalizeFirstLetter(meter.getMeterNumber() + "allocated");
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

//    public static String capitalizeFirstLetter(String input) {
//        if (input == null || input.isEmpty()) return input;
//        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
//    }
}
