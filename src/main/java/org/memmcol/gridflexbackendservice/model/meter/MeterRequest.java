package org.memmcol.gridflexbackendservice.model.meter;

import lombok.Data;

import java.io.Serializable;

@Data
public class MeterRequest implements Serializable {
    private String meterNumber;
    private String regionId;
    private String approveState;

    public String getMeterNumber() {
        return meterNumber;
    }

    public void setMeterNumber(String meterNumber) {
        this.meterNumber = meterNumber;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getApproveState() {
        return approveState;
    }

    public void setApproveState(String approveState) {
        this.approveState = approveState;
    }
}
