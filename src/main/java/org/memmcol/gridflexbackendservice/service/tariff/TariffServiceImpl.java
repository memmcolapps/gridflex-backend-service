package org.memmcol.gridflexbackendservice.service.tariff;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.BandMapper;
import org.memmcol.gridflexbackendservice.mapper.TariffMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.model.tariff.BulkApprovalRequest;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
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

import java.util.*;
import java.util.stream.Collectors;

import static org.memmcol.gridflexbackendservice.util.GenericHandler.capitalizeFirstLetter;
import static org.memmcol.gridflexbackendservice.util.GenericHandler.getClientIp;
import static org.memmcol.gridflexbackendservice.components.handleValidUser.handleUserValidation;

@Service
public class TariffServiceImpl implements TariffService {
    private static final Logger log = LoggerFactory.getLogger(TariffServiceImpl.class);
    @Autowired
    private TariffMapper tariffMapper;

    @Autowired
    private AuthMapper operatorMapper;

    @Autowired
    private BandMapper bandMapper;

    @Autowired
    private ResponseProperties status;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;

    private final IMap<String, Object> tariffCache;

    private final IMap<String, Object> auditCache;

    private String tariffName = "Tariff";

    private String bandName = "Band";

    public TariffServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.tariffCache = hazelcastInstance.getMap("tariffCache");
        this.auditCache = hazelcastInstance.getMap("auditCache");
    }

    @Transactional
    @Override
    public Map<String, Object> createTariff(Tariff tariff) {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            int result;
            String desc = tariff.getName()+" created";
            UserModel um = handleUserValidation();

            Tariff isExist = tariffMapper.getTariffByName(tariff.getName(), um.getOrgId());
            if (isExist != null) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(tariffName + " " + status.getExistDesc());
            }

            Tariff isVersionExist = tariffMapper.getTariffVersionByName(tariff.getName(), um.getOrgId());
            if(isVersionExist != null) {
                throw new GlobalExceptionHandler.NotFoundException(isVersionExist.getName()+ " have a pending status needs to be cleared");
            }

            Band isBand = bandMapper.getApprovedBandById(tariff.getBand_id(), um.getOrgId());
            if (isBand == null) {
                throw new GlobalExceptionHandler.NotFoundException(bandName + " is either not found, not approved or deactivated" );
            }
            tariff.setApprove_status("Pending-created");
//            tariff.setStatus(true);
            tariff.setOrg_id(um.getOrgId());
            tariff.setCreated_by(um.getId());
//            tariff.setAction("Created");
            tariff.setDescription(desc);
            result = tariffMapper.createTariff(tariff);
            if (result == 0) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(tariffName + " " + status.getRegFailureDesc());
            }
            tariff.setT_id(tariff.getId());
            result = tariffMapper.createTariffVersion(tariff);
            if (result == 0) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(tariffName + " " + status.getRegFailureDesc());
            }

            Tariff tariffByName = tariffMapper.getTariff(tariff.getId(), um.getOrgId());
            um.setPassword("");
