package org.memmcol.gridflexbackendservice.model.user;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class SubModuleWithPermissions implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID id;
    private UUID orgId;
    private String name;
    private Boolean access;
    private UUID moduleId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public UUID getModuleId() {
        return moduleId;
    }

    public void setModuleId(UUID moduleId) {
        this.moduleId = moduleId;
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
