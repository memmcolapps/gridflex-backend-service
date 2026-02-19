package org.memmcol.gridflexbackendservice.model.user;

import java.io.Serializable;
import java.util.UUID;

public class CreateUserRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private UserModel user;
    private UUID groupId;

    public UserModel getUser() {
        return user;
    }

    public void setUser(UserModel user) {
        this.user = user;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }
}
