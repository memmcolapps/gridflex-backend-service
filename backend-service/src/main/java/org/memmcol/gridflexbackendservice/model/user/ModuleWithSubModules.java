package org.memmcol.gridflexbackendservice.model.user;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
public class ModuleWithSubModules implements Serializable {
    private static final long serialVersionUID = 1L;
//    private Module module;
    private UUID id;

    private UUID orgId;

    private String name;

    private Boolean access;

    private UUID groupId;

    private List<SubModuleWithPermissions> subModules;

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

    public UUID getGroupId() {
        return groupId;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAccess(Boolean access) {
        this.access = access;
    }

    public Boolean getAccess() {
        return access;
    }


    public List<SubModuleWithPermissions> getSubModules() {
        return subModules;
    }

    public void setSubModules(List<SubModuleWithPermissions> subModules) {
        this.subModules = subModules;
    }
}
