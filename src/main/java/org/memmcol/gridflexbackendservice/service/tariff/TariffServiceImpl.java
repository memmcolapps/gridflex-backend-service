package org.memmcol.gridflexbackendservice.service.tariff;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.BandMapper;
import org.memmcol.gridflexbackendservice.mapper.MeterMapper;
import org.memmcol.gridflexbackendservice.mapper.TariffMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.service.audit.SafeAuditService;
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

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.memmcol.gridflexbackendservice.components.GenericHandler.capitalizeFirstLetter;
import static org.memmcol.gridflexbackendservice.components.GenericHandler.getClientIp;
import static org.memmcol.gridflexbackendservice.components.HandleValidUser.handleUserValidation;

@Service
public class TariffServiceImpl implements TariffService {
    private static final Logger log = LoggerFactory.getLogger(TariffServiceImpl.class);
    @Autowired
    private TariffMapper tariffMapper;

    @Autowired
    private GenericHandler genericHandler;

    @Autowired
    private BandMapper bandMapper;

    @Autowired
    private ResponseProperties status;

//    @Autowired
//    private AuditRepository auditRepository;

    @Autowired
    private SafeAuditService safeAuditService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private MeterMapper meterMapper;

    @Autowired
    private AuthMapper staticOperatorMapper;

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
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            int result;
            String desc = "Newly Added";
            UserModel um = handleUserValidation();

            Tariff isExist = tariffMapper.getTariffByName(tariff.getName(), um.getOrgId());
            if (isExist != null) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(tariffName + " (" +tariff.getName()+ ") " + status.getExistDesc());
            }

            Band isBand = bandMapper.getApprovedBandById(tariff.getBand_id(), um.getOrgId());
            if (isBand == null) {
                throw new GlobalExceptionHandler.NotFoundException(bandName + " is either not found, not approved or deactivated" );
            }
            tariff.setApprove_status("Pending-created");
            tariff.setOrg_id(um.getOrgId());
            tariff.setCreated_by(um.getId());
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

            Tariff newTariff = tariffMapper.getTariff(tariff.getId(), um.getOrgId());
            um.setPassword("");
//            handleAddCache(tariffByName);
            AuditLog auditLog = buildAuditLog(um, desc, tariffName, newTariff, metadata);
            safeAuditService.saveAudit(auditLog);
            return ResponseMap.response(status.getSuccessCode(), tariffName + " " + status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Creating tariff service failed");
            genericHandler.logAndSaveException(exception, "creating tariff");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> approve(UUID tariffVersionId, String approveStatus) throws MissingServletRequestParameterException {
        int result;
        String desc = "";
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
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

                } else if(tariff.getApprove_status().trim().contains("Pending-activated")){

                    tariff.setApprove_status("Deactivated");
                    int u = tariffMapper.updateTariff(tariff.getApprove_status(), tariff.getT_id(), tariff.getUpdated_at());
                    if(u == 0) throw new GlobalExceptionHandler.NotFoundException(bandName + " activation failed");

                } else if(tariff.getApprove_status().trim().equalsIgnoreCase("Pending-edited")){

                    tariff.setApprove_status("Approved");
                    int u = tariffMapper.updateTariff(tariff.getApprove_status(), tariff.getT_id(), tariff.getUpdated_at());
                    if(u == 0) throw new GlobalExceptionHandler.NotFoundException(bandName + " edited failed");

                } else {
                    throw new GlobalExceptionHandler.NotFoundException("Pending state not found");
                }
                desc = capitalizeFirstLetter(tariff.getName()) + approveStatus;
            }
            else {
                assert approveStatus != null;
                throw new MissingServletRequestParameterException("Required request parameter '%s' is not present", approveStatus);
            }

            Tariff newTariff = tariffMapper.getTariff(tariff.getId(), um.getOrgId());
