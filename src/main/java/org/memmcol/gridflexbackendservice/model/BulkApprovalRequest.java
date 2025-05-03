package org.memmcol.gridflexbackendservice.model;

import lombok.Data;

import java.util.List;

@Data
public class BulkApprovalRequest {
    private List<Long> tariffIds;
    private String approveStatus;

    public List<Long> getTariffIds() {
        return tariffIds;
    }

    public void setTariffIds(List<Long> tariffIds) {
        this.tariffIds = tariffIds;
    }

    public String getApproveStatus() {
        return approveStatus;
    }

    public void setApproveStatus(String approveStatus) {
        this.approveStatus = approveStatus;
    }
}
