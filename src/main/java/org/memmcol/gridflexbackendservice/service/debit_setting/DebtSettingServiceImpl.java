package org.memmcol.gridflexbackendservice.service.debit_setting;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.memmcol.gridflexbackendservice.mapper.BandMapper;
import org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper;
import org.memmcol.gridflexbackendservice.mapper.DebtSettingMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditAdjust;
import org.memmcol.gridflexbackendservice.model.debt_setting.LiabilityCause;
import org.memmcol.gridflexbackendservice.model.debt_setting.PercentageRange;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.meter.MDMeterInfo;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.meter.SmartMeterInfo;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.service.audit.SafeAuditService;
import org.memmcol.gridflexbackendservice.service.tariff.TariffServiceImpl;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.util.GenericResp;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.HandlePermission;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.memmcol.gridflexbackendservice.components.GenericHandler.capitalizeFirstLetter;
import static org.memmcol.gridflexbackendservice.components.HandleValidUser.handleUserValidation;

@Service
public class DebtSettingServiceImpl implements DebtSettingService {
    private static final Logger log = LoggerFactory.getLogger(TariffServiceImpl.class);

    @Autowired
    private DebtSettingMapper debtMapper;

    @Autowired
    private ResponseProperties status;

//    @Autowired
//    private AuditRepository auditRepository;

    @Autowired
    private SafeAuditService safeAuditService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private DebitCreditAdjustmentMapper debitCreditAdjustmentMapper;


    @Autowired
    private GenericHandler genericHandler;

    private final IMap<String, Object> debtCache;

    private final IMap<String, Object> auditCache;

    private String lc = "Liability Cause";

    private String pr = "Percentage Range";
    @Autowired
    private BandMapper bandMapper;

    public DebtSettingServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.debtCache = hazelcastInstance.getMap("debtCache");
        this.auditCache = hazelcastInstance.getMap("auditCache");
    }

    @Transactional
    @Override
    public Map<String, Object> createLiabilityCause(LiabilityCause request) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            int result;
            String desc = "Newly Added";
            UserModel um = handleUserValidation();
            UUID nodeId = um.getNodeInfo().getNodeId();
            String nodeType = um.getNodeInfo().getType();

            HandlePermission.perm(nodeType);

            LiabilityCause isExist = debtMapper.getLiabilityCauseByName(request.getName(), request.getCode(), um.getOrgId());

            if(isExist != null){
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(lc + " ("+request.getName()+") " + status.getExistDesc());
            }
            request.setApproveStatus("Pending-created");
            request.setOrgId(um.getOrgId());
            request.setCreatedBy(um.getId());
            request.setDescription(desc);
            result = debtMapper.createLiabilityCause(request);
            if(result == 0){
                throw new GlobalExceptionHandler.NotFoundException(lc + " " + status.getNotFoundDesc());
            }

            request.setLiabilityCauseId(request.getId());

            result = debtMapper.createLiabilityCauseVersion(request);
            if(result == 0){
                throw new GlobalExceptionHandler.NotFoundException(lc + " " + status.getNotFoundDesc());
            }
            LiabilityCause liabilityCause = debtMapper.getLiabilityCauseById(request.getId(), um.getOrgId());
            um.setPassword("");
