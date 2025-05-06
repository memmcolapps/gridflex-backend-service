package org.memmcol.gridflexbackendservice.model;

import java.io.Serializable;
import java.util.List;

public class CreateUserRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private UserModel user;
    private List<Long> groupIds;

    public UserModel getUser() {
        return user;
    }

    public void setUser(UserModel user) {
        this.user = user;
    }

    public List<Long> getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(List<Long> groupIds) {
        this.groupIds = groupIds;
    }
}
