package org.memmcol.gridflexbackendservice.service.tariff;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.BandMapper;
import org.memmcol.gridflexbackendservice.mapper.TariffMapper;
import org.memmcol.gridflexbackendservice.model.*;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

            Tariff isExist = tariffMapper.getTariff(tariff.getName());
            if (isExist != null) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(tariffName + " " + status.getExistDesc());
//                return ResponseMap.response(status.getExistCode(), tariffName + " " + status.getExistDesc(), "");
            }
            Band isBand = bandMapper.getBand(tariff.getBand());
            if (isBand == null) {
                throw new GlobalExceptionHandler.NotFoundException(bandName + " " + status.getNotFoundDesc());
//                return ResponseMap.response(status.getNotFoundCode(), bandName + " " + status.getNotFoundDesc(), "");
            }
            tariff.setApprove_status("pending");
            tariff.setStatus(true);
            int result = tariffMapper.createTariff(tariff);
            if (result == 0) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(tariffName + " " + status.getRegFailureDesc());
//                return ResponseMap.response(status.getRegCode(), tariffName + " " + status.getRegFailureDesc(), "");
            }
            Tariff tariffByName = tariffMapper.getTariff(tariff.getName());
            isOperatorExist.setPassword("");
            handleAddCache(tariffByName);
            auditNotificationDTO.setCreator(isOperatorExist);
            auditNotificationDTO.setDescription("Created Tariff [" + tariff.getName() + "]");
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
    public Map<String, Object> manageTariffStatus(Long tariffId, Boolean state, String approveStatus) {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        int result;
        String desc = "";
        try {
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

            Tariff tariffById = tariffMapper.getTariffById(tariffId);
            if(tariffById == null) {
                throw new GlobalExceptionHandler.NotFoundException(tariffName + " " + status.getNotFoundDesc());
//                return ResponseMap.response(status.getNotFoundCode(), tariffName + " " + status.getNotFoundDesc(), "");
            }

            if(state != null && approveStatus != null) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException("you can not perform two operations at the same time");
//                return ResponseMap.response(status.getUpdateCode(), "you can not perform two operations at the same time", "");
            }
            if(approveStatus != null && (approveStatus.equalsIgnoreCase("pending") || approveStatus.equalsIgnoreCase("approved") || approveStatus.equalsIgnoreCase("rejected"))) {
                result = tariffMapper.approveTariff(tariffId, approveStatus);
                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(tariffName +" "+ approveStatus + " "+ status.getUpdateFailureDesc());
//                    return ResponseMap.response(status.getUpdateCode(), tariffName +" "+ approveStatus + " "+ status.getUpdateFailureDesc(), "");
                }
                desc = capitalizeFirstLetter(approveStatus) +" Tariff [" + tariffById.getName() + "]";
            } else if (state != null) {
                result = tariffMapper.disableTariff(tariffId, state);
                if (result == 0) {
                    return ResponseMap.response(status.getUpdateCode(), tariffName +" Activated or Deactivated "+ status.getUpdateFailureDesc(), "");
                }
                desc = state ? "Activated" : "Deactivated" + " Tariff [" + tariffById.getName() + "]";
            } else {
                return ResponseMap.response(status.getNotFoundCode(), "Status parameter missing", "");
            }

            Tariff tariff = tariffMapper.getTariffById(tariffById.getId());
            handleAddCache(tariffById);
            isOperatorExist.setPassword("");
            auditNotificationDTO.setCreator(isOperatorExist);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setType("tariff");
            auditNotificationDTO.setCreatedTariff(tariff);
            auditRepository.save(auditNotificationDTO);
            if(state != null) {
                return ResponseMap.response(status.getSuccessCode(), tariff.getName() + " " + (tariff.getStatus() ? "Activated Successfully" : status.getDeleteDesc()), "");
            } else {
                return ResponseMap.response(status.getSuccessCode(), tariff.getName() + " " + (capitalizeFirstLetter(approveStatus) +" Successfully"), "");
            }


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
            String tariffName,
            String tariffIndex,
            String tariffType,
            String tariffRate,
            String bandCode,
            Boolean state,
            String effectiveDate,
            String approveStatus) {

        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
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

            // Build a unique cache key
            StringBuilder cacheKeyBuilder = new StringBuilder("tariffs");
            if (tariffName != null && !tariffName.isEmpty()) cacheKeyBuilder.append("_name_").append(tariffName);
            if (tariffIndex != null && !tariffIndex.isEmpty()) cacheKeyBuilder.append("_index_").append(tariffIndex);
            if (tariffType != null && !tariffType.isEmpty()) cacheKeyBuilder.append("_type_").append(tariffType);
            if (tariffRate != null && !tariffRate.isEmpty()) cacheKeyBuilder.append("_rate_").append(tariffRate);
            if (bandCode != null && !bandCode.isEmpty()) cacheKeyBuilder.append("_band_").append(bandCode);
            if (effectiveDate != null && !effectiveDate.isEmpty()) cacheKeyBuilder.append("_date_").append(effectiveDate);
            if (approveStatus != null && !approveStatus.isEmpty()) cacheKeyBuilder.append("_status_").append(approveStatus);
            if (state != null) cacheKeyBuilder.append("_state_").append(state);

            String cacheKey = cacheKeyBuilder.toString();

            // Return from cache if available
            Object cachedTariff = tariffCache.get(cacheKey);
            if (cachedTariff != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached tariffs " + status.getDesc(), cachedTariff);
            }

            // Ideally, this should be a dynamic query in the mapper layer
            List<Tariff> allTariffs = tariffMapper.GetTariffs();

            List<Tariff> filteredTariffs = allTariffs.stream()
                    .filter(t -> tariffName == null || tariffName.isEmpty() || t.getName().equalsIgnoreCase(tariffName))
                    .filter(t -> tariffIndex == null || tariffIndex.isEmpty() || Objects.equals(t.getTariff_index(), parseLongOrNull(tariffIndex)))
                    .filter(t -> tariffType == null || tariffType.isEmpty() || t.getTariff_type().equalsIgnoreCase(tariffType))
                    .filter(t -> tariffRate == null || tariffRate.isEmpty() || t.getTariff_rate().equalsIgnoreCase(tariffRate))
                    .filter(t -> bandCode == null || bandCode.isEmpty() || t.getBand().equalsIgnoreCase(bandCode))
                    .filter(t -> effectiveDate == null || effectiveDate.isEmpty() || t.getEffective_date().equalsIgnoreCase(effectiveDate))
                    .filter(t -> approveStatus == null || approveStatus.isEmpty() || t.getApprove_status().equalsIgnoreCase(approveStatus))
                    .filter(t -> state == null || t.getStatus().equals(state))
                    .collect(Collectors.toList());

            tariffCache.put(cacheKey, filteredTariffs);
            return ResponseMap.response(status.getSuccessCode(),  "Tariffs "+status.getDesc(), filteredTariffs);

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
    public Map<String, Object> getUniqueTariffId() {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
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


            List<String> tariffName = tariffMapper.getUniqueTariffName();
            List<String> tariffIndex = tariffMapper.getUniqueTariffIndex();
            List<String> tariffType = tariffMapper.getUniqueTariffType();
            List<String> bandCode = tariffMapper.getUniqueBandCode();
            List<String> tariffRate = tariffMapper.getUniqueTariffRate();
            List<Boolean> state = tariffMapper.getUniqueStatus();
            List<String> effectiveDate = tariffMapper.getUniqueEffectiveDate();
            List<String> lastModifiedDate = tariffMapper.getUniqueModifiedDate();

            UniqueTariffId uniqueTariffId = new UniqueTariffId();
            uniqueTariffId.setTariffName(tariffName);
            uniqueTariffId.setTariffIndex(tariffIndex);
            uniqueTariffId.setTariffType(tariffType);
            uniqueTariffId.setBandCode(bandCode);
            uniqueTariffId.setTariffRate(tariffRate);
            uniqueTariffId.setStatus(state);
            uniqueTariffId.setEffectiveDate(effectiveDate);
            uniqueTariffId.setLastModifiedDate(lastModifiedDate);

            Map<String, Object> response = new HashMap<>();
            response.put("responsecode", status.getSuccessCode());
            response.put("responsedesc", "Unique tariff identifiers " + status.getDesc());
            response.put("responsedata", uniqueTariffId);

            return response;
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
    public Map<String, Object> bulkApproveTariff(BulkApprovalRequest request) {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
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

            String s = capitalizeFirstLetter(request.getApproveStatus());
            if (!"Approved".equalsIgnoreCase(s)) {
                throw new GlobalExceptionHandler.NotFoundException(request.getApproveStatus() + " value not accepted, try [approved]");
//                return ResponseMap.response(
//                        this.status.getUpdateCode(),
//                        request.getApproveStatus() + " value not accepted, try [approved]",
//                        ""
//                );
            }


//            if(s.equalsIgnoreCase("approved")) {
                for(Long id : request.getTariffIds()) {
                    tariffMapper.approveTariff(id, s);

                    Tariff tariff = tariffMapper.getTariffById(id);
                    if(tariff == null) {
                        String desc = s+ "Tariff [" + id + "] does not exist ";
                        auditNotificationDTO.setCreator(isOperatorExist);
                        auditNotificationDTO.setDescription(desc);
                        auditNotificationDTO.setType("tariff");
                        auditNotificationDTO.setCreatedTariff(null);
                        auditRepository.save(auditNotificationDTO);
                        continue;
                    }
                    handleAddCache(tariff);
                    String desc = s + " Tariff [" + tariff.getName() + "]";
                    isOperatorExist.setPassword("");
                    auditNotificationDTO.setCreator(isOperatorExist);
                    auditNotificationDTO.setDescription(desc);
                    auditNotificationDTO.setType("tariff");
                    auditNotificationDTO.setCreatedTariff(tariff);
                    auditRepository.save(auditNotificationDTO);
                }
                return ResponseMap.response(status.getSuccessCode(), tariffName + " " + s +" Successfully", "");
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


    public static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }



    private void handleAddCache(Tariff tariff) {
        tariffCache.remove(tariff.getName());
        for (String key : auditCache.keySet()) {
            if (key.startsWith("grid_flex_audit_log_page_")) {
                auditCache.remove(key);
            }
        }
        for (String key : tariffCache.keySet()) {
            if (key.startsWith("tariffs")) {
                tariffCache.remove(key);
            }
        }
        tariffCache.put(tariff.getId().toString(), tariff);  // Cache updated or deleted entity
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