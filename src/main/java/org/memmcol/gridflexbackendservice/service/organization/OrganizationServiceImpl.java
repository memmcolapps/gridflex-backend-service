package org.memmcol.gridflexbackendservice.service.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.apache.commons.lang.RandomStringUtils;
import org.memmcol.gridflexbackendservice.mapper.NodeMapper;
import org.memmcol.gridflexbackendservice.mapper.OrganizationMapper;
import org.memmcol.gridflexbackendservice.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.node.Node;
import org.memmcol.gridflexbackendservice.model.user.*;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.util.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;


import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.memmcol.gridflexbackendservice.util.GenericHandler.capitalizeFirstLetter;


@Service
@Transactional
public class OrganizationServiceImpl implements OrganizationService {


    private final OrganizationMapper organizationMapper;
    private final ExceptionAuditRepository exceptionAuditRepository;
    private static final Logger log = LoggerFactory.getLogger(OrganizationServiceImpl.class);
    private final UserMapper userMapper;

    @Autowired
    private ResponseProperties status;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Other mappers can be added as needed
    public OrganizationServiceImpl(OrganizationMapper organizationMapper,
                                   ExceptionAuditRepository exceptionAuditRepository,
                                   UserMapper userMapper) {
        this.organizationMapper = organizationMapper;
        this.exceptionAuditRepository = exceptionAuditRepository;
        this.userMapper = userMapper;
    }

    @Override
    public Map<String, Object> addOrganization(Organization organization) {

        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();

        try {

            // Save to database
            organizationMapper.insertOrganization(organization);
            UUID orgId = organization.getId();
            String name = organization.getBusinessName();
            // Create root node
            Map<String, Object> rootNodeResponse = creatRootNode(orgId, name);
            UUID rootNodeId = (UUID) ((Map<?, ?>) rootNodeResponse.get("data")).get("id");

            // Create Permissions
            createDefaultPermission(orgId);
            // Create Group
            createDefaultGroup(orgId);
            // Create Group Permissions
            createDefaultGroupPermission(orgId);
            // Create Default User
            createDefaultUser(orgId, rootNodeId);

            return ResponseMap.response(
                    status.getSuccessCode(),
                    (organization.getBusinessName() + " Organization Created " + " Successfully"),
                    "");

        } catch (Exception exception) {
            log.error("Error creating organization: {}", exception.getMessage(), exception);

            // Log exception to audit system
            errorLog.setDescription("Error creating organization");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

//            throw new RuntimeException("Error creating organization: " + exception.getMessage(), exception);
            return ResponseMap.response(
                    status.getFailCode(),
                    "Failed to create " + organization.getBusinessName() + " Organization",
                    exception.getMessage()
            );
        }
    }


    @Override
    public Map<String, Object> creatRootNode(UUID organizationId, String name) {

        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
        Map<String, Object> response = new HashMap<>();
        Node rootNode = new Node();

        try {

            rootNode.setName(name);
            rootNode.setOrgId(organizationId);

            organizationMapper.insertNodes(rootNode);

            Node savedNode = organizationMapper.getNodeByNameAndOrgId(name, organizationId);

            response.put("success", true);
            response.put("message", "Root Node created successfully");
            response.put("data", Map.of(
                    "id", savedNode.getId(),
                    "name", savedNode.getName()
            ));
            return response;
        } catch (Exception exception) {
            log.error("Error adding node: {}", exception.getMessage(), exception);

            errorLog.setDescription("Error creating Root Node");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            throw new RuntimeException("Error creating Root Node: " + exception.getMessage(), exception);
        }
    }

    @Override
    public Map<String, Object> createDefaultUser(UUID organizationId, UUID nodeId) {

        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
        Map<String, Object> response = new HashMap<>();
        UserModel user = new UserModel();

        try {

            Organization organization = organizationMapper.getOrganizationById(organizationId)
                    .orElseThrow(()-> new RuntimeException("Organization not found with ID: " + organizationId));

            user.setOrgId(organizationId);
            user.setFirstname("Admin");
            user.setLastname("Admin");
            user.setEmail(organization.getEmail());
            user.setNodeId(nodeId);
            user.setStatus(true);
            user.setActive(true);

            user.setPassword(passwordEncoder.encode("Passw@rd0951"));

            organizationMapper.insertUser(user);


            return response;

        } catch (Exception exception) {
            log.error("Error creating default user: {}", exception.getMessage(), exception);

            // Log error details
            errorLog.setDescription("Error creating default user");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            throw new RuntimeException("Error creating user: " + exception.getMessage(), exception);

        }

    }

    @Override
    public Map<String, Object> createDefaultPermission(UUID organizationId) {

        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
        Map<String, Object> response = new HashMap<>();

        try {
            Permission permission = new Permission();

            permission.setView(true);
            permission.setEdit(true);
            permission.setApprove(true);
            permission.setDisable(true);
            permission.setOrgId(organizationId);

            organizationMapper.insertPermission(permission);

            response.put("success", true);
            response.put("message", "Permission created successfully");
            response.put("data", permission);
            return response;

        } catch (Exception exception) {
            log.error("Error creating default Permission: {}", exception.getMessage(), exception);

            // Log error details
            errorLog.setDescription("Error creating default Permission");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            throw new RuntimeException("Error creating Permission: " + exception.getMessage(), exception);
        }
    }