//            handleAddCache(liabilityCause);
            AuditLog auditLog = buildAuditLog(um, desc, lc, liabilityCause, metadata);
            safeAuditService.saveAudit(auditLog);
            return ResponseMap.response(status.getSuccessCode(), lc + " " + status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("creating liability cause service failed");
            genericHandler.logAndSaveException(exception, "creating liability cause");
            throw exception;
        }

    }

    @Transactional
    @Override
    public Map<String, Object> updateLiabilityCause(LiabilityCause request) {
        try {
            int result;
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel um = handleUserValidation();
            UUID nodeId = um.getNodeInfo().getNodeId();
            String nodeType = um.getNodeInfo().getType();
            LiabilityCause isExist = debtMapper.getLiabilityCauseById(request.getLiabilityCauseId(), um.getOrgId());

            if (isExist == null) {
                throw new GlobalExceptionHandler.NotFoundException(lc + " " + status.getNotFoundDesc());
            }
            HandlePermission.perm(nodeType);

//            if (isExist.getName().equalsIgnoreCase(request.getName())) {
//                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(lc + " (" + request.getName() + ") "+status.getExistDesc());
//            }

            LiabilityCause isVersionExist = debtMapper.getLiabilityCauseVersionById(request.getLiabilityCauseId(), um.getOrgId());

            request.setApproveStatus("Pending-edited");
            request.setOrgId(um.getOrgId());
            request.setCreatedBy(um.getId());
            String changeDescription = buildChangeDescription(isExist, request);
            request.setDescription("Liability Cause Edited");

            if(isVersionExist != null ){
                throw new GlobalExceptionHandler.NotFoundException(isVersionExist.getName()+ " have a pending status needs to be cleared");
            } else {
                int res = debtMapper.updateLiabilityCause("Pending-edited", request.getLiabilityCauseId(), request.getUpdatedAt());
                result = debtMapper.createLiabilityCauseVersion(request);
                if (result == 0 || res == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(lc + " " + status.getUpdateFailureDesc());
                }
            }

            LiabilityCause liabilityCause = debtMapper.getLiabilityCauseById(request.getLiabilityCauseId(), um.getOrgId());
            um.setPassword("");
//            handleAddCache(liabilityCause);
            AuditLog auditLog = buildAuditLog(um, changeDescription, lc, liabilityCause, metadata);
            safeAuditService.saveAudit(auditLog);
            return ResponseMap.response(status.getSuccessCode(), lc + " " + status.getUpdateDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("fetching liability cause service failed");
            genericHandler.logAndSaveException(exception, "editing liability cause");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getLiabilityCauses(String type) {
        try {
            UserModel um = handleUserValidation();

            String cacheKey = "lc_"+ um.getOrgId()+"_"+type;
            Object cachedBand = debtCache.get(cacheKey);

            if (cachedBand != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached " + lc + "s " + status.getDesc(), cachedBand);
            }
            List<LiabilityCause> result;
            if(type.equalsIgnoreCase("pending-state")) {
                result = debtMapper.getLiabilityCauseVersion(um.getOrgId());
            } else {
                result = debtMapper.getLiabilityCause(um.getOrgId());
            }

            if(result == null) {
                throw new GlobalExceptionHandler.NotFoundException(lc + " " + status.getNotFoundDesc());
            }
//            debtCache.put(cacheKey, result);
            return ResponseMap.response(status.getSuccessCode(), lc + " " + status.getDesc(), result);
        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("fetching liability cause services failed");
            genericHandler.logAndSaveException(exception, "fetching liability causes");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getLiabilityCause(UUID id, UUID lcVersionId) {
        try {
            UserModel um = handleUserValidation();
            Object cachedLc = null;
            if(id != null){
                cachedLc = debtCache.get(id.toString());
            }
            if(lcVersionId != null){
                cachedLc = debtCache.get(lcVersionId.toString());
            }

            if (cachedLc != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached " + lc + " " + status.getDesc(), cachedLc);
            }
            LiabilityCause result = null;

            if(id != null){
                result = debtMapper.getLiabilityCauseById(id, um.getOrgId());
            }

            if(lcVersionId != null){
                result = debtMapper.getSingleLcVersionById(lcVersionId, um.getOrgId());
            }

            if(result == null) {
                throw new GlobalExceptionHandler.NotFoundException(lc + " " + status.getNotFoundDesc());
            }

//            handleAddCache(result);

            return ResponseMap.response(status.getSuccessCode(), lc + " " + status.getDesc(), result);
        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("fetching liability cause service failed");
            genericHandler.logAndSaveException(exception, "fetching liability cause");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> approveLiabilityCause(UUID liabilityCauseId, String approveStatus) throws MissingServletRequestParameterException {
        int result;
        String desc = "";
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel um = handleUserValidation();
            UUID nodeId = um.getNodeInfo().getNodeId();
            String nodeType = um.getNodeInfo().getType();

            HandlePermission.perm(nodeType);

            LiabilityCause liabilityCause = debtMapper.getLiabilityCauseVersionById(liabilityCauseId, um.getOrgId());
            if(liabilityCause == null) {
                throw new GlobalExceptionHandler.NotFoundException(lc+" either have no pending state or not found");
            }


            liabilityCause.setOrgId(um.getOrgId());
            liabilityCause.setApproveBy(um.getId());

            if(approveStatus != null && approveStatus.contains("approve")) {

                if (liabilityCause.getApproveStatus().equals("Pending-deactivated")) {
                    liabilityCause.setApproveStatus("Deactivated");
                } else {
                    liabilityCause.setApproveStatus("Approved");
                }
                result = debtMapper.approveLiabilityCauseVersion(liabilityCause);
                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(lc +" "+ approveStatus + "d "+ status.getUpdateFailureDesc());
                }
                result = debtMapper.approveLiability(liabilityCause);
                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(lc +" "+ approveStatus + "d "+ status.getUpdateFailureDesc());
                }

                desc = capitalizeFirstLetter(liabilityCause.getName()) + " " + liabilityCause.getApproveStatus();
            }
            else if (approveStatus != null && approveStatus.contains("reject")){

                result = debtMapper.rejectedLiabilityVersion("Rejected", liabilityCause.getLiabilityCauseId(), liabilityCause.getUpdatedAt(), um.getId());
                if (result == 0) throw new GlobalExceptionHandler.NotFoundException(lc +" "+ approveStatus + "ed "+ status.getUpdateFailureDesc());

                if(liabilityCause.getApproveStatus().trim().equalsIgnoreCase("Pending-created")){

                    int d = debtMapper.deleteLiabilityCause(liabilityCause.getLiabilityCauseId());
                    if(d == 0) throw new GlobalExceptionHandler.NotFoundException(lc + " failed to delete");

                } else if(liabilityCause.getApproveStatus().trim().contains("Pending-deactivated")){

                    liabilityCause.setApproveStatus("Approved");
                    int u = debtMapper.updateLiabilityCause(liabilityCause.getApproveStatus(), liabilityCause.getLiabilityCauseId(), liabilityCause.getUpdatedAt());
                    if(u == 0) throw new GlobalExceptionHandler.NotFoundException(lc + " deactivation failed");

                } else if(liabilityCause.getApproveStatus().trim().contains("Pending-activated")){

                    liabilityCause.setApproveStatus("Deactivated");
                    int u = debtMapper.updateLiabilityCause(liabilityCause.getApproveStatus(), liabilityCause.getLiabilityCauseId(), liabilityCause.getUpdatedAt());
                    if(u == 0) throw new GlobalExceptionHandler.NotFoundException(lc + " activated failed");
                } else if(liabilityCause.getApproveStatus().trim().contains("Pending-edited")){

                    liabilityCause.setApproveStatus("Approved");
                    int u = debtMapper.updateLiabilityCause(liabilityCause.getApproveStatus(), liabilityCause.getLiabilityCauseId(), liabilityCause.getUpdatedAt());
                    if(u == 0) throw new GlobalExceptionHandler.NotFoundException(lc + " edited failed");
                } else {
                    throw new GlobalExceptionHandler.NotFoundException("Pending state not found");
                }
                desc = capitalizeFirstLetter(liabilityCause.getName()) + " " + liabilityCause.getApproveStatus();
            } else {
                assert approveStatus != null;
                throw new MissingServletRequestParameterException("Required request parameter '%s' is not present", approveStatus);
            }

            LiabilityCause newLc = debtMapper.getLiabilityCauseById(liabilityCause.getId(), um.getOrgId());

//            handleAddCache(liabilityCause);
            um.setPassword("");
            AuditLog auditLog = buildAuditLog(um, desc, lc, newLc, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(), liabilityCause.getName() + " " + (capitalizeFirstLetter(approveStatus) +" Successfully"), "");

        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);

            genericHandler.logIncidentReport("approve liability cause service failed");
            genericHandler.logAndSaveException(exception, "approving liability cause");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> createPercentage(PercentageRange request) {
        try {
            int result;
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            String desc = "Newly Added";
            UserModel um = handleUserValidation();
            UUID nodeId = um.getNodeInfo().getNodeId();
            String nodeType = um.getNodeInfo().getType();

            HandlePermission.perm(nodeType);

            PercentageRange isExist = debtMapper.getPercentageByCode(request.getCode(), um.getOrgId());
            if (isExist != null) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException("Percentage range code ("+request.getCode()+") " + status.getExistDesc());
            }

            int startRange = Integer.parseInt(request.getAmountStartRange());
            int endRange = Integer.parseInt(request.getAmountEndRange());


            PercentageRange isRangeExist = debtMapper.getPercentageByRange(startRange,endRange,
                    um.getOrgId(), request.getBandId());
            if (isRangeExist != null) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(
                        "Percentage range amount (" + request.getAmountStartRange() + " - " +
                                request.getAmountEndRange() + ") overlaps with existing range (" +
                                isRangeExist.getAmountStartRange() + " - " + isRangeExist.getAmountEndRange() + ") " +
                                status.getExistDesc());
            }

            PercentageRange isVersionExist = debtMapper.getPercentageVersionByCode(request.getCode(), um.getOrgId());
            if(isVersionExist != null) {
                throw new GlobalExceptionHandler.NotFoundException(isVersionExist.getCode()+ " have a pending status that needs to be cleared");
            }

            Band band = debtMapper.getBand(request.getBandId(), um.getOrgId());
            if (band == null) {
                throw new GlobalExceptionHandler.NotFoundException("Band is either not found, not approved or deactivated" );
            }

            request.setApproveStatus("Pending-created");
            request.setCreatedBy(um.getId());
            request.setDescription(desc);
            request.setBandId(request.getBandId());
            request.setOrgId(um.getOrgId());

            result = debtMapper.createPercentageRange(request);
            if(result == 0){
                throw new GlobalExceptionHandler.NotFoundException(pr + " " + status.getNotFoundDesc());
            }

            request.setPercentageId(request.getId());

            result = debtMapper.createPercentageVersion(request);
            if(result == 0){
                throw new GlobalExceptionHandler.NotFoundException(pr + " " + status.getNotFoundDesc());
            }
            PercentageRange percentageRange = debtMapper.getPercentageById(request.getId(), um.getOrgId());

            um.setPassword("");
//            handleAddPercentageCache(percentageRange);
            AuditLog auditLog = buildAuditLog(um, desc, pr, percentageRange, metadata);
            safeAuditService.saveAudit(auditLog);
            return ResponseMap.response(status.getSuccessCode(), pr + " " + status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("creating percentage range service failed");
            genericHandler.logAndSaveException(exception, "creating percentage range");
            throw exception;
        }

    }

    @Transactional
    @Override
    public Map<String, Object> updatePercentage(PercentageRange request) {
        try {
            int result;
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel um = handleUserValidation();
            UUID nodeId = um.getNodeInfo().getNodeId();
            String nodeType = um.getNodeInfo().getType();

            HandlePermission.perm(nodeType);
            PercentageRange isExist = debtMapper.getPercentageById(request.getPercentageId(), um.getOrgId());

            if (isExist == null) {
                throw new GlobalExceptionHandler.NotFoundException(pr + " " + status.getNotFoundDesc());
            }

//            if (isExist.getCode().equalsIgnoreCase(request.getCode())){
//                throw new GlobalExceptionHandler.NotFoundException(pr +"code ("+request.getCode()+") " + status.getExistDesc());
//            }

            Band band = debtMapper.getBand(request.getBandId(), um.getOrgId());
            if (band == null) {
                throw new GlobalExceptionHandler.NotFoundException("Band is either not found, not approved or deactivated" );
            }

            int startRange = Integer.parseInt(request.getAmountStartRange());
            int endRange = Integer.parseInt(request.getAmountEndRange());

            PercentageRange isRangeExist = debtMapper.getPercentageByRange(
                    startRange, endRange, um.getOrgId(),
                    request.getBandId());
            if (isRangeExist != null && !isRangeExist.getId().equals(request.getPercentageId())) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(
                        "Percentage range amount (" + request.getAmountStartRange() + " - " +
                                request.getAmountEndRange() + ") overlaps with existing range (" +
                                isRangeExist.getAmountStartRange() + " - " + isRangeExist.getAmountEndRange() + ") " +
                                status.getExistDesc());
            }

            PercentageRange isVersionExist = debtMapper.getPercentageVersionByName(request.getPercentage(), um.getOrgId());

            request.setApproveStatus("Pending-edited");
            request.setOrgId(um.getOrgId());
            request.setCreatedBy(um.getId());
            String changeDescription = buildPercentageChangeDescription(isExist, request);
            request.setDescription("Percentage Range Edited");

            if(isVersionExist != null){
                throw new GlobalExceptionHandler.NotFoundException(isVersionExist.getCode()+ "percentage code have a pending status needs to be cleared");
            } else {
                int res = debtMapper.updatePercentage("Pending-edited", request.getPercentageId(), request.getUpdatedAt());
                result = debtMapper.createPercentageVersion(request);
                if (result == 0 || res == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(pr + " " + status.getUpdateFailureDesc());
                }
            }

            PercentageRange percentageRange = debtMapper.getPercentageById(request.getPercentageId(), um.getOrgId());
            um.setPassword("");
//            handleAddPercentageCache(percentageRange);

            AuditLog auditLog = buildAuditLog(um, changeDescription, pr, percentageRange, metadata);
            safeAuditService.saveAudit(auditLog);
            return ResponseMap.response(status.getSuccessCode(), pr + " " + status.getUpdateDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("editing percentage range service failed");
            genericHandler.logAndSaveException(exception, "editing percentage range");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getAllPercentages(String type) {
        try {
            UserModel um = handleUserValidation();

            String cacheKey = "pr_"+um.getOrgId()+type;
            Object cachedBand = debtCache.get(cacheKey);

            if (cachedBand != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached " + pr + "s " + status.getDesc(), cachedBand);
            }
            List<PercentageRange> result;
            if(type.equalsIgnoreCase("pending-state")) {
                result = debtMapper.getPercentageVersion(um.getOrgId());
            } else {
                result = debtMapper.getPercentage(um.getOrgId());
            }

            if(result == null) {
                throw new GlobalExceptionHandler.NotFoundException(pr + " " + status.getNotFoundDesc());
            }
//            debtCache.put(cacheKey, result);
            return ResponseMap.response(status.getSuccessCode(), pr + " " + status.getDesc(), result);
        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("fetching all percentage range service failed");
            genericHandler.logAndSaveException(exception, "fetching percentage ranges");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getPercentage(UUID id, UUID percentageVersionId) {
        try {
            UserModel um = handleUserValidation();
            Object cachedLc = null;
            if(id != null){
                cachedLc = debtCache.get(id.toString());
            }
            if(percentageVersionId != null){
                cachedLc = debtCache.get(percentageVersionId.toString());
            }

            if (cachedLc != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached " + pr + " " + status.getDesc(), cachedLc);
            }
            PercentageRange result = null;

            if(id != null){
                result = debtMapper.getPercentageById(id, um.getOrgId());
            }

            if(percentageVersionId != null){
                result = debtMapper.getSinglePercentageVersionById(percentageVersionId, um.getOrgId());
            }

            if(result == null) {
                throw new GlobalExceptionHandler.NotFoundException(pr + " " + status.getNotFoundDesc());
            }

//            handleAddPercentageCache(result);

            return ResponseMap.response(status.getSuccessCode(), pr + " " + status.getDesc(), result);
        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("fetching percentage range service failed");
            genericHandler.logAndSaveException(exception, "fetching percentage range");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> approvePercentage(UUID percentageId, String approveStatus) throws MissingServletRequestParameterException {
        int result;
        String desc = "";
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel um = handleUserValidation();
            UUID nodeId = um.getNodeInfo().getNodeId();
            String nodeType = um.getNodeInfo().getType();

            HandlePermission.perm(nodeType);

            PercentageRange percentage = debtMapper.getPercentageVersionById(percentageId, um.getOrgId());
            if(percentage == null) {
                throw new GlobalExceptionHandler.NotFoundException(pr+" either have no pending state or not found");
            }
            percentage.setOrgId(um.getOrgId());
            percentage.setApproveBy(um.getId());
            percentage.setOrgId(um.getOrgId());
            if(approveStatus != null && approveStatus.contains("approve")) {
                if (percentage.getApproveStatus().equals("Pending-deactivated")) {
                    percentage.setApproveStatus("Deactivated");
                } else {
                    percentage.setApproveStatus("Approved");
                }
                result = debtMapper.approvePercentageVersion(percentage);
                if (result == 0) throw new GlobalExceptionHandler.NotFoundException(pr +" "+ approveStatus + "d "+ status.getUpdateFailureDesc());

                result = debtMapper.approvePercentage(percentage);
                if (result == 0) throw new GlobalExceptionHandler.NotFoundException(pr +" "+ approveStatus + "d "+ status.getUpdateFailureDesc());

                desc = capitalizeFirstLetter(percentage.getPercentage()) + " " + percentage.getApproveStatus();
            }
            else if (approveStatus != null && approveStatus.contains("reject")){

                result = debtMapper.rejectedPercentageVersion("Rejected", percentage.getPercentageId(), percentage.getUpdatedAt(), um.getId());

                if (result == 0) throw new GlobalExceptionHandler.NotFoundException(pr +" "+ approveStatus + "ed "+ status.getUpdateFailureDesc());
                if(percentage.getApproveStatus().trim().equalsIgnoreCase("Pending-created")){

                    int d = debtMapper.deletePercentage(percentage.getPercentageId());
                    if(d == 0) throw new GlobalExceptionHandler.NotFoundException(pr + " failed to delete");

                } else if(percentage.getApproveStatus().trim().contains("Pending-deactivated")){

                    percentage.setApproveStatus("Approved");
                    int u = debtMapper.updatePercentage(percentage.getApproveStatus(), percentage.getPercentageId(), percentage.getUpdatedAt());
                    if(u == 0) throw new GlobalExceptionHandler.NotFoundException(pr + " deactivation failed");

                } else if(percentage.getApproveStatus().trim().contains("Pending-activated")){

                    percentage.setApproveStatus("Deactivated");
                    int u = debtMapper.updatePercentage(percentage.getApproveStatus(), percentage.getPercentageId(), percentage.getUpdatedAt());
                    if(u == 0) throw new GlobalExceptionHandler.NotFoundException(pr + " activation failed");

                } else if(percentage.getApproveStatus().trim().contains("Pending-edited")){
                    percentage.setApproveStatus("Approved");
                    int u = debtMapper.updatePercentage(percentage.getApproveStatus(), percentage.getPercentageId(), percentage.getUpdatedAt());
                    if(u == 0) throw new GlobalExceptionHandler.NotFoundException(pr + " update failed");
                }
                else {
                    throw new GlobalExceptionHandler.NotFoundException("Pending state not found");
                }
                desc = capitalizeFirstLetter(percentage.getPercentage()) + " " + percentage.getApproveStatus();
            } else {
                assert approveStatus != null;
                throw new MissingServletRequestParameterException("Required request parameter '%s' is not present", approveStatus);
            }

            PercentageRange newPercentageRange = debtMapper.getPercentageById(percentage.getId(), um.getOrgId());

//            handleAddPercentageCache(percentage);
            um.setPassword("");
            AuditLog auditLog = buildAuditLog(um, desc, pr, newPercentageRange, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(), capitalizeFirstLetter(approveStatus) +" successfully", "");

        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("approve percentage range service failed");
            genericHandler.logAndSaveException(exception, "fetching percentage range");
            throw exception;
        }
    }

    @Override
    public Map<String, Object> liabilityCauseChangeState(UUID id, Boolean state) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            int result;
            UserModel um = handleUserValidation();
            UUID nodeId = um.getNodeInfo().getNodeId();
            String nodeType = um.getNodeInfo().getType();

            HandlePermission.perm(nodeType);

            LiabilityCause liabilityCause = debtMapper.getLiabilityCauseById(id, um.getOrgId());
            if(liabilityCause == null){
                throw new GlobalExceptionHandler.NotFoundException("Liability cause "+status.getNotFoundDesc());
            }

            if(liabilityCause.getApproveStatus().contains("Pending")){
                throw new GlobalExceptionHandler.NotFoundException("Liability cause have a pending state that needs to be cleared");
            }
            if(liabilityCause.getApproveStatus().contains("Deactivated") && !state){
                throw new GlobalExceptionHandler.NotFoundException("Liability cause already deactivated");
            }
            if(liabilityCause.getApproveStatus().contains("Approved") && state){
                throw new GlobalExceptionHandler.NotFoundException("Liability cause already active");
            }
            if(state){
                DebitCreditAdjust resp = debitCreditAdjustmentMapper.getDebitAdjustmentByStatus(liabilityCause.getId(), um.getOrgId());
                if (resp != null) {
                    throw new GlobalExceptionHandler.NotFoundException(lc+" can not be deactivated because is currently in use by debit or credit adjustment" );
                }
            }
//            LiabilityCause isVersionExist = debtMapper.getLiabilityCauseVersionById(id, um.getOrgId());
            liabilityCause.setApproveStatus("Pending-"+(state ? "activated" : "deactivated"));
            liabilityCause.setOrgId(um.getOrgId());
            liabilityCause.setCreatedBy(um.getId());
            liabilityCause.setLiabilityCauseId(id);
            String changeDescription = buildLcChangeStatusDescription(liabilityCause, state);
            liabilityCause.setDescription(state ? "Liability Cause Activated" : "Liability Cause Deactivated");

//            if(isVersionExist != null){
//                throw new GlobalExceptionHandler.NotFoundException(isVersionExist.getName()+ " have a pending status needs to be cleared");
//            } else {
                result = debtMapper.createLiabilityCauseVersion(liabilityCause);
                if(result == 0){
                    throw new GlobalExceptionHandler.NotFoundException(lc + " " + status.getUpdateDesc());
                }
//            }
            int u = debtMapper.updateLiabilityCause(liabilityCause.getApproveStatus(), liabilityCause.getId(), liabilityCause.getUpdatedAt());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException(lc + (state ? " activate " : " deactivate ")+ "failed");
            LiabilityCause lca = debtMapper.getLiabilityCauseById(id, um.getOrgId());
//            handleAddCache(lca);
            um.setPassword("");
            AuditLog auditLog = buildAuditLog(um, changeDescription, pr, lca, metadata);
            safeAuditService.saveAudit(auditLog);
            return ResponseMap.response(status.getSuccessCode(), lc + (state ? " activated ": " deactivated ")+"successfully", "");
        }  catch (Exception exception) {
            genericHandler.logIncidentReport("changing liability cause status service failed");
            genericHandler.logAndSaveException(exception, "changing liability cause state");
            throw exception;
        }
    }

    @Override
    public Map<String, Object> parcentageChangeState(UUID id, Boolean state) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            int result;
            UserModel um = handleUserValidation();
            UUID nodeId = um.getNodeInfo().getNodeId();
            String nodeType = um.getNodeInfo().getType();

            HandlePermission.perm(nodeType);
            PercentageRange percentage = debtMapper.getPercentageById(id, um.getOrgId());
            if(percentage == null){
                throw new GlobalExceptionHandler.NotFoundException(pr+" "+status.getNotFoundDesc());
            }

            if(percentage.getApproveStatus().contains("Pending")){
                throw new GlobalExceptionHandler.NotFoundException("Percentage range have a pending state that needs to be cleared");
            }
            if(percentage.getApproveStatus().contains("Deactivated") && !state){
                throw new GlobalExceptionHandler.NotFoundException("percentage range already deactivated");
            }
            if(percentage.getApproveStatus().contains("Approved") && state){
                throw new GlobalExceptionHandler.NotFoundException("percentage range already activated");
            }

            if(state){
                Band isBand = debtMapper.getBand(percentage.getBandId(), um.getOrgId());
                if (isBand == null) {
                    throw new GlobalExceptionHandler.NotFoundException("Band is either not found, not approved or deactivated" );
                }
            }


//            PercentageRange isVersionExist = debtMapper.getPercentageVersionById(id, um.getOrgId());
            percentage.setApproveStatus("Pending-"+(state ? "activated" : "deactivated"));
            percentage.setOrgId(um.getOrgId());
            percentage.setCreatedBy(um.getId());
            percentage.setPercentageId(id);
            String changeDescription = buildPrChangeStatusDescription(percentage, state);
            percentage.setDescription(state ? "Percentage Range Activated" : "Percentage Range Deactivated");

//            if(isVersionExist != null){
//                throw new GlobalExceptionHandler.NotFoundException(isVersionExist.getCode()+ "code have a pending status needs to be cleared");
//            } else {
                result = debtMapper.createPercentageVersion(percentage);
                if(result == 0){
                    throw new GlobalExceptionHandler.NotFoundException(lc + " " + status.getUpdateDesc());
                }
//            }
            int u = debtMapper.updatePercentage(percentage.getApproveStatus(), percentage.getId(), percentage.getUpdatedAt());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException(lc + (state ? " activated " : " deactivated ")+ "failed");

            PercentageRange percentageRange = debtMapper.getPercentageById(id, um.getOrgId());
//            handleAddPercentageCache(percentageRange);
            um.setPassword("");
//            handleAddPercentageCache(percentageRange);
            AuditLog auditLog = buildAuditLog(um, changeDescription, pr, percentageRange, metadata);
            safeAuditService.saveAudit(auditLog);
            return ResponseMap.response(status.getSuccessCode(), pr +(state ? " activated ": "deactivated ")+"successfully", "");
        }  catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Changing percentage range status service failed");
            genericHandler.logAndSaveException(exception, "changing state percentage range");
            throw exception;
        }
    }

    @Override
    public Map<String, Object> bulkApproveLiabilityCause(List<LiabilityCause> lcs) {
        UserModel user = handleUserValidation();
        Map<String, Object> result = new HashMap<>();
        List<GenericResp> failedRecords = new ArrayList<>();

        UUID nodeId = user.getNodeInfo().getNodeId();
        String nodeType = user.getNodeInfo().getType();

        HandlePermission.perm(nodeType);

        int successCount = 0;

        if (lcs == null || lcs.isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("No records found");
        }

        final int BATCH_SIZE = 500; // Tune as needed for performance


        for (int i = 0; i < lcs.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, lcs.size());
            List<LiabilityCause> batch = new ArrayList<>(lcs.subList(i, end));
            // Collect all meter numbers in this subBatch
            List<String> lcNames = batch.stream()
                    .map(b -> b.getName().trim())
                    .filter(num -> !num.isEmpty())
                    .toList();

            if (lcNames.isEmpty()) {
                batch.forEach(req -> {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Missing liability cause name");
                    resp.setData(req.getName());

                    failedRecords.add(resp);
                });

                continue;
            }
            List<String> lcCodes = batch.stream()
                    .map(b -> b.getCode().trim())
                    .filter(num -> !num.isEmpty())
                    .toList();

            if (lcCodes.isEmpty()) {
                batch.forEach(req -> {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Missing liability cause code");
                    resp.setData(req.getName());

                    failedRecords.add(resp);
                });

                continue;
            }

            // One DB call to fetch all corresponding version records
            List<LiabilityCause> versionBatch = debtMapper.getLiabilityCauseBulkVersion(lcNames, user.getOrgId());

            Set<String> foundNames = versionBatch.stream()
                    .map(LiabilityCause::getName)
                    .map(String::trim)
                    .collect(Collectors.toSet());

            List<String> missingNames = lcNames.stream()
                    .filter(name -> !foundNames.contains(name.trim()))
                    .toList();

//            // Record missing/invalid tariffs
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
                prepareUpdateLc(versionBatch, user, failedRecords);

                int updatedCount = updateBatchTransactional(versionBatch, user);
                successCount += updatedCount;

            } catch (Exception e) {
                log.warn("Batch {} failed — retrying smaller sub-batches: {}", (i / BATCH_SIZE) + 1, e.getMessage());
                int retrySuccess = updateSubBatchTransactional(versionBatch, user, failedRecords);
                successCount += retrySuccess;
            }
        }
        int total = lcs.size();

        result.put("totalRecords", total);
        result.put("successCount", successCount);
        result.put("failedCount", failedRecords.size());
        result.put("failedRecords", failedRecords);

        // If any failed → throw browser error
        if (!failedRecords.isEmpty()) {
            return ResponseMap.response(
                    "131",
                    failedRecords.size() + " of " + total + " Liability cause approval failed",
                    result
            );
        }

        return ResponseMap.response(
                status.getSuccessCode(),
                successCount + " of " + total + " Liability cause approved successfully",
                result
        );
    }

    private void prepareUpdateLc(List<LiabilityCause> batch, UserModel user, List<GenericResp> failedRecords) {
        Iterator<LiabilityCause> iterator = batch.iterator();
        while (iterator.hasNext()) {
            LiabilityCause lc = iterator.next();
            if (lc.getName() == null || lc.getName().trim().isEmpty()) {
                GenericResp resp = new GenericResp();
                resp.setId("");
                resp.setMessage("Missing liability cause name");
                resp.setData(lc.getName());
                failedRecords.add(resp);
//                failedRecords.add("(Missing tariff name)");
                iterator.remove();
                continue;
            }

            lc.setOrgId(user.getOrgId());
            lc.setApproveBy(user.getId());
            lc.setId(lc.getLiabilityCauseId());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int updateBatchTransactional(List<LiabilityCause> batch, UserModel user) {
        String desc = "";
        if (batch.isEmpty()) return 0;
        try {

            List<LiabilityCause> approvedCreatedLc = batch.stream()
                    .filter(m -> "Pending-created".equalsIgnoreCase(m.getApproveStatus()))
                    .peek(m -> m.setApproveStatus("Approved"))
                    .toList();

            List<LiabilityCause> approvedActivatedLc = batch.stream()
                    .filter(m -> "Pending-activated".equalsIgnoreCase(m.getApproveStatus()))
                    .peek(m -> m.setApproveStatus("Approved"))
                    .toList();

            List<LiabilityCause> approvedDeactivatedLc = batch.stream()
                    .filter(m -> "Pending-deactivated".equalsIgnoreCase(m.getApproveStatus()))
                    .peek(m -> m.setApproveStatus("Deactivated"))
                    .toList();

            List<LiabilityCause> approvedEditedLc = batch.stream()
                    .filter(m -> "Pending-edited".equalsIgnoreCase(m.getApproveStatus()))
                    .peek(m -> m.setApproveStatus("Approved"))
                    .toList();

            // Combine all for main update
            List<LiabilityCause> toUpdate = Stream.of(
                            approvedCreatedLc,
                            approvedActivatedLc,
                            approvedDeactivatedLc,
                            approvedEditedLc)
                    .flatMap(Collection::stream)
                    .toList();

            if (!toUpdate.isEmpty()) {
                desc = "Liability cause approved";
                debtMapper.updateBatchLcs(toUpdate);
                debtMapper.updateBatchVersionLcs(toUpdate);
            }

            //  Audit success
            auditApproveBatch(batch, user, desc);

            log.info("Batch updated successfully: {}", batch.size());
            return batch.size();

        } catch (Exception e) {
            log.error("Approval failed, rolling back batch of size {}: {}", batch.size(), e.getMessage());
            throw new RuntimeException("Batch transaction failed. Rolled back.", e);
        }
    }

    private int updateSubBatchTransactional(List<LiabilityCause> batch, UserModel user, List<GenericResp> failedRecords) {
        int success = 0;
        int subSize = 100;

        for (int i = 0; i < batch.size(); i += subSize) {
            int end = Math.min(i + subSize, batch.size());
//            List<LiabilityCause> subList = batch.subList(i, end);
            List<LiabilityCause> subList = new ArrayList<>(batch.subList(i, end));
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

    public int approveSinglesFallbackAsync(List<LiabilityCause> batch, UserModel user, List<GenericResp> failedRecords) {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        for (LiabilityCause lc : batch) {
            futures.add(approveSingleAsync(lc, user, failedRecords));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream().mapToInt(CompletableFuture::join).sum();
    }

    public int approveSinglesFallback(List<LiabilityCause> lcs, UserModel user, List<GenericResp> failedRecords) {
        int successCount = 0;

        for (LiabilityCause lc : lcs) {
            try {
                log.debug("Fallback single allocation for meter: {}", lc.getName());
                approveSingleTransactional(lc, user);
                successCount++;
            } catch (Exception e) {
                String reason = extractErrorMessage(e);
                GenericResp resp = new GenericResp();
                resp.setId(lc.getLiabilityCauseId().toString());
                resp.setMessage("Approve failed: "+reason);
                resp.setData(lc.getName());
                failedRecords.add(resp);

//                failedRecords.add(String.format(
//                        "%s (Approve failed: %s)",
//                        lc.getName(),
//                        reason
//                ));
                log.warn("Liability cause {} failed individually: {}", lc.getName(), reason);
            }
        }

        return successCount;
    }

    @Async
    public CompletableFuture<Integer> approveSingleAsync(LiabilityCause lc, UserModel user, List<GenericResp> failedRecords) {
        try {
            approveSingleTransactional(lc, user);
            return CompletableFuture.completedFuture(1);
        } catch (Exception e) {
            String reason = extractErrorMessage(e);
            GenericResp resp = new GenericResp();
            resp.setId(lc.getLiabilityCauseId().toString());
            resp.setMessage("Approve failed: "+reason);
            resp.setData(lc.getName());
            failedRecords.add(resp);
//            failedRecords.add(String.format(
//                    "%s (Approve failed: %s)",
//                    lc.getName(),
//                    reason
//            ));
            log.warn("Async approve failed for tariff {}: {}",  lc.getName(), reason);
            return CompletableFuture.completedFuture(0);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void approveSingleTransactional(LiabilityCause lca, UserModel user) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

        debtMapper.approveLiability(lca);

        debtMapper.approveLiabilityCauseVersion(lca);

        //fetch meter from the database
        LiabilityCause lbt = debtMapper.getLcById(lca.getLiabilityCauseId());

        //save to audit (mongodb)
        AuditLog auditLog = buildAuditLog(user, "Liability cause approved", lc, lbt, metadata);
        safeAuditService.saveAudit(auditLog);

    }


    private List<LiabilityCause> getMetersByStatus(List<LiabilityCause> batch, String stage, String newStage) {
        List<LiabilityCause> ms;
        ms = batch.stream()
                .filter(m -> stage.equalsIgnoreCase(m.getApproveStatus()))
                .peek(m -> m.setApproveStatus(newStage))
                .toList();

        return ms;
    }


    private void auditApproveBatch(List<LiabilityCause> batch, UserModel user, String desc) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
        for (LiabilityCause m : batch) {
            AuditLog auditLog = buildAuditLog(user, desc, lc, m, metadata);
            safeAuditService.saveAudit(auditLog);
        }
    }


    private String extractErrorMessage(Exception e) {
        String message = e.getMessage();

        if (message == null) return "Unknown error";

        if (message.contains("duplicate key value")) {
            return "Duplicate record — Record already exists.";
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

    @Override
    public Map<String, Object> bulkApprovePercentageRange(List<PercentageRange> prs) {
        UserModel user = handleUserValidation();
        Map<String, Object> result = new HashMap<>();
        List<GenericResp> failedRecords = new ArrayList<>();
        int successCount = 0;

        if (prs == null || prs.isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("No records found");
        }

        final int BATCH_SIZE = 500; // Tune as needed for performance


        for (int i = 0; i < prs.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, prs.size());
//            List<PercentageRange> batch = prs.subList(i, end);
            List<PercentageRange> batch = new ArrayList<>(prs.subList(i, end));
            // Collect all meter numbers in this subBatch
            List<String> prCodes = batch.stream()
                    .map(b -> b.getCode().trim())
                    .filter(num -> !num.isEmpty())
                    .toList();

            if (prCodes.isEmpty()) {
                batch.forEach(req -> {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Missing percentage code");
                    resp.setData(req.getCode());

                    failedRecords.add(resp);
                });

                continue;
            }

//            if (prCodes.isEmpty()) {
//                batch.forEach(req -> failedRecords.add(
//                        String.format("%s (Invalid or missing data)",
//                                req.getCode())
//                ));
//                continue;
//            }



            // One DB call to fetch all corresponding version records
            List<PercentageRange> versionBatch = debtMapper.getPercentageBulkVersion(prCodes, user.getOrgId());

            Set<String> foundNames = versionBatch.stream()
                    .map(PercentageRange::getCode)
                    .map(String::trim)
                    .collect(Collectors.toSet());

            List<String> missingNames = prCodes.stream()
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
                prepareUpdatePr(versionBatch, user, failedRecords);

                int updatedCount = updatePrBatchTransactional(versionBatch, user);
                successCount += updatedCount;

            } catch (Exception e) {
                log.warn("Batch {} failed — retrying smaller sub-batches: {}", (i / BATCH_SIZE) + 1, e.getMessage());
                int retrySuccess = updatePrSubBatchTransactional(versionBatch, user, failedRecords);
                successCount += retrySuccess;
            }
        }
        int total = prs.size();

        result.put("totalRecords", total);
        result.put("successCount", successCount);
        result.put("failedCount", failedRecords.size());
        result.put("failedRecords", failedRecords);

//        // If any failed → throw browser error
//        if (!failedRecords.isEmpty()) {
//            throw new GlobalExceptionHandler.PartialFailureException(
//                    failedRecords.size() + " of " + total + " Percentage range approval failed",
//                    result
//            );
//        }

        if (!failedRecords.isEmpty()) {
            return ResponseMap.response(
                    "131",
                    failedRecords.size() + " of " + total + " Percentage range approval failed",
                    result
            );
        }

        return ResponseMap.response(
                status.getSuccessCode(),
                successCount + " of " + total + " Percentage range approved successfully",
                result
        );
    }


    private void prepareUpdatePr(List<PercentageRange> batch, UserModel user, List<GenericResp> failedRecords) {
        Iterator<PercentageRange> iterator = batch.iterator();
        while (iterator.hasNext()) {
            PercentageRange pr = iterator.next();
            if (pr.getCode() == null) {
                GenericResp resp = new GenericResp();
                resp.setId(pr.getPercentageId().toString());
                resp.setMessage("Missing percentage code");
                resp.setData(pr.getCode());

                failedRecords.add(resp);
//                failedRecords.add("(Missing percentage code)");
                iterator.remove();
                continue;
            }

            pr.setOrgId(user.getOrgId());
            pr.setApproveBy(user.getId());
            pr.setId(pr.getPercentageId());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int updatePrBatchTransactional(List<PercentageRange> batch, UserModel user) {
        String desc = "";
        if (batch.isEmpty()) return 0;
        try {
//            List<PercentageRange> approvedCreatedBands = getPrByStatus(batch, "Pending-created", "Approved");
//            List<PercentageRange> approvedActivatedBands = getPrByStatus(batch, "Pending-activated", "Approved");
//            List<PercentageRange> approvedDeactivatedBands = getPrByStatus(batch, "Pending-deactivated", "Deactivated");
//            List<PercentageRange> approvedEditedBands = getPrByStatus(batch, "Pending-edited", "Approved");

            List<PercentageRange> approvedCreatedPr = batch.stream()
                    .filter(m -> "Pending-created".equalsIgnoreCase(m.getApproveStatus()))
                    .peek(m -> m.setApproveStatus("Approved"))
                    .toList();

            List<PercentageRange> approvedActivatedPr = batch.stream()
                    .filter(m -> "Pending-activated".equalsIgnoreCase(m.getApproveStatus()))
                    .peek(m -> m.setApproveStatus("Approved"))
                    .toList();

            List<PercentageRange> approvedDeactivatedPr = batch.stream()
                    .filter(m -> "Pending-deactivated".equalsIgnoreCase(m.getApproveStatus()))
                    .peek(m -> m.setApproveStatus("Deactivated"))
                    .toList();

            List<PercentageRange> approvedEditedPr = batch.stream()
                    .filter(m -> "Pending-edited".equalsIgnoreCase(m.getApproveStatus()))
                    .peek(m -> m.setApproveStatus("Approved"))
                    .toList();

            // Combine all for main update
            List<PercentageRange> toUpdate = Stream.of(
                            approvedCreatedPr,
                            approvedActivatedPr,
                            approvedDeactivatedPr,
                            approvedEditedPr)
                    .flatMap(Collection::stream)
                    .toList();

            if (!toUpdate.isEmpty()) {
                desc = "Percentage range approved";
                debtMapper.updateBatchPrs(toUpdate);
                debtMapper.updateBatchVersionPrs(toUpdate);
            }

            //  Audit success
            auditPrApproveBatch(batch, user, desc);

            log.info("Batch updated successfully: {}", batch.size());
            return batch.size();

        } catch (Exception e) {
            log.error("Approval failed, rolling back batch of size {}: {}", batch.size(), e.getMessage());
            throw new RuntimeException("Batch transaction failed. Rolled back.", e);
        }
    }

    private int updatePrSubBatchTransactional(List<PercentageRange> batch, UserModel user, List<GenericResp> failedRecords) {
        int success = 0;
        int subSize = 100;

        for (int i = 0; i < batch.size(); i += subSize) {
            int end = Math.min(i + subSize, batch.size());
//            List<PercentageRange> subList = batch.subList(i, end);
            List<PercentageRange> subList = new ArrayList<>(batch.subList(i, end));
            try {
                success += updatePrBatchTransactional(subList, user);
            } catch (Exception e) {
                log.error("Sub-batch {} failed: {}", (i / subSize) + 1, e.getMessage());
                if (batch.size() > 50) {
                    success += approvePrSinglesFallbackAsync(batch, user, failedRecords);
                } else {
                    success += approvePrSinglesFallback(batch, user, failedRecords);
                }
            }
        }
        return success;
    }

    public int approvePrSinglesFallbackAsync(List<PercentageRange> batch, UserModel user, List<GenericResp> failedRecords) {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        for (PercentageRange lc : batch) {
            futures.add(approvePrSingleAsync(lc, user, failedRecords));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream().mapToInt(CompletableFuture::join).sum();
    }

    public int approvePrSinglesFallback(List<PercentageRange> lcs, UserModel user, List<GenericResp> failedRecords) {
        int successCount = 0;

        for (PercentageRange pr : lcs) {
            try {
                log.debug("Fallback single assign for pr: {}", pr.getCode());
                approvePrSingleTransactional(pr, user);
                successCount++;
            } catch (Exception e) {
                String reason = extractErrorMessage(e);
                GenericResp resp = new GenericResp();
                resp.setId(pr.getPercentageId().toString());
                resp.setMessage("Approve failed: "+reason);
                resp.setData(pr.getCode());

//                failedRecords.add(String.format(
//                        "%s (Approve failed: %s)",
//                        lc.getCode(),
//                        reason
//                ));
                log.warn("Percentage range {} failed individually: {}", pr.getCode(), reason);
            }
        }

        return successCount;
    }

    @Async
    public CompletableFuture<Integer> approvePrSingleAsync(PercentageRange pr, UserModel user, List<GenericResp> failedRecords) {
        try {
            approvePrSingleTransactional(pr, user);
            return CompletableFuture.completedFuture(1);
        } catch (Exception e) {
            String reason = extractErrorMessage(e);
            GenericResp resp = new GenericResp();
            resp.setId(pr.getPercentageId().toString());
            resp.setMessage("Approve failed: "+reason);
            resp.setData(pr.getCode());

//            failedRecords.add(String.format(
//                    "%s (Approve failed: %s)",
//                    lc.getCode(),
//                    reason
//            ));
            log.warn("Async approve failed for tariff {}: {}",  pr.getCode(), reason);
            return CompletableFuture.completedFuture(0);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void approvePrSingleTransactional(PercentageRange lca, UserModel user) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

        debtMapper.approvePercentage(lca);

        debtMapper.approvePercentageVersion(lca);

        //fetch meter from the database
        PercentageRange percentageRange = debtMapper.getPercentageById(lca.getPercentageId(), user.getOrgId());

        //save to audit (mongodb)
        AuditLog auditLog = buildAuditLog(user, "Percentage range approved", lc, percentageRange, metadata);
        safeAuditService.saveAudit(auditLog);

    }

    private List<PercentageRange> getPrByStatus(List<PercentageRange> batch, String stage, String newStage) {
        List<PercentageRange> ms;
        ms = batch.stream()
                .filter(m -> stage.equalsIgnoreCase(m.getApproveStatus()))
                .peek(m -> m.setApproveStatus(newStage))
                .toList();

        return ms;
    }


    private void auditPrApproveBatch(List<PercentageRange> batch, UserModel user, String desc) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
        for (PercentageRange m : batch) {
            AuditLog auditLog = buildAuditLog(user, desc, lc, m, metadata);
            safeAuditService.saveAudit(auditLog);
        }
    }

//    @Override
//    public Map<String, Object> bulkPercentageRange(MultipartFile file) throws IOException {
//        UserModel user = handleUserValidation();
//        try {
//        // Determine file type
//        String filename = Optional.ofNullable(file.getOriginalFilename())
//                .orElseThrow(() -> new IOException("File has no name"));
//
//        List<PercentageRange> percentageRanges;
//        if (filename.endsWith(".csv")) {
//            percentageRanges = processPercentageCsv(file.getInputStream(), user);
//        } else if (filename.endsWith(".xlsx")) {
//            percentageRanges = processPercentageExcel(file.getInputStream(), user);
//        } else {
//            throw new IOException("Unsupported file format. Only .csv or .xlsx allowed.");
//        }
//
//        Map<String, Object> result = bulkInsertPercentageRange(percentageRanges, user);
//
//        return result;
//
//    } catch (Exception e) {
//        log.error("Error in bulk upload: {}", e.getMessage(), e);
//        genericHandler.logIncidentReport("Bulk upload service failed");
//        genericHandler.logAndSaveException(e, "Bulk upload meter");
//        throw new IOException("Bulk upload failed: " + e.getMessage());
//    }
//    }
//
//    public static List<PercentageRange> processPercentageExcel(InputStream inputStream, UserModel user) throws IOException {
//        List<PercentageRange> percentages = new ArrayList<>();
//
//        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            Iterator<Row> rows = sheet.iterator();
//
//            // Skip header row safely
//            if (rows.hasNext()) {
//                rows.next();
//            }
//
//            while (rows.hasNext()) {
//                Row row = rows.next();
//                PercentageRange pr = new PercentageRange();
//
//                pr.setPercentage(getStringCellValue(row.getCell(0)));
//                pr.setCode(getStringCellValue(row.getCell(1)));
//                pr.setAmountStartRange(getStringCellValue(row.getCell(3)));
//                pr.setAmountEndRange(getStringCellValue(row.getCell(4)));
//
//                percentages.add(pr);
//            }
//        }
//        return percentages;
//    }
//
//    public static List<PercentageRange> processPercentageCsv(InputStream inputStream, UserModel user) throws IOException {
//        List<PercentageRange> percentageRanges = new ArrayList<>();
//
//        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {
//
//            for (CSVRecord record : csvParser) {
//                PercentageRange pr = new PercentageRange();
//                pr.setPercentage(record.get("Percentage".trim()));
//                pr.setCode(record.get("Code".trim().trim()));
//                pr.setAmountStartRange(record.get("Amount Start Range".trim()));
//                pr.setAmountEndRange(record.get("Amount End Range".trim()));
//
//                percentageRanges.add(pr);
//            }
//        }
//        return percentageRanges;
//    }
//
//
//    private Map<String, Object> bulkInsertPercentageRange(List<PercentageRange> percentageRanges, UserModel user) {
//        return Map.of();
////        Map<String, Object> result = new HashMap<>();
////        List<GenericResp> failedRecords = new ArrayList<>();
////
////        if (percentageRanges == null || percentageRanges.isEmpty()) {
////            throw new IllegalArgumentException("Percentage range list cannot be empty");
////        }
////
////        int totalRecords = percentageRanges.size();
////        int successCount = 0;
////
////        // ------------------------------------------
////        // Load Manufacturers
////        // ------------------------------------------
////        List<Band> bands = bandMapper.fetchBands(user.getOrgId());
////        Map<String, UUID> manufacturerNameToId = bands.stream()
////                .collect(Collectors.toMap(
////                        m -> m.getName().trim().toLowerCase(),
////                        Band::getId
////                ));
////
////        if(manufacturerNameToId.isEmpty()) {
////            throw new GlobalExceptionHandler.PartialFailureException(
////                    "Meters upload failed - manufacturer not found",
////                    result
////            );
////        }
////
////        //------------------------------------------------
////        // Validate duplicates INSIDE FILE
////        //------------------------------------------------
////
////        Set<String> seenMeters = new HashSet<>();
////        Set<String> seenSims = new HashSet<>();
////
////        Iterator<PercentageRange> fileIterator = percentageRanges.iterator();
////
////        while (fileIterator.hasNext()) {
////
////            PercentageRange pr = fileIterator.next();
////
////            String percentage = Optional.ofNullable(pr.getPercentage()).orElse("").trim();
////            String code = Optional.ofNullable(pr.getCode()).orElse("").trim();
////            String amountStartRange = Optional.ofNullable(pr.getAmountStartRange()).orElse("").trim();
////            String amountEndRange = Optional.ofNullable(pr.getAmountEndRange()).orElse("").trim();
////
////            if (!seenMeters.add(percentage)) {
////                GenericResp resp = new GenericResp();
////                resp.setId(code);
////                resp.setMessage("Duplicate percentage in uploaded file");
////                resp.setData(percentage);
////                failedRecords.add(resp);
////                fileIterator.remove();
////                continue;
////            }
////
////            if (!code.isEmpty() && !seenSims.add(code)) {
////                GenericResp resp = new GenericResp();
////                resp.setId(code);
////                resp.setMessage("Duplicate code in uploaded file");
////                resp.setData(code);
////                failedRecords.add(resp);
////                fileIterator.remove();
////                continue;
////            }
////        }
////
////        // ------------------------------------------
////        // Extract MeterNumbers + SimNumbers
////        // ------------------------------------------
////
////        Set<String> meterNumbers = meters.stream()
////                .map(Meter::getMeterNumber)
////                .filter(Objects::nonNull)
////                .map(String::trim)
////                .collect(Collectors.toSet());
////
////        Set<String> simNumbers = meters.stream()
////                .map(Meter::getSimNumber)
////                .filter(Objects::nonNull)
////                .map(String::trim)
////                .collect(Collectors.toSet());
////
////        // ---------------------------------------------------
////        // Fetch Existing Meter Numbers (ONE DB CALL)
////        // ---------------------------------------------------
////        Set<String> allMeterNumbers = meters.stream()
////                .map(Meter::getMeterNumber)
////                .filter(Objects::nonNull)
////                .map(String::trim)
////                .filter(s -> !s.isEmpty())
////                .collect(Collectors.toSet());
////
////
////        // ------------------------------------------
////        // Fetch Existing
////        // ------------------------------------------
////
////        List<Meter> existingMeters =
////                meterMapper.getMetersList(
////                        new ArrayList<>(meterNumbers),
//////                        new ArrayList<>(simNumbers),
////                        user.getOrgId()
////                );
////
////        Set<String> existingMeterNumbers = existingMeters.stream()
////                .map(Meter::getMeterNumber)
////                .collect(Collectors.toSet());
////
////        Set<String> existingSimNumbers = existingMeters.stream()
////                .map(Meter::getSimNumber)
////                .collect(Collectors.toSet());
////
////
////        int batchSize = 500; // try 500–1000 for optimal JDBC performance
////
////        for (int i = 0; i < meters.size(); i += batchSize) {
////            int end = Math.min(i + batchSize, meters.size());
//////            List<Meter> batch = meters.subList(i, end);
////            List<Meter> batch = new ArrayList<>(meters.subList(i, end));
////
////            // -----------------------------------------------
////            // Remove duplicates (already existing meters)
////            // -----------------------------------------------
////            Iterator<Meter> iterator = batch.iterator();
////
////            while (iterator.hasNext()) {
////
////                Meter meter = iterator.next();
////
////                String meterNumber = meter.getMeterNumber();
////                String simNumber = meter.getSimNumber();
////                String manufacturer = meter.getMeterManufacturerName();
////
////                if (meterNumber == null || meterNumber.trim().isEmpty()) {
////                    GenericResp resp = new GenericResp();
////                    resp.setId(null);
////                    resp.setMessage("Missing meter number");
////                    resp.setData(null);
////                    failedRecords.add(resp);
////                    iterator.remove();
////                    continue;
////                }
////
////                meterNumber = meterNumber.trim();
////
////                if (existingMeterNumbers.contains(meterNumber)) {
////
////                    GenericResp resp = new GenericResp();
////                    resp.setId(meterNumber);
////                    resp.setMessage("Meter already exists");
////                    resp.setData(meterNumber);
////                    failedRecords.add(resp);
////                    iterator.remove();
////                    continue;
////                }
////
////                if (simNumber != null && existingSimNumbers.contains(simNumber.trim())) {
////
////                    GenericResp resp = new GenericResp();
////                    resp.setId(meterNumber);
////                    resp.setMessage("SIM number already exists");
////                    resp.setData(meterNumber);
////                    failedRecords.add(resp);
////                    iterator.remove();
////                    continue;
////                }
////
////                if (manufacturer == null ||
////                        !manufacturerNameToId.containsKey(manufacturer.trim().toLowerCase())) {
////
////                    GenericResp resp = new GenericResp();
////                    resp.setId(meterNumber);
////                    resp.setMessage("Manufacturer does not exist: " + manufacturer);
////                    resp.setData(manufacturer);
////                    failedRecords.add(resp);
////                    iterator.remove();
//////                    continue;
////                }
////            }
////
////            if (batch.isEmpty()) {
////                continue;
////            }
////
////
////            try {
////                insertBatchTransactional(batch, user, manufacturerNameToId, failedRecords);
////                successCount += batch.size();
////            } catch (Exception e) {
////                log.warn("Batch {} failed — retrying sub batch upload", (i / batchSize) + 1);
////                // Attempt smaller sub-batches to isolate failure
////                successCount += insertSubBatchTransactional(batch, user, manufacturerNameToId, failedRecords);
////            }
////        }
////
////        result.put("totalRecords", totalRecords);
////        result.put("successCount", successCount);
////        result.put("failedCount", failedRecords.size());
////        result.put("failedRecords", failedRecords);
////
////        if (!failedRecords.isEmpty()) {
////            return ResponseMap.response(
////                    "131",
////                    failedRecords.size() + " of " + totalRecords + " Meters upload failed",
////                    result
////            );
////        }
////
////        return ResponseMap.response(
////                status.getSuccessCode(),
////                successCount + " of " + totalRecords + " Meters uploaded successfully",
////                result
////        );
//    }
//
//    @Override
//    public Map<String, Object> bulkLiabilityCause(MultipartFile file) throws IOException {
//        return Map.of();
//    }

    private static String getStringCellValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    private AuditLog buildAuditLog(UserModel creator, String description, String type, Object createdEntity, Map<String, String> metadata) {
        AuditLog log = new AuditLog();
        log.setCreator(creator);
        log.setDescription(description);
        log.setType(type);
        log.setPercentageRange(createdEntity instanceof PercentageRange ? (PercentageRange) createdEntity : null);
        log.setLiabilityCause(createdEntity instanceof LiabilityCause ? (LiabilityCause) createdEntity : null);
        log.setIpAddress(metadata.get("ipAddress"));
        log.setUserAgent(metadata.get("userAgent"));
        log.setEndpoint(metadata.get("endpoint"));
        log.setHttpMethod(metadata.get("httpMethod"));
        return log;
    }

    private void handleAddCache(LiabilityCause liabilityCause) {
        debtCache.remove(liabilityCause.getId().toString()+"_"+liabilityCause.getOrgId());
        for (String key : auditCache.keySet()) {
            if (key.startsWith("grid_flex_audit_log_page_")) {
                auditCache.remove(key);
            }
        }
        for (String key : debtCache.keySet()) {
            if (key.startsWith("lc_"+liabilityCause.getOrgId())) {
                debtCache.remove(key);
            }
        }
        debtCache.put(liabilityCause.getId().toString()+"_"+liabilityCause.getOrgId(), liabilityCause);  // Cache updated or deleted entity
    }

    private void handleAddPercentageCache(PercentageRange percentageRange) {
        debtCache.remove(percentageRange.getId().toString()+"_"+percentageRange.getOrgId());
        for (String key : auditCache.keySet()) {
            if (key.startsWith("grid_flex_audit_log_page_")) {
                auditCache.remove(key);
            }
        }
        for (String key : debtCache.keySet()) {
            if (key.startsWith("pr_"+percentageRange.getOrgId())) {
                debtCache.remove(key);
            }
        }
        debtCache.put(percentageRange.getId().toString()+"_"+percentageRange.getOrgId(), percentageRange);  // Cache updated or deleted entity
    }

    private String buildLcChangeStatusDescription(LiabilityCause liabilityCause, Boolean status) {
        StringBuilder changes = new StringBuilder("Edited "+lc);
        String oldState = liabilityCause.getApproveStatus().trim().equalsIgnoreCase("Approved") ? "activated" : "deactivated";
        String newState = status ? "activated" : "deactivated";
        if (!Objects.equals(liabilityCause.getApproveStatus(), newState)) {
            changes.append(String.format(" status: '%s' → '%s' ", oldState, newState));
        }

        return changes.toString();
    }

    private String buildPrChangeStatusDescription(PercentageRange percentageRange, Boolean status) {
        StringBuilder changes = new StringBuilder("Edited "+pr);
        String oldState = percentageRange.getApproveStatus().trim().equalsIgnoreCase("Approved") ? "activated" : "deactivated";
        String newState = status ? "activated" : "deactivated";
        if (!Objects.equals(percentageRange.getApproveStatus(), newState)) {
            changes.append(String.format(" status: '%s' → '%s' ", oldState, newState));
        }

        return changes.toString();
    }

    private String buildChangeDescription(LiabilityCause oldLc, LiabilityCause newLc) {
        StringBuilder changes = new StringBuilder("Edited ");

        if (!Objects.equals(oldLc.getName(), newLc.getName())) {
            changes.append(String.format("name: '%s' → '%s' ", oldLc.getName(), newLc.getName()));
        }

        System.out.println("old: " + oldLc.getCode());
        System.out.println("new: " + newLc.getCode());
        if (!Objects.equals(oldLc.getCode(), newLc.getCode())) {
            changes.append(String.format("code: '%s' → '%s' ", oldLc.getCode(), newLc.getCode()));
        }
        System.out.println("changes: " + changes);
        return changes.toString();
    }

    private String buildPercentageChangeDescription(PercentageRange oldPercentage, PercentageRange newPercentage) {
        StringBuilder changes = new StringBuilder("Edited ");

        if (!Objects.equals(oldPercentage.getPercentage(), newPercentage.getPercentage())) {
            changes.append(String.format("percentage: '%s' → '%s'; ", oldPercentage.getPercentage(), newPercentage.getPercentage()));
        }

        if (!Objects.equals(oldPercentage.getCode(), newPercentage.getCode())) {
            changes.append(String.format("code: '%s' → '%s'; ", oldPercentage.getCode(), newPercentage.getCode()));
        }

//        if (!Objects.equals(oldPercentage.getBand().getName(), newPercentage.getBand().getName())) {
//            changes.append(String.format("band: '%s' → '%s'; ", oldPercentage.getBand().getName(), newPercentage.getBand().getName()));
//        }

        if (!Objects.equals(oldPercentage.getAmountStartRange(), newPercentage.getAmountStartRange())) {
            changes.append(String.format("amountStartRange: '%s' → '%s'; ", oldPercentage.getAmountStartRange(), newPercentage.getAmountStartRange()));
        }

        if (!Objects.equals(oldPercentage.getAmountEndRange(), newPercentage.getAmountEndRange())) {
            changes.append(String.format("amountEndRange: '%s' → '%s'; ", oldPercentage.getAmountEndRange(), newPercentage.getAmountEndRange()));
        }

        return changes.toString();
    }

}
