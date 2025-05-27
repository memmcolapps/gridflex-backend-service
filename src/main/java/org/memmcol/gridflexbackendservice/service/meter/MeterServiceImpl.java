package org.memmcol.gridflexbackendservice.service.meter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.MeterMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.node.Node;
import org.memmcol.gridflexbackendservice.model.node.SubStationTransformerFeederLine;
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
    private ExceptionAuditRepository exceptionAuditRepository;

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
            UserModel um = handleUserValidation();

            if(request.getManufacturer().isEmpty() || request.getManufacturer() == null) {
                throw new GlobalExceptionHandler.NotFoundException("Manufacturer not found");
            }
            request.setApprovedStatus("pending");

            request.setOrgId(um.getOrgId());

            mapperMapper.insertMeter(request);

            UUID meterId = request.getId();
            System.out.println("id: " + meterId);

            Meter meter = meterMapper.findById(meterId, request.getOrgId());
            handleAddCache(meter);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription("Created Meter [" + meter.getMeterNumber() + "]");
            auditNotificationDTO.setType("meter");
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
            UserModel um = handleUserValidation();
            request.setOrgId(um.getOrgId());
            mapperMapper.updateMeter(request);

//            UUID meterId = request.getId();
//            System.out.println("id: " + meterId);

            Meter meter = meterMapper.findById(request.getId(), um.getOrgId());
//            handleAddCache(meter);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription("Updated Meter [" + meter.getMeterNumber() + "]");
            auditNotificationDTO.setType("meter");
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
            String meterClass, String category, String approvedStatus, Boolean state, String createdAt) {
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
            if (approvedStatus != null && !approvedStatus.isEmpty()) cacheKeyBuilder.append("_approvedStatus_").append(approvedStatus);
            if (createdAt != null && !createdAt.isEmpty()) cacheKeyBuilder.append("_createdAt_").append(createdAt);
            if (state != null) cacheKeyBuilder.append("_state_").append(state);
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

            if (approvedStatus != null && !approvedStatus.isEmpty()) {
                meterStream = meterStream.filter(u -> u.getApprovedStatus() != null && u.getApprovedStatus().equalsIgnoreCase(approvedStatus));
            }

            if (state != null) {
                meterStream = meterStream.filter(u -> u.getStatus() != null && u.getStatus());
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
    public Map<String, Object> getSingleMeter(UUID meterId) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {

            UserModel um = handleUserValidation();

            Object cachedUser = meterCache.get(meterId.toString()+"_"+um.getOrgId());

            if (cachedUser != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached " + meterName + " " + status.getDesc(), cachedUser);
            }

            Meter meter = meterMapper.getMeter(um.getOrgId(), meterId);

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
    public Map<String, Object> changeStatus(UUID meterId, Boolean state, String approveStatus) throws MissingServletRequestParameterException {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        int result;
        String desc = "";
        try {

            UserModel um = handleUserValidation();

            Meter meterById = meterMapper.getMeter(um.getOrgId(), meterId);
            if(meterById == null) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getNotFoundDesc());
            }

            if(state != null && approveStatus != null) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException("you can not perform two operations at the same time");
            }

            if(approveStatus != null && (approveStatus.equalsIgnoreCase("pending") || approveStatus.equalsIgnoreCase("approved") || approveStatus.equalsIgnoreCase("rejected"))) {
                result = meterMapper.approveMeter(meterId, approveStatus, um.getOrgId());
                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(meterName +" "+ approveStatus + " "+ status.getUpdateFailureDesc());
                }
                desc = capitalizeFirstLetter(approveStatus) +" Tariff [" + meterById.getMeterNumber() + "]";
            } else if (state != null) {
                result = meterMapper.disableMeter(meterId, state, um.getOrgId());
                if (result == 0) {
                    return ResponseMap.response(status.getUpdateCode(), meterName +" Activated or Deactivated "+ status.getUpdateFailureDesc(), "");
                }
                desc = state ? "Assigned" : "In-Stock" + " User [" + meterById.getMeterNumber() + "]";
            } else {
                throw new MissingServletRequestParameterException("Required request parameter '%s' is not present", "approveStatus or status");
            }


            Meter meter = meterMapper.findById(meterId, um.getOrgId());

            handleAddCache(meter);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
//            auditNotificationDTO.setReason(reason);
            auditNotificationDTO.setType("meter");
            auditNotificationDTO.setCreatedMeter(meter);
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
    public Map<String, Object> fetchAllSubstationsTransformersFeederLine() {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {

            UserModel um = handleUserValidation();

            List<SubStationTransformerFeederLine> subStationTransformerFeederLine = meterMapper.getSubStationTransformerFeederLine(um.getOrgId());
            List<Manufacturer> manufacturers = meterMapper.getManufacturers(um.getOrgId());

            Map<String, Object> response = new HashMap<>();
            response.put("subStationTransformerFeederLines", subStationTransformerFeederLine);
            response.put("manufacturers", manufacturers);

//            handleAddCache(meter);

            return ResponseMap.response(status.getSuccessCode(),  status.getDesc(), response);
        } catch (Exception exception) {
            log.error("Error occurred while fetching feeder lines [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to fetch transformer");
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

        if (!Boolean.TRUE.equals(isOperatorExist.getStatus())) {
            throw new LockedException("User is disabled");
        }

        return isOperatorExist;
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

    public static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }
}
