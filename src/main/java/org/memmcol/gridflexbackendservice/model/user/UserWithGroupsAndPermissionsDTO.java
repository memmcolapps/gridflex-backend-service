package org.memmcol.gridflexbackendservice.model.user;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class UserWithGroupsAndPermissionsDTO implements Serializable {
    private static final long serialVersionUID = 1L;
//    private Long userId;
//
//    private String firstname;
//
//    private String lastname;
//
//    private String email;
//
//    private Long hierarchyId;
//
//    private Boolean status;
//
//    private Boolean active;
//
//    private Timestamp lastActive;

    private UserModel user;

    private List<GroupWithPermissionsDTO> groups;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
