package org.memmcol.gridflexbackendservice.service.band;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.BandMapper;
import org.memmcol.gridflexbackendservice.mapper.TariffMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.IncidentReport;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.meter.MeterRequest;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.service.audit.SafeAuditService;
import org.memmcol.gridflexbackendservice.util.GenericResp;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
//import org.memmcol.gridflexbackendservice.util.HandleCatchError;
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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.memmcol.gridflexbackendservice.components.GenericHandler.capitalizeFirstLetter;
//import static org.memmcol.gridflexbackendservice.components.GenericHandler.getClientIp;
import static org.memmcol.gridflexbackendservice.components.HandleValidUser.handleUserValidation;

@Service
public class BandServiceImpl implements BandService {
    private static final Logger log = LoggerFactory.getLogger(BandServiceImpl.class);

    @Autowired
    private BandMapper bandMapper;

    @Autowired
    private AuthMapper operatorMapper;

    @Autowired
    private ResponseProperties status;

//    @Autowired
//    private AuditRepository auditRepository;

    @Autowired
    private SafeAuditService safeAuditService;

    @Autowired
    private GenericHandler genericHandler;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;

    private final IMap<String, Object> bandCache;

    private final IMap<String, Object> tariffCache;

    private final IMap<String, Object> auditCache;

    private String bandName = "Band";
    @Autowired
    private TariffMapper tariffMapper;

