package org.memmcol.gridflexbackendservice.service.node;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.mapper.NodeMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.node.*;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.service.audit.SafeAuditService;
import org.memmcol.gridflexbackendservice.exception.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.HandlePermission;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static org.memmcol.gridflexbackendservice.components.HandleValidUser.handleUserValidation;

@Service
public class NodeServiceImpl implements NodeService {
    private static final Logger log = LoggerFactory.getLogger(NodeServiceImpl.class);

    @Autowired
    private NodeMapper nodeMapper;

    @Autowired
    private ResponseProperties status;

//    @Autowired
//    private AuditRepository auditRepository;

    @Autowired
    private SafeAuditService safeAuditService;

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

            handleRegionPayloadCheck(request);

            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            String desc;
            UserModel um = handleUserValidation();
            String nodeType = um.getNodeInfo().getType();

            HandlePermission.perm(nodeType);

            String type = request.getType().toLowerCase();

            Node node = new Node();
            node.setName(request.getName());
            node.setOrgId(um.getOrgId());
            node.setParentId(request.getParentId());

            Node nd = nodeMapper.isNodeExist(request.getParentId(), um.getOrgId());

            if(nd == null) {
                throw new GlobalExceptionHandler.NotFoundException("Parent node does not exist");
            }

            RegionBhubServiceCenter duplicate = nodeMapper.verifyNodes(request.getRegionId(), um.getOrgId(), request.getType());

            if (duplicate != null && request.getType().equalsIgnoreCase(duplicate.getType())
                    && duplicate.getRegionId().equalsIgnoreCase(request.getRegionId())) {
                throw new GlobalExceptionHandler.NotFoundException(
                        request.getType().substring(0, 1).toUpperCase()
                                + request.getType().substring(1).toLowerCase()
                                + " already exists for region ID (" + request.getRegionId() + ")"
                );
            }

            if (Boolean.TRUE.equals(nodeMapper.existsByRegionEmail(request.getEmail()))) {
                throw new GlobalExceptionHandler.NotFoundException(
                        "Email (" + request.getEmail() + ") already been used"
                );
            }

            RegionBhubServiceCenter rgBhubService = nodeMapper.getBhubByOrgIdAndName(um.getOrgId(), request.getName());
            if (rgBhubService != null){
                switch (type){
                    case "region":
                        throw new GlobalExceptionHandler.NotFoundException("Region Name (" + request.getName()+") " + status.getExistDesc() +" for a "+rgBhubService.getType());
                    case "business hub":
                        throw new GlobalExceptionHandler.NotFoundException("Business Name (" + request.getName()+") " + status.getExistDesc() +" for a "+rgBhubService.getType());
                    case "service center":
                        throw new GlobalExceptionHandler.NotFoundException("Service Name (" + request.getName()+") " + status.getExistDesc() +" for a "+rgBhubService.getType());

                    default:
                        throw new GlobalExceptionHandler.NotFoundException("Node Name (" + request.getName()+") " + status.getExistDesc() +" for a "+rgBhubService.getType());
                }
            }

            nodeMapper.createNode(node);

            UUID nodeId = node.getId();
            UUID parentNodeId = node.getParentId();

            request.setNodeId(nodeId);
            request.setOrgId(um.getOrgId());
            request.setParentId(parentNodeId);


