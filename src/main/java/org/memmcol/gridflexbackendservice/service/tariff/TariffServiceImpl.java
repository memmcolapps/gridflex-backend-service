package org.memmcol.gridflexbackendservice.service.tariff;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.BandMapper;
import org.memmcol.gridflexbackendservice.mapper.TariffMapper;
import org.memmcol.gridflexbackendservice.model.*;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
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
            String username = (authentication != null) ? authentication.getName() : "Unknown";
            Operator isOperatorExist = operatorMapper.findByAuthEmail(username);
            if (!isOperatorExist.isUstate()) {
                throw new LockedException("User is blocked");
            }
            Tariff isExist = tariffMapper.getTariff(tariff.getName());
            if (isExist != null) {
                return ResponseMap.response(status.getExistCode(), tariffName + " " + status.getExistDesc(), "");
            }
            Band isBand = bandMapper.getBand(tariff.getBand());
            if (isBand == null) {
                return ResponseMap.response(status.getNotFoundCode(), bandName + " " + status.getNotFoundDesc(), "");
            }
            int result = tariffMapper.createTariff(tariff);
            if (result == 0) {
                return ResponseMap.response(status.getRegCode(), tariffName + " " + status.getRegFailureDesc(), "");
            }
            Tariff tariffByName = tariffMapper.getTariff(tariff.getName());
            isOperatorExist.setPasswordEncrypt("");
            handleAddCache(tariffByName);
            auditNotificationDTO.setCreator(isOperatorExist);
            auditNotificationDTO.setDescription("Created tariff "+tariffByName.getName());
            auditNotificationDTO.setType("tariff");
            auditNotificationDTO.setCreatedTariff(tariffByName);
            auditRepository.save(auditNotificationDTO);
            return ResponseMap.response(status.getSuccessCode(), tariffName + " " + status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to create tariff");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> disableTariff(Long tariffId, Boolean state) {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = (authentication != null) ? authentication.getName() : "Unknown";
            Operator isOperatorExist = operatorMapper.findByAuthEmail(username);
            if (!isOperatorExist.isUstate()) {
                throw new LockedException("User is blocked");
            }
            Tariff tariffById = tariffMapper.getTariffById(tariffId);
            if(tariffById == null) {
                return ResponseMap.response(status.getNotFoundCode(), tariffName + " " + status.getNotFoundDesc(), "");
            }
            int result = tariffMapper.disableTariff(tariffId, state);
            if (result == 0) {
                return ResponseMap.response(status.getNotFoundCode(), tariffName + " " + status.getNotFoundDesc(), "");
            }
            Tariff tariff = tariffMapper.getTariffById(tariffById.getId());
            handleAddCache(tariffById);
            isOperatorExist.setPasswordEncrypt("");
            auditNotificationDTO.setCreator(isOperatorExist);
            auditNotificationDTO.setDescription("Disabled tariff "+tariffById.getName());
            auditNotificationDTO.setType("tariff");
            auditNotificationDTO.setCreatedTariff(tariff);

            return ResponseMap.response(status.getSuccessCode(), tariff.getName() + " " + (tariff.getStatus() ? "Enabled Successfully" : status.getDeleteDesc()), "");


        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to create tariff");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString());
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
            validateAuthenticatedOperator();

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
                    .filter(t -> tariffRate == null || tariffRate.isEmpty() || t.getTariff_rate().equalsIgnoreCase(tariffType))
                    .filter(t -> bandCode == null || bandCode.isEmpty() || t.getBand().equalsIgnoreCase(bandCode))
                    .filter(t -> effectiveDate == null || effectiveDate.isEmpty() || t.getEffective_date().equalsIgnoreCase(effectiveDate))
                    .filter(t -> approveStatus == null || approveStatus.isEmpty() || t.getApprove_status().equalsIgnoreCase(approveStatus))
                    .filter(t -> state == null || t.getStatus().equals(state))
                    .collect(Collectors.toList());

            tariffCache.put(cacheKey, filteredTariffs);
            return ResponseMap.response(status.getSuccessCode(),  "Tariffs "+status.getDesc(), filteredTariffs);

        } catch (Exception exception) {
            log.error("Error occurred while filtering tariffs: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to filter tariffs");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString());
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


    @Override
    public Map<String, Object> getUniqueTariffId() {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            validateAuthenticatedOperator();

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
            exceptionErrorLogs.setError(exception.toString());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }


    private void validateAuthenticatedOperator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (authentication != null) ? authentication.getName() : "Unknown";
        Operator isOperatorExist = operatorMapper.findByAuthEmail(username);
        if (isOperatorExist == null || !isOperatorExist.isUstate()) {
            throw new LockedException("User is blocked");
        }
    }

    private void handleAddCache(Tariff tariff) {
        tariffCache.remove(tariff.getName());
        for (String key : auditCache.keySet()) {
            if (key.startsWith("grid_flex_audit_log_page_")) {
                auditCache.remove(key);
            }
        }
        for (String key : tariffCache.keySet()) {
            if (key.startsWith("tariffs_")) {
                tariffCache.remove(key);
            }
        }
        tariffCache.put(tariff.getId().toString(), tariff);  // Cache updated or deleted entity
    }
}
