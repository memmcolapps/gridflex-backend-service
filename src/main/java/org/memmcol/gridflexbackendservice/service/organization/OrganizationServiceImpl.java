package org.memmcol.gridflexbackendservice.service.organization;


import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.mapper.OrganizationMapper;
import org.memmcol.gridflexbackendservice.mapper.UserMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.user.*;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.service.audit.SafeAuditService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.HandlePermission;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


import java.util.*;

import static org.memmcol.gridflexbackendservice.components.HandleValidUser.handleUserValidation;


@Service
public class OrganizationServiceImpl implements OrganizationService {


    private final OrganizationMapper organizationMapper;
    private final ExceptionAuditRepository exceptionAuditRepository;
    private static final Logger log = LoggerFactory.getLogger(OrganizationServiceImpl.class);
    private final UserMapper userMapper;

    @Autowired
    private ResponseProperties status;

    @Autowired
    private GenericHandler genericHandler;

//    @Autowired
//    private AuditRepository auditRepository;

    @Autowired
    private SafeAuditService safeAuditService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    // Other mappers can be added as needed
    public OrganizationServiceImpl(OrganizationMapper organizationMapper,
                                   ExceptionAuditRepository exceptionAuditRepository,
                                   UserMapper userMapper) {
        this.organizationMapper = organizationMapper;
        this.exceptionAuditRepository = exceptionAuditRepository;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getOrganizationById(UUID id) {
        try {
            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            Organization result = organizationMapper.getOrganizationById(id);

            if(result == null){
                throw new GlobalExceptionHandler.NotFoundException("Organization not found");
            }
            if (result.getImage() != null) {
                // Convert relative path to full URL
                String fullUrl = baseUrl + result.getImage();
                result.setImage(fullUrl);
            }

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Organization "+status.getDesc(),
                    result
            );

        } catch (Exception exception) {
            log.error("Error fetching organization {}: {}", id, exception.getMessage(), exception);
            genericHandler.logIncidentReport("Fetching organization service failed");
            genericHandler.logAndSaveException(exception, "fetching organization");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> updateOrganization(Organization organization) {

        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel um = handleUserValidation();
            UUID nodeId = um.getNodeInfo().getNodeId();
            String nodeType = um.getNodeInfo().getType();

            HandlePermission.perm(nodeType);

            Organization originalData = organizationMapper.getOrganizationByIdForUpdate(um.getOrgId());
//            if (originalData.getBusinessName().equalsIgnoreCase(organization.getBusinessName())) {
//                throw new GlobalExceptionHandler.NotFoundException("Organization ("+organization.getBusinessName()+") "+status.getExistDesc());
//            }

            organizationMapper.updateOrganizationSelective(organization);

            Organization updatedData = organizationMapper.getOrganizationById(um.getOrgId());

            Map<String, Map<String, String>> changes = new HashMap<>();

            addChangeIfDifferent("businessName", originalData.getBusinessName(), updatedData.getBusinessName(), changes);
            addChangeIfDifferent("businessType", originalData.getPostalCode(), updatedData.getPostalCode(), changes);
            addChangeIfDifferent("registrationNumber", originalData.getAddress(), updatedData.getAddress(), changes);
            addChangeIfDifferent("country", originalData.getCountry(), updatedData.getCountry(), changes);
            addChangeIfDifferent("state", originalData.getState(), updatedData.getState(), changes);
            addChangeIfDifferent("city", originalData.getCity(), updatedData.getCity(), changes);

            AuditLog auditLog = buildAuditLog(um, "Editing organization", "organization", organization, metadata);
            safeAuditService.saveAudit(auditLog);
            return ResponseMap.response(status.getSuccessCode(), "Organization updated Successfully", "");

        } catch (Exception exception) {
            log.error("Error updating organization: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Editing organization service failed");
            genericHandler.logAndSaveException(exception, "editing organization");
            throw exception;
        }
    }

    private AuditLog buildAuditLog(UserModel creator, String description, String type, Organization createdEntity, Map<String, String> metadata) {
        AuditLog log = new AuditLog();
        log.setCreator(creator);
        log.setDescription(description);
        log.setType(type);
        log.setOrganization(createdEntity);
        log.setIpAddress(metadata.get("ipAddress"));
        log.setUserAgent(metadata.get("userAgent"));
        log.setEndpoint(metadata.get("endpoint"));
        log.setHttpMethod(metadata.get("httpMethod"));
        return log;
    }

    private void addChangeIfDifferent(String fieldName, String oldValue, String newValue,
                                      Map<String, Map<String, String>> changes) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.put(fieldName, Map.of(
                    "old", oldValue != null ? oldValue : "null",
                    "new", newValue != null ? newValue : "null"
            ));
        }
    }
}
