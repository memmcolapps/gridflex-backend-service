package org.memmcol.gridflexbackendservice.model;

import java.io.Serializable;
import java.util.List;

public class CreateUserRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private UserModel user;
    private Long groupId;

    public UserModel getUser() {
        return user;
    }

    public void setUser(UserModel user) {
        this.user = user;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
}
