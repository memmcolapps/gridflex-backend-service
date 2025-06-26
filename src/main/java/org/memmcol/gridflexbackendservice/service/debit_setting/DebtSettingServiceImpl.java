package org.memmcol.gridflexbackendservice.service.debit_setting;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.BandMapper;
import org.memmcol.gridflexbackendservice.mapper.DebtSettingMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.model.debt_setting.LiabilityCause;
import org.memmcol.gridflexbackendservice.model.debt_setting.PercentageRange;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
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
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class DebtSettingServiceImpl implements DebtSettingService {
    private static final Logger log = LoggerFactory.getLogger(TariffServiceImpl.class);

    @Autowired
    private AuthMapper operatorMapper;

    @Autowired
    private BandMapper bandMapper;

    @Autowired
    private DebtSettingMapper debtMapper;

    @Autowired
    private ResponseProperties status;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;

    private final IMap<String, Object> debtCache;

    private final IMap<String, Object> auditCache;

    private String lc = "Liability cause";

    private String pr = "Percentage Range";

    public DebtSettingServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.debtCache = hazelcastInstance.getMap("debt-Cache");
        this.auditCache = hazelcastInstance.getMap("audit-Cache");
    }

    @Override
    public Map<String, Object> createLiabilityCause(LiabilityCause request) {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            int result;
            String desc = "Liability Cause Newly Created";
            UserModel um = handleUserValidation();

            LiabilityCause isExist = debtMapper.getLiabilityCauseByName(request.getName(), request.getCode(), um.getOrgId());

            if(isExist != null){
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(lc + " " + status.getExistDesc());
            }
            request.setStatus(false); //inactive
            request.setApproveStatus("pending");
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
            auditNotificationDTO.setDescription(desc);//("Created Tariff [" + tariff.getName() + "]");
            auditNotificationDTO.setType("liability cause");
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
        try {
            int result;
            UserModel um = handleUserValidation();
            LiabilityCause isExist = debtMapper.getLiabilityCauseById(request.getId(), um.getOrgId());

            if (isExist == null) {
                throw new GlobalExceptionHandler.NotFoundException(lc + " " + status.getNotFoundDesc());
            }

            LiabilityCause isVersionExist = debtMapper.getLiabilityCauseVersionByName(request.getName(), um.getOrgId());

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

            LiabilityCause liabilityCause = debtMapper.getLiabilityCauseById(request.getId(), um.getOrgId());
            um.setPassword("");
            handleAddCache(liabilityCause);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(changeDescription);
            auditNotificationDTO.setType("liability cause");
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
    public Map<String, Object> getLiabilityCauses(String type) {
        try {
            UserModel um = handleUserValidation();

            String cacheKey = "lc_"+type+"_"+um.getOrgId();
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
                result = debtMapper.getSingleLcVersionById(id, um.getOrgId());
            }

            if(result == null) {
                throw new GlobalExceptionHandler.NotFoundException(lc + " " + status.getNotFoundDesc());
            }
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
        int result;
        String desc = "";
        try {
            UserModel um = handleUserValidation();

            LiabilityCause liabilityCause = debtMapper.getLiabilityCauseVersionById(liabilityCauseId, um.getOrgId());
            if(liabilityCause == null) {
                throw new GlobalExceptionHandler.NotFoundException(lc + " " + status.getNotFoundDesc());
            }


            liabilityCause.setOrgId(um.getOrgId());
            liabilityCause.setApprovedBy(um.getId());

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
                desc = capitalizeFirstLetter(approveStatus) + lc + " [" + liabilityCause.getName() + "]";
            }
            else if (approveStatus != null && approveStatus.contains("reject")){
                liabilityCause.setApproveStatus("rejected");
                liabilityCause.setStatus(false);
                result = debtMapper.rejectedLiabilityVersion(liabilityCause);
//                result = tariffMapper.rejectedTariff(tariff);
                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(lc +" "+ approveStatus + "ed "+ status.getUpdateFailureDesc());
                }
                desc = capitalizeFirstLetter(approveStatus) +" Liability Cause [" + liabilityCause.getName() + "]";
            } else {
                assert approveStatus != null;
                throw new MissingServletRequestParameterException("Required request parameter '%s' is not present", approveStatus);
            }

            LiabilityCause newLc = debtMapper.getLiabilityCauseById(liabilityCause.getId(), um.getOrgId());

            handleAddCache(liabilityCause);
            um.setPassword("");
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setType("Liability Cause");
            auditNotificationDTO.setLiabilityCause(newLc);
            auditRepository.save(auditNotificationDTO);
//            if(state != null) {
//                return ResponseMap.response(status.getSuccessCode(), tariff.getName() + " " + (tariff.getStatus() ? "Activated Successfully" : status.getDeleteDesc()), "");
//            } else {
            return ResponseMap.response(status.getSuccessCode(), liabilityCause.getName() + " " + (capitalizeFirstLetter(approveStatus) +" Successfully"), "");
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

    @Override
    public Map<String, Object> createPercentage(PercentageRange request) {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            int result;
            String desc = "Percentage Range Newly Created";
            UserModel um = handleUserValidation();

            PercentageRange isExist = debtMapper.getPercentageByName(request.getName(), request.getCode(), um.getOrgId());

            if(isExist != null){
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(pr + " " + status.getExistDesc());
            }
            request.setStatus(false); //inactive
            request.setApproveStatus("pending");
            request.setCreatedBy(um.getId());
            request.setDescription(desc);
            result = debtMapper.createPercentageCause(request);
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
            auditNotificationDTO.setDescription(desc);//("Created Tariff [" + tariff.getName() + "]");
            auditNotificationDTO.setType("percentage range");
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
        try {
            int result;
            UserModel um = handleUserValidation();
            PercentageRange isExist = debtMapper.getPercentageById(request.getId(), um.getOrgId());

            if (isExist == null) {
                throw new GlobalExceptionHandler.NotFoundException(pr + " " + status.getNotFoundDesc());
            }

            PercentageRange isVersionExist = debtMapper.getPercentageVersionByName(request.getName(), um.getOrgId());

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

            PercentageRange percentageRange = debtMapper.getPercentageById(request.getId(), um.getOrgId());
            um.setPassword("");
            handleAddPercentageCache(percentageRange);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(changeDescription);
            auditNotificationDTO.setType("percentage range");
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
    public Map<String, Object> getAllPercentages(String type) {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            return Map.of();
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
    public Map<String, Object> getPercentage(UUID id, UUID percentageVersionId) {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            return Map.of();
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
    public Map<String, Object> managePercentageState(UUID liabilityCauseId, String approveStatus) throws MissingServletRequestParameterException {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            return Map.of();
        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to create tariff");
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
            throw new LockedException("User is disable");
        }

        return isOperatorExist;
    }

    public static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
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
            changes.append(String.format("name: '%s' → '%s'; ", oldLc.getName(), newLc.getName()));
        }

        if (!Objects.equals(oldLc.getCode(), newLc.getCode())) {
            changes.append(String.format("code: '%s' → '%s'; ", oldLc.getCode(), newLc.getCode()));
        }

        return changes.toString();
    }

    private String buildPercentageChangeDescription(PercentageRange oldPercentage, PercentageRange newPercentage) {
        StringBuilder changes = new StringBuilder("Edited ");

        if (!Objects.equals(oldPercentage.getName(), newPercentage.getName())) {
            changes.append(String.format("name: '%s' → '%s'; ", oldPercentage.getName(), newPercentage.getName()));
        }

        if (!Objects.equals(oldPercentage.getCode(), newPercentage.getCode())) {
            changes.append(String.format("code: '%s' → '%s'; ", oldPercentage.getCode(), newPercentage.getCode()));
        }

        if (!Objects.equals(oldPercentage.getBand(), newPercentage.getBand())) {
            changes.append(String.format("band: '%s' → '%s'; ", oldPercentage.getBand(), newPercentage.getBand()));
        }

        if (!Objects.equals(oldPercentage.getAmountStartRange(), newPercentage.getAmountStartRange())) {
            changes.append(String.format("amountStartRange: '%s' → '%s'; ", oldPercentage.getAmountStartRange(), newPercentage.getAmountStartRange()));
        }

        if (!Objects.equals(oldPercentage.getAmountEndRange(), newPercentage.getAmountEndRange())) {
            changes.append(String.format("amountEndRange: '%s' → '%s'; ", oldPercentage.getAmountEndRange(), newPercentage.getAmountEndRange()));
        }

        return changes.toString();
    }

}
