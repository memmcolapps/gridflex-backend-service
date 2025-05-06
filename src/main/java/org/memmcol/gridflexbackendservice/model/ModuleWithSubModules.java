package org.memmcol.gridflexbackendservice.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ModuleWithSubModules implements Serializable {
    private static final long serialVersionUID = 1L;
    private Module module;
    private List<SubModuleWithPermissions> subModules;

    public Module getModule() {
        return module;
    }

    public void setModule(Module module) {
        this.module = module;
    }

    public List<SubModuleWithPermissions> getSubModules() {
        return subModules;
    }

    public void setSubModules(List<SubModuleWithPermissions> subModules) {
        this.subModules = subModules;
    }
}
