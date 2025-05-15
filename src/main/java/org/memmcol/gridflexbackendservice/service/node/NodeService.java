package org.memmcol.gridflexbackendservice.service.node;

import org.memmcol.gridflexbackendservice.model.node.*;

import java.util.Map;

public interface NodeService {

    Map<String, Object> createBusinessHubNode(BusinessHub request);

    Map<String, Object> createSubStationNode(SubStation request);

    Map<String, Object> createFeederLineNode(FeederLine request);

    Map<String, Object> createRegionNode(Region request);

    Map<String, Object> createTransformerNode(Transformer request);

    Map<String, Object> updateBusinessHubNode(BusinessHub request);

    Map<String, Object> updateSubStationNode(SubStation request);

    Map<String, Object> updateFeederLineNode(FeederLine request);

    Map<String, Object> updateRegionNode(Region request);

    Map<String, Object> updateTransformerNode(Transformer request);

    Map<String, Object> singleNodes(Long nodeId);

    Map<String, Object> getAllNodes();
}