//            handleAddCache(tariff);
            um.setPassword("");
            AuditLog auditLog = buildAuditLog(um, desc, tariffName, newTariff, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(), tariff.getName() + " " + (capitalizeFirstLetter(approveStatus) +" Successfully"), "");

        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Approving tariff service failed");
            genericHandler.logAndSaveException(exception, "approving tariff");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> changeStatus(UUID id, Boolean state) {
        try {
            int result;
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel um = handleUserValidation();

            Tariff tariff = tariffMapper.getTariff(id, um.getOrgId());

            if(tariff == null){
                throw new GlobalExceptionHandler.NotFoundException("Tariff not found");
            }
            if(tariff.getApprove_status().contains("Pending")){
                throw new GlobalExceptionHandler.NotFoundException("Tariff have a pending state that needs to be cleared");
            }
            if(tariff.getApprove_status().contains("Deactivated") && !state){
                throw new GlobalExceptionHandler.NotFoundException("Tariff already deactivated");
            }
            if(tariff.getApprove_status().contains("Approved") && state){
                throw new GlobalExceptionHandler.NotFoundException("Tariff already activated");
            }

            if(state){
                Band isBand = bandMapper.getApprovedBandById(tariff.getBand_id(), um.getOrgId());
                if (isBand == null) {
                    throw new GlobalExceptionHandler.NotFoundException(bandName + " is either not found, not approved or deactivated" );
                }
            }

            if(!state){
                int meter = meterMapper.getTariffMeterById(id, um.getOrgId());
                if (meter > 0) {
                    throw new GlobalExceptionHandler.NotFoundException
                            ("tariff can not be deactivated because is currently in use by " + meter + " meters");
                }
            }
            tariff.setApprove_status("Pending-"+(state ? "activated" : "deactivated"));
            tariff.setOrg_id(um.getOrgId());
            tariff.setCreated_by(um.getId());
            tariff.setT_id(id);

            String changeDescription = buildChangeStatusDescription(tariff, state);
            tariff.setDescription(state ? "Tariff Activated" : "Tariff Deactivated");

//            if(tariff.getApprove_status().contains("Pending")){
//                throw new GlobalExceptionHandler.NotFoundException("Tariff have a pending state that needs to be cleared");
//            } else if(tariff.getApprove_status().contains("Deactivated") && !state){
//                throw new GlobalExceptionHandler.NotFoundException("Tariff already deactivated");
//            } else if(tariff.getApprove_status().contains("Approved") && state){
//                throw new GlobalExceptionHandler.NotFoundException("Tariff already activated");
//            } else {
                result = tariffMapper.createTariffVersion(tariff);
                if(result == 0){
                    throw new GlobalExceptionHandler.NotFoundException(tariffName + " " + status.getUpdateDesc());
                }
//            }
            int u = tariffMapper.updateTariff(tariff.getApprove_status(), tariff.getId(), tariff.getUpdated_at());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException(tariffName + (state ? " activated " : " deactivated ")+ "failed");
            Tariff newTariff = tariffMapper.getTariff(tariff.getT_id(), um.getOrgId());
            um.setPassword("");
//            handleAddCache(newTariff);
            AuditLog auditLog = buildAuditLog(um, changeDescription, tariffName, newTariff, metadata);
            safeAuditService.saveAudit(auditLog);
            return ResponseMap.response(status.getSuccessCode(), tariffName + (state ? " activated ": "deactivated ")+"successfully", "");
        }  catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Changing tariff status service failed");
            genericHandler.logAndSaveException(exception, "changing tariff status");
            throw exception;
        }
    }


    @Override
    public ByteArrayInputStream exportTariff() {

        UserModel user = handleUserValidation();

        int page = 0;
        int size = 0;

        List<Tariff> allTariffs = tariffMapper.GetAllTariffs(user.getOrgId(), page, size);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Tariff Report");

            // Create header
            String[] headers = {
                    "S/N", "Tariff Name", "Tariff Type", "Band Code",
                    "Tariff Rate", "Effective Date", "Approve Status", "Created At", "Updated_at"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // Data rows
            for (int i = 0; i < allTariffs.size(); i++) {
                Tariff tariff = allTariffs.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(tariff.getName());
                row.createCell(2).setCellValue(tariff.getTariff_type());
                row.createCell(3).setCellValue(tariff.getBand() != null ? tariff.getBand().getName() : "");
                row.createCell(4).setCellValue(tariff.getTariff_rate());
                row.createCell(5).setCellValue(tariff.getEffective_date());
                row.createCell(6).setCellValue(tariff.getApprove_status());
                row.createCell(7).setCellValue(tariff.getCreated_at());
                row.createCell(8).setCellValue(tariff.getUpdated_at());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Error exporting tariff data", e);
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
                allTariffs = tariffMapper.GetPendingTariffs(um.getOrgId(), page, size);
            } else {
                allTariffs = tariffMapper.GetAllTariffs(um.getOrgId(), page, size);
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
            if (size <= 0) {
                paginatedTariffs = filteredTariffs; // Return all users
                page = 0;
            } else {
                int fromIndex = Math.min(page * size, totalTariffs);
                int toIndex = Math.min(fromIndex + size, totalTariffs);
                paginatedTariffs = filteredTariffs.subList(fromIndex, toIndex);
            }

            int totalTariff = size <= 0 ? 1 : (int) Math.ceil((double) totalTariffs / size);

            // Prepare response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedTariffs);
            response.put("totalData", totalTariffs);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", totalTariff);

//            tariffCache.put(cacheKey, response);
            return ResponseMap.response(status.getSuccessCode(), "Tariffs " + status.getDesc(), response);

        } catch (Exception exception) {
            log.error("Error occurred while filtering tariffs: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Fetching all tariffs service failed");
            genericHandler.logAndSaveException(exception, "fetching tariffs");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> updateTariff(Tariff tariff) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            int result;
            UserModel um = handleUserValidation();

            Tariff isExist = tariffMapper.getTariff(tariff.getT_id(), um.getOrgId());
            if (isExist == null) {
                throw new GlobalExceptionHandler.NotFoundException(tariffName + " " + status.getNotFoundDesc());
            }

//            if (isExist.getName().equalsIgnoreCase(tariff.getName())) {
//                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(tariffName + " (" +tariff.getName()+ ") " + status.getExistDesc());
//            }

            Band isBand = bandMapper.getApprovedBandById(tariff.getBand_id(), um.getOrgId());
            if (isBand == null) {
                throw new GlobalExceptionHandler.NotFoundException(bandName + " is either not found, not approved or deactivated" );
            }

//            Tariff isVersionExist = tariffMapper.getTariffVersionByName(tariff.getName(), um.getOrgId());

            tariff.setApprove_status("Pending-edited");
            tariff.setOrg_id(um.getOrgId());
            tariff.setCreated_by(um.getId());
            String changeDescription = buildChangeDescription(isExist, tariff);
            tariff.setDescription("Tariff Edited");

            if(isExist.getApprove_status().contains("Pending")){
                throw new GlobalExceptionHandler.NotFoundException("Tariff have a pending state that needs to be cleared");
            } else if(isExist.getApprove_status().contains("Deactivated")){
                throw new GlobalExceptionHandler.NotFoundException("Tariff is deactivated");
            } else {
                int res = tariffMapper.updateTariff("Pending-edited", tariff.getT_id(), tariff.getUpdated_at());
                result = tariffMapper.createTariffVersion(tariff);
                if (result == 0 || res == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(tariffName + " " + status.getUpdateFailureDesc());
                }
            }

            Tariff newTariff = tariffMapper.getTariff(tariff.getT_id(), um.getOrgId());
            um.setPassword("");
//            handleAddCache(newTariff);
            AuditLog auditLog = buildAuditLog(um, changeDescription, tariffName, newTariff, metadata);
            safeAuditService.saveAudit(auditLog);
            return ResponseMap.response(status.getSuccessCode(), tariffName + " " + status.getUpdateDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Editing tariff service failed");
            genericHandler.logAndSaveException(exception, "editing tariff");
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
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Fetching tariff service failed");
            genericHandler.logAndSaveException(exception, "fetching tariff");
            throw exception;
        }
    }


    @Override
    public Map<String, Object> bulkApproveTariff(List<Tariff> tariffs) {
        UserModel user = handleUserValidation();
        Map<String, Object> result = new HashMap<>();
        List<GenericResp> failedRecords = new ArrayList<>();
        int successCount = 0;

        if (tariffs == null || tariffs.isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("No records found");
        }

        final int BATCH_SIZE = 500; // Tune as needed for performance


        for (int i = 0; i < tariffs.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, tariffs.size());
//            List<Tariff> batch = tariffs.subList(i, end);

            List<Tariff> batch = new ArrayList<>(tariffs.subList(i, end));

            // Collect all meter numbers in this subBatch
            List<String> tariffNames = batch.stream()
                    .map(b -> b.getName().trim())
                    .filter(num -> !num.isEmpty())
                    .toList();

            if (tariffNames.isEmpty()) {
                batch.forEach(req -> {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Missing tariff name");
                    resp.setData(req.getName());

                    failedRecords.add(resp);
                });

                continue;
            }

            // One DB call to fetch all corresponding version records
            List<Tariff> versionBatch = tariffMapper.getTariffVersionByNames(tariffNames, user.getOrgId());

            Set<String> foundNames = versionBatch.stream()
                    .map(Tariff::getName)
                    .map(String::trim)
                    .collect(Collectors.toSet());

            List<String> missingNames = tariffNames.stream()
                    .filter(name -> !foundNames.contains(name.trim()))
                    .toList();

            // Record missing/invalid tariffs
//            missingNames.forEach(name ->
//                    failedRecords.add(name + " (Not found or does not pending state)")
//            );
            for (String name : missingNames) {
                GenericResp resp = new GenericResp();
                resp.setId("");
                resp.setMessage("Not found or not in pending state");
                resp.setData(name);
                failedRecords.add(resp);
            }
            if (versionBatch.isEmpty()) {
                continue;
            }

            try {
                prepareUpdateTariffs(versionBatch, user, failedRecords);

                int updatedCount = updateBatchTransactional(versionBatch, user);
                successCount += updatedCount;

            } catch (Exception e) {
                log.warn("Batch {} failed — retrying smaller sub-batches: {}", (i / BATCH_SIZE) + 1, e.getMessage());
                int retrySuccess = updateSubBatchTransactional(versionBatch, user, failedRecords);
                successCount += retrySuccess;
            }
        }

        int total = tariffs.size();

        result.put("totalRecords", total);
        result.put("successCount", successCount);
        result.put("failedCount", failedRecords.size());
        result.put("failedRecords", failedRecords);

        if (!failedRecords.isEmpty()) {
            return ResponseMap.response(
                    "131",
                    failedRecords.size() + " of " + total + " tariffs approval failed",
                    result
            );
        }

        return ResponseMap.response(
                status.getSuccessCode(),
                successCount + " of " + total + " tariffs approved successfully",
                result
        );
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int updateBatchTransactional(List<Tariff> batch, UserModel user) {
        String desc = "";
        if (batch.isEmpty()) return 0;
        try {
            List<Tariff> approvedCreatedTariffs = batch.stream()
                    .filter(m -> "Pending-created".equalsIgnoreCase(m.getApprove_status()))
                    .peek(m -> m.setApprove_status("Approved"))
                    .toList();

            List<Tariff> approvedActivatedTariffs = batch.stream()
                    .filter(m -> "Pending-activated".equalsIgnoreCase(m.getApprove_status()))
                    .peek(m -> m.setApprove_status("Approved"))
                    .toList();

            List<Tariff> approvedDeactivatedTariffs = batch.stream()
                    .filter(m -> "Pending-deactivated".equalsIgnoreCase(m.getApprove_status()))
                    .peek(m -> m.setApprove_status("Deactivated"))
                    .toList();

            List<Tariff> approvedEditedTariffs = batch.stream()
                    .filter(m -> "Pending-edited".equalsIgnoreCase(m.getApprove_status()))
                    .peek(m -> m.setApprove_status("Approved"))
                    .toList();

            // Combine all for main update
            List<Tariff> toUpdate = Stream.of(
                            approvedCreatedTariffs,
                            approvedActivatedTariffs,
                            approvedDeactivatedTariffs,
                            approvedEditedTariffs)
                    .flatMap(Collection::stream)
                    .toList();

            if (!toUpdate.isEmpty()) {
                desc = "Tariff approved";
                tariffMapper.updateBatchTariffs(toUpdate);
                tariffMapper.updateBatchVersionTariffs(toUpdate);
            }

            //  Audit success
            auditApproveBatch(batch, user, desc);

            log.info("Batch updated successfully: {}", batch.size());
            return batch.size();

        } catch (Exception e) {
            log.error("Transaction failed, rolling back batch of size {}: {}", batch.size(), e.getMessage());
            throw new RuntimeException("Batch transaction failed. Rolled back.", e);
        }
    }

    private int updateSubBatchTransactional(List<Tariff> batch, UserModel user, List<GenericResp> failedRecords) {
        int success = 0;
        int subSize = 100;

        for (int i = 0; i < batch.size(); i += subSize) {
            int end = Math.min(i + subSize, batch.size());
//            List<Tariff> subList = batch.subList(i, end);
            List<Tariff> subList = new ArrayList<>(batch.subList(i, end));
            try {
                success += updateBatchTransactional(subList, user);
            } catch (Exception e) {
                log.error("Sub-batch {} failed: {}", (i / subSize) + 1, e.getMessage());
                if (batch.size() > 50) {
                    success += approveSinglesFallbackAsync(batch, user, failedRecords);
                } else {
                    success += approveSinglesFallback(batch, user, failedRecords);
                }
            }
        }
        return success;
    }

    public int approveSinglesFallbackAsync(List<Tariff> batch, UserModel user, List<GenericResp> failedRecords) {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        for (Tariff tariff : batch) {
            futures.add(approveSingleAsync(tariff, user, failedRecords));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream().mapToInt(CompletableFuture::join).sum();
    }

    public int approveSinglesFallback(List<Tariff> tariffs, UserModel user, List<GenericResp> failedRecords) {
        int successCount = 0;

        for (Tariff tariff : tariffs) {
            try {
                log.debug("Fallback single allocation for meter: {}", tariff.getName());
                approveSingleTransactional(tariff, user);
                successCount++;
            } catch (Exception e) {
                String reason = extractErrorMessage(e);
                GenericResp resp = new GenericResp();
                resp.setId("");
                resp.setMessage("Approve failed: "+reason);
                resp.setData(tariff.getName());
                failedRecords.add(resp);
                log.warn("Tariff {} failed individually: {}", tariff.getName(), reason);
            }
        }

        return successCount;
    }

    @Async
    public CompletableFuture<Integer> approveSingleAsync(Tariff tariff, UserModel user, List<GenericResp> failedRecords) {
        try {
            approveSingleTransactional(tariff, user);
            return CompletableFuture.completedFuture(1);
        } catch (Exception e) {
            String reason = extractErrorMessage(e);
            GenericResp resp = new GenericResp();
            resp.setId("");
            resp.setMessage("Approve failed: "+reason);
            resp.setData(tariff.getName());
            failedRecords.add(resp);
//            failedRecords.add(String.format(
//                    "%s (Approve failed: %s)",
//                    tariff.getName(),
////                    meter.getNodeInfo().getRegionId(),
//                    reason
//            ));
            log.warn("Async approve failed for tariff {}: {}",  tariff.getName(), reason);
            return CompletableFuture.completedFuture(0);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void approveSingleTransactional(Tariff tariff, UserModel user) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

        // --- Step 2: Insert into main + version tables ---

        tariffMapper.approvedTariffVersion(tariff);
//        bandMapper.updateBandVer(tariff);

        tariffMapper.approveTariff(tariff);
//        tariffMapper.updateTariff(tariff.getApprove_status(), tariff.getT_id(), tariff.getUpdated_at());



        //fetch meter from the database
        Tariff t = tariffMapper.getTariffByName(tariff.getName(), tariff.getOrg_id());
//        Band m = bandMapper.getBand(tariff.getName());
//            String desc = capitalizeFirstLetter(meter.getMeterNumber() + " allocated " + node.getName());
        //save to audit (mongodb)
        AuditLog auditLog = buildAuditLog(user, "Tariff approved", bandName, t, metadata);
        safeAuditService.saveAudit(auditLog);

    }


    private List<Tariff> getMetersByStatus(List<Tariff> batch, String stage, String newStage) {
        List<Tariff> ms;
        ms = batch.stream()
                .filter(m -> stage.equalsIgnoreCase(m.getApprove_status()))
                .peek(m -> m.setApprove_status(newStage))
                .toList();

        return ms;
    }

    private void prepareUpdateTariffs(List<Tariff> batch, UserModel user, List<GenericResp> failedRecords) {
        Iterator<Tariff> iterator = batch.iterator();
        while (iterator.hasNext()) {
            Tariff tariff = iterator.next();
            if (tariff.getName() == null || tariff.getName().trim().isEmpty()) {
                GenericResp resp = new GenericResp();
                resp.setId("");
                resp.setMessage("Missing tariff name");
                resp.setData(tariff.getName());
                failedRecords.add(resp);

//                failedRecords.add("(Missing tariff name)");
                iterator.remove();
                continue;
            }

            tariff.setOrg_id(user.getOrgId());
            tariff.setApproved_by(user.getId());
            tariff.setId(tariff.getT_id());
        }
    }

    private void auditApproveBatch(List<Tariff> batch, UserModel user, String desc) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
        for (Tariff m : batch) {
            AuditLog auditLog = buildAuditLog(user, desc, tariffName, m, metadata);
            safeAuditService.saveAudit(auditLog);
        }
    }

    private String extractErrorMessage(Exception e) {
        String message = e.getMessage();

        if (message == null) return "Unknown error";

        if (message.contains("duplicate key value")) {
            return "Duplicate record — Tariff already exists.";
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

    private AuditLog buildAuditLog(UserModel creator, String description, String type, Tariff createdEntity, Map<String, String> metadata) {
        AuditLog log = new AuditLog();
        log.setCreator(creator);
        log.setDescription(description);
        log.setType(type);
        log.setCreatedTariff(createdEntity);
        log.setIpAddress(metadata.get("ipAddress"));
        log.setUserAgent(metadata.get("userAgent"));
        log.setEndpoint(metadata.get("endpoint"));
        log.setHttpMethod(metadata.get("httpMethod"));
        return log;
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