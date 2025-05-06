package org.memmcol.gridflexbackendservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class CreateGroupRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    private Group group;
    private List<ModuleWithSubModules> modules;
    private List<SubModuleWithPermissions> subModules;
//    private List<Long> userIds;

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

    public List<SubModuleWithPermissions> getSubModules() {
        return subModules;
    }

    public void setSubModules(List<SubModuleWithPermissions> subModules) {
        this.subModules = subModules;
    }
//
//    public List<Long> getUserIds() {
//        return userIds;
//    }
//
//    public void setUserIds(List<Long> userIds) {
//        this.userIds = userIds;
//    }
}
