package org.memmcol.gridflexbackendservice.model.node;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class Node implements Serializable {
    static final long serialVersionUID = 0L;
    private UUID id;
    private UUID orgId;
    private String name;
    private UUID parentId;
    private NodeInfo nodeInfo;
    private List<Node> nodesTree = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public NodeInfo getNodeInfo() {
        return nodeInfo;
    }

    public void setNodeInfo(NodeInfo nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    public List<Node> getNodesTree() {
        return nodesTree;
    }

    public void setNodesTree(List<Node> nodesTree) {
        this.nodesTree = nodesTree;
    }
}
