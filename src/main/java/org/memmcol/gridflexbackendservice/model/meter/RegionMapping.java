package org.memmcol.gridflexbackendservice.model.meter;

import lombok.Data;

import java.util.UUID;

@Data
public class RegionMapping {
    private UUID parentId;
//    private String regionId;
    private UUID nodeId;

    public RegionMapping(UUID parentId, UUID nodeId) {
        this.parentId = parentId;
//        this.regionId = regionId;
        this.nodeId = nodeId;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

//    public String getRegionId() {
//        return regionId;
//    }
//
//    public void setRegionId(String regionId) {
//        this.regionId = regionId;
//    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }
}
