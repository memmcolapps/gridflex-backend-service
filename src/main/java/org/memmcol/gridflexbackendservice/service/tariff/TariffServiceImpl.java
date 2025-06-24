package org.memmcol.gridflexbackendservice.service.tariff;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.BandMapper;
import org.memmcol.gridflexbackendservice.mapper.TariffMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.band.Band;
//import org.memmcol.gridflexbackendservice.model.tariff.BulkApprovalRequest;
import org.memmcol.gridflexbackendservice.model.tariff.BulkApprovalRequest;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
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
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.*;
import java.util.stream.Collectors;

@Transactional
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
    private ExceptionAuditRepository exceptionAuditRepository;

    private final IMap<String, Object> tariffCache;

    private final IMap<String, Object> auditCache;

    private String tariffName = "Tariff";

    private String bandName = "Band";

    public TariffServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.tariffCache = hazelcastInstance.getMap("tariff-Cache");
        this.auditCache = hazelcastInstance.getMap("audit-Cache");
    }

    @Override
    public Map<String, Object> createTariff(Tariff tariff) {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            int result;
            String desc = "Tariff Newly Created";
            UserModel um = handleUserValidation();

            Tariff isExist = tariffMapper.getTariffByName(tariff.getName(), um.getOrgId(), tariff.getTariff_id());
            if (isExist != null) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(tariffName + " " + status.getExistDesc());
            }
//            Tariff isVersionExist = tariffMapper.getTariffVersionByName(tariff.getName(), um.getOrgId());
//            if (isVersionExist != null) {
//                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(tariffName + " " + status.getExistDesc());
//            }
            Band isBand = bandMapper.getBand(tariff.getBand());
            if (isBand == null) {
                throw new GlobalExceptionHandler.NotFoundException(bandName + " " + status.getNotFoundDesc());
            }
            tariff.setApprove_status("pending");
            tariff.setStatus(false);
            tariff.setOrg_id(um.getOrgId());
            tariff.setCreated_by(um.getId());
            tariff.setDescription(desc);
            result = tariffMapper.createTariff(tariff);
//            UUID tId = tariff.getId();
            if (result == 0) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(tariffName + " " + status.getRegFailureDesc());
            }
            tariff.setT_id(tariff.getId());
//            if(isVersionExist.getApprove_status().equalsIgnoreCase("pending")){
//                result = tariffMapper.updateTariffVersion(tariff);
//                if (result == 0) {
//                    throw new GlobalExceptionHandler.ResourceAlreadyExistsException(tariffName + " " + status.getRegFailureDesc());
//                }
//            } else{
            result = tariffMapper.createTariffVersion(tariff);
            if (result == 0) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(tariffName + " " + status.getRegFailureDesc());
            }
//            }

            Tariff tariffByName = tariffMapper.getTariff(tariff.getId(), um.getOrgId());
            um.setPassword("");
            handleAddCache(tariffByName);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);//("Created Tariff [" + tariff.getName() + "]");
            auditNotificationDTO.setType("tariff");
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

    @Override
    public Map<String, Object> manageTariffStatus(UUID tariffVersionId, String approveStatus) throws MissingServletRequestParameterException {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        int result;
        String desc = "";
        try {
            UserModel um = handleUserValidation();

            Tariff tariff = tariffMapper.getTariffVersionById(tariffVersionId, um.getOrgId());
            if(tariff == null) {
                throw new GlobalExceptionHandler.NotFoundException(tariffName + " " + status.getNotFoundDesc());
            }


            tariff.setOrg_id(um.getOrgId());
            tariff.setApproved_by(um.getId());

            if(approveStatus != null && approveStatus.contains("approve")) {
                tariff.setApprove_status("approved");
                tariff.setStatus(true);
                result = tariffMapper.approvedTariffVersion(tariff);
                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(tariffName +" "+ approveStatus + "d "+ status.getUpdateFailureDesc());
                }
                result = tariffMapper.approveTariff(tariff);
                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(tariffName +" "+ approveStatus + "d "+ status.getUpdateFailureDesc());
                }
                desc = capitalizeFirstLetter(approveStatus) +" Tariff [" + tariff.getName() + "]";
            }
            else if (approveStatus != null && approveStatus.contains("reject")){
                tariff.setApprove_status("rejected");
                tariff.setStatus(false);
                result = tariffMapper.rejectedTariffVersion(tariff);
//                result = tariffMapper.rejectedTariff(tariff);
                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(tariffName +" "+ approveStatus + "ed "+ status.getUpdateFailureDesc());
                }
                desc = capitalizeFirstLetter(approveStatus) +" Tariff [" + tariff.getName() + "]";
            }
