package org.memmcol.gridflexbackendservice.model.user;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class UserGroup implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID userId;
    private UUID groupId;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }
}

