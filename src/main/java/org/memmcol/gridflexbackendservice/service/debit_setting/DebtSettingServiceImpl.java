package org.memmcol.gridflexbackendservice.service.debit_setting;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.BandMapper;
import org.memmcol.gridflexbackendservice.mapper.DebtSettingMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.model.debt_setting.LiabilityCause;
import org.memmcol.gridflexbackendservice.model.debt_setting.PercentageRange;
import org.memmcol.gridflexbackendservice.model.user.CustomUserPrincipal;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.service.tariff.TariffServiceImpl;
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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.memmcol.gridflexbackendservice.util.GenericHandler.capitalizeFirstLetter;
import static org.memmcol.gridflexbackendservice.util.GenericHandler.getClientIp;
import static org.memmcol.gridflexbackendservice.util.handleValidUser.handleUserValidation;

@Service
@Transactional
public class DebtSettingServiceImpl implements DebtSettingService {
    private static final Logger log = LoggerFactory.getLogger(TariffServiceImpl.class);

    @Autowired
    private DebtSettingMapper debtMapper;

    @Autowired
    private ResponseProperties status;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;

    private final IMap<String, Object> debtCache;

    private final IMap<String, Object> auditCache;

    private String lc = "Liability Cause";

    private String pr = "Percentage Range";

