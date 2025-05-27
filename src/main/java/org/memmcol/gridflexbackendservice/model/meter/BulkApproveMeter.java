package org.memmcol.gridflexbackendservice.model.meter;

import java.util.List;
import java.util.UUID;

public class BulkApproveMeter {
    private List<UUID> meterIds;
    private String approveStatus;

    public List<UUID> getMeterIds() {
        return meterIds;
    }

    public void setMeterIds(List<UUID> meterIds) {
        this.meterIds = meterIds;
    }

    public String getApproveStatus() {
        return approveStatus;
    }

    public void setApproveStatus(String approveStatus) {
        this.approveStatus = approveStatus;
    }
}