//            else if (state != null) {
//                String approve_state = state ? "approved" : "pending";
//                result = tariffMapper.disableTariff(id, state,um.getOrgId(), approve_state);
//                if (result == 0) {
//                    return ResponseMap.response(status.getUpdateCode(), tariffName +" Activated or Deactivated "+ status.getUpdateFailureDesc(), "");
//                }
//                desc = state ? "Activated" : "Deactivated" + " Tariff [" + tariffById.getName() + "]";
//            }
            else {
                assert approveStatus != null;
                throw new MissingServletRequestParameterException("Required request parameter '%s' is not present", approveStatus);
            }

            Tariff newTariff = tariffMapper.getTariff(tariff.getId(), um.getOrgId());
            handleAddCache(tariff);
            um.setPassword("");
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setType("tariff");
            auditNotificationDTO.setCreatedTariff(newTariff);
            auditRepository.save(auditNotificationDTO);
//            if(state != null) {
//                return ResponseMap.response(status.getSuccessCode(), tariff.getName() + " " + (tariff.getStatus() ? "Activated Successfully" : status.getDeleteDesc()), "");
//            } else {
                return ResponseMap.response(status.getSuccessCode(), tariff.getName() + " " + (capitalizeFirstLetter(approveStatus) +" Successfully"), "");
//            }


        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to create tariff");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

