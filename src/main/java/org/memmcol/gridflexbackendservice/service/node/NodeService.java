package org.memmcol.gridflexbackendservice.service.node;

import org.memmcol.gridflexbackendservice.model.node.*;

import java.util.Map;
import java.util.UUID;

public interface NodeService {

    Map<String, Object> singleNode(UUID nodeId);

    Map<String, Object> getAllNodes();

    Map<String, Object> createRegionBhubServiceCenterNode(RegionBhubServiceCenter request);

    Map<String, Object> createSubStationFeederLineTransformerNode(SubStationTransformerFeederLine request);

    Map<String, Object> updateRegionBhubServiceCenterNode(RegionBhubServiceCenter request);

    Map<String, Object> updateSubStationFeederLineTransformerNode(SubStationTransformerFeederLine request);

    Map<String, Object> getBusinessHubByOrgId();

    Map<String, Object> getFeederAndDssNode();

    Map<String, Object> getAllFeeder();

    Map<String, Object> getAllDss(String assetId);
}
