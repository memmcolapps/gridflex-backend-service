package org.memmcol.gridflexbackendservice.model;

import java.io.Serializable;
import java.util.List;

public class GroupWithPermissionsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Group group;

    private List<ModuleWithSubModules> modules;

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public List<ModuleWithSubModules> getModules() {
        return modules;
    }

    public void setModules(List<ModuleWithSubModules> modules) {
        this.modules = modules;
    }
}
