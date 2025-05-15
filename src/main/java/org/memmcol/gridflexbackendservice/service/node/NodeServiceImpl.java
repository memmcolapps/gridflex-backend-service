package org.memmcol.gridflexbackendservice.service.node;

import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.NodeMapper;
import org.memmcol.gridflexbackendservice.mapper.UserMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.node.*;
import org.memmcol.gridflexbackendservice.model.user.CustomUserPrincipal;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.service.user.UserServiceImpl;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.util.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

//@Transactional
@Service
public class NodeServiceImpl implements NodeService {
    private static final Logger log = LoggerFactory.getLogger(NodeServiceImpl.class);

    @Autowired
    private NodeMapper nodeMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ResponseProperties status;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private AuthMapper operatorMapper;

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;
    @Override
    public Map<String, Object> createBusinessHubNode(BusinessHub request) {
        return Map.of();
    }

    @Override
    public Map<String, Object> createSubStationNode(SubStation request) {
        return Map.of();
    }

    @Override
    public Map<String, Object> createFeederLineNode(FeederLine request) {
        return Map.of();
    }

    @Override
    public Map<String, Object> createRegionNode(Region request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            UserModel um = handleUserValidation();

            nodeMapper.isNodeExist(request.getParentNodeId());

            nodeMapper.createNode(request.getName(), request.getParentNodeId());

            Node n = nodeMapper.getNode(request.getName());

            Long nodeId = n.getId();

            request.setNodeId(nodeId);

            nodeMapper.createRegion(request);


            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ request.getName() +"' "+ status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to creating node");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }

    }

    @Override
    public Map<String, Object> createTransformerNode(Transformer request) {
        return Map.of();
    }

    @Override
    public Map<String, Object> updateBusinessHubNode(BusinessHub request) {
        return Map.of();
    }

    @Override
    public Map<String, Object> updateSubStationNode(SubStation request) {
        return Map.of();
    }

    @Override
    public Map<String, Object> updateFeederLineNode(FeederLine request) {
        return Map.of();
    }

    @Override
    public Map<String, Object> updateRegionNode(Region request) {
        return Map.of();
    }

    @Override
    public Map<String, Object> updateTransformerNode(Transformer request) {
        return Map.of();
    }

    @Override
    public Map<String, Object> singleNodes(Long nodeId) {
        return Map.of();
    }

    @Override
    public Map<String, Object> getAllNodes() {
        return Map.of();
    }


    UserModel handleUserValidation() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = "Unknown";

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal) {
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            username = principal.getUsername();  // or principal.getEmail() if you named it that way
        }

        UserModel isOperatorExist = operatorMapper.findAuthByUserEmail(username);

        return isOperatorExist;
    }

}
