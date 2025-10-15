package org.memmcol.gridflexbackendservice.service.node;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.mapper.NodeMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.node.*;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.memmcol.gridflexbackendservice.components.handleValidUser.handleUserValidation;

@Service
public class NodeServiceImpl implements NodeService {
    private static final Logger log = LoggerFactory.getLogger(NodeServiceImpl.class);

    @Autowired
    private NodeMapper nodeMapper;

    @Autowired
    private ResponseProperties status;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private GenericHandler genericHandler;

    private final IMap<String, Object> nodeCache;

    private final IMap<String, Object> auditCache;

    public NodeServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.nodeCache = hazelcastInstance.getMap("nodeCache");
        this.auditCache = hazelcastInstance.getMap("auditCache");
    }

    @Transactional
    @Override
    public Map<String, Object> createRegionBhubServiceCenterNode(RegionBhubServiceCenter request) {
        RegionBhubServiceCenter regionBhubServiceCenter;
        UUID id;
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            String desc;
            UserModel um = handleUserValidation();

            Node node = new Node();
            node.setName(request.getName());
            node.setOrgId(um.getOrgId());
            node.setParentId(request.getParentId());

            Node nd = nodeMapper.isNodeExist(request.getParentId());

            if(nd == null) {
                throw new GlobalExceptionHandler.NotFoundException("Parent node does not exist");
            }
            nodeMapper.createNode(node);

            UUID nodeId = node.getId();
            UUID parentNodeId = node.getParentId();

            request.setNodeId(nodeId);
            request.setOrgId(um.getOrgId());
            request.setParentId(parentNodeId);

            if(request.getType().equalsIgnoreCase("region") ||
                    request.getType().equalsIgnoreCase("business hub") ||
                    request.getType().equalsIgnoreCase("service center")){
                nodeMapper.createRegionBhubServiceCenter(request);
                id = request.getId();
                regionBhubServiceCenter = nodeMapper.getRegionBhubServiceCenter(id);
                desc = regionBhubServiceCenter.getName() + "newly created";
            } else {
                throw new GlobalExceptionHandler.NotFoundException("Request type " +" ("+ request.getType()+" )"+ " not found");
            }
            handleClearCache(node);
            AuditLog auditLog = buildAuditLog(um, desc, request.getType().equals("region") ? "region" : request.getType().equals("service center") ? "service center" : "business hub", regionBhubServiceCenter, metadata);
            auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ regionBhubServiceCenter.getName() +"' "+ status.getRegDesc(), "");

        } catch (Exception exception) {
            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Creating region business hub service failed");
            genericHandler.logAndSaveException(exception, "creating region business hub");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> createSubStationFeederLineTransformerNode(SubStationTransformerFeederLine request) {
        SubStationTransformerFeederLine subStationTransformerFeederLine;
        UUID id;
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            String desc;
            UserModel um = handleUserValidation();

            Node node = new Node();
            node.setName(request.getName());
            node.setOrgId(um.getOrgId());
            node.setParentId(request.getParentId());

            Node nd = nodeMapper.isNodeExist(request.getParentId());

            if(nd == null) {
                throw new GlobalExceptionHandler.NotFoundException("Parent node does not exist");
            }
            nodeMapper.createNode(node);

            UUID nodeId = node.getId();
            UUID parentNodeId = node.getParentId();

            request.setNodeId(nodeId);
            request.setOrgId(um.getOrgId());
            request.setParentId(parentNodeId);

            if(request.getType().equalsIgnoreCase("dss") ||
                    request.getType().equalsIgnoreCase("feeder line") ||
                    request.getType().equalsIgnoreCase("substation")){
                nodeMapper.createSubStationTransformerFeederLine(request);
                id = request.getId();
                subStationTransformerFeederLine = nodeMapper.getSubStationTransformerFeederLine(id);
                desc = subStationTransformerFeederLine.getName() + "newly created";
            } else {
                throw new GlobalExceptionHandler.NotFoundException("Request type " +" ("+ request.getType()+" )"+ " not found");
            }

            handleClearCache(node);

            AuditLog auditLog = buildAuditLog(um, desc, request.getType().equals("dss") ? "dss" : request.getType().equals("feeder line") ? "feeder line" : "substation", subStationTransformerFeederLine, metadata);
            auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ subStationTransformerFeederLine.getName() +"' "+ status.getRegDesc(), "");

        } catch (Exception exception) {
            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Creating substation/feeder service failed");
            genericHandler.logAndSaveException(exception, "creating substation/feeder line node");

            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> updateRegionBhubServiceCenterNode(RegionBhubServiceCenter request) {
        RegionBhubServiceCenter regionBhubServiceCenter;
        UUID id;
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            String desc;
            UserModel um = handleUserValidation();

            Node node = new Node();
            node.setId(request.getNodeId());
            node.setName(request.getName());
            node.setOrgId(um.getOrgId());
            node.setParentId(request.getParentId());

            Node nd = nodeMapper.isNodeExist(request.getNodeId());

            if(nd == null) {
                throw new GlobalExceptionHandler.NotFoundException("Node does not exist");
            }
            nodeMapper.updateNode(node);

            UUID nodeId = node.getId();

            request.setNodeId(nodeId);
            request.setOrgId(um.getOrgId());

            if(request.getType().equalsIgnoreCase("region") ||
                    request.getType().equalsIgnoreCase("business hub") ||
                    request.getType().equalsIgnoreCase("service center")) {
                nodeMapper.updateRegionBhubServiceCenter(request);
                id = request.getId();
                regionBhubServiceCenter = nodeMapper.getRegionBhubServiceCenter(id);
                desc = regionBhubServiceCenter.getName()  + "edited";
            }  else {
                throw new GlobalExceptionHandler.NotFoundException("Request type " +" ("+ request.getType()+" )"+ " not found");
            }

            handleClearCache(node);

            AuditLog auditLog = buildAuditLog(um, desc, request.getType().equals("region") ? "Region" : request.getType().equals("service center") ? "Service center" : "Business hub", regionBhubServiceCenter, metadata);
            auditRepository.save(auditLog);
//            auditNotificationDTO.setCreator(um);
//            auditNotificationDTO.setDescription(desc);
//            auditNotificationDTO.setType(request.getType().equals("region") ? "Region" : request.getType().equals("service center") ? "Service center" : "Business hub");
//            auditNotificationDTO.setUserAgent(userAgent);
//            auditNotificationDTO.setIpAddress(ipAddress);
//            auditNotificationDTO.setRegionBhubServiceCenter(regionBhubServiceCenter);
//            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ regionBhubServiceCenter.getName() +"' "+ status.getUpdateDesc(), "");

        } catch (Exception exception) {
            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Editing region business hub service failed");
            genericHandler.logAndSaveException(exception, "editing region business node");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> updateSubStationFeederLineTransformerNode(SubStationTransformerFeederLine request) {
        SubStationTransformerFeederLine subStationTransformerFeederLine;
        UUID id;
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            String desc;
            UserModel um = handleUserValidation();

            Node node = new Node();
            node.setId(request.getNodeId());
            node.setName(request.getName());
            node.setOrgId(um.getOrgId());
            node.setParentId(request.getParentId());

            Node nd = nodeMapper.isNodeExist(request.getNodeId());

            if(nd == null) {
                throw new GlobalExceptionHandler.NotFoundException("Parent node does not exist");
            }
            nodeMapper.updateNode(node);

            UUID nodeId = node.getId();

            request.setNodeId(nodeId);
            request.setOrgId(um.getOrgId());

            if(request.getType().equalsIgnoreCase("dss") ||
                    request.getType().equalsIgnoreCase("feeder line") ||
                    request.getType().equalsIgnoreCase("substation")){
                nodeMapper.updateSubStationTransformerFeederLine(request);
                id = request.getId();
                subStationTransformerFeederLine = nodeMapper.getSubStationTransformerFeederLine(id);
                desc = subStationTransformerFeederLine.getName()  + "edited";
            } else {
                throw new GlobalExceptionHandler.NotFoundException("Request type " +" ("+ request.getType()+" )"+ " not found");
            }

            handleClearCache(node);

            AuditLog auditLog = buildAuditLog(um, desc, request.getType().equalsIgnoreCase("transformer") ? "Transformer" : request.getType().equalsIgnoreCase("feeder line") ? "Feeder line" : "Substation", subStationTransformerFeederLine, metadata);
            auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ subStationTransformerFeederLine.getName() +"' "+ status.getUpdateDesc(), "");

        } catch (Exception exception) {
            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Editing substation/feeder line service failed");
            genericHandler.logAndSaveException(exception, "editing substation/feeder line node");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> singleNode(UUID nodeId) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        try {
            UserModel um = handleUserValidation();
//
            Object cachedUser = nodeCache.get(nodeId.toString() + "_" + um.getOrgId());
            if (cachedUser != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached Node " + status.getDesc(), cachedUser);
            }

            List<Node> flatList = nodeMapper.getNodeWithChildren(nodeId, um.getOrgId());
            if (flatList == null || flatList.isEmpty()) {
                return ResponseMap.response(status.getSuccessCode(), "No nodes found", "");
            }

            Map<UUID, Node> nodeMap = new HashMap<>();
            Node root = null;

            for (Node node : flatList) {
                node.setNodesTree(new ArrayList<>());
                nodeMap.put(node.getId(), node);
            }

            for (Node node : flatList) {
                if (node.getId().equals(nodeId)) {
                    root = node; // this is the node we're querying for
                }
                if (node.getParentId() != null && nodeMap.containsKey(node.getParentId())) {
                    Node parent = nodeMap.get(node.getParentId());
                    parent.getNodesTree().add(node);
                }
            }

            assert root != null;
            handleAddCache(root);
            return ResponseMap.response(status.getSuccessCode(), "Node " + status.getDesc(), root);

        } catch (Exception exception) {
            log.error("Error occurred while fetching node [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Fetching node service failed");
            genericHandler.logAndSaveException(exception, "fetching single node");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getAllNodes() {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {

            UserModel um = handleUserValidation();

            StringBuilder cacheKeyBuilder = new StringBuilder("nodes_"+um.getOrgId());
            String cacheKey = cacheKeyBuilder.toString();

//             Return from cache if available
            Object cachedNode = nodeCache.get(cacheKey);
            if (cachedNode != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached Nodes " + status.getDesc(), cachedNode);
            }

            List<Node> flatList =  nodeMapper.getAllNode(um.getOrgId());
            if(flatList == null || flatList.isEmpty()){
                return ResponseMap.response(status.getSuccessCode(), status.getDesc(), flatList);
            }
            Map<UUID, Node> nodeMap = new HashMap<>();
            List<Node> roots = new ArrayList<>();

            // Map nodes by ID
            for (Node node : flatList) {
                nodeMap.put(node.getId(), node);
                node.setNodesTree(new ArrayList<>()); // Initialize children list
            }

            // Reconstruct the tree
            for (Node node : flatList) {
                if (node.getParentId() == null) {
                    roots.add(node); // Add root nodes to the list
                } else {
                    Node parent = nodeMap.get(node.getParentId());
                    if (parent != null) {
                        parent.getNodesTree().add(node); // Add as a child to the parent
                    }
                }
            }

            nodeCache.put(cacheKey, roots);

            return ResponseMap.response(status.getSuccessCode(),  "Node "+status.getDesc(), roots);
        } catch (Exception exception) {
            log.error("Error occurred while updated node [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Fetching all node service failed");
            genericHandler.logAndSaveException(exception, "fetching all node");
            throw exception;
        }
    }

    @Override
    public Map<String, Object> getBusinessHubByOrgId(UUID orgId) {
        try {
            UserModel um = handleUserValidation();
            UUID operatorOrgId = um.getOrgId();
            if (!operatorOrgId.equals(orgId)) {
                return ResponseMap.response(status.getFailCode(), "Unauthorized access", null);
            }
            List<RegionBhubServiceCenter> result = nodeMapper.getBhubByOrgId(orgId);
            return ResponseMap.response(status.getSuccessCode(),  status.getDesc(), result);
        }catch (Exception exception) {
            log.error("Error occurred while fetching business hub [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Fetching all business hub service failed");
            genericHandler.logAndSaveException(exception, "fetching all business hub");
            throw exception;
        }
    }

    private AuditLog buildAuditLog(UserModel creator, String description, String type, Object createdEntity, Map<String, String> metadata) {
        AuditLog log = new AuditLog();
        log.setCreator(creator);
        log.setDescription(description);
        log.setType(type);
        log.setRegionBhubServiceCenter(createdEntity instanceof RegionBhubServiceCenter ? (RegionBhubServiceCenter) createdEntity : null);
        log.setSubStationTransformerFeederLine(createdEntity instanceof SubStationTransformerFeederLine ? (SubStationTransformerFeederLine) createdEntity : null);
        log.setIpAddress(metadata.get("ipAddress"));
        log.setUserAgent(metadata.get("userAgent"));
        log.setEndpoint(metadata.get("endpoint"));
        log.setHttpMethod(metadata.get("httpMethod"));
        return log;
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

    private void handleClearCache(Node node) {
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
    }

}