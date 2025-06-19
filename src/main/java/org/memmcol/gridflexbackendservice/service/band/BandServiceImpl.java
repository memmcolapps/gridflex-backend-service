package org.memmcol.gridflexbackendservice.service.band;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.BandMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.band.Band;
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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Transactional
@Service
public class BandServiceImpl implements BandService {
    private static final Logger log = LoggerFactory.getLogger(BandServiceImpl.class);

    @Autowired
    private BandMapper bandMapper;

    @Autowired
    private AuthMapper operatorMapper;

    @Autowired
    private ResponseProperties status;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;

    private final IMap<String, Object> bandCache;

    private final IMap<String, Object> auditCache;

    private String bandName = "Band";

    public BandServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.bandCache = hazelcastInstance.getMap("band-Cache");
        this.auditCache = hazelcastInstance.getMap("audit-Cache");
    }

    @Override
    public Map<String, Object> createBand(Band band) {
        AuditLog auditNotificationDTO = new AuditLog();
        try {
            int result;
            UserModel um = handleUserValidation();

            Band isExist = bandMapper.getBand(band.getName());
            if (isExist != null) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(bandName + " " + status.getExistDesc());
            }

            band.setOrgId(um.getOrgId());
            band.setApproveStatus("pending");
            band.setCreatedBy(um.getId());
            band.setDescription("Band Newly Created");
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
            handleAddCache(bandByName);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription("Created Band [" + band.getName() + "]");
            auditNotificationDTO.setType("band");
            auditNotificationDTO.setCreatedBand(bandByName);
            auditRepository.save(auditNotificationDTO);
            return ResponseMap.response(status.getSuccessCode(), bandName + " " + status.getRegDesc(), "");
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
    public Map<String, Object> updateBand(Band band) {
        AuditLog auditNotificationDTO = new AuditLog();
        try {
            UserModel um = handleUserValidation();

            Band isExist = bandMapper.getBandById(band.getBandId(), um.getOrgId());
            if (isExist == null) {
                throw new GlobalExceptionHandler.NotFoundException(bandName + " " + status.getNotFoundDesc());
            }
            band.setApproveStatus("pending");;
            band.setOrgId(um.getOrgId());
            band.setCreatedBy(um.getId());
            String changeDescription = buildChangeDescription(isExist, band);
            band.setDescription(changeDescription);
            band.setOrgId(um.getOrgId());
            int result = bandMapper.createBandVersion(band);
            if(result == 0){
                throw new GlobalExceptionHandler.NotFoundException(bandName + " " + status.getUpdateFailureDesc());
            }
            Band bandById = bandMapper.getBandById(band.getBandId(), um.getOrgId());
            handleAddCache(bandById);
            um.setPassword("");
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription("Updated Band [" + band.getName() + "]");
            auditNotificationDTO.setType("band");
            auditNotificationDTO.setCreatedBand(bandById);
//			authCache.remove("dashboard");
            auditRepository.save(auditNotificationDTO);
            return ResponseMap.response(status.getSuccessCode(), bandName + " Update Awaiting Successfully", "");
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
    public Map<String, Object> manageBandState(UUID bandId, String approveStatus) throws MissingServletRequestParameterException {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        int result;
        String desc = "";
        try {
            UserModel um = handleUserValidation();

            Band band = bandMapper.getBandVersionById(bandId, um.getOrgId());
            if(band == null) {
                throw new GlobalExceptionHandler.NotFoundException(bandName + " " + status.getNotFoundDesc());
            }

            if(approveStatus != null && approveStatus.equalsIgnoreCase("approve")) {
                band.setApproveStatus("approved");
                result = bandMapper.approveBandVersion(band, um.getId());
                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(bandName +" "+ approveStatus + "d "+ status.getUpdateFailureDesc());
                }
                result = bandMapper.approveBand(band);
                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(bandName +" "+ approveStatus + "d "+ status.getUpdateFailureDesc());
                }
                desc = capitalizeFirstLetter(approveStatus) +" Band [" + band.getName() + "]";
            }
            else if (approveStatus.equalsIgnoreCase("reject")){
                band.setApproveStatus("rejected");
                band.setApproveBy(um.getId());
                result = bandMapper.rejectedBandVersion(band, um.getId());
                if (result == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(bandName +" "+ approveStatus + "ed "+ status.getUpdateFailureDesc());
                }
                desc = capitalizeFirstLetter(approveStatus) + " Band [" + band.getName() + "]";
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
                throw new MissingServletRequestParameterException("Required request parameter '%s' is not present", "approveStatus or status");
            }

            Band newBand = bandMapper.getBandById(band.getId(), um.getOrgId());
            handleAddCache(band);
            um.setPassword("");
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setType("band");
            auditNotificationDTO.setCreatedBand(newBand);
            auditRepository.save(auditNotificationDTO);
//            if(state != null) {
//                return ResponseMap.response(status.getSuccessCode(), tariff.getName() + " " + (tariff.getStatus() ? "Activated Successfully" : status.getDeleteDesc()), "");
//            } else {
            return ResponseMap.response(status.getSuccessCode(), band.getName() + " " + (capitalizeFirstLetter(approveStatus) +" Successfully"), "");
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
//    public Map<String, Object> manageBandState(UUID bandId, Boolean state) {
//        AuditLog auditNotificationDTO = new AuditLog();
//        try {
//            UserModel um = handleUserValidation();
//
//
//            Band bandById = bandMapper.getBandById(bandId, um.getOrgId());
//            if(bandById == null) {
//                throw new GlobalExceptionHandler.NotFoundException(bandName + " " + status.getNotFoundDesc());
//            }
//            int result = bandMapper.disableBand(bandId, state, um.getOrgId());
//            if (result == 0) {
//                throw new GlobalExceptionHandler.NotFoundException(bandName + " " + status.getNotFoundDesc());
//            }
//            Band band = bandMapper.getBandById(bandId, um.getOrgId());
//            handleAddCache(bandById);
//            um.setPassword("");
//            auditNotificationDTO.setCreator(um);
//            auditNotificationDTO.setDescription("Disabled Band [" + band.getName() + "]");
//            auditNotificationDTO.setType("band");
//            auditNotificationDTO.setCreatedBand(band);
//
//            return ResponseMap.response(status.getSuccessCode(), band.getName() + " " + (band.getStatus() ? "Enabled Successfully" : status.getDeleteDesc()), "");
//        } catch (Exception exception) {
//            ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
//            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
//            exceptionErrorLogs.setDescription("Error occurred while trying to create band");
//            exceptionErrorLogs.setError_message(exception.getMessage().trim());
//            exceptionErrorLogs.setError(exception.toString().trim());
//            exceptionAuditRepository.save(exceptionErrorLogs);
//            throw exception;
//        }
//
//    }

    @Override
    public Map<String, Object> getBands(String type) {
        try {
            UserModel um = handleUserValidation();

            String cacheKey = "bands_"+type+"_"+um.getOrgId();
            Object cachedBand = bandCache.get(cacheKey);

            if (cachedBand != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached " + bandName + "s " + status.getDesc(), cachedBand);
            }
            List<Band> result;
            if(type.equalsIgnoreCase("pending")) {
                result = bandMapper.fetchBandsVersion(um.getOrgId());
            } else {
                result = bandMapper.fetchBands(um.getOrgId());
            }

            if(result == null) {
                throw new GlobalExceptionHandler.NotFoundException(bandName + " " + status.getNotFoundDesc());
            }
            bandCache.put(cacheKey, result);
            return ResponseMap.response(status.getSuccessCode(), bandName + " " + status.getDesc(), result);
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
    public Map<String, Object> getBand(UUID bandId) {
        try {
            UserModel um = handleUserValidation();

            Object cachedBand = bandCache.get(bandId.toString());

            if (cachedBand != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached " + bandName + " " + status.getDesc(), cachedBand);
            }
            Band result = bandMapper.getBandById(bandId, um.getOrgId());
            if(result == null) {
                throw new GlobalExceptionHandler.NotFoundException(bandName + " " + status.getNotFoundDesc());
            }
            return ResponseMap.response(status.getSuccessCode(), bandName + " " + status.getDesc(), result);
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

    private void handleAddCache(Band band) {
        bandCache.remove(band.getId().toString()+"_"+band.getOrgId());
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

    private String buildChangeDescription(Band oldBand, Band newBand) {
        StringBuilder changes = new StringBuilder("Edited band");

        if (!Objects.equals(oldBand.getName(), newBand.getName())) {
            changes.append(String.format("name: '%s' → '%s'; ", oldBand.getName(), newBand.getName()));
        }

        if (!Objects.equals(oldBand.getHour(), newBand.getHour())) {
            changes.append(String.format("hour: '%s' → '%s'; ", oldBand.getHour(), newBand.getHour()));
        }

        return changes.toString();
    }
}
