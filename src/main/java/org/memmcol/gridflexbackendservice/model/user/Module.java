package org.memmcol.gridflexbackendservice.model.user;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class Module implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;

    private String name;

    private Boolean access;

    private UUID groupId;

    private UUID orgId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getAccess() {
        return access;
    }

    public void setAccess(Boolean access) {
        this.access = access;
    }
}
