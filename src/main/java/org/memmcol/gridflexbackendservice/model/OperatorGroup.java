package org.memmcol.gridflexbackendservice.model;

import lombok.Data;

@Data
public class OperatorGroup {
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