            if(request.getType().toLowerCase().equals("region") ||
                    request.getType().toLowerCase().equals("business hub") ||
                    request.getType().toLowerCase().equals("service center")){
                request.setEmail(
                        StringUtils.isBlank(request.getEmail()) ? null : request.getEmail()
                );
                nodeMapper.createRegionBhubServiceCenter(request);
                id = request.getNodeId();
                regionBhubServiceCenter = nodeMapper.getRegionBhubServiceCenter(id);
                desc = regionBhubServiceCenter.getName() + "newly created";
            } else {
                throw new GlobalExceptionHandler.NotFoundException("Request type " +"("+ request.getType()+")"+ " not found");
            }
//            handleClearCache(node);
            AuditLog auditLog = buildAuditLog(um, desc, request.getType().equals("region") ? "region" : request.getType().equals("service center") ? "service center" : "business hub", regionBhubServiceCenter, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(),  "Node "+ status.getRegDesc(), "");

        } catch (Exception exception) {
            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Creating region business hub service failed");
            genericHandler.logAndSaveException(exception, "creating region business hub");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> createSubStationFeederLineTransformerNode(
            SubStationTransformerFeederLine request) {
        SubStationTransformerFeederLine subStationTransformerFeederLine;
        UUID id;
        try {

            handlePayloadCheck(request);

            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            String desc;
            UserModel um = handleUserValidation();
            String nodeType = um.getNodeInfo().getType();

            HandlePermission.perm(nodeType);

            String type = request.getType().toLowerCase();

            Node node = new Node();
            node.setName(request.getName());
            node.setOrgId(um.getOrgId());
            node.setParentId(request.getParentId());

            Node nd = nodeMapper.isNodeExist(request.getParentId(), um.getOrgId());
            if(nd == null) {
                throw new GlobalExceptionHandler.NotFoundException("Parent node does not exist");
            }


            SubStationTransformerFeederLine sub = nodeMapper.verifySubNode(request.getAssetId(), um.getOrgId(), request.getType());
            if(sub != null && sub.getType().equalsIgnoreCase(request.getType())
                    && sub.getAssetId().equalsIgnoreCase(request.getAssetId())){
                throw new GlobalExceptionHandler.NotFoundException(
                        request.getType().substring(0, 1).toUpperCase()
                                + request.getType().substring(1).toLowerCase()
                                + " already exists for asset ID (" + request.getAssetId() + ")");
//                throw new GlobalExceptionHandler.NotFoundException("Asset ID ("+ request.getAssetId()+") " + status.getExistDesc() +" for a "+ request.getType());
            }

            if (Boolean.TRUE.equals(nodeMapper.existsBySerial(request.getSerialNo(), um.getOrgId(), request.getType().toLowerCase()))) {
                throw new GlobalExceptionHandler.NotFoundException(
                        "Serial No (" + request.getSerialNo() + ") " + status.getExistDesc());
            }

            if (Boolean.TRUE.equals(nodeMapper.existsByEmail(request.getEmail()))) {
                throw new GlobalExceptionHandler.NotFoundException(
                        "Email (" + request.getEmail() + ") already been used"
                );
            }

            SubStationTransformerFeederLine subTransFeeder = nodeMapper.getSubTransformerFeederLineByOrgIdAndName(um.getOrgId(), request.getName());
            if (subTransFeeder != null){

                switch (type){
                    case "dss":
                        throw new GlobalExceptionHandler.NotFoundException("DSS Name ("+ request.getName()+") " + status.getExistDesc() +" for a "+subTransFeeder.getType());
                    case "feeder line":
                        throw new GlobalExceptionHandler.NotFoundException("Feeder line Name ("+ request.getName()+") " + status.getExistDesc() +" for a "+subTransFeeder.getType());
                    case "substation":
                        throw new GlobalExceptionHandler.NotFoundException("Substation Name (" + request.getName()+") " + status.getExistDesc() +" for a "+subTransFeeder.getType());
                    default:
                        throw new GlobalExceptionHandler.NotFoundException("Parameter type "+(type)+ " not supported");

//                        throw new GlobalExceptionHandler.NotFoundException("Node Name (" + request.getName()+") " + status.getExistDesc() +" for a "+subTransFeeder.getType());
                }
            }

//            NodeSummary nodeSummary = nodeMapper.nodeSummary(request.getParentId(), um.getOrgId());
//
//            if(nodeSummary != null
//                    && nodeSummary.getType().equalsIgnoreCase("Business hub")) {
//                request.setBhubId(request.getParentId());
//            } else {
//                assert nodeSummary != null;
//                request.setBhubId(nodeSummary.getBhubId());
//            }

            nodeMapper.createNode(node);

            UUID nodeId = node.getId();
            UUID parentNodeId = node.getParentId();

            request.setNodeId(nodeId);
            request.setOrgId(um.getOrgId());
            request.setParentId(parentNodeId);

            if(request.getType().toLowerCase().equals("dss") ||
                    request.getType().toLowerCase().equals("feeder line") ||
                    request.getType().toLowerCase().equals("substation")){
//                request.getEmail().equalsIgnoreCase("") ? request.setEmail(NULL) : request.getEmail();
                request.setEmail(
                        StringUtils.isBlank(request.getEmail()) ? null : request.getEmail()
                );
                nodeMapper.createSubStationTransformerFeederLine(request);
                id = request.getNodeId();
                subStationTransformerFeederLine = nodeMapper.getSubStationTransformerFeederLine(id);
                desc = subStationTransformerFeederLine.getName() + "newly created";
            } else {
                throw new GlobalExceptionHandler.NotFoundException("Request type " +" ("+ request.getType()+" )"+ " not found");
            }

//            handleClearCache(node);

            AuditLog auditLog = buildAuditLog(um, desc, request.getType().equals("dss") ? "dss" : request.getType().equals("feeder line") ? "feeder line" : "substation", subStationTransformerFeederLine, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(),  "Node "+ status.getRegDesc(), "");

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
        try {

            handleRegionPayloadCheck(request);

            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            String desc;
            UserModel um = handleUserValidation();
            UUID nodeId = um.getNodeInfo().getNodeId();
            String nodeType = um.getNodeInfo().getType();

            HandlePermission.perm(nodeType);

            Node node = new Node();
            node.setId(request.getNodeId());
            node.setName(request.getName());
            node.setOrgId(um.getOrgId());
            node.setParentId(request.getParentId());

            Node nd = nodeMapper.getNodeExistByIdForUpdate(request.getNodeId(), um.getOrgId());

            if(nd == null) {
                throw new GlobalExceptionHandler.NotFoundException("Node does not exist");
            }

            RegionBhubServiceCenter existingRecord = nodeMapper.getRegionBhubServiceCenter(request.getNodeId());

            if (existingRecord == null) {
                throw new GlobalExceptionHandler.NotFoundException("Region/Business Hub/Service Center record not found");
            }

            // Validate Region ID + Type combination (only if regionId changed)
            if (!request.getRegionId().equals(existingRecord.getRegionId())) {

                if (Boolean.TRUE.equals(nodeMapper.existsByRegionIdAndTypeExcludingCurrent(
                        request.getRegionId(), um.getOrgId(), request.getType(), request.getNodeId()))) {
                    throw new GlobalExceptionHandler.NotFoundException(
                            request.getType().substring(0, 1).toUpperCase()
                                    + request.getType().substring(1).toLowerCase()
                                    + " already exists for Region ID (" + request.getRegionId() + ")"
                    );
                }
            }
//            Boolean existingEmail = nodeMapper.existsByRegionEmail(request.getEmail());

            // Validate Email (only if email changed)
            if (request.getEmail() != null && !request.getEmail().isEmpty()) {

                if (Boolean.TRUE.equals(nodeMapper.existsByRegionEmailExcludingCurrent(
                        request.getEmail(), request.getNodeId()))) {
                    throw new GlobalExceptionHandler.NotFoundException(
                        "Email (" + request.getEmail() + ") already been used"
                    );
                }
            }

            // Validate Name (only if name changed)
            if (!request.getName().equalsIgnoreCase(existingRecord.getName())) {

                if (Boolean.TRUE.equals(nodeMapper.existsByNameExcludingCurrent(
                        request.getName(), um.getOrgId(), request.getNodeId()))) {
                    throw new GlobalExceptionHandler.NotFoundException(
                            "Node Name (" + request.getName() + ") " + status.getExistDesc()
                    );
                }
            }

//            // Validate Phone Number (only if phone number changed)
//            if (request.getPhoneNo() != null && !request.getPhoneNo().isEmpty() &&
//                    !request.getPhoneNo().equals(existingRecord.getPhoneNo())) {
//
//                if (Boolean.TRUE.equals(nodeMapper.existsByPhoneNumberExcludingCurrent(
//                        request.getPhoneNo(), um.getOrgId(), request.getNodeId()))) {
//                    throw new GlobalExceptionHandler.NotFoundException(
//                            "Phone Number (" + request.getPhoneNo() + ") already been used"
//                    );
//                }
//            }

            nodeMapper.updateNode(node);

            request.setOrgId(um.getOrgId());
            request.setUpdatedAt(LocalDateTime.now());

            if(request.getType().equalsIgnoreCase("region") ||
                    request.getType().equalsIgnoreCase("business hub") ||
                    request.getType().equalsIgnoreCase("service center") ||
                            request.getType().equalsIgnoreCase("root")) {
                request.setEmail(
                        StringUtils.isBlank(request.getEmail()) ? null : request.getEmail()
                );
                nodeMapper.updateRegionBhubServiceCenter(request);
                regionBhubServiceCenter = nodeMapper.getRegionBhubServiceCenter(request.getNodeId());
                desc = regionBhubServiceCenter.getName()  + "edited";
            }  else {
                throw new GlobalExceptionHandler.NotFoundException("Request type " +" ("+ request.getType()+" )"+ " not found");
            }

//            handleClearCache(node);

            AuditLog auditLog = buildAuditLog(um, desc, request.getType().equals("region") ? "Region" : request.getType().equals("service center") ? "Service center" : "Business hub", regionBhubServiceCenter, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(),  "Node "+ status.getUpdateDesc(), "");

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
        try {

            handlePayloadCheck(request);

            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            String desc;
            UserModel um = handleUserValidation();
            String nodeType = um.getNodeInfo().getType();

            HandlePermission.perm(nodeType);

            Node node = new Node();
            node.setId(request.getNodeId());
            node.setName(request.getName());
            node.setOrgId(um.getOrgId());
            node.setParentId(request.getParentId());

            Node nd = nodeMapper.getNodeExistByIdForUpdate(request.getNodeId(), um.getOrgId());

            if(nd == null) {
                throw new GlobalExceptionHandler.NotFoundException("Parent node does not exist");
            }

            SubStationTransformerFeederLine existingRecord = nodeMapper.getSubStationTransformerFeederLine(request.getNodeId());

            if (existingRecord == null) {
                throw new GlobalExceptionHandler.NotFoundException("Substation/Feeder/DSS record not found for the given node ID");
            }

            // Serial Number Validation - only check if value changed
            if (!request.getSerialNo().equals(existingRecord.getSerialNo())) {

                // Check if another record of same type has this serial (excluding current)
                if (Boolean.TRUE.equals(nodeMapper.existsBySerialForSameTypeExcludingCurrent(
                        request.getSerialNo(), um.getOrgId(), request.getType().toLowerCase(), request.getNodeId()))) {
                    throw new GlobalExceptionHandler.NotFoundException(
                            "Serial No (" + request.getSerialNo() + ") " + status.getExistDesc()
                                    + " for a " + request.getType());
                }
            }

//            Boolean existingEmail = nodeMapper.existsByEmail(request.getEmail());

            // Validate Email (only if email changed)
            if (request.getEmail() != null && !request.getEmail().isEmpty() ) {

                if (Boolean.TRUE.equals(nodeMapper.existsByEmailForDifferentNode(
                        request.getEmail(), request.getNodeId()))) {
                    throw new GlobalExceptionHandler.NotFoundException(
                            "Email (" + request.getEmail() + ") already been used"
                    );
                }
            }

            // Asset ID Validation - only check if value changed
            if (!request.getAssetId().equalsIgnoreCase(existingRecord.getAssetId())) {

                // Check if another record of same type has this assetId (excluding current)
                if (Boolean.TRUE.equals(nodeMapper.existsByAssetIdForSameTypeExcludingCurrent(
                        request.getAssetId(), um.getOrgId(), request.getType().toLowerCase(), request.getNodeId()))) {
                    throw new GlobalExceptionHandler.NotFoundException(
                            "Asset ID (" + request.getAssetId() + ") " + status.getExistDesc()
                                    + " for a " + request.getType());
                }
            }

            if (!request.getName().equalsIgnoreCase(existingRecord.getName())) {

                if (Boolean.TRUE.equals(nodeMapper.existsByNameExcludingCurrentName(
                        request.getName(), um.getOrgId(), request.getNodeId()))) {
                    throw new GlobalExceptionHandler.NotFoundException(
                            "Node Name (" + request.getName() + ") " + status.getExistDesc()
                    );
                }
            }

//            SubStationTransformerFeederLine sub = nodeMapper.getSubStationTransformerFeederLineById(request.getId(), um.getOrgId());
//            if (sub.getName().equalsIgnoreCase(request.getName())){
//                throw new GlobalExceptionHandler.NotFoundException("Node ("+ request.getName()+") " + status.getExistDesc());
//            }

//            SubStationTransformerFeederLine subAsset = nodeMapper.verifySubNode(request.getAssetId(), um.getOrgId());
//            if(subAsset != null){
//                throw new GlobalExceptionHandler.NotFoundException("Asset ID ("+ request.getAssetId()+") " + status.getExistDesc());
//            }

            nodeMapper.updateNode(node);

            request.setOrgId(um.getOrgId());
            request.setUpdatedAt(LocalDateTime.now());

            if(request.getType().equalsIgnoreCase("dss") ||
                    request.getType().equalsIgnoreCase("feeder line") ||
                    request.getType().equalsIgnoreCase("substation")){
                request.setEmail(
                        StringUtils.isBlank(request.getEmail()) ? null : request.getEmail()
                );
                nodeMapper.updateSubStationTransformerFeederLine(request);
                subStationTransformerFeederLine = nodeMapper.getSubStationTransformerFeederLine(request.getNodeId());
                desc = subStationTransformerFeederLine.getName()  + "edited";
            } else {
                throw new GlobalExceptionHandler.NotFoundException("Request type " +" ("+ request.getType()+" )"+ " not found");
            }

//            handleClearCache(node);

            AuditLog auditLog = buildAuditLog(um, desc, request.getType().equalsIgnoreCase("transformer") ? "Transformer" : request.getType().equalsIgnoreCase("feeder line") ? "Feeder line" : "Substation", subStationTransformerFeederLine, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(),  "Node "+ status.getUpdateDesc(), "");

        } catch (Exception exception) {
            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Editing substation/feeder line service failed");
            genericHandler.logAndSaveException(exception, "editing substation/feeder line node");
            throw exception;
        }
    }

    private void handlePayloadCheck(SubStationTransformerFeederLine request) {

        if(request.getName() == null || request.getName().isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("Name is required");
        }
        if (request.getSerialNo() == null || request.getSerialNo().isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("Serial number is required");
        }
        if (request.getAssetId() == null || request.getAssetId().isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("Asset Id is required");
        }
        if (request.getStatus() == null) {
            throw new GlobalExceptionHandler.NotFoundException("Status is required");
        }
        if (request.getVoltage() == null || request.getVoltage().isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("Voltage is required");
        }
    }

    private void handleRegionPayloadCheck(RegionBhubServiceCenter request) {
        if(request.getName() == null || request.getName().isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("Name is required");
        }
        if (request.getRegionId() == null || request.getRegionId().isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("Region Id is required");
        }
    }

//    @Transactional(readOnly = true)
//    @Override
//    public Map<String, Object> singleNode(UUID nodeId) {
//        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
//        try {
//            UserModel um = handleUserValidation();
////
//            Object cachedUser = nodeCache.get(nodeId.toString() + "_" + um.getOrgId());
//            if (cachedUser != null) {
//                return ResponseMap.response(status.getSuccessCode(), "Cached Node " + status.getDesc(), cachedUser);
//            }
//
//            List<Node> flatList = nodeMapper.getNodeWithChildren(nodeId, um.getOrgId());
//            if (flatList == null || flatList.isEmpty()) {
//                return ResponseMap.response(status.getSuccessCode(), "No nodes found", "");
//            }
//
//            Map<UUID, Node> nodeMap = new HashMap<>();
//            Node root = null;
//
//            for (Node node : flatList) {
//                node.setNodesTree(new ArrayList<>());
//                nodeMap.put(node.getId(), node);
//            }
//
//            for (Node node : flatList) {
//                if (node.getId().equals(nodeId)) {
//                    root = node; // this is the node we're querying for
//                }
//                if (node.getParentId() != null && nodeMap.containsKey(node.getParentId())) {
//                    Node parent = nodeMap.get(node.getParentId());
//                    parent.getNodesTree().add(node);
//                }
//            }
//
//            assert root != null;
////            handleAddCache(root);
//            return ResponseMap.response(status.getSuccessCode(), "Node " + status.getDesc(), root);
//
//        } catch (Exception exception) {
//            log.error("Error occurred while fetching node [ACTION]: {}", exception.getMessage().trim(), exception);
//            genericHandler.logIncidentReport("Fetching node service failed");
//            genericHandler.logAndSaveException(exception, "fetching single node");
//            throw exception;
//        }
//    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getAllNodes() {
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

//            nodeCache.put(cacheKey, roots);

            return ResponseMap.response(status.getSuccessCode(),  "Node "+status.getDesc(), roots);
        } catch (Exception exception) {
            log.error("Error occurred while updated node [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Fetching all node service failed");
            genericHandler.logAndSaveException(exception, "fetching all node");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getBusinessHubByOrgId() {
        try {

            UserModel user = handleUserValidation();
            String nodeType = user.getNodeInfo().getType();
            UUID userRegionId = user.getNodeInfo().getNodeId();

            List<RegionBhubServiceCenter> result;

            if(nodeType.equalsIgnoreCase("Region")
                    || nodeType.equalsIgnoreCase("Root")) {
                result = nodeMapper.getBhubByOrgId(userRegionId, user.getOrgId());
            } else {
                throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
            }

            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), result);

        }catch (Exception exception) {
            log.error("Error occurred while fetching business hub [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Fetching all business hub service failed");
            genericHandler.logAndSaveException(exception, "fetching all business hub");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getAllFeeder(){
        try {
            UserModel um = handleUserValidation();

            UUID nodeId = um.getNodeInfo().getNodeId();
            String nodeType = um.getNodeInfo().getType();

            List<NodeSummary> result;

                if(nodeType.equalsIgnoreCase("Region")
                        || nodeType.equalsIgnoreCase("Root")) {
                    result = nodeMapper.getAllRegionFeeder(um.getOrgId(), nodeId);
                } else {
                    result = nodeMapper.getFeedersUnderNode(um.getOrgId(), nodeId);
                }
//                throw new GlobalExceptionHandler.NotFoundException("User does not belong to any hierarchy");
//            }

            return ResponseMap.response(status.getSuccessCode(),  status.getDesc(), result);
        } catch (Exception exception) {
            log.error("Error fetching feeders: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("fetching feeders service failed");
            genericHandler.logAndSaveException(exception, "fetching feeders");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getAllDss(String assetId){
        try {
            UserModel um = handleUserValidation();
            UUID nodeId = nodeMapper.getFeederNodeId(um.getOrgId(),assetId);
            List<SubStationTransformerFeederLine> result = nodeMapper.getAllDssByNodeId(um.getOrgId(), nodeId);

            return ResponseMap.response(status.getSuccessCode(),  status.getDesc(), result);
        }catch (Exception exception) {
            log.error("Error fetching dss: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("fetching dss service failed");
            genericHandler.logAndSaveException(exception, "fetching dss");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getFeederAndDssNode() {
        try{

            UserModel um = handleUserValidation();

            List<SubStationTransformerFeederLine> result = nodeMapper.getFeederDss(um.getOrgId());

            return ResponseMap.response(status.getSuccessCode(),  status.getDesc(), result);

        } catch (Exception exception) {
            log.error("Error filtering / fetching feeders and dss: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("fetching feeders and dss service failed");
            genericHandler.logAndSaveException(exception, "fetching feeders and dss");
            throw exception;
        }
    }

    private void resolveNodeHierarchy(Meter request, UUID startNodeId, UUID orgId) {

        UUID currentNodeId = startNodeId;
        Set<UUID> visited = new HashSet<>();

        while (currentNodeId != null) {

            if (!visited.add(currentNodeId)) {
                throw new IllegalStateException("Circular hierarchy detected");
            }

            NodeSummary node = nodeMapper.getNodeByNodeId(currentNodeId, orgId);
            if (node == null) break;

            String type = node.getType() == null ? "" : node.getType().toLowerCase();

            switch (type) {
//                case "business hub":
//                    System.out.println("bbbhhh:: "+node.getNodeId());
//                    if(bhubId.equals(node.getNodeId())){
//                        request.setNodeId(node.getNodeId());
//                    } else {
//                        throw new GlobalExceptionHandler
//                                .NotFoundException("Feeder does not belong to the bushiness hub meter is allocated");
//                    }
//
//                    break;
//                case "service center":
//                    request.setServiceCenter(node.getNodeId());
//                    break;
                case "region":
                    request.setRegion(node.getNodeId());
                    break;
//                case "substation":
//                    request.setSubstation(node.getNodeId());
//                    break;
                case "root":
                    request.setRoot(node.getNodeId());
                    break;
            }

            currentNodeId = node.getParentId();
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