    public DebtSettingServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.debtCache = hazelcastInstance.getMap("debt-Cache");
        this.auditCache = hazelcastInstance.getMap("audit-Cache");
    }

    @Override
    public Map<String, Object> createLiabilityCause(LiabilityCause request) {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        String ipAddress = getClientIp(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");
        try {
            int result;
            String desc = capitalizeFirstLetter(request.getName()) + " newly created";
            UserModel um = handleUserValidation();

            LiabilityCause isExist = debtMapper.getLiabilityCauseByName(request.getName(), request.getCode(), um.getOrgId());

            if(isExist != null){
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(lc + " " + status.getExistDesc());
            }
            request.setStatus(false); //inactive
            request.setApproveStatus("pending");
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
            handleAddCache(liabilityCause);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setType(lc);
            auditNotificationDTO.setLiabilityCause(liabilityCause);
            auditRepository.save(auditNotificationDTO);
            return ResponseMap.response(status.getSuccessCode(), lc + " " + status.getRegDesc(), "");
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
    public Map<String, Object> updateLiabilityCause(LiabilityCause request) {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        String ipAddress = getClientIp(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");
        try {
            int result;
            UserModel um = handleUserValidation();
            LiabilityCause isExist = debtMapper.getLiabilityCauseById(request.getLiabilityCauseId(), um.getOrgId());

            if (isExist == null) {
                throw new GlobalExceptionHandler.NotFoundException(lc + " " + status.getNotFoundDesc());
            }

            LiabilityCause isVersionExist = debtMapper.getLiabilityCauseVersionById(request.getLiabilityCauseId(), um.getOrgId());

            request.setApproveStatus("pending");
            request.setStatus(false);
            request.setOrgId(um.getOrgId());
            request.setCreatedBy(um.getId());
            String changeDescription = buildChangeDescription(isExist, request);
            request.setDescription(changeDescription);

            if(isVersionExist != null && isVersionExist.getApproveStatus().equalsIgnoreCase("pending")){
                result = debtMapper.updateLiabilityCauseVer(request);
                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(lc + " " + status.getUpdateFailureDesc());
                }
            } else {
                result = debtMapper.createLiabilityCauseVersion(request);
                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(lc + " " + status.getUpdateFailureDesc());
                }
            }

            LiabilityCause liabilityCause = debtMapper.getLiabilityCauseById(request.getLiabilityCauseId(), um.getOrgId());
            um.setPassword("");
            handleAddCache(liabilityCause);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(changeDescription);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setType(lc);
            auditNotificationDTO.setLiabilityCause(liabilityCause);
            auditRepository.save(auditNotificationDTO);
            return ResponseMap.response(status.getSuccessCode(), lc + " " + status.getUpdateDesc(), "");
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
    public Map<String, Object> getLiabilityCauses(String type) {
        try {
            UserModel um = handleUserValidation();

            String cacheKey = "lc_"+ um.getOrgId()+"_"+type;
            Object cachedBand = debtCache.get(cacheKey);

            if (cachedBand != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached " + lc + "s " + status.getDesc(), cachedBand);
            }
            List<LiabilityCause> result;
            if(type.equalsIgnoreCase("pending")) {
                result = debtMapper.getLiabilityCauseVersion(um.getOrgId());
            } else {
                result = debtMapper.getLiabilityCause(um.getOrgId());
            }

            if(result == null) {
                throw new GlobalExceptionHandler.NotFoundException(lc + " " + status.getNotFoundDesc());
            }
            debtCache.put(cacheKey, result);
            return ResponseMap.response(status.getSuccessCode(), lc + " " + status.getDesc(), result);
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

            handleAddCache(result);

            return ResponseMap.response(status.getSuccessCode(), lc + " " + status.getDesc(), result);
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

    @Override
    public Map<String, Object> manageLiabilityCauseState(UUID liabilityCauseId, String approveStatus) throws MissingServletRequestParameterException {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        String ipAddress = getClientIp(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");
        int result;
        String desc = "";
        try {
            UserModel um = handleUserValidation();

            LiabilityCause liabilityCause = debtMapper.getLiabilityCauseVersionById(liabilityCauseId, um.getOrgId());
            if(liabilityCause == null) {
                throw new GlobalExceptionHandler.NotFoundException(lc + " " + status.getNotFoundDesc());
            }


            liabilityCause.setOrgId(um.getOrgId());
            liabilityCause.setApproveBy(um.getId());

            if(approveStatus != null && approveStatus.contains("approve")) {
                liabilityCause.setApproveStatus("approved");
                liabilityCause.setStatus(true);
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
                liabilityCause.setApproveStatus("rejected");
                liabilityCause.setStatus(false);
                result = debtMapper.rejectedLiabilityVersion(liabilityCause);

                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(lc +" "+ approveStatus + "ed "+ status.getUpdateFailureDesc());
                }
                desc = capitalizeFirstLetter(liabilityCause.getName()) + " " + liabilityCause.getApproveStatus();
            } else {
                assert approveStatus != null;
                throw new MissingServletRequestParameterException("Required request parameter '%s' is not present", approveStatus);
            }

            LiabilityCause newLc = debtMapper.getLiabilityCauseById(liabilityCause.getId(), um.getOrgId());

            handleAddCache(liabilityCause);
            um.setPassword("");
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setType("Liability Cause");
            auditNotificationDTO.setLiabilityCause(newLc);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(), liabilityCause.getName() + " " + (capitalizeFirstLetter(approveStatus) +" Successfully"), "");

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
    public Map<String, Object> createPercentage(PercentageRange request) {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        String ipAddress = getClientIp(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");
        try {
            int result;
            String desc = request.getPercentage()+ "% newly created";
            UserModel um = handleUserValidation();

            Band band = debtMapper.getBand(request.getBandId(), um.getOrgId());
            if (band == null) {
                throw new GlobalExceptionHandler.NotFoundException("Band "+ status.getNotFoundDesc());
            }

            request.setStatus(false); //inactive
            request.setApproveStatus("pending");
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
            handleAddPercentageCache(percentageRange);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setType(pr);
            auditNotificationDTO.setPercentageRange(percentageRange);
            auditRepository.save(auditNotificationDTO);
            return ResponseMap.response(status.getSuccessCode(), pr + " " + status.getRegDesc(), "");
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
    public Map<String, Object> updatePercentage(PercentageRange request) {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        String ipAddress = getClientIp(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");
        try {
            int result;
            UserModel um = handleUserValidation();
            PercentageRange isExist = debtMapper.getPercentageById(request.getPercentageId(), um.getOrgId());

            if (isExist == null) {
                throw new GlobalExceptionHandler.NotFoundException(pr + " " + status.getNotFoundDesc());
            }

            PercentageRange isVersionExist = debtMapper.getPercentageVersionByName(request.getPercentage(), um.getOrgId());

            request.setApproveStatus("pending");
            request.setStatus(false);
            request.setOrgId(um.getOrgId());
            request.setCreatedBy(um.getId());
            String changeDescription = buildPercentageChangeDescription(isExist, request);
            request.setDescription(changeDescription);

            if(isVersionExist != null && isVersionExist.getApproveStatus().equalsIgnoreCase("pending")){
                result = debtMapper.updatePercentageVer(request);
                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(pr + " " + status.getUpdateFailureDesc());
                }
            } else {
                result = debtMapper.createPercentageVersion(request);
                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(pr + " " + status.getUpdateFailureDesc());
                }
            }

            PercentageRange percentageRange = debtMapper.getPercentageById(request.getPercentageId(), um.getOrgId());
            um.setPassword("");
            handleAddPercentageCache(percentageRange);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(changeDescription);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setType(pr);
            auditNotificationDTO.setPercentageRange(percentageRange);
            auditRepository.save(auditNotificationDTO);
            return ResponseMap.response(status.getSuccessCode(), pr + " " + status.getUpdateDesc(), "");
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
    public Map<String, Object> getAllPercentages(String type) {
        try {
            UserModel um = handleUserValidation();

            String cacheKey = "pr_"+um.getOrgId()+type;
            Object cachedBand = debtCache.get(cacheKey);

            if (cachedBand != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached " + pr + "s " + status.getDesc(), cachedBand);
            }
            List<PercentageRange> result;
            if(type.equalsIgnoreCase("pending")) {
                result = debtMapper.getPercentageVersion(um.getOrgId());
            } else {
                result = debtMapper.getPercentage(um.getOrgId());
            }

            if(result == null) {
                throw new GlobalExceptionHandler.NotFoundException(pr + " " + status.getNotFoundDesc());
            }
            debtCache.put(cacheKey, result);
            return ResponseMap.response(status.getSuccessCode(), pr + " " + status.getDesc(), result);
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

            handleAddPercentageCache(result);

            return ResponseMap.response(status.getSuccessCode(), pr + " " + status.getDesc(), result);
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

    @Override
    public Map<String, Object> managePercentageState(UUID percentageId, String approveStatus) throws MissingServletRequestParameterException {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        int result;
        String desc = "";
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            UserModel um = handleUserValidation();

            PercentageRange percentage = debtMapper.getPercentageVersionById(percentageId, um.getOrgId());
            if(percentage == null) {
                throw new GlobalExceptionHandler.NotFoundException(pr + " " + status.getNotFoundDesc());
            }
            percentage.setOrgId(um.getOrgId());
            percentage.setApproveBy(um.getId());
            percentage.setOrgId(um.getOrgId());
            if(approveStatus != null && approveStatus.contains("approve")) {
                percentage.setApproveStatus("approved");
                percentage.setStatus(true);
                result = debtMapper.approvePercentageVersion(percentage);
                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(pr +" "+ approveStatus + "d "+ status.getUpdateFailureDesc());
                }
                result = debtMapper.approvePercentage(percentage);
                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(pr +" "+ approveStatus + "d "+ status.getUpdateFailureDesc());
                }

                desc = capitalizeFirstLetter(percentage.getPercentage()) + " " + percentage.getApproveStatus();
            }
            else if (approveStatus != null && approveStatus.contains("reject")){
                percentage.setApproveStatus("rejected");
                percentage.setStatus(false);
                result = debtMapper.rejectedPercentageVersion(percentage);

                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(pr +" "+ approveStatus + "ed "+ status.getUpdateFailureDesc());
                }
                desc = capitalizeFirstLetter(percentage.getPercentage()) + " " + percentage.getApproveStatus();
            } else {
                assert approveStatus != null;
                throw new MissingServletRequestParameterException("Required request parameter '%s' is not present", approveStatus);
            }

            PercentageRange newPercentageRange = debtMapper.getPercentageById(percentage.getId(), um.getOrgId());

            handleAddPercentageCache(percentage);
            um.setPassword("");
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setType(pr);
            auditNotificationDTO.setPercentageRange(newPercentageRange);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(), capitalizeFirstLetter(approveStatus) +" successfully", "");

        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to create tariff");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
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
