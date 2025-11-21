package org.memmcol.gridflexbackendservice.model.hes;

import java.util.List;
import java.util.UUID;

public class RealTimeReadRequest {
    private UUID orgId;
    private String meterType; // e.g., "MD" or "NonMD"
    private List<String> meters; // List of meter serial numbers
    private List<String> obisString; // List of OBIS codes

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public String getMeterType() {
        return meterType;
    }

    public void setMeterType(String meterType) {
        this.meterType = meterType;
    }

    public List<String> getObisString() {
        return obisString;
    }

    public void setObisString(List<String> obisString) {
        this.obisString = obisString;
    }

    public List<String> getMeters() {
        return meters;
    }

    public void setMeters(List<String> meters) {
        this.meters = meters;
    }
}
