package org.memmcol.gridflexbackendservice.service.band;

import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.BandMapper;
import org.memmcol.gridflexbackendservice.model.Band;
import org.memmcol.gridflexbackendservice.model.Operator;
import org.memmcol.gridflexbackendservice.model.OperatorAudit;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.util.ResponseProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BandServiceImpl implements BandService {
    @Autowired
    private BandMapper bandMapper;

    @Autowired
    private AuthMapper operatorMapper;

    @Autowired
    private ResponseProperties status;

    @Autowired
    private AuditRepository auditRepository;

    private String bandName = "Band";

    private String user = "Operator";

    @Override
    public Map<String, Object> createBand(Band band) {
        OperatorAudit auditNotificationDTO = new OperatorAudit();
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = (authentication != null) ? authentication.getName() : "Unknown";
            Operator isOperatorExist = operatorMapper.findByAuthEmail(username);
            if (isOperatorExist == null) {
                return ResponseMap.response(status.getNotFoundCode(), user + " " + status.getNotFoundDesc(), "");
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
            auditNotificationDTO.setCreator(isOperatorExist);
            auditNotificationDTO.setDescription(band.getName() + " Created band");
            auditNotificationDTO.setType("band");
            auditNotificationDTO.setCreatedBand(bandByName);
//            auditRepository.setCreaytion
//            for (String key : auditCache.keySet()) {
//                if (key.startsWith("grid_flex_audit_log_page_")) {
//                    auditCache.remove(key);
//                }
//            }
//			authCache.remove("dashboard");
            auditRepository.save(auditNotificationDTO);
            return ResponseMap.response(status.getSuccessCode(), bandName + " " + status.getRegDesc(), "");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw e;
        }

    }

    @Override
    public Map<String, Object> updateBand(Band band) {
        OperatorAudit auditNotificationDTO = new OperatorAudit();
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = (authentication != null) ? authentication.getName() : "Unknown";
            Operator isOperatorExist = operatorMapper.findByAuthEmail(username);
            if (isOperatorExist == null) {
                return ResponseMap.response(status.getNotFoundCode(), user + status.getNotFoundDesc(), "");
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
            auditNotificationDTO.setCreator(isOperatorExist);
            auditNotificationDTO.setDescription(band.getName() + " Updated band");
            auditNotificationDTO.setType("band");
            auditNotificationDTO.setCreatedBand(bandById);
//            for (String key : auditCache.keySet()) {
//                if (key.startsWith("grid_flex_audit_log_page_")) {
//                    auditCache.remove(key);
//                }
//            }
//			authCache.remove("dashboard");
            auditRepository.save(auditNotificationDTO);
            return ResponseMap.response(status.getSuccessCode(), bandName + " " + status.getUpdateDesc(), "");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw e;
        }
    }

    @Override
    public Map<String, Object> getBands() {
        try {
            List<Band> result = bandMapper.fetchBands();
            if(result == null) {
                return ResponseMap.response(status.getNotFoundCode(), bandName + " " + status.getNotFoundDesc(), "");
            }
            return ResponseMap.response(status.getSuccessCode(), bandName + " " + status.getDesc(), result);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public Map<String, Object> disableBand(Long bandId, Boolean state) {
        OperatorAudit auditNotificationDTO = new OperatorAudit();
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = (authentication != null) ? authentication.getName() : "Unknown";
            Operator isOperatorExist = operatorMapper.findByAuthEmail(username);

            Band bandById = bandMapper.getBandById(bandId);
            if(bandById == null) {
                return ResponseMap.response(status.getNotFoundCode(), bandName + " " + status.getNotFoundDesc(), "");
            }
            int result = bandMapper.disableBand(bandId, state);
            if (result == 0) {
                return ResponseMap.response(status.getNotFoundCode(), bandName + " " + status.getNotFoundDesc(), "");
            }
            Band band = bandMapper.getBandById(bandById.getId());
            auditNotificationDTO.setCreator(isOperatorExist);
            auditNotificationDTO.setDescription(bandById.getName() + " Disabled band");
            auditNotificationDTO.setType("band");
            auditNotificationDTO.setCreatedBand(band);
            return ResponseMap.response(status.getSuccessCode(), bandName + " " + status.getDeleteDesc(), "");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw e;
        }

    }
}
