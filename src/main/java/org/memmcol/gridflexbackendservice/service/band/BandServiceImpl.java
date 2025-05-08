package org.memmcol.gridflexbackendservice.service.band;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.BandMapper;
import org.memmcol.gridflexbackendservice.model.*;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.service.auth.AuthServiceImpl;
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

import java.util.List;
import java.util.Map;

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
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = (authentication != null) ? authentication.getName() : "Unknown";
            UserDTO isOperatorExist = operatorMapper.findAuthByUserEmail(username);
            if (!isOperatorExist.getUser().getStatus()) {
                throw new LockedException("User is blocked");
            }
            Band isExist = bandMapper.getBand(band.getName());
            if (isExist != null) {
                return ResponseMap.response(status.getExistCode(), bandName + " " + status.getExistDesc(), "");
            }
            int result = bandMapper.createBand(band);
            if(result == 0){
                return ResponseMap.response(status.getRegCode(), bandName + " " + status.getRegFailureDesc(), "");
            }
            Band bandByName = bandMapper.getBand(band.getName());
            isOperatorExist.getUser().setPassword("");
            handleAddCache(bandByName);
            auditNotificationDTO.setCreator(isOperatorExist.getUser());
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
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = (authentication != null) ? authentication.getName() : "Unknown";
            UserDTO isOperatorExist = operatorMapper.findAuthByUserEmail(username);
            if (!isOperatorExist.getUser().getStatus()) {
                throw new LockedException("User is blocked");
            }
            Band isExist = bandMapper.getBand(band.getName());
            if (isExist == null) {
                return ResponseMap.response(status.getNotFoundCode(), bandName + " " + status.getNotFoundDesc(), "");
            }
            if(!isExist.getStatus()){
                return ResponseMap.response(status.getDeleteCode(), bandName + " disabled", "");
            }
            int result = bandMapper.updateBand(band);
            if(result == 0){
                return ResponseMap.response(status.getUpdateCode(), bandName + " " + status.getUpdateFailureDesc(), "");
            }
            Band bandById = bandMapper.getBandById(band.getId());
            handleAddCache(bandById);
            isOperatorExist.getUser().setPassword("");
            auditNotificationDTO.setCreator(isOperatorExist.getUser());
            auditNotificationDTO.setDescription("Updated Band [" + band.getName() + "]");
            auditNotificationDTO.setType("band");
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

    @Override
    public Map<String, Object> manageBandState(Long bandId, Boolean state) {
        AuditLog auditNotificationDTO = new AuditLog();
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = (authentication != null) ? authentication.getName() : "Unknown";
            UserDTO isOperatorExist = operatorMapper.findAuthByUserEmail(username);
            if (!isOperatorExist.getUser().getStatus()) {
                throw new LockedException("User is blocked");
            }
            Band bandById = bandMapper.getBandById(bandId);
            if(bandById == null) {
                return ResponseMap.response(status.getNotFoundCode(), bandName + " " + status.getNotFoundDesc(), "");
            }
            int result = bandMapper.disableBand(bandId, state);
            if (result == 0) {
                return ResponseMap.response(status.getNotFoundCode(), bandName + " " + status.getNotFoundDesc(), "");
            }
            Band band = bandMapper.getBandById(bandById.getId());
            handleAddCache(bandById);
            isOperatorExist.getUser().setPassword("");
            auditNotificationDTO.setCreator(isOperatorExist.getUser());
            auditNotificationDTO.setDescription("Disabled Band [" + band.getName() + "]");
            auditNotificationDTO.setType("band");
            auditNotificationDTO.setCreatedBand(band);

            return ResponseMap.response(status.getSuccessCode(), band.getName() + " " + (band.getStatus() ? "Enabled Successfully" : status.getDeleteDesc()), "");
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
    public Map<String, Object> getBands() {
        try {
//            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//            String username = (authentication != null) ? authentication.getName() : "Unknown";
//            UserDTO isOperatorExist = operatorMapper.findAuthByUserEmail(username);
//            if (!isOperatorExist.getUser().getStatus()) {
//                throw new LockedException("User is blocked");
//            }
            String cacheKey = "bands_";
            Object cachedBand = bandCache.get(cacheKey);

            if (cachedBand != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached " + bandName + "s " + status.getDesc(), cachedBand);
            }
            List<Band> result = bandMapper.fetchBands();
            if(result == null) {
                return ResponseMap.response(status.getNotFoundCode(), bandName + " " + status.getNotFoundDesc(), "");
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
    public Map<String, Object> getBand(Long bandId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = (authentication != null) ? authentication.getName() : "Unknown";
            UserDTO isOperatorExist = operatorMapper.findAuthByUserEmail(username);
            if (!isOperatorExist.getUser().getStatus()) {
                throw new LockedException("User is blocked");
            }
            Object cachedBand = bandCache.get(bandId.toString());

            if (cachedBand != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached " + bandName + " " + status.getDesc(), cachedBand);
            }
            Band result = bandMapper.getBandById(bandId);
            if(result == null) {
                return ResponseMap.response(status.getNotFoundCode(), bandName + " " + status.getNotFoundDesc(), "");
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
        bandCache.remove(band.getName());
        for (String key : auditCache.keySet()) {
            if (key.startsWith("grid_flex_audit_log_page_")) {
                auditCache.remove(key);
            }
        }
        for (String key : bandCache.keySet()) {
            if (key.startsWith("bands_")) {
                bandCache.remove(key);
            }
        }
        bandCache.put(band.getId().toString(), band);  // Cache updated or deleted entity
    }

    private void removeFromCache() {
        for (String key : auditCache.keySet()) {
            if (key.startsWith("grid_flex_audit_log_page_")) {
                auditCache.remove(key);
            }
        }
    }
}
