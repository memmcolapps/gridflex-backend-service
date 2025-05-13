package org.memmcol.gridflexbackendservice.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SubModuleWithPermissions implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private Boolean access;
    private Long orgId;
    private Long moduleId;

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

    public Boolean getAccess() {
        return access;
    }

    public void setAccess(Boolean access) {
        this.access = access;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getModuleId() {
        return moduleId;
    }

    public void setModuleId(Long moduleId) {
        this.moduleId = moduleId;
    }
    //    private SubModule subModule;
//    private List<Permission> permissions;



//    public SubModule getSubModule() {
//        return subModule;
//    }
//
//    public void setSubModule(SubModule subModule) {
//        this.subModule = subModule;
//    }

//    public List<Permission> getPermissions() {
//        return permissions;
//    }
//
//    public void setPermissions(List<Permission> permissions) {
//        this.permissions = permissions;
//    }
}
