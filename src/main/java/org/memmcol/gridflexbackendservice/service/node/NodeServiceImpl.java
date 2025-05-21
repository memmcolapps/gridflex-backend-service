package org.memmcol.gridflexbackendservice.service.node;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.mapper.NodeMapper;
import org.memmcol.gridflexbackendservice.mapper.UserMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.node.*;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Transactional
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

    private final IMap<String, Object> nodeCache;

    private final IMap<String, Object> auditCache;

    public NodeServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.nodeCache = hazelcastInstance.getMap("node-Cache");
        this.auditCache = hazelcastInstance.getMap("audit-Cache");
    }

    @Override
    public Map<String, Object> createBusinessHubNode(BusinessHub request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            UserModel um = handleUserValidation();

            Node node = new Node();
            node.setName(request.getName());
            node.setOrgId(um.getOrgId());
            node.setParentId(request.getParentId());

            Node nd = nodeMapper.isNodeExist(request.getParentId());

            if(nd == null) {
                throw new GlobalExceptionHandler.NotFoundException("parent node does not exist");
            }
            nodeMapper.createNode(node);

            UUID nodeId = node.getId();

            request.setNodeId(nodeId);
            request.setOrgId(um.getOrgId());
            nodeMapper.createBusinessHub(request);

            UUID id = request.getId();

            BusinessHub businessHub = nodeMapper.getBusinessNode(id);

            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription("Created node [" + businessHub.getName() + "]");
            auditNotificationDTO.setType("businessHub");
            auditNotificationDTO.setBusinessHub(businessHub);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ request.getName() +"' "+ status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to creating region node");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> createSubStationNode(SubStation request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            UserModel um = handleUserValidation();

            Node node = new Node();
            node.setName(request.getName());
            node.setOrgId(um.getOrgId());
            node.setParentId(request.getParentId());

            Node nd = nodeMapper.isNodeExist(request.getParentId());

            if(nd == null) {
                throw new GlobalExceptionHandler.NotFoundException("parent node does not exist");
            }

            nodeMapper.createNode(node);

            UUID nodeId = node.getId();

            request.setNodeId(nodeId);
            request.setOrgId(um.getOrgId());
            nodeMapper.createSubStation(request);

            UUID id = request.getId();

            SubStation subStation = nodeMapper.getSubStationNode(id);

            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription("Created node [" + subStation.getName() + "]");
            auditNotificationDTO.setType("substation");
            auditNotificationDTO.setSubStation(subStation);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ request.getName() +"' "+ status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to creating region node");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> createFeederLineNode(FeederLine request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            UserModel um = handleUserValidation();

            Node node = new Node();
            node.setName(request.getName());
            node.setOrgId(um.getOrgId());
            node.setParentId(request.getParentId());

            Node nd = nodeMapper.isNodeExist(request.getParentId());

            if(nd == null) {
                throw new GlobalExceptionHandler.NotFoundException("parent node does not exist");
            }

            nodeMapper.createNode(node);
//            assert nd != null;
            UUID nodeId = node.getId();

            request.setNodeId(nodeId);
            request.setOrgId(um.getOrgId());
            nodeMapper.createFeederLine(request);

            UUID id = request.getId();

            FeederLine feederLine = nodeMapper.getFeederLineNode(id);

            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription("Created node [" + feederLine.getName() + "]");
            auditNotificationDTO.setType("feederLine");
            auditNotificationDTO.setFeederLine(feederLine);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ request.getName() +"' "+ status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to creating region node");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> createRegionNode(Region request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            UserModel um = handleUserValidation();

            Node node = new Node();
            node.setName(request.getName());
            node.setOrgId(um.getOrgId());
            node.setParentId(request.getParentId());

            Node nd = nodeMapper.isNodeExist(request.getParentId());

            if(nd == null) {
                throw new GlobalExceptionHandler.NotFoundException("parent node does not exist");
            }

            nodeMapper.createNode(node);

            UUID nodeId = node.getId();
            request.setNodeId(nodeId);
            request.setOrgId(um.getOrgId());
            nodeMapper.createRegion(request);

            UUID id = request.getId();

            Region region = nodeMapper.getRegionNode(id);

            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription("Created node [" + region.getName() + "]");
            auditNotificationDTO.setType("region");
            auditNotificationDTO.setRegion(region);
            auditRepository.save(auditNotificationDTO);


            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ region.getName() +"' "+ status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to creating region node");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }

    }

    @Override
    public Map<String, Object> createTransformerNode(Transformer request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            UserModel um = handleUserValidation();

            Node node = new Node();
            node.setName(request.getName());
            node.setOrgId(um.getOrgId());
            node.setParentId(request.getParentId());

            Node nd = nodeMapper.isNodeExist(request.getParentId());

            if(nd == null) {
                throw new GlobalExceptionHandler.NotFoundException("parent node not found");
            }

            nodeMapper.createNode(node);

            UUID nodeId = node.getId();

            request.setNodeId(nodeId);

            request.setOrgId(um.getOrgId());

            nodeMapper.createTransformer(request);

            UUID id = request.getId();

            Transformer transformer = nodeMapper.getTransformerNode(id);

            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription("Created node [" + transformer.getName() + "]");
            auditNotificationDTO.setType("transformer");
            auditNotificationDTO.setTransformer(transformer);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ request.getName() +"' "+ status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to creating region node");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> updateBusinessHubNode(BusinessHub request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            UserModel um = handleUserValidation();

            request.setOrgId(um.getOrgId());
            nodeMapper.updateBusinessHub(request);

            UUID id = request.getId();

            BusinessHub businessHub = nodeMapper.getBusinessNode(id);

            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription("Updated node [" + businessHub.getName() + "]");
            auditNotificationDTO.setType("businessHub");
            auditNotificationDTO.setBusinessHub(businessHub);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ request.getName() +"' "+ status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while updated node [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to creating region node");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> updateSubStationNode(SubStation request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            UserModel um = handleUserValidation();

            request.setOrgId(um.getOrgId());
            nodeMapper.updateSubstation(request);

            UUID id = request.getId();

            SubStation subStation = nodeMapper.getSubStationNode(id);

            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription("Updated node [" + subStation.getName() + "]");
            auditNotificationDTO.setType("substation");
            auditNotificationDTO.setSubStation(subStation);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ request.getName() +"' "+ status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while updated node [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to creating region node");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> updateFeederLineNode(FeederLine request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            UserModel um = handleUserValidation();

            request.setOrgId(um.getOrgId());
            nodeMapper.updateFeederLine(request);

            UUID id = request.getId();

            FeederLine feederLine = nodeMapper.getFeederLineNode(id);

            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription("Updated node [" + feederLine.getName() + "]");
            auditNotificationDTO.setType("feederLine");
            auditNotificationDTO.setFeederLine(feederLine);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ request.getName() +"' "+ status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while updated node [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to creating region node");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> updateRegionNode(Region request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            UserModel um = handleUserValidation();

            request.setOrgId(um.getOrgId());
            nodeMapper.updateRegionNode(request);

            UUID id = request.getId();

            Region region = nodeMapper.getRegionNode(id);

            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription("Updated node [" + region.getName() + "]");
            auditNotificationDTO.setType("region");
            auditNotificationDTO.setRegion(region);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ request.getName() +"' "+ status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while updated node [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to creating region node");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> updateTransformerNode(Transformer request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            UserModel um = handleUserValidation();

            request.setOrgId(um.getOrgId());
            nodeMapper.updateTransformerNode(request);

            UUID id = request.getId();

            Transformer transformer = nodeMapper.getTransformerNode(id);

            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription("Updated node [" + transformer.getName() + "]");
            auditNotificationDTO.setType("transformer");
            auditNotificationDTO.setTransformer(transformer);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ request.getName() +"' "+ status.getRegDesc(), "");
        } catch (Exception exception) {
            log.error("Error occurred while updated node [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to creating region node");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> singleNode(UUID nodeId) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            UserModel um = handleUserValidation();
            if (!Boolean.TRUE.equals(um.getStatus())) {
                throw new LockedException("User is disabled");
            }

            Object cachedUser = nodeCache.get(nodeId.toString()+"_"+um.getOrgId());

            if (cachedUser != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached Node " + " " + status.getDesc(), cachedUser);
            }

            Node node =  nodeMapper.getSingleNode(nodeId, um.getOrgId());

            handleAddCache(node);

            return ResponseMap.response(status.getSuccessCode(),  "Node "+status.getDesc(), node);
        } catch (Exception exception) {
            log.error("Error occurred while updated node [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to creating region node");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }

    @Override
    public Map<String, Object> getAllNodes() {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            UserModel um = handleUserValidation();
            if (!Boolean.TRUE.equals(um.getStatus())) {
                throw new LockedException("User is disabled");
            }
            StringBuilder cacheKeyBuilder = new StringBuilder("nodes_"+um.getOrgId());
            String cacheKey = cacheKeyBuilder.toString();

            // Return from cache if available
            Object cachedNode = nodeCache.get(cacheKey);
            if (cachedNode != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached Nodes " + status.getDesc(), cachedNode);
            }

            List<Node> node =  nodeMapper.getAllNode(um.getOrgId());
            nodeCache.put(cacheKey, node);

            return ResponseMap.response(status.getSuccessCode(),  "Node "+status.getDesc(), node);
        } catch (Exception exception) {
            log.error("Error occurred while updated node [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while trying to creating region node");
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

        return isOperatorExist;
    }

    private void handleAddCache(Node node) {
        nodeCache.remove(node.getId().toString()+"_"+node.getOrgId());
        for (String key : auditCache.keySet()) {
            if (key.startsWith("grid_flex_audit_log_page_")) {
                auditCache.remove(key);
            }
        }
        for (String key : nodeCache.keySet()) {
            if (key.startsWith("nodes_"+node.getOrgId())) {
                nodeCache.remove(key);
            }
        }
        nodeCache.put(node.getId().toString(), node);  // Cache updated or deleted entity
    }

}