//            handleAddCache(tariffByName);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setType(tariffName);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setCreatedTariff(tariffByName);
            auditRepository.save(auditNotificationDTO);
            return ResponseMap.response(status.getSuccessCode(), tariffName + " " + status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to create tariff");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> approve(UUID tariffVersionId, String approveStatus) throws MissingServletRequestParameterException {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        int result;
        String desc = "";
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            UserModel um = handleUserValidation();

            Tariff tariff = tariffMapper.getTariffVersionById(tariffVersionId, um.getOrgId());
            if(tariff == null) {
                throw new GlobalExceptionHandler.NotFoundException("Tariff either have no pending state or not found");
            }
//            Band isBand = bandMapper.checkBandStatus(tariff.getBand_id(), um.getOrgId());
//            if (isBand == null) {
//                throw new GlobalExceptionHandler.NotFoundException("Approval failed because the band assign to this tariff is either on pending state or deactivated");
//            }

            tariff.setOrg_id(um.getOrgId());
            tariff.setApproved_by(um.getId());

            if(approveStatus != null && approveStatus.trim().equalsIgnoreCase("approve")) {

                if (tariff.getApprove_status().equals("Pending-deactivated")) {
                    tariff.setApprove_status("Deactivated");
                } else {
                    tariff.setApprove_status("Approved");
                }
                result = tariffMapper.approvedTariffVersion(tariff);

                if (result == 0) throw new GlobalExceptionHandler.NotFoundException(tariffName +" "+ approveStatus + "d "+ status.getUpdateFailureDesc());

                System.out.println(">>>>>>>>>>>>>>::: "+tariff.getBand_id());
                result = tariffMapper.approveTariff(tariff);
                if (result == 0) throw new GlobalExceptionHandler.NotFoundException(tariffName +" "+ approveStatus + "d "+ status.getUpdateFailureDesc());

                desc = capitalizeFirstLetter(tariff.getName()) + approveStatus;

            } else if (approveStatus != null && approveStatus.trim().equalsIgnoreCase("reject")) {

                result = tariffMapper.rejectedTariffVersion("Rejected", tariff.getT_id(), tariff.getUpdated_at(), um.getId());

                if (result == 0) throw new GlobalExceptionHandler.NotFoundException(tariffName +" "+ approveStatus + "ed "+ status.getUpdateFailureDesc());

                if(tariff.getApprove_status().trim().equalsIgnoreCase("Pending-created")){

                    int d = tariffMapper.deleteTariff(tariff.getT_id());
                    if(d == 0) throw new GlobalExceptionHandler.NotFoundException(bandName + " failed to delete");

                } else if(tariff.getApprove_status().trim().contains("Pending-deactivated")){

                    tariff.setApprove_status("Approved");
                    int u = tariffMapper.updateTariff(tariff.getApprove_status(), tariff.getT_id(), tariff.getUpdated_at());
                    if(u == 0) throw new GlobalExceptionHandler.NotFoundException(bandName + " deactivation failed");

                } else {

                    tariff.setApprove_status("Approved");
                    int u = tariffMapper.updateTariff(tariff.getApprove_status(), tariff.getT_id(), tariff.getUpdated_at());
                    if(u == 0) throw new GlobalExceptionHandler.NotFoundException(bandName + " update failed");
                }
                desc = capitalizeFirstLetter(tariff.getName()) + approveStatus;
            }
            else {
                assert approveStatus != null;
                throw new MissingServletRequestParameterException("Required request parameter '%s' is not present", approveStatus);
            }

            Tariff newTariff = tariffMapper.getTariff(tariff.getId(), um.getOrgId());
            handleAddCache(tariff);
            um.setPassword("");
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setType(tariffName);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setCreatedTariff(newTariff);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(), tariff.getName() + " " + (capitalizeFirstLetter(approveStatus) +" Successfully"), "");

        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to create tariff");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> changeStatus(UUID id, Boolean state) {
        AuditLog auditNotificationDTO = new AuditLog();
        String ipAddress = getClientIp(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");
        try {
            int result;
            UserModel um = handleUserValidation();
            Tariff tariff = tariffMapper.getTariff(id, um.getOrgId());
            if(tariff == null){
                throw new GlobalExceptionHandler.NotFoundException("Tariff not found");
            }
            Tariff isVersionExist = tariffMapper.getTariffVersionById(id, um.getOrgId());
            tariff.setApprove_status("Pending-"+(state ? "activated" : "deactivated"));
            tariff.setOrg_id(um.getOrgId());
            tariff.setCreated_by(um.getId());
            tariff.setT_id(id);

            String changeDescription = buildChangeStatusDescription(tariff, state);
            tariff.setDescription(changeDescription);

            if(isVersionExist != null){
                throw new GlobalExceptionHandler.NotFoundException(isVersionExist.getName()+ " have a pending status needs to be cleared");
            } else {
                result = tariffMapper.createTariffVersion(tariff);
                if(result == 0){
                    throw new GlobalExceptionHandler.NotFoundException(bandName + " " + status.getUpdateDesc());
                }
            }
            int u = tariffMapper.updateTariff(tariff.getApprove_status(), tariff.getId(), tariff.getUpdated_at());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException(bandName + (state ? " activate " : " deactivate ")+ "failed");
            Tariff tariffByName = tariffMapper.getTariff(tariff.getT_id(), um.getOrgId());
//            handleAddCache(tariffByName);
            um.setPassword("");
            handleAddCache(tariffByName);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(changeDescription);
            auditNotificationDTO.setType(tariffName);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setCreatedTariff(tariffByName);
            auditRepository.save(auditNotificationDTO);
            return ResponseMap.response(status.getSuccessCode(), bandName + " " + status.getDesc(), "");
        }  catch (Exception exception) {
            ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to create band");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getFilterTariffs(
            int page, int size,
            String tariffName,
            String tariffType,
            String tariffRate,
            String bandCode,
            String effectiveDate,
            String approveStatus,
            String type) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            UserModel um = handleUserValidation();

            // Build a unique cache key
            StringBuilder cacheKeyBuilder = new StringBuilder("tariffs_" + um.getOrgId());
            if (tariffName != null && !tariffName.isEmpty()) cacheKeyBuilder.append("_name_").append(tariffName);
//            if (tariffId != null && !tariffId.isEmpty()) cacheKeyBuilder.append("_tariffId_").append(tariffId);
            if (tariffType != null && !tariffType.isEmpty()) cacheKeyBuilder.append("_type_").append(tariffType);
            if (tariffRate != null && !tariffRate.isEmpty()) cacheKeyBuilder.append("_rate_").append(tariffRate);
            if (bandCode != null && !bandCode.isEmpty()) cacheKeyBuilder.append("_band_").append(bandCode);
            if (effectiveDate != null && !effectiveDate.isEmpty())
                cacheKeyBuilder.append("_date_").append(effectiveDate);
            if (approveStatus != null && !approveStatus.isEmpty())
                cacheKeyBuilder.append("_status_").append(approveStatus);
            if (type != null && !type.isEmpty()) cacheKeyBuilder.append("_type_").append(type);
//            if (state != null) cacheKeyBuilder.append("_state_").append(state);
            cacheKeyBuilder.append("_page_").append(page);
            cacheKeyBuilder.append("_size_").append(size);

//            String cacheKey = cacheKeyBuilder.toString();
//
//            // Return from cache if available
//            Object cachedTariff = tariffCache.get(cacheKey);
//            if (cachedTariff != null) {
//                return ResponseMap.response(status.getSuccessCode(), "Cached tariffs " + status.getDesc(), cachedTariff);
//            }

            List<Tariff> allTariffs;
            // Ideally, this should be a dynamic query in the mapper layer
            if (type.equalsIgnoreCase("pending-state")) {
                allTariffs = tariffMapper.GetPendingTariffs(um.getOrgId());
            } else {
                allTariffs = tariffMapper.GetAllTariffs(um.getOrgId());
            }
            List<Tariff> filteredTariffs = allTariffs.stream()
                    .filter(t -> tariffName == null || tariffName.isEmpty() || t.getName().equalsIgnoreCase(tariffName))
                    .filter(t -> tariffType == null || tariffType.isEmpty() || t.getTariff_type().equalsIgnoreCase(tariffType))
                    .filter(t -> tariffRate == null || tariffRate.isEmpty() || t.getTariff_rate().equalsIgnoreCase(tariffRate))
                    .filter(t -> effectiveDate == null || effectiveDate.isEmpty() || t.getEffective_date().equalsIgnoreCase(effectiveDate))
                    .filter(t -> approveStatus == null || approveStatus.isEmpty() || t.getApprove_status().equalsIgnoreCase(approveStatus))
                    .collect(Collectors.toList());


            // Pagination logic
            int totalTariffs = filteredTariffs.size();
            List<Tariff> paginatedTariffs;
            if (size == 0) {
                paginatedTariffs = filteredTariffs; // Return all users
            } else {
                int fromIndex = Math.min(page * size, totalTariffs);
                int toIndex = Math.min(fromIndex + size, totalTariffs);
                paginatedTariffs = filteredTariffs.subList(fromIndex, toIndex);
            }

            // Prepare response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedTariffs);
            response.put("totalData", totalTariffs);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", (int) Math.ceil((double) paginatedTariffs.size() / size));

//            tariffCache.put(cacheKey, response);
            return ResponseMap.response(status.getSuccessCode(), "Tariffs " + status.getDesc(), response);

        } catch (Exception exception) {
            log.error("Error occurred while filtering tariffs: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to filter tariffs");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> bulkApproveTariff(BulkApprovalRequest request) {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            UserModel um = handleUserValidation();

                for(UUID id : request.getTariffIds()) {
                    Tariff t = tariffMapper.getTariffVersionById(id, um.getOrgId());
                    if(t == null){
                        throw new GlobalExceptionHandler.NotFoundException(status.getNotFoundDesc());
                    }
                    t.setApprove_status("approved");
//                    t.setStatus(true);
                    t.setApproved_by(um.getId());

                    // update tariff version main table
                    tariffMapper.approvedTariffVersion(t);

                    // update tariff main table
                    tariffMapper.approveTariff(t);
                    Tariff tariff = tariffMapper.getTariff(id, um.getOrgId());
                    if(tariff == null) {
                        String desc = t.getApprove_status() + "tariff [" + id + "] does not exist ";
                        auditNotificationDTO.setCreator(um);
                        auditNotificationDTO.setDescription(desc);
                        auditNotificationDTO.setType(tariffName);
                        auditNotificationDTO.setCreatedTariff(null);
                        auditRepository.save(auditNotificationDTO);
                        continue;
                    }
                    handleAddCache(tariff);
                    String desc = capitalizeFirstLetter(tariff.getName()) + t.getApprove_status();
                    um.setPassword("");
                    auditNotificationDTO.setCreator(um);
                    auditNotificationDTO.setDescription(desc);
                    auditNotificationDTO.setIpAddress(ipAddress);
                    auditNotificationDTO.setUserAgent(userAgent);
                    auditNotificationDTO.setType(tariffName);
                    auditNotificationDTO.setCreatedTariff(tariff);
                    auditRepository.save(auditNotificationDTO);
                }
                return ResponseMap.response(status.getSuccessCode(), tariffName + " approved successfully ", "");

        } catch (Exception exception) {
            log.error("Error occurred while bulk approving tariff(s): {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to create tariff");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }

    }

    @Transactional
    @Override
    public Map<String, Object> updateTariff(Tariff tariff) {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            int result;
            UserModel um = handleUserValidation();

            Tariff isExist = tariffMapper.getTariff(tariff.getT_id(), um.getOrgId());
            if (isExist == null) {
                throw new GlobalExceptionHandler.NotFoundException(tariffName + " " + status.getNotFoundDesc());
            }
//            Band isBand = bandMapper.getBandById(tariff.getBand_id(), um.getOrgId());
            Band isBand = bandMapper.getApprovedBandById(tariff.getBand_id(), um.getOrgId());
            if (isBand == null) {
                throw new GlobalExceptionHandler.NotFoundException(bandName + " is either not found, not approved or deactivated" );
            }

            Tariff isVersionExist = tariffMapper.getTariffVersionByName(tariff.getName(), um.getOrgId());

            tariff.setApprove_status("Pending-edited");
            tariff.setOrg_id(um.getOrgId());
            tariff.setCreated_by(um.getId());
            String changeDescription = buildChangeDescription(isExist, tariff);
            tariff.setDescription(changeDescription);

            if(isVersionExist != null){
                throw new GlobalExceptionHandler.NotFoundException(isVersionExist.getName()+ " have a pending status needs to be cleared");
            } else {
                int res = tariffMapper.updateTariff("Pending-edited", tariff.getT_id(), tariff.getUpdated_at());
                result = tariffMapper.createTariffVersion(tariff);
                if (result == 0 || res == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(tariffName + " " + status.getUpdateFailureDesc());
                }
            }

            Tariff tariffByName = tariffMapper.getTariff(tariff.getT_id(), um.getOrgId());
            um.setPassword("");
//            handleAddCache(tariffByName);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(changeDescription);
            auditNotificationDTO.setType(tariffName);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setCreatedTariff(tariffByName);
            auditRepository.save(auditNotificationDTO);
            return ResponseMap.response(status.getSuccessCode(), tariffName + " " + status.getUpdateDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to create tariff");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getTariff(UUID tariffId, UUID tariffVersionId) {
        try {
            UserModel um = handleUserValidation();
//            Object cachedTariff = null;
//            if(tariffId != null) {
//                cachedTariff = tariffCache.get(tariffId.toString());
//            }
//            if(tariffVersionId != null){
//                cachedTariff = tariffCache.get(tariffVersionId.toString());
//            }
//
//            if (cachedTariff != null) {
//                return ResponseMap.response(status.getSuccessCode(), "Cached " + tariffName + " " + status.getDesc(), cachedTariff);
//            }
            Tariff result = null;

            if(tariffId != null){
                result = tariffMapper.getTariff(tariffId, um.getOrgId());
            }

            if(tariffVersionId != null){
                result = tariffMapper.getTariffVersionById(tariffVersionId, um.getOrgId());
            }

            if(result == null) {
                throw new GlobalExceptionHandler.NotFoundException(tariffName + " " + status.getNotFoundDesc());
            }

//            handleAddCache(result);

            return ResponseMap.response(status.getSuccessCode(), tariffName + " " + status.getDesc(), result);
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

    private void handleAddCache(Tariff tariff) {
        tariffCache.remove(tariff.getId().toString()+"_"+tariff.getOrg_id());
        for (String key : auditCache.keySet()) {
            if (key.startsWith("grid_flex_audit_log_page_")) {
                auditCache.remove(key);
            } //"tariffs_"+um.getOrgId()
        }
        for (String key : tariffCache.keySet()) {
            if (key.startsWith("tariffs_"+tariff.getOrg_id())) {
                tariffCache.remove(key);
            }
        }
        tariffCache.put(tariff.getId().toString()+"_"+tariff.getOrg_id(), tariff);  // Cache updated or deleted entity
    }

    private String buildChangeDescription(Tariff oldTariff, Tariff newTariff) {
        StringBuilder changes = new StringBuilder("Edited ");

        if (!Objects.equals(oldTariff.getName(), newTariff.getName())) {
            changes.append(String.format("name: '%s' → '%s' ", oldTariff.getName(), newTariff.getName()));
        }
//
//        if (!Objects.equals(oldTariff.getTariff_id(), newTariff.getTariff_id())) {
//            changes.append(String.format("tariff: '%s' → '%s'; ", oldTariff.getTariff_id(), newTariff.getTariff_id()));
//        }

        if (!Objects.equals(oldTariff.getTariff_type(), newTariff.getTariff_type())) {
            changes.append(String.format("tariff_type: '%s' → '%s' ", oldTariff.getTariff_type(), newTariff.getTariff_type()));
        }

        if (!Objects.equals(oldTariff.getTariff_rate(), newTariff.getTariff_rate())) {
            changes.append(String.format("tariff_rate: '%s' → '%s' ", oldTariff.getTariff_rate(), newTariff.getTariff_rate()));
        }

        if (!Objects.equals(oldTariff.getBand(), newTariff.getBand())) {
            changes.append(String.format("band: '%s' → '%s' ", oldTariff.getBand(), newTariff.getBand()));
        }

        if (!Objects.equals(oldTariff.getEffective_date(), newTariff.getEffective_date())) {
            changes.append(String.format("effective_date: '%s' → '%s' ", oldTariff.getEffective_date(), newTariff.getEffective_date()));
        }

        return changes.toString();
    }

    private String buildChangeStatusDescription(Tariff oldtariff, Boolean status) {
        StringBuilder changes = new StringBuilder("Edited tariff ");
        String oldState = oldtariff.getApprove_status().trim().equalsIgnoreCase("Approved") ? "activated" : "deactivated";
        String newState = status ? "activated" : "deactivated";
        if (!Objects.equals(oldtariff.getApprove_status(), newState)) {
            changes.append(String.format("status: '%s' → '%s' ", oldState, newState));
        }

        return changes.toString();
    }

}



//    @Override
//    public Map<String, Object> getFilterTariffs(String tariffName, String tariffIndex, String tariffType, String bandCode, Boolean state, String effectiveDate, String approveStatus) {
//        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
//        try {
//            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//            String username = (authentication != null) ? authentication.getName() : "Unknown";
//            Operator isOperatorExist = operatorMapper.findByAuthEmail(username);
//            if (!isOperatorExist.isUstate()) {
//                throw new LockedException("User is blocked");
//            }
//            String cacheKey = "tariffs_" + tariffName == "" ? Objects.equals(tariffIndex, "") ? Objects.equals(tariffType, "") ? Objects.equals(tariffType, "") ? Objects.equals(bandCode, "") ? Objects.equals(effectiveDate, "") ? approveStatus : "";
//            Object cachedTariff = tariffCache.get(cacheKey);
//
//            if (cachedTariff != null) {
//                return ResponseMap.response(status.getSuccessCode(), "Cached " + tariffName + "s " + status.getDesc(), cachedTariff);
//            }
//            List<Tariff> response = List.of();
//            if(Objects.equals(state, "true") || Objects.equals(state, "false")) {
//                response = tariffMapper.GetTariffByStatusFilter(state);
//            }
//
//            if(!tariffName.isEmpty()) {
//                response = tariffMapper.GetTariffByNameFilter(tariffName);
//            }
//
//            if(!tariffIndex.isEmpty()) {
//                response = tariffMapper.GetTariffByIndexFilter(tariffIndex);
//            }
//
//            if(!tariffType.isEmpty()) {
//                response = tariffMapper.GetTariffByTypeFilter(tariffType);
//            }
//
//            if(!bandCode.isEmpty()) {
//                response = tariffMapper.GetTariffBandCodeFilter(bandCode);
//            }
//
//            if(!effectiveDate.isEmpty()) {
//                response = tariffMapper.GetTariffEffectiveDateFilter(effectiveDate);
//            }
//            tariffCache.put(cacheKey, response);
//            return ResponseMap.response(status.getSuccessCode(), tariffName + "s " + status.getDesc(), response);
//
//        } catch (Exception exception) {
//            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
//            exceptionErrorLogs.setDescription("Error occurred while trying to create tariff");
//            exceptionErrorLogs.setError_message(exception.getMessage().trim());
//            exceptionErrorLogs.setError(exception.toString());
//            exceptionAuditRepository.save(exceptionErrorLogs);
//            throw exception;
//        }
//
//    }
/// -----------------------------------------
//    private Map<String, Object> handleTariffDataFilter(String filter, int page, int size, int filterDataCount, Boolean state) {
//
//        String cacheKey = "tariffs_" + filter;
//        Object cachedTariff = tariffCache.get(cacheKey);
//
//        if (cachedTariff != null) {
//            return ResponseMap.response(status.getSuccessCode(), "Cached " + tariffName + "s " + status.getDesc(), cachedTariff);
//        }
//        int offset = (page - 1) * size;
//        int maxLimit = Math.min(size, filterDataCount - offset);
//        if (maxLimit <= 0) {
//            return ResponseMap.response(status.getSuccessCode(), "No more records available", "");
//        }
//        List<Tariff> isTariff;
//        if(filter.equals("true") || Objects.equals(filter, "false")) {
//            // Retrieve tariff list based on size
//            Boolean filterBool = Boolean.parseBoolean(filter);
//            isTariff = tariffMapper.GetTariffByStatusFilter(page, size, filterBool);
//        } else {
//            // Retrieve tariff list based on size
//            isTariff = tariffMapper.GetTariffByFilter(page, size, filter);
//        }
//
//        if (isTariff == null) {
//            return ResponseMap.response(status.getNotFoundCode(), tariffName + " " + status.getNotFoundDesc(), "");
//        }
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("data", isTariff); // List of audits
//        response.put("currentPage", page);
//        response.put("totalItems", filterDataCount);
//        response.put("totalPages", (int) Math.ceil((double) filterDataCount / size));
//        tariffCache.put(cacheKey, response);
//        return ResponseMap.response(status.getSuccessCode(), tariffName + "s " + status.getDesc(), response);
//    }