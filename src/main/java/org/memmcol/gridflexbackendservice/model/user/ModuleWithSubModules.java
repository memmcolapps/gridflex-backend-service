package org.memmcol.gridflexbackendservice.model.user;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ModuleWithSubModules implements Serializable {
    private static final long serialVersionUID = 1L;
//    private Module module;
    private Long id;

    private String name;

    private Long orgId;

    private Boolean access;

    private Long groupId;

    private List<SubModuleWithPermissions> subModules;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public void setAccess(Boolean access) {
        this.access = access;
    }

    public Boolean getAccess() {
        return access;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    //    public Module getModule() {
//        return module;
//    }
//
//    public void setModule(Module module) {
//        this.module = module;
//    }

    public List<SubModuleWithPermissions> getSubModules() {
        return subModules;
    }

    public void setSubModules(List<SubModuleWithPermissions> subModules) {
        this.subModules = subModules;
    }
}
