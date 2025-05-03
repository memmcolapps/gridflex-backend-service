package org.memmcol.gridflexbackendservice.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class OperatorGroup implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long operatorId;
    private Long groupId;

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
}