    @Override
    public Map<String, Object> createDefaultGroup(UUID organizationId) {

        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
        Map<String, Object> response = new HashMap<>();

        try {
            Group group = new Group();

            group.setGroupTitle("Super Admin");
            group.setOrgId(organizationId);

            organizationMapper.insertGroup(group);

            response.put("success", true);
            response.put("message", "Default Group created successfully");
            response.put("data", group);
            return response;

        } catch (Exception exception) {
            log.error("Error creating default Group: {}", exception.getMessage(), exception);

            // Log error details
            errorLog.setDescription("Error creating default Group");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            throw new RuntimeException("Error creating default Group: " + exception.getMessage(), exception);

        }
    }

    @Override
    public Map<String, Object> createDefaultGroupPermission(UUID organizationId) {

        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
        Map<String, Object> response = new HashMap<>();

        try {

            Permission permission = organizationMapper.getPermissionByOrgId(organizationId);
            Group group = organizationMapper.getGroupByOrgId(organizationId);

            organizationMapper.insertGroupPermission(group.getId(), permission.getId(), organizationId);

            response.put("success", true);
            response.put("message", "Default Group Permission created successfully");
            return response;

        } catch (Exception exception) {
            log.error("Error creating default Group Permission: {}", exception.getMessage(), exception);

            // Log error details
            errorLog.setDescription("Error creating default Group Permission");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            throw new RuntimeException("Error creating Group Permission: " + exception.getMessage(), exception);
        }
    }

    @Override
    public Map<String, Object> getOrganization(int page, int size) {
        try {

            // Calculate offset
            int offset = page * size;

            // Get paginated data
            List<Organization> organizations = organizationMapper.getOrganizations(size, offset);

            long totalCount = organizationMapper.getOrganizationCount();
            int totalPages = (int) Math.ceil((double) totalCount / size);

            Map<String, Object> paginationData = new HashMap<>();
            paginationData.put("content", organizations);
            paginationData.put("pageNumber", page);
            paginationData.put("pageSize", size);
            paginationData.put("totalElements", totalCount);
            paginationData.put("totalPages", totalPages);
            paginationData.put("isFirst", page == 0);
            paginationData.put("isLast", (offset + size) >= totalCount);

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Organizations retrieved successfully",
                    paginationData
            );

        } catch (Exception exception) {
            log.error("Error fetching organizations - Page: {}, Size: {}: {}",
                    page, size, exception.getMessage(), exception);

            ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
            errorLog.setDescription("Error fetching organizations");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            return ResponseMap.response(
                    status.getFailCode(),
                    "Failed to fetch organizations",
                    Map.of(
                            "error", exception.getMessage(),
                            "page", page,
                            "size", size
                    )
            );
        }
    }

    @Override
    public Map<String, Object> getOrganizationById(UUID id) {
        try {
            Optional<Organization> result = organizationMapper.getOrganizationById(id);

            if (result.isEmpty()) {
                return ResponseMap.response(
                        status.getNotFoundCode(),
                        "Organization not found with ID: " + id,
                        Map.of("organizationId", id)
                );
            }

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Organization retrieved successfully",
                    result
            );

        } catch (Exception exception) {
            log.error("Error fetching organization {}: {}", id, exception.getMessage(), exception);

            ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
            errorLog.setDescription("Error fetching organization");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            return ResponseMap.response(
                    status.getFailCode(),
                    "Failed to fetch organization",
                    Map.of(
                            "error", exception.getMessage(),
                            "organizationId", id
                    )
            );
        }
    }

    @Override
    public Map<String, Object> updateOrganization(Organization organization, UUID orgId) {

        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
        try {
            Organization originalData = organizationMapper.getOrganizationById(orgId)
                    .orElseThrow(()-> new RuntimeException("Organization not found with ID: " + orgId));

            organization.setId(orgId);
            organizationMapper.updateOrganizationSelective(organization);

            Organization updatedData = organizationMapper.getOrganizationById(orgId)
                    .orElseThrow(()-> new RuntimeException("Organization not found with ID: " + orgId));

            Map<String, Map<String, String>> changes = new HashMap<>();

            addChangeIfDifferent("businessName", originalData.getBusinessName(), updatedData.getBusinessName(), changes);
            addChangeIfDifferent("businessContact", originalData.getBusinessContact(), updatedData.getBusinessContact(), changes);
            addChangeIfDifferent("businessType", originalData.getBusinessType(), updatedData.getBusinessType(), changes);
            addChangeIfDifferent("registrationNumber", originalData.getRegistrationNumber(), updatedData.getRegistrationNumber(), changes);
            addChangeIfDifferent("country", originalData.getCountry(), updatedData.getCountry(), changes);
            addChangeIfDifferent("state", originalData.getState(), updatedData.getState(), changes);
            addChangeIfDifferent("city", originalData.getCity(), updatedData.getCity(), changes);
            addChangeIfDifferent("email", originalData.getEmail(), updatedData.getEmail(), changes);


            return ResponseMap.response(status.getSuccessCode(),
                    "Organization updated Successfully",
                    "");

        } catch (Exception exception) {
            log.error("Error updating organization: {}", exception.getMessage(), exception);

            // Log error details
            errorLog.setDescription("Error updating organization");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);
            return ResponseMap.response(
                    status.getFailCode(),
                    "Failed to update organization",
                    exception.getMessage());
        }

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
