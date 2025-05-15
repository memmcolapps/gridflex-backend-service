package org.memmcol.gridflexbackendservice.controller;

import org.memmcol.gridflexbackendservice.model.node.*;
import org.memmcol.gridflexbackendservice.service.node.NodeService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler.SQLServerException;
import org.memmcol.gridflexbackendservice.util.ResponseProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/node/service")
public class NodeController {

    @Autowired private NodeService nodeService;
    @Autowired private ResponseProperties status;
    @Autowired private GlobalExceptionHandler exception;


    @PostMapping("/create/business-hub")
    public ResponseEntity<Map<String, Object>> createBusinessHubNode(@RequestBody BusinessHub request) {
        try {
            Map<String, Object> result = nodeService.createBusinessHubNode(request);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/create/substation")
    public ResponseEntity<Map<String, Object>> createSubStationNode(@RequestBody SubStation request) {
        try {
            Map<String, Object> result = nodeService.createSubStationNode(request);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/create/feeder-line")
    public ResponseEntity<Map<String, Object>> createFeederLineNode(@RequestBody FeederLine request) {
        try {
            Map<String, Object> result = nodeService.createFeederLineNode(request);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/create/region")
    public ResponseEntity<Map<String, Object>> createRegionNode(@RequestBody Region request) {
        try {
            Map<String, Object> result = nodeService.createRegionNode(request);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/create/transformer")
    public ResponseEntity<Map<String, Object>> createTransformerNode(@RequestBody Transformer request) {
        try {
            Map<String, Object> result = nodeService.createTransformerNode(request);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }


    @PostMapping("/update/business-hub")
    public ResponseEntity<Map<String, Object>> updateBusinessHubNode(@RequestBody BusinessHub request) {
        try {
            Map<String, Object> result = nodeService.updateBusinessHubNode(request);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/update/substation")
    public ResponseEntity<Map<String, Object>> updateSubStationNode(@RequestBody SubStation request) {
        try {
            Map<String, Object> result = nodeService.updateSubStationNode(request);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/update/feeder-line")
    public ResponseEntity<Map<String, Object>> updateFeederLineNode(@RequestBody FeederLine request) {
        try {
            Map<String, Object> result = nodeService.updateFeederLineNode(request);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/update/region")
    public ResponseEntity<Map<String, Object>> updateRegionNode(@RequestBody Region request) {
        try {
            Map<String, Object> result = nodeService.updateRegionNode(request);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/update/transformer")
    public ResponseEntity<Map<String, Object>> updateTransformerNode(@RequestBody Transformer request) {
        try {
            Map<String, Object> result = nodeService.updateTransformerNode(request);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/single-node")
    public ResponseEntity<Map<String, Object>> singleNodes(@RequestParam Long nodeId) {

        try {
            Map<String, Object> result =  nodeService.singleNodes(nodeId);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }

    }

    @GetMapping("/all-nodes")
    public ResponseEntity<Map<String, Object>> fetchAllNodes() {

        try {
            Map<String, Object> result =  nodeService.getAllNodes();

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }

    }


    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
