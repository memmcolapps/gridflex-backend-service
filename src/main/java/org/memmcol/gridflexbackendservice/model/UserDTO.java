package org.memmcol.gridflexbackendservice.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class UserDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private UserModel user;
    private List<GroupWithPermissionsDTO> groups;

    public UserModel getUser() {
        return user;
    }

    public void setUser(UserModel user) {
        this.user = user;
    }

    public List<GroupWithPermissionsDTO> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupWithPermissionsDTO> groups) {
        this.groups = groups;
    }
}
