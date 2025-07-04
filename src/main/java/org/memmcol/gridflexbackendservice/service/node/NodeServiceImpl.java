package org.memmcol.gridflexbackendservice.service.node;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.*;

import static org.memmcol.gridflexbackendservice.util.GenericHandler.getClientIp;
import static org.memmcol.gridflexbackendservice.util.handleValidUser.handleUserValidation;

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

    @Autowired
    private HttpServletRequest httpServletRequest;

    private final IMap<String, Object> nodeCache;

    private final IMap<String, Object> auditCache;

    public NodeServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.nodeCache = hazelcastInstance.getMap("node-Cache");
        this.auditCache = hazelcastInstance.getMap("audit-Cache");
    }

    @Override
    public Map<String, Object> createRegionBhubServiceCenterNode(RegionBhubServiceCenter request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        RegionBhubServiceCenter regionBhubServiceCenter;
        UUID id;
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
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

            request.setNodeId(nodeId);
            request.setOrgId(um.getOrgId());

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

            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setType(request.getType().equals("region") ? "region" : request.getType().equals("service center") ? "service center" : "business hub");
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setRegionBhubServiceCenter(regionBhubServiceCenter);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ regionBhubServiceCenter.getName() +"' "+ status.getRegDesc(), "");

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
    public Map<String, Object> createSubStationFeederLineTransformerNode(SubStationTransformerFeederLine request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        SubStationTransformerFeederLine subStationTransformerFeederLine;
        UUID id;
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
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

            request.setNodeId(nodeId);
            request.setOrgId(um.getOrgId());

            if(request.getType().equalsIgnoreCase("transformer") ||
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

            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setType(request.getType().equals("transformer") ? "transformer" : request.getType().equals("feeder line") ? "feeder line" : "substation");
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setSubStationTransformerFeederLine(subStationTransformerFeederLine);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ subStationTransformerFeederLine.getName() +"' "+ status.getRegDesc(), "");

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
    public Map<String, Object> updateRegionBhubServiceCenterNode(RegionBhubServiceCenter request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        RegionBhubServiceCenter regionBhubServiceCenter;
        UUID id;
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
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

            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setType(request.getType().equals("region") ? "Region" : request.getType().equals("service center") ? "Service center" : "Business hub");
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setRegionBhubServiceCenter(regionBhubServiceCenter);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ regionBhubServiceCenter.getName() +"' "+ status.getUpdateDesc(), "");

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
    public Map<String, Object> updateSubStationFeederLineTransformerNode(SubStationTransformerFeederLine request) {
        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        SubStationTransformerFeederLine subStationTransformerFeederLine;
        UUID id;
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
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

            if(request.getType().equalsIgnoreCase("transformer") ||
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

            auditNotificationDTO.setCreator(um);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setType(request.getType().equalsIgnoreCase("transformer") ? "Transformer" : request.getType().equalsIgnoreCase("feeder line") ? "Feeder line" : "Substation");
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setSubStationTransformerFeederLine(subStationTransformerFeederLine);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ subStationTransformerFeederLine.getName() +"' "+ status.getUpdateDesc(), "");

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
            exceptionErrorLogs.setDescription("Error occurred while trying to fetch single node with children");
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
            exceptionErrorLogs.setDescription("Error occurred while trying to creating region node");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
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



//    public Map<String, Object> createBusinessHubNode(RegionBhubServiceCenter request) {
//        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
//        AuditLog auditNotificationDTO = new AuditLog();
//        try {
//
//            Node node = new Node();
//            node.setName(request.getName());
//            node.setOrgId(um.getOrgId());
//            node.setParentId(request.getParentId());
//
//            Node nd = nodeMapper.isNodeExist(request.getParentId());
//
//            if(nd == null) {
//                throw new GlobalExceptionHandler.NotFoundException("parent node does not exist");
//            }
//            nodeMapper.createNode(node);
//
//            UUID nodeId = node.getId();
//
//            request.setNodeId(nodeId);
//            request.setOrgId(um.getOrgId());
//            nodeMapper.createBusinessHub(request);
//
//            UUID id = request.getId();
//
//            RegionBhubServiceCenter businessHub = nodeMapper.getBusinessNode(id);
////            handleAddCache(node);
//            auditNotificationDTO.setCreator(um);
//            auditNotificationDTO.setDescription("Created node [" + businessHub.getName() + "]");
//            auditNotificationDTO.setType("businessHub");
//            auditNotificationDTO.setRegionBhubServiceCenter(businessHub);
//            auditRepository.save(auditNotificationDTO);
//
//            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ businessHub.getName() +"' "+ status.getRegDesc(), "");
//        } catch (Exception exception) {
//            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
//            exceptionErrorLogs.setDescription("Error occurred while trying to creating region node");
//            exceptionErrorLogs.setError_message(exception.getMessage().trim());
//            exceptionErrorLogs.setError(exception.toString().trim());
//            exceptionAuditRepository.save(exceptionErrorLogs);
//            throw exception;
//        }
//    }

//    public Map<String, Object> createRegionNode(RegionBhubServiceCenter request) {
//        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
//        AuditLog auditNotificationDTO = new AuditLog();
//        try {
//
//            UserModel um = handleUserValidation();
//
//            Node node = new Node();
//            node.setName(request.getName());
//            node.setOrgId(um.getOrgId());
//            node.setParentId(request.getParentId());
//
//            Node nd = nodeMapper.isNodeExist(request.getParentId());
//
//            if(nd == null) {
//                throw new GlobalExceptionHandler.NotFoundException("parent node does not exist");
//            }
//
//            nodeMapper.createNode(node);
//
//            UUID nodeId = node.getId();
//            request.setNodeId(nodeId);
//            request.setOrgId(um.getOrgId());
//            nodeMapper.createRegion(request);
//
//            UUID id = request.getId();
//
//            RegionBhubServiceCenter region = nodeMapper.getRegionNode(id);
//
//            auditNotificationDTO.setCreator(um);
//            auditNotificationDTO.setDescription("Created node [" + region.getName() + "]");
//            auditNotificationDTO.setType("region");
//            auditNotificationDTO.setRegionBhubServiceCenter(region);
//            auditRepository.save(auditNotificationDTO);
//
//
//            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ region.getName() +"' "+ status.getRegDesc(), "");
//        } catch (Exception exception) {
//            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
//            exceptionErrorLogs.setDescription("Error occurred while trying to creating region node");
//            exceptionErrorLogs.setError_message(exception.getMessage().trim());
//            exceptionErrorLogs.setError(exception.toString().trim());
//            exceptionAuditRepository.save(exceptionErrorLogs);
//            throw exception;
//        }
//
//    }
//
//    public Map<String, Object> createServiceCenterNode(RegionBhubServiceCenter request) {
//        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
//        AuditLog auditNotificationDTO = new AuditLog();
//        try {
//
//            UserModel um = handleUserValidation();
//
//            Node node = new Node();
//            node.setName(request.getName());
//            node.setOrgId(um.getOrgId());
//            node.setParentId(request.getParentId());
//
//            Node nd = nodeMapper.isNodeExist(request.getParentId());
//
//            if(nd == null) {
//                throw new GlobalExceptionHandler.NotFoundException("parent node does not exist");
//            }
//
//            nodeMapper.createNode(node);
//
//            UUID nodeId = node.getId();
//            request.setNodeId(nodeId);
//            request.setOrgId(um.getOrgId());
//            nodeMapper.createRegion(request);
//
//            UUID id = request.getId();
//
//            RegionBhubServiceCenter serviceCenter = nodeMapper.getRegionNode(id);
//
//            auditNotificationDTO.setCreator(um);
//            auditNotificationDTO.setDescription("Created node [" + serviceCenter.getName() + "]");
//            auditNotificationDTO.setType("region");
//            auditNotificationDTO.setRegionBhubServiceCenter(serviceCenter);
//            auditRepository.save(auditNotificationDTO);
//
//
//            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ serviceCenter.getName() +"' "+ status.getRegDesc(), "");
//        } catch (Exception exception) {
//            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
//            exceptionErrorLogs.setDescription("Error occurred while trying to creating region node");
//            exceptionErrorLogs.setError_message(exception.getMessage().trim());
//            exceptionErrorLogs.setError(exception.toString().trim());
//            exceptionAuditRepository.save(exceptionErrorLogs);
//            throw exception;
//        }
//
//    }

//    public Map<String, Object> createSubStationNode(SubStationTransformerFeederLine request) {
//        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
//        AuditLog auditNotificationDTO = new AuditLog();
//        try {
//
//            UserModel um = handleUserValidation();
//
//            Node node = new Node();
//            node.setName(request.getName());
//            node.setOrgId(um.getOrgId());
//            node.setParentId(request.getParentId());
//
//            Node nd = nodeMapper.isNodeExist(request.getParentId());
//
//            if(nd == null) {
//                throw new GlobalExceptionHandler.NotFoundException("parent node does not exist");
//            }
//
//            nodeMapper.createNode(node);
//
//            UUID nodeId = node.getId();
//
//            request.setNodeId(nodeId);
//            request.setOrgId(um.getOrgId());
//            nodeMapper.createSubStation(request);
//
//            UUID id = request.getId();
//
//            SubStationTransformerFeederLine subStation = nodeMapper.getSubStationNode(id);
//
//            auditNotificationDTO.setCreator(um);
//            auditNotificationDTO.setDescription("Created node [" + subStation.getName() + "]");
//            auditNotificationDTO.setType("substation");
//            auditNotificationDTO.setSubStationTransformerFeederLine(subStation);
//            auditRepository.save(auditNotificationDTO);
//
//            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ subStation.getName() +"' "+ status.getRegDesc(), "");
//        } catch (Exception exception) {
//            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
//            exceptionErrorLogs.setDescription("Error occurred while trying to creating region node");
//            exceptionErrorLogs.setError_message(exception.getMessage().trim());
//            exceptionErrorLogs.setError(exception.toString().trim());
//            exceptionAuditRepository.save(exceptionErrorLogs);
//            throw exception;
//        }
//    }
//
//    public Map<String, Object> createFeederLineNode(SubStationTransformerFeederLine request) {
//        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
//        AuditLog auditNotificationDTO = new AuditLog();
//        try {
//
//            UserModel um = handleUserValidation();
//
//            Node node = new Node();
//            node.setName(request.getName());
//            node.setOrgId(um.getOrgId());
//            node.setParentId(request.getParentId());
//
//            Node nd = nodeMapper.isNodeExist(request.getParentId());
//
//            if(nd == null) {
//                throw new GlobalExceptionHandler.NotFoundException("parent node does not exist");
//            }
//
//            nodeMapper.createNode(node);
////            assert nd != null;
//            UUID nodeId = node.getId();
//
//            request.setNodeId(nodeId);
//            request.setOrgId(um.getOrgId());
//            nodeMapper.createFeederLine(request);
//
//            UUID id = request.getId();
//
//            SubStationTransformerFeederLine feederLine = nodeMapper.getFeederLineNode(id);
//
//            auditNotificationDTO.setCreator(um);
//            auditNotificationDTO.setDescription("Created node [" + feederLine.getName() + "]");
//            auditNotificationDTO.setType("feederLine");
//            auditNotificationDTO.setSubStationTransformerFeederLine(feederLine);
//            auditRepository.save(auditNotificationDTO);
//
//            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ feederLine.getName() +"' "+ status.getRegDesc(), "");
//        } catch (Exception exception) {
//            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
//            exceptionErrorLogs.setDescription("Error occurred while trying to creating region node");
//            exceptionErrorLogs.setError_message(exception.getMessage().trim());
//            exceptionErrorLogs.setError(exception.toString().trim());
//            exceptionAuditRepository.save(exceptionErrorLogs);
//            throw exception;
//        }
//    }
//
//    public Map<String, Object> createTransformerNode(SubStationTransformerFeederLine request) {
//        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
//        AuditLog auditNotificationDTO = new AuditLog();
//        try {
//
//            UserModel um = handleUserValidation();
//
//            Node node = new Node();
//            node.setName(request.getName());
//            node.setOrgId(um.getOrgId());
//            node.setParentId(request.getParentId());
//
//            Node nd = nodeMapper.isNodeExist(request.getParentId());
//
//            if(nd == null) {
//                throw new GlobalExceptionHandler.NotFoundException("parent node not found");
//            }
//
//            nodeMapper.createNode(node);
//
//            UUID nodeId = node.getId();
//
//            request.setNodeId(nodeId);
//
//            request.setOrgId(um.getOrgId());
//
//            nodeMapper.createTransformer(request);
//
//            UUID id = request.getId();
//
//            SubStationTransformerFeederLine transformer = nodeMapper.getTransformerNode(id);
//
//            auditNotificationDTO.setCreator(um);
//            auditNotificationDTO.setDescription("Created node [" + transformer.getName() + "]");
//            auditNotificationDTO.setType("transformer");
//            auditNotificationDTO.setSubStationTransformerFeederLine(transformer);
//            auditRepository.save(auditNotificationDTO);
//
//            return ResponseMap.response(status.getSuccessCode(),  "Node '"+ transformer.getName() +"' "+ status.getRegDesc(), "");
//        } catch (Exception exception) {
//            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
//            exceptionErrorLogs.setDescription("Error occurred while trying to creating region node");
//            exceptionErrorLogs.setError_message(exception.getMessage().trim());
//            exceptionErrorLogs.setError(exception.toString().trim());
//            exceptionAuditRepository.save(exceptionErrorLogs);
//            throw exception;
//        }
//    }
