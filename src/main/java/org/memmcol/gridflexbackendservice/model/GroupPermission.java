package org.memmcol.gridflexbackendservice.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class GroupPermission implements Serializable {

    private static final long serialVersionUID = 1L;

    private Group group;
    private List<Permission> permissions;

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }
}