//    @Override
//    public Map<String, Object> getTariffs(int page, int size) {
//        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
//       try {
//           Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//           String username = (authentication != null) ? authentication.getName() : "Unknown";
//           Operator isOperatorExist = operatorMapper.findByAuthEmail(username);
//           if (!isOperatorExist.isUstate()) {
//               throw new LockedException("User is blocked");
//           }
//           String cacheKey = "tariffs_" + "_page_" + page + "_size_" + size; //+ "_startDate_" + startDate + "_endDate_" + endDate;
//           Object cachedTariff = tariffCache.get(cacheKey);
//
//           if (cachedTariff != null) {
//               return ResponseMap.response(status.getSuccessCode(), "Cached " + tariffName + "s " + status.getDesc(), cachedTariff);
//           }
//
//           if(page >= 0 && size == 0) {
//               List<Tariff> response = tariffMapper.GetTariffs();
//               tariffCache.put(cacheKey, response);
//               return ResponseMap.response(status.getSuccessCode(), "All  "+tariffName + "s " + status.getDesc(), response);
//           }
//           int offset = (page - 1) * size;
//           int totalCount = tariffMapper.getTotalCount();
//           int maxLimit = Math.min(size, totalCount - offset);
//
//           // No more records available
//           if (maxLimit <= 0) {
//               return ResponseMap.response(status.getSuccessCode(), "No more records available", "");
//           }
//           // Retrieve operator list based on size
//           List<Tariff> isTariff = tariffMapper.GetTariffBySize(page, size);
//
//           if (isTariff == null) {
//               return ResponseMap.response(status.getNotFoundCode(), tariffName + " " + status.getNotFoundDesc(), "");
//           }
//
//           Map<String, Object> response = new HashMap<>();
//           response.put("data", isTariff); // List of audits
//           response.put("currentPage", page);
//           response.put("totalItems", totalCount);
//           response.put("totalPages", (int) Math.ceil((double) totalCount / size));
//           tariffCache.put(cacheKey, response);
//           return ResponseMap.response(status.getSuccessCode(), tariffName + "s " + status.getDesc(), response);
//       } catch (Exception exception) {
//           log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
//           exceptionErrorLogs.setDescription("Error occurred while trying to create tariff");
//           exceptionErrorLogs.setError_message(exception.getMessage().trim());
//           exceptionErrorLogs.setError(exception.toString());
//           exceptionAuditRepository.save(exceptionErrorLogs);
//           throw exception;
//       }
//
//    }


    @Override
    public Map<String, Object> getFilterTariffs(
            int page, int size,
            String tariffName,
            String tariffId,
            String tariffType,
            String tariffRate,
            String bandCode,
            Boolean state,
            String effectiveDate,
            String approveStatus,
            String type
            ) {

        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            UserModel um = handleUserValidation();

            // Build a unique cache key
            StringBuilder cacheKeyBuilder = new StringBuilder("tariffs_"+um.getOrgId());
            if (tariffName != null && !tariffName.isEmpty()) cacheKeyBuilder.append("_name_").append(tariffName);
            if (tariffId != null && !tariffId.isEmpty()) cacheKeyBuilder.append("_tariffId_").append(tariffId);
            if (tariffType != null && !tariffType.isEmpty()) cacheKeyBuilder.append("_type_").append(tariffType);
            if (tariffRate != null && !tariffRate.isEmpty()) cacheKeyBuilder.append("_rate_").append(tariffRate);
            if (bandCode != null && !bandCode.isEmpty()) cacheKeyBuilder.append("_band_").append(bandCode);
            if (effectiveDate != null && !effectiveDate.isEmpty()) cacheKeyBuilder.append("_date_").append(effectiveDate);
            if (approveStatus != null && !approveStatus.isEmpty()) cacheKeyBuilder.append("_status_").append(approveStatus);
            if (type != null && !type.isEmpty()) cacheKeyBuilder.append("_type_").append(type);
            if (state != null) cacheKeyBuilder.append("_state_").append(state);
            cacheKeyBuilder.append("_page_").append(page);
            cacheKeyBuilder.append("_size_").append(size);

            String cacheKey = cacheKeyBuilder.toString();

            // Return from cache if available
            Object cachedTariff = tariffCache.get(cacheKey);
            if (cachedTariff != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached tariffs " + status.getDesc(), cachedTariff);
            }

            List<Tariff> allTariffs;
            // Ideally, this should be a dynamic query in the mapper layer
            if(type.equalsIgnoreCase("pending")){
                allTariffs = tariffMapper.GetPendingTariffs(um.getOrgId());
            } else {
                allTariffs = tariffMapper.GetTariffs(um.getOrgId());
            }
            List<Tariff> filteredTariffs = allTariffs.stream()
                    .filter(t -> tariffName == null || tariffName.isEmpty() || t.getName().equalsIgnoreCase(tariffName))
                    .filter(t -> tariffId == null || tariffId.isEmpty() || t.getTariff_id().equalsIgnoreCase(tariffId))
                    .filter(t -> tariffType == null || tariffType.isEmpty() || t.getTariff_type().equalsIgnoreCase(tariffType))
                    .filter(t -> tariffRate == null || tariffRate.isEmpty() || t.getTariff_rate().equalsIgnoreCase(tariffRate))
                    .filter(t -> bandCode == null || bandCode.isEmpty() || t.getBand().equalsIgnoreCase(bandCode))
                    .filter(t -> effectiveDate == null || effectiveDate.isEmpty() || t.getEffective_date().equalsIgnoreCase(effectiveDate))
                    .filter(t -> approveStatus == null || approveStatus.isEmpty() || t.getApprove_status().equalsIgnoreCase(approveStatus))
                    .filter(t -> state == null || t.getStatus().equals(state))
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

            tariffCache.put(cacheKey, response);
            return ResponseMap.response(status.getSuccessCode(),  "Tariffs "+status.getDesc(), response);

        } catch (Exception exception) {
            log.error("Error occurred while filtering tariffs: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to filter tariffs");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    private Long parseLongOrNull(String value) {
        try {
            return (value != null && !value.isEmpty()) ? Long.parseLong(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Map<String, Object> bulkApproveTariff(BulkApprovalRequest request) {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            UserModel um = handleUserValidation();

                for(UUID id : request.getTariffIds()) {
                    Tariff t = tariffMapper.getTariffVersionById(id, um.getOrgId());
                    if(t == null){
                        throw new GlobalExceptionHandler.NotFoundException(status.getNotFoundDesc());
                    }
                    t.setApprove_status("approved");
                    t.setStatus(true);
                    t.setApproved_by(um.getId());

                    // update tariff version main table
                    tariffMapper.approvedTariffVersion(t);

                    // update tariff main table
                    tariffMapper.approveTariff(t);
                    Tariff tariff = tariffMapper.getTariff(id, um.getOrgId());
                    if(tariff == null) {
                        String desc = t.getApprove_status() + "Tariff [" + id + "] does not exist ";
                        auditNotificationDTO.setCreator(um);
                        auditNotificationDTO.setDescription(desc);
                        auditNotificationDTO.setType("tariff");
                        auditNotificationDTO.setCreatedTariff(null);
                        auditRepository.save(auditNotificationDTO);
                        continue;
                    }
                    handleAddCache(tariff);
                    String desc = t.getApprove_status() + " Tariff [" + tariff.getName() + "]";
                    um.setPassword("");
                    auditNotificationDTO.setCreator(um);
                    auditNotificationDTO.setDescription(desc);
                    auditNotificationDTO.setType("tariff");
                    auditNotificationDTO.setCreatedTariff(tariff);
                    auditRepository.save(auditNotificationDTO);
                }
                return ResponseMap.response(status.getSuccessCode(), tariffName + " Approved Successfully ", "");
//            }
//            return ResponseMap.response(status.getUpdateCode(), request.getApproveStatus() + " value not accepted try [approved]", "");

        } catch (Exception exception) {
            log.error("Error occurred while bulk approving tariff(s): {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to create tariff");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }

    }

    @Override
    public Map<String, Object> updateTariff(Tariff tariff) {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            int result;
            UserModel um = handleUserValidation();

            Tariff isExist = tariffMapper.getTariff(tariff.getT_id(), um.getOrgId());
            if (isExist == null) {
                throw new GlobalExceptionHandler.NotFoundException(tariffName + " " + status.getNotFoundDesc());
            }
            Band isBand = bandMapper.getBand(tariff.getBand());
            if (isBand == null) {
                throw new GlobalExceptionHandler.NotFoundException(bandName + " " + status.getNotFoundDesc());
            }

            Tariff isVersionExist = tariffMapper.getTariffVersionByName(tariff.getName(), um.getOrgId());

            tariff.setApprove_status("pending");
            tariff.setStatus(false);
            tariff.setOrg_id(um.getOrgId());
            tariff.setCreated_by(um.getId());
            String changeDescription = buildChangeDescription(isExist, tariff);
            tariff.setDescription(changeDescription);

            if(isVersionExist != null && isVersionExist.getApprove_status().equalsIgnoreCase("pending")){
                result = tariffMapper.updateTariffVer(tariff);
                if (result == 0) {
                    throw new GlobalExceptionHandler.ResourceAlreadyExistsException(tariffName + " " + status.getRegFailureDesc());
                }
            } else {
                result = tariffMapper.createTariffVersion(tariff);
                if (result == 0) {
                    throw new GlobalExceptionHandler.ResourceAlreadyExistsException(tariffName + " " + status.getRegFailureDesc());
                }
            }

            if (result == 0) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(tariffName + " " + status.getUpdateFailureDesc());
            }

            Tariff tariffByName = tariffMapper.getTariff(tariff.getT_id(), um.getOrgId());
            um.setPassword("");
            handleAddCache(tariffByName);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(changeDescription);
            auditNotificationDTO.setType("tariff");
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

    @Override
    public Map<String, Object> getTariff(UUID id, UUID tariffVersionId) {
        try {
            UserModel um = handleUserValidation();
            Object cachedTariff = null;
            if(id != null){
                cachedTariff = tariffCache.get(id.toString());
            }
            if(tariffVersionId != null){
                cachedTariff = tariffCache.get(tariffVersionId.toString());
            }


            if (cachedTariff != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached " + tariffName + " " + status.getDesc(), cachedTariff);
            }
            Tariff result = null;

            if(id != null){
                result = tariffMapper.getTariff(id, um.getOrgId());
            }

            if(tariffVersionId != null){
                result = tariffMapper.getSingleTariffVersionById(id, um.getOrgId());
            }
//            Tariff result = tariffMapper.getTariff(id, um.getOrgId());
            if(result == null) {
                throw new GlobalExceptionHandler.NotFoundException(tariffName + " " + status.getNotFoundDesc());
            }
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


    public static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
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
            throw new LockedException("User is disable");
        }

        return isOperatorExist;
    }


    private void handleAddCache(Tariff tariff) {
        tariffCache.remove(tariff.getId().toString()+"_"+tariff.getOrg_id());
        for (String key : auditCache.keySet()) {
            if (key.startsWith("grid_flex_audit_log_page_")) {
                auditCache.remove(key);
            }
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
            changes.append(String.format("name: '%s' → '%s'; ", oldTariff.getName(), newTariff.getName()));
        }
//
//        if (!Objects.equals(oldTariff.getTariff_id(), newTariff.getTariff_id())) {
//            changes.append(String.format("tariff: '%s' → '%s'; ", oldTariff.getTariff_id(), newTariff.getTariff_id()));
//        }

        if (!Objects.equals(oldTariff.getTariff_type(), newTariff.getTariff_type())) {
            changes.append(String.format("tariff_type: '%s' → '%s'; ", oldTariff.getTariff_type(), newTariff.getTariff_type()));
        }

        if (!Objects.equals(oldTariff.getTariff_rate(), newTariff.getTariff_rate())) {
            changes.append(String.format("tariff_rate: '%s' → '%s'; ", oldTariff.getTariff_rate(), newTariff.getTariff_rate()));
        }

        if (!Objects.equals(oldTariff.getBand(), newTariff.getBand())) {
            changes.append(String.format("band: '%s' → '%s'; ", oldTariff.getBand(), newTariff.getBand()));
        }

        if (!Objects.equals(oldTariff.getEffective_date(), newTariff.getEffective_date())) {
            changes.append(String.format("effective_date: '%s' → '%s'; ", oldTariff.getEffective_date(), newTariff.getEffective_date()));
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