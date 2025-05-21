package org.memmcol.gridflexbackendservice.model.user;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class GroupWithPermissionsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

//    private Group group;
    private UUID id;

    private UUID orgId;

    private String groupTitle;

    private List<ModuleWithSubModules> modules;

    private Permission permissions;

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

    public String getGroupTitle() {
        return groupTitle;
    }

    public void setGroupTitle(String groupTitle) {
        this.groupTitle = groupTitle;
    }


    public List<ModuleWithSubModules> getModules() {
        return modules;
    }

    public void setModules(List<ModuleWithSubModules> modules) {
        this.modules = modules;
    }

    public Permission getPermissions() {
        return permissions;
    }

    public void setPermissions(Permission permissions) {
        this.permissions = permissions;
    }

}
