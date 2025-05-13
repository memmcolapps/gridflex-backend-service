package org.memmcol.gridflexbackendservice.model;

import java.io.Serializable;
import java.util.List;

public class GroupWithPermissionsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

//    private Group group;
    private Long id;

    private String groupTitle;

    private String orgId;

    private List<ModuleWithSubModules> modules;

    private Permission permissions;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupTitle() {
        return groupTitle;
    }

    public void setGroupTitle(String groupTitle) {
        this.groupTitle = groupTitle;
    }

//    public Group getGroup() {
//        return group;
//    }
//
//    public void setGroup(Group group) {
//        this.group = group;
//    }


    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
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

    //
//    public List<Permission> getPermissions() {
//        return permissions;
//    }
//
//    public void setPermissions(List<Permission> permissions) {
//        this.permissions = permissions;
//    }
}
