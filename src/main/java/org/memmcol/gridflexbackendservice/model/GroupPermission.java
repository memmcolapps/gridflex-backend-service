package org.memmcol.gridflexbackendservice.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class GroupPermission implements Serializable {

    private static final long serialVersionUID = 1L;

//    private Group group;
//    private Permission permissions;

    private Long id;

    private String groupTitle;

    private Long orgId;

    private Permission permissions;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public String getGroupTitle() {
        return groupTitle;
    }

    public void setGroupTitle(String groupTitle) {
        this.groupTitle = groupTitle;
    }

    //    public Group getGroup() {
//        return group;
//    }
//
//    public void setGroup(Group group) {
//        this.group = group;
//    }

    public Permission getPermissions() {
        return permissions;
    }

    public void setPermissions(Permission permissions) {
        this.permissions = permissions;
    }
}