    public BandServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.bandCache = hazelcastInstance.getMap("bandCache");
        this.auditCache = hazelcastInstance.getMap("auditCache");
        this.tariffCache = hazelcastInstance.getMap("tariffCache");
    }

    @Transactional
    @Override
    public Map<String, Object> createBand(Band band) {
        try {

            handleRequestCheck(band);

            int result;
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            String desc = "Newly Added";
            UserModel um = handleUserValidation();
            UUID orgId = um.getOrgId();

            Band isExist = bandMapper.getBand(band.getName(), orgId);
            if (isExist != null) {
                throw new GlobalExceptionHandler.NotFoundException(bandName + ": (" + band.getName() + ") " + status.getExistDesc());
            }
//            Band isVersionExist = bandMapper.getVersionBand(band.getName(), um.getOrgId());
//            if(isVersionExist != null) {
//                throw new GlobalExceptionHandler.NotFoundException(isVersionExist.getName()+ " have a pending task to attend to");
//            }

            band.setOrgId(um.getOrgId());
            band.setApproveStatus("Pending-created");
            band.setCreatedBy(um.getId());
            band.setDescription(desc);
            result = bandMapper.createBand(band);
            if(result == 0){
                throw new GlobalExceptionHandler.NotFoundException(bandName + " " + status.getRegFailureDesc());
            }
            band.setBandId(band.getId());

            result = bandMapper.createBandVersion(band);
            if(result == 0){
                throw new GlobalExceptionHandler.NotFoundException(bandName + " " + status.getRegFailureDesc());
            }

            Band bandByName = bandMapper.getBandById(band.getBandId(), um.getOrgId());
            um.setPassword("");

            AuditLog auditLog = buildAuditLog(um, desc, bandName, bandByName, metadata);
            safeAuditService.saveAudit(auditLog);
//            handleAddCache(bandByName);
            return ResponseMap.response(status.getSuccessCode(), bandName + " " + status.getRegDesc(), "");
        } catch (Exception exception) {
            genericHandler.logIncidentReport("Creating band service failed");
            genericHandler.logAndSaveException(exception, "creating band");
            throw exception;
        }

    }

    private void handleRequestCheck(Band request) {

        if(request.getName() == null || request.getName().isEmpty()){
            throw new GlobalExceptionHandler.NotFoundException("Name is required");
        }
        if(request.getHour() == null || request.getHour().isEmpty()){
            throw new GlobalExceptionHandler.NotFoundException("Hour is required");
        }
    }

    @Transactional
    @Override
    public Map<String, Object> updateBand(Band band) {
        try {
            handleRequestCheck(band);
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            int result;
            UserModel um = handleUserValidation();

            Band isExist = bandMapper.getBandById(band.getBandId(), um.getOrgId());
            if (isExist == null) {
                throw new GlobalExceptionHandler.NotFoundException(bandName + " " + status.getNotFoundDesc());
            }

//            if (isExist.getName() != null && isExist.getName().equalsIgnoreCase(band.getName())) {
//                throw new GlobalExceptionHandler.NotFoundException(bandName + " (" + band.getName() + ") " + status.getExistDesc());
//            }

            band.setApproveStatus("Pending-edited");
            band.setOrgId(um.getOrgId());
            band.setCreatedBy(um.getId());
            String changeDescription = buildChangeDescription(isExist, band);
            band.setDescription("Band Edited");

//            Band isVersionExist = bandMapper.getBandVersionById(band.getBandId(), um.getOrgId());

            if(isExist.getApproveStatus().contains("Pending")){
                throw new GlobalExceptionHandler.NotFoundException("Band have a pending state that needs to be cleared");
            } else if(isExist.getApproveStatus().contains("Deactivated")){
                throw new GlobalExceptionHandler.NotFoundException("Band is deactivated");
            }
            else {
                int res = bandMapper.updateBand("Pending-edited", band.getBandId(), band.getUpdatedAt());
                result = bandMapper.createBandVersion(band);
                if(result == 0 || res == 0){
                    throw new GlobalExceptionHandler.NotFoundException(bandName + " " + status.getUpdateDesc());
                }
            }
            Band bandById = bandMapper.getBandById(band.getBandId(), um.getOrgId());


//            handleAddCache(bandById);

            um.setPassword("");
            AuditLog auditLog = buildAuditLog(um, changeDescription, bandName, bandById, metadata);
            safeAuditService.saveAudit(auditLog);
////			authCache.remove("dashboard");
            return ResponseMap.response(status.getSuccessCode(), bandName + " " + status.getUpdateDesc(), "");
        } catch (Exception exception) {
            genericHandler.logIncidentReport("Editing band service failed");
            genericHandler.logAndSaveException(exception, "edited band");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> approve(UUID bandId, String approveStatus) throws MissingServletRequestParameterException {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
        int result;
        String desc = "";
        try {
            //check if organization user have access
            UserModel um = handleUserValidation();

            // verify band in band version table
            Band band = bandMapper.getBandVersionById(bandId, um.getOrgId());

            if(band == null) {
                throw new GlobalExceptionHandler.NotFoundException("Band either have no pending state or not found");
            }
            band.setApproveBy(um.getId());
            if(approveStatus != null && approveStatus.equalsIgnoreCase("approve")) {

                if (band.getApproveStatus().equals("Pending-deactivated")) {
                    band.setApproveStatus("Deactivated");
                } else {
                    band.setApproveStatus("Approved");
                }

                //update band in band version table
                result = bandMapper.updateBandVersion(band);
                if (result == 0) throw new GlobalExceptionHandler.NotFoundException(bandName +" Approved "+ status.getUpdateFailureDesc());


                //update band in band table
                result = bandMapper.approveBand(band);
                    if (result == 0) throw new GlobalExceptionHandler.NotFoundException(bandName +" Approved "+ status.getUpdateFailureDesc());

                desc = capitalizeFirstLetter(band.getName()) + " " + band.getApproveStatus();

            }
            else if (approveStatus != null && approveStatus.trim().equalsIgnoreCase("reject")) {

                // Reject band
                int s = bandMapper.rejectedBandVersion("Rejected", band.getBandId(), band.getUpdatedAt(), um.getId());
                if(s == 0) throw new GlobalExceptionHandler.NotFoundException(bandName + " rejection failed");

                if(band.getApproveStatus().trim().equalsIgnoreCase("Pending-created")){
                    // Delete band if just created
                    int d = bandMapper.deleteBand(band.getBandId());
                    if(d == 0) throw new GlobalExceptionHandler.NotFoundException(bandName + " failed to delete");

                } else if (band.getApproveStatus().trim().contains("Pending-deactivated")) {
                    band.setApproveStatus("Approved");
                    // Fallback to Approve if deactivate rejected
                    int u = bandMapper.updateBand(band.getApproveStatus(), band.getBandId(), band.getUpdatedAt());
                    if(u == 0) throw new GlobalExceptionHandler.NotFoundException(bandName + " deactivation failed");
                } else if (band.getApproveStatus().trim().contains("Pending-activated")) {
                    band.setApproveStatus("Deactivated");
                    // Fallback to Approve if deactivate rejected
                    int u = bandMapper.updateBand(band.getApproveStatus(), band.getBandId(), band.getUpdatedAt());
                    if(u == 0) throw new GlobalExceptionHandler.NotFoundException(bandName + " activation failed");
                }
                else if (band.getApproveStatus().trim().contains("Pending-edited")) {
                    band.setApproveStatus("Approved");
                    // Fallback to Approve if deactivate rejected
                    int u = bandMapper.updateBand(band.getApproveStatus(), band.getBandId(), band.getUpdatedAt());
                    if(u == 0) throw new GlobalExceptionHandler.NotFoundException(bandName + " edited failed");
                }
                else {
                    throw new GlobalExceptionHandler.NotFoundException("Pending state not found");
                }

                desc = capitalizeFirstLetter(band.getName()) + " " + band.getApproveStatus();
            } else {
                assert approveStatus != null;
                throw new MissingServletRequestParameterException("Required request parameter '%s' is not present", approveStatus);
            }

            Band newBand = bandMapper.getBandById(band.getId(), um.getOrgId());
//            handleAddCache(band);
            um.setPassword("");
            AuditLog auditLog = buildAuditLog(um, desc, bandName, newBand, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(), band.getName() + " " +
                    (capitalizeFirstLetter(approveStatus) + " Successfully"), "");

        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("approving band service failed");
            genericHandler.logAndSaveException(exception, "approve band");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getBands(String type) {
        try {
            UserModel um = handleUserValidation();

//            String cacheKey = "bands_"+um.getOrgId()+type;
//            Object cachedBand = bandCache.get(cacheKey);

//            if (cachedBand != null) {
//                return ResponseMap.response(status.getSuccessCode(), "Cached " + bandName + "s " + status.getDesc(), cachedBand);
//            }
            List<Band> result;
            if(type.equalsIgnoreCase("pending-state")) {
                result = bandMapper.fetchBandsVersion(um.getOrgId());
            } else {
                result = bandMapper.fetchBands(um.getOrgId());
            }

            if(result == null) {
                throw new GlobalExceptionHandler.NotFoundException(bandName + " " + status.getNotFoundDesc());
            }
//            bandCache.put(cacheKey, result);
            return ResponseMap.response(status.getSuccessCode(), bandName + " " + status.getDesc(), result);
        } catch (Exception exception) {
            genericHandler.logIncidentReport("fetching all bands service failed");
           genericHandler.logAndSaveException(exception, "fetch bands");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getBand(UUID bandId, UUID bandVersionId) {
        try {
            UserModel um = handleUserValidation();

//            Object cachedBand = null;
//
//            if(bandId != null){
//                cachedBand = bandCache.get(bandId.toString());
//            }
//            if(bandVersionId != null){
//                cachedBand = bandCache.get(bandVersionId.toString());
//            }
//
//            if (cachedBand != null) {
//                return ResponseMap.response(status.getSuccessCode(), "Cached " + bandName + " " + status.getDesc(), cachedBand);
//            }
            Band result = null;
            if(bandId != null){
                result = bandMapper.getBandById(bandId, um.getOrgId());
            }

            if(bandVersionId != null){
                result = bandMapper.getBandVersionById(bandVersionId, um.getOrgId());
            }

            if(result == null) {
                throw new GlobalExceptionHandler.NotFoundException(bandName + " " + status.getNotFoundDesc());
            }

//            handleAddCache(result);

            return ResponseMap.response(status.getSuccessCode(), bandName + " " + status.getDesc(), result);
        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("fetching band service failed");
            genericHandler.logAndSaveException(exception, "fetch band");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> changeStatus(UUID bandId, Boolean state) {
        try {
            int result;
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel um = handleUserValidation();
            Band band = bandMapper.getBandById(bandId, um.getOrgId());
            if(band == null){
                throw new GlobalExceptionHandler.NotFoundException("Band "+status.getNotFoundDesc());
            }

            if(band.getApproveStatus().contains("Pending")){
                throw new GlobalExceptionHandler.NotFoundException("Band have a pending state that needs to be cleared");
            }
            if(band.getApproveStatus().contains("Deactivated") && !state){
                throw new GlobalExceptionHandler.NotFoundException("Band already deactivated");
            }
            if(band.getApproveStatus().contains("Approved") && state){
                throw new GlobalExceptionHandler.NotFoundException("Band already activated");
            }

            if(!state){
                List<String> errors = new ArrayList<>();
                int tariff = tariffMapper.getTariffBandById(bandId, um.getOrgId());
                if(tariff > 0)  errors.add(tariff + " tariffs");

                int percentageRange = bandMapper.getPercentageBandById(bandId);
                if(percentageRange > 0) errors.add(percentageRange + " percentage range set");

                if (!errors.isEmpty()) {
                    throw new GlobalExceptionHandler.NotFoundException
                            ("Band can not be deactivated because is currently in use by "+errors);
                }
            }

//            Band isVersionExist = bandMapper.getBandVersionById(bandId, um.getOrgId());
            band.setApproveStatus("Pending-"+(state ? "activated" : "deactivated"));
            band.setOrgId(um.getOrgId());
            band.setCreatedBy(um.getId());
            band.setBandId(bandId);
            String changeDescription = buildChangeStatusDescription(band, state);
            band.setDescription(state ? "Band Activated" : "Band Deactivated");

//            if(band.getApproveStatus().contains("Pending")){
//                throw new GlobalExceptionHandler.NotFoundException("Band have a pending state that needs to be cleared");
//            } else if(band.getApproveStatus().contains("Deactivated") && !state){
//                throw new GlobalExceptionHandler.NotFoundException("Band already deactivated");
//            } else if(band.getApproveStatus().contains("Approved") && state){
//                throw new GlobalExceptionHandler.NotFoundException("Band already activated");
//            } else {
                result = bandMapper.createBandVersion(band);
                if(result == 0) throw new GlobalExceptionHandler.NotFoundException(bandName + " " + status.getUpdateDesc());
//            }
            int u = bandMapper.updateBand(band.getApproveStatus(), band.getId(), band.getUpdatedAt());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException(bandName + " "+ status.getUpdateFailureDesc());
            Band bandById = bandMapper.getBandById(band.getBandId(), um.getOrgId());
//            handleAddCache(bandById);
            um.setPassword("");
//			authCache.remove("dashboard");
            AuditLog auditLog = buildAuditLog(um, changeDescription, bandName, bandById, metadata);
            safeAuditService.saveAudit(auditLog);
            return ResponseMap.response(status.getSuccessCode(), bandName+(state ? " Activated " : " Deactivated ")+ "Successfully", "");
        }  catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("changing band service status failed");
            genericHandler.logAndSaveException(exception, "change band status");
            throw exception;
        }

    }

    @Override
    public Map<String, Object> clearCache() {
        tariffCache.clear(); // Clear the cache
        bandCache.clear();
        auditCache.clear();
        return ResponseMap.response(status.getSuccessCode(), "Cache cleared successfully", "");
    }

    @Override
    public Map<String, Object> bulkApprove(List<Band> bands) {
        UserModel user = handleUserValidation();
        Map<String, Object> result = new HashMap<>();
        List<GenericResp> failedRecords = new ArrayList<>();
        int successCount = 0;

        if (bands == null || bands.isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("No records found");
        }

        final int BATCH_SIZE = 500; // Tune as needed for performance


        for (int i = 0; i < bands.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, bands.size());
            List<Band> batch = bands.subList(i, end);

            // Collect all meter numbers in this subBatch
            List<String> bandNames = batch.stream()
                    .map(b -> b.getName().trim())
                    .filter(num -> !num.isEmpty())
                    .toList();

//            if (bandNames.isEmpty()) {
//                batch.forEach(req -> failedRecords.add(
//                        String.format("%s (Invalid or missing data)",
//                                req.getName())
//                ));
//                continue;
//            }

            if (bandNames.isEmpty()) {
                batch.forEach(req -> {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Missing band");
                    resp.setData(req.getName());

                    failedRecords.add(resp);
                });

                continue;
            }

            // One DB call to fetch all corresponding version records
            List<Band> versionBatch = bandMapper.getBandsByVersion(bandNames, user.getOrgId());

//            if (versionBatch.isEmpty()) {
//                failedRecords.addAll(bandNames.stream()
//                        .map(num -> num + " (Not found in version table)")
//                        .toList());
//                continue;
//            }
            Set<String> foundNames = versionBatch.stream()
                    .map(Band::getName)
                    .map(String::trim)
                    .collect(Collectors.toSet());

            List<String> missingNames = bandNames.stream()
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
                prepareUpdateBands(versionBatch, user, failedRecords);

                int updatedCount = updateBatchTransactional(versionBatch, user);
                successCount += updatedCount;

            } catch (Exception e) {
                log.warn("Batch {} failed — retrying smaller sub-batches: {}", (i / BATCH_SIZE) + 1, e.getMessage());
                int retrySuccess = updateSubBatchTransactional(versionBatch, user, failedRecords);
                successCount += retrySuccess;
            }
        }

        int total = bands.size();

        result.put("totalRecords", total);
        result.put("successCount", successCount);
        result.put("failedCount", failedRecords.size());
        result.put("failedRecords", failedRecords);

        // If any failed → throw browser error
        if (!failedRecords.isEmpty()) {
            throw new GlobalExceptionHandler.PartialFailureException(
                    failedRecords.size() + " of " + total + " bands approval failed",
                    result
            );
        }

        return ResponseMap.response(
                status.getSuccessCode(),
                successCount + " of " + total + " bands approved successfully",
                result
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int updateBatchTransactional(List<Band> batch, UserModel user) {
        String desc = "";
        if (batch.isEmpty()) return 0;
        try {

            List<Band> approvedCreatedBands = batch.stream()
                    .filter(m -> "Pending-created".equalsIgnoreCase(m.getApproveStatus()))
                    .peek(m -> m.setApproveStatus("Approved"))
//                    .peek(m -> m.setApproveStatus("Created"))
                    .toList();

            List<Band> approvedActivatedBands = batch.stream()
                    .filter(m -> "Pending-activated".equalsIgnoreCase(m.getApproveStatus()))
                    .peek(m -> m.setApproveStatus("Approved"))
                    .toList();

            List<Band> approvedDeactivatedBands = batch.stream()
                    .filter(m -> "Pending-deactivated".equalsIgnoreCase(m.getApproveStatus()))
                    .peek(m -> m.setApproveStatus("Deactivated"))
                    .toList();

            List<Band> approvedEditedBands = batch.stream()
                    .filter(m -> "Pending-edited".equalsIgnoreCase(m.getApproveStatus()))
                    .peek(m -> m.setApproveStatus("Approved"))
                    .toList();

            // Combine all for main update
            List<Band> toUpdate = Stream.of(
                            approvedCreatedBands,
                            approvedActivatedBands,
                            approvedDeactivatedBands,
                            approvedEditedBands)
                    .flatMap(Collection::stream)
                    .toList();

            if (!toUpdate.isEmpty()) {
                desc = "Band approved";
                bandMapper.updateBatchBands(toUpdate);
                bandMapper.updateBatchVersionBands(toUpdate);
            }

            //  Audit success
            auditApproveBatch(batch, user, desc);

            log.info("Batch updated successfully: {}", batch.size());
            return batch.size();

        } catch (Exception e) {
//            log.error("Transaction failed, rolling back batch of size {}: {}", batch.size(), e.getMessage());
            throw new RuntimeException("Batch transaction failed. Rolled back.", e);
        }
    }

    private int updateSubBatchTransactional(List<Band> batch, UserModel user, List<GenericResp> failedRecords) {
        int success = 0;
        int subSize = 100;

        for (int i = 0; i < batch.size(); i += subSize) {
            int end = Math.min(i + subSize, batch.size());
            List<Band> subList = batch.subList(i, end);
            try {
                success += updateBatchTransactional(subList, user);
            } catch (Exception e) {
                log.error("Sub-batch {} failed: {}", (i / subSize) + 1, e.getMessage());
//                subList.forEach(m -> failedRecords.add(m.getName() + " - " + e.getMessage()));
                if (batch.size() > 50) {
                    success += approveSinglesFallbackAsync(batch, user, failedRecords);
                } else {
                    success += approveSinglesFallback(batch, user, failedRecords);
                }
            }
        }
        return success;
    }

    public int approveSinglesFallbackAsync(List<Band> batch, UserModel user, List<GenericResp> failedRecords) {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        for (Band band : batch) {
            futures.add(approveSingleAsync(band, user, failedRecords));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream().mapToInt(CompletableFuture::join).sum();
    }

    public int approveSinglesFallback(List<Band> bands, UserModel user, List<GenericResp> failedRecords) {
        int successCount = 0;

        for (Band band : bands) {
            try {
                log.debug("Fallback single allocation for meter: {}", band.getName());
                approveSingleTransactional(band, user);
                successCount++;
            } catch (Exception e) {
                String reason = extractErrorMessage(e);
                GenericResp resp = new GenericResp();
                resp.setId("");
                resp.setMessage("Approve failed: "+reason);
                resp.setData(band.getName());
                failedRecords.add(resp);
//                failedRecords.add(String.format(
//                        "%s (Approve failed: %s)",
//                        band.getName(),
////                        meter.getNodeInfo().getRegionId(),
//                        reason
//                ));
                log.warn("Approve {} failed individually: {}", band.getName(), reason);
            }
        }

        return successCount;
    }

    @Async
    public CompletableFuture<Integer> approveSingleAsync(Band band, UserModel user, List<GenericResp> failedRecords) {
        try {
            approveSingleTransactional(band, user);
            return CompletableFuture.completedFuture(1);
        } catch (Exception e) {
            String reason = extractErrorMessage(e);
            GenericResp resp = new GenericResp();
            resp.setId(band.getBandId().toString());
            resp.setMessage("Band Approve failed: "+reason);
            resp.setData(band.getName());

            failedRecords.add(resp);
//            failedRecords.add(String.format(
//                    "%s (Approve failed: %s)",
//                    band.getName(),
////                    meter.getNodeInfo().getRegionId(),
//                    reason
//            ));
            log.warn("Async allocation failed for meter {}: {}",  band.getName(), reason);
            return CompletableFuture.completedFuture(0);
        }
    }


    private String extractErrorMessage(Exception e) {
        String message = e.getMessage();

        if (message == null) return "Unknown error";

        if (message.contains("duplicate key value")) {
            return "Duplicate record — Band already exists.";
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


    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void approveSingleTransactional(Band band, UserModel user) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
        UUID orgId = user.getOrgId();
        // --- Step 2: Insert into main + version tables ---
        bandMapper.updateBandVer(band);

        bandMapper.approveBand(band);
//                updateBand(band.getApproveStatus(), band.getBandId(), band.getUpdatedAt());


        //fetch meter from the database
        Band m = bandMapper.getBand(band.getName(), orgId);
//            String desc = capitalizeFirstLetter(meter.getMeterNumber() + " allocated " + node.getName());
        //save to audit (mongodb)
        AuditLog auditLog = buildAuditLog(user, "Band approved", bandName, m, metadata);
        safeAuditService.saveAudit(auditLog);

    }

    /**
     * Record audit logs for each approved meter.
     */
    private void auditApproveBatch(List<Band> batch, UserModel user, String desc) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
        for (Band m : batch) {
            AuditLog auditLog = buildAuditLog(user, desc, bandName, m, metadata);
            safeAuditService.saveAudit(auditLog);
        }
    }

    private List<Band> getMetersByStatus(List<Band> batch, String stage, String newStage) {
        List<Band> ms;
        ms = batch.stream()
                .filter(m -> stage.equalsIgnoreCase(m.getApproveStatus()))
                .peek(m -> m.setApproveStatus(newStage))
                .toList();

        return ms;
    }

    private void prepareUpdateBands(List<Band> batch, UserModel user, List<GenericResp> failedRecords) {
        Iterator<Band> iterator = batch.iterator();
        while (iterator.hasNext()) {
            Band band = iterator.next();
            if (band.getName() == null || band.getName().trim().isEmpty()) {
                GenericResp resp = new GenericResp();
                resp.setId(band.getBandId().toString());
                resp.setMessage("Missing band name");
                resp.setData(band.getName());

                failedRecords.add(resp);

                iterator.remove();
                continue;
            }

            band.setOrgId(user.getOrgId());
            band.setApproveBy(user.getId());
            band.setId(band.getBandId());
        }
    }

    private AuditLog buildAuditLog(UserModel creator, String description, String type, Object createdEntity, Map<String, String> metadata) {
        AuditLog log = new AuditLog();
        log.setCreator(creator);
        log.setDescription(description);
        log.setType(type);
        log.setCreatedBand(createdEntity instanceof Band ? (Band) createdEntity : null);
        log.setIpAddress(metadata.get("ipAddress"));
        log.setUserAgent(metadata.get("userAgent"));
        log.setEndpoint(metadata.get("endpoint"));
        log.setHttpMethod(metadata.get("httpMethod"));
        return log;
    }

    private void handleAddCache(Band band) {
        bandCache.remove(band.getId().toString()+"_"+band.getOrgId());
        tariffCache.clear();
        for (String key : auditCache.keySet()) {
            if (key.startsWith("grid_flex_audit_log_page_")) {
                auditCache.remove(key);
            }
        }
        for (String key : bandCache.keySet()) {
            if (key.startsWith("bands_"+band.getOrgId())) {
                bandCache.remove(key);
            }
        }
        bandCache.put(band.getId().toString()+"_"+band.getOrgId(), band);  // Cache updated or deleted entity
    }

    private String buildChangeStatusDescription(Band oldBand, Boolean status) {
        StringBuilder changes = new StringBuilder("Edited band ");
        String oldState = oldBand.getApproveStatus().trim().equalsIgnoreCase("Approved") ? "activated" : "deactivated";
        String newState = status ? "activated" : "deactivated";
        if (!Objects.equals(oldBand.getApproveStatus(), newState)) {
            changes.append(String.format("status: '%s' → '%s' ", oldState, newState));
        }

        return changes.toString();
    }

    private String buildChangeDescription(Band oldBand, Band newBand) {
        StringBuilder changes = new StringBuilder("Edited band ");

        if (!Objects.equals(oldBand.getName(), newBand.getName())) {
            changes.append(String.format("name: '%s' → '%s' ", oldBand.getName(), newBand.getName()));
        }

        if (!Objects.equals(oldBand.getHour(), newBand.getHour())) {
            changes.append(String.format("hour: '%s' → '%s' ", oldBand.getHour(), newBand.getHour()));
        }

        return changes.toString();
    }
}
