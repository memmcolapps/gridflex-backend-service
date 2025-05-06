package org.memmcol.gridflexbackendservice.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SubModuleWithPermissions implements Serializable {
    private static final long serialVersionUID = 1L;
    private SubModule subModule;
    private List<Permission> permissions;

    public SubModule getSubModule() {
        return subModule;
    }

    public void setSubModule(SubModule subModule) {
        this.subModule = subModule;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }
}
