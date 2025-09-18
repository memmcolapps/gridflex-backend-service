package org.memmcol.gridflexbackendservice.service.band;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.BandMapper;
import org.memmcol.gridflexbackendservice.mapper.TariffMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
//import org.memmcol.gridflexbackendservice.util.HandleCatchError;
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

import static org.memmcol.gridflexbackendservice.util.GenericHandler.capitalizeFirstLetter;
import static org.memmcol.gridflexbackendservice.util.GenericHandler.getClientIp;
import static org.memmcol.gridflexbackendservice.components.handleValidUser.handleUserValidation;

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
        AuditLog auditNotificationDTO = new AuditLog();
        String ipAddress = getClientIp(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");
        try {
            int result;
            String desc = capitalizeFirstLetter(band.getName())+ " created";
            UserModel um = handleUserValidation();

            Band isExist = bandMapper.getBand(band.getName());
            if (isExist != null) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException(bandName + " " + status.getExistDesc());
            }

            Band isVersionExist = bandMapper.getVersionBand(band.getName(), um.getOrgId());
            if(isVersionExist != null) {
                throw new GlobalExceptionHandler.NotFoundException(isVersionExist.getName()+ " have a pending task to attend to");
            }

            band.setOrgId(um.getOrgId());
            band.setApproveStatus("Pending-created");
            band.setCreatedBy(um.getId());
//            band.setAction("Created");
//            band.setStatus(true);
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
//            handleAddCache(bandByName);
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setType(bandName);
            auditNotificationDTO.setCreatedBand(bandByName);
            auditRepository.save(auditNotificationDTO);
            return ResponseMap.response(status.getSuccessCode(), bandName + " " + status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
//            HandleCatchError.catchError(exception);
            ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();

            exceptionErrorLogs.setDescription("Error occurred while trying to create band");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }

    }

    @Transactional
    @Override
    public Map<String, Object> updateBand(Band band) {
        AuditLog auditNotificationDTO = new AuditLog();
        String ipAddress = getClientIp(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");
        try {
            int result;
            UserModel um = handleUserValidation();

            Band isExist = bandMapper.getBandById(band.getBandId(), um.getOrgId());
            if (isExist == null) {
                throw new GlobalExceptionHandler.NotFoundException(bandName + " " + status.getNotFoundDesc());
            }

            band.setApproveStatus("Pending-edited");
            band.setOrgId(um.getOrgId());
            band.setCreatedBy(um.getId());
            String changeDescription = buildChangeDescription(isExist, band);
            band.setDescription(changeDescription);

            Band isVersionExist = bandMapper.getBandVersionById(band.getBandId(), um.getOrgId());

            if(isVersionExist != null){
                throw new GlobalExceptionHandler.NotFoundException(isVersionExist.getName()+ " have a pending status needs to be cleared");
            } else {
                int res = bandMapper.updateBand("Pending-edited", band.getBandId(), band.getUpdatedAt());
                result = bandMapper.createBandVersion(band);
                if(result == 0 || res == 0){
                    throw new GlobalExceptionHandler.NotFoundException(bandName + " " + status.getUpdateDesc());
                }
            }
            Band bandById = bandMapper.getBandById(band.getBandId(), um.getOrgId());


//            handleAddCache(bandById);

            um.setPassword("");
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(changeDescription);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setType(bandName);
            auditNotificationDTO.setCreatedBand(bandById);
//			authCache.remove("dashboard");
            auditRepository.save(auditNotificationDTO);
            return ResponseMap.response(status.getSuccessCode(), bandName + " " + status.getUpdateDesc(), "");
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

    @Transactional
    @Override
    public Map<String, Object> approve(UUID bandId, String approveStatus) throws MissingServletRequestParameterException {
        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        String ipAddress = getClientIp(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");
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
                }
                else {
                    band.setApproveStatus("Approved");
                    // Fallback to Approve if rejected
                    int u = bandMapper.updateBand(band.getApproveStatus(), band.getBandId(), band.getUpdatedAt());
                    if(u == 0) throw new GlobalExceptionHandler.NotFoundException(bandName + " update failed");
                }

                desc = capitalizeFirstLetter(band.getName()) + " " + band.getApproveStatus();
            } else {
                assert approveStatus != null;
                throw new MissingServletRequestParameterException("Required request parameter '%s' is not present", approveStatus);
            }

            Band newBand = bandMapper.getBandById(band.getId(), um.getOrgId());
//            handleAddCache(band);
            um.setPassword("");
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setType(bandName);
            auditNotificationDTO.setCreatedBand(newBand);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(), band.getName() + " " +
                    (capitalizeFirstLetter(approveStatus) + " Successfully"), "");



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
//            HandleCatchError.catchError(exception);
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
            ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to create band");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> changeStatus(UUID bandId, Boolean state) {
        AuditLog auditNotificationDTO = new AuditLog();
        String ipAddress = getClientIp(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");
        try {
            int result;
            UserModel um = handleUserValidation();
            Band band = bandMapper.getBandById(bandId, um.getOrgId());
            if(band == null){
                throw new GlobalExceptionHandler.NotFoundException("Band "+status.getNotFoundDesc());
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

            Band isVersionExist = bandMapper.getBandVersionById(bandId, um.getOrgId());
            band.setApproveStatus("Pending-"+(state ? "activated" : "deactivated"));
            band.setOrgId(um.getOrgId());
            band.setCreatedBy(um.getId());
            band.setBandId(bandId);
            String changeDescription = buildChangeStatusDescription(band, state);
            band.setDescription(changeDescription);

            if(isVersionExist != null){
                throw new GlobalExceptionHandler.NotFoundException(isVersionExist.getName()+ " have a pending status that needs to be cleared");
            } else {
                result = bandMapper.createBandVersion(band);
                if(result == 0) throw new GlobalExceptionHandler.NotFoundException(bandName + " " + status.getUpdateDesc());
            }
            int u = bandMapper.updateBand(band.getApproveStatus(), band.getId(), band.getUpdatedAt());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException(bandName + " "+ status.getUpdateFailureDesc());
            Band bandById = bandMapper.getBandById(band.getBandId(), um.getOrgId());
            handleAddCache(bandById);
            um.setPassword("");
            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(changeDescription);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setType(bandName);
            auditNotificationDTO.setCreatedBand(bandById);
//			authCache.remove("dashboard");
            auditRepository.save(auditNotificationDTO);
            return ResponseMap.response(status.getSuccessCode(), bandName+(state ? " Activate " : " Deactivate ")+ "Successfully", "");
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

    @Override
    public Map<String, Object> clearCache() {
        tariffCache.clear(); // Clear the cache
        bandCache.clear();
        auditCache.clear();
        return ResponseMap.response(status.getSuccessCode(), "Cache cleared successfully", "");
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
