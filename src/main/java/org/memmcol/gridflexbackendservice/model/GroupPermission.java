package org.memmcol.gridflexbackendservice.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class GroupPermission implements Serializable {
    private Long groupId;
    private Long permissionId;

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }
}

