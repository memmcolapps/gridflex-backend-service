package org.memmcol.gridflexbackendservice.model.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserGroup implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long groupId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
}

