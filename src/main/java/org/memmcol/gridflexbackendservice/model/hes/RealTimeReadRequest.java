package org.memmcol.gridflexbackendservice.model.hes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RealTimeReadRequest {
    private UUID orgId;
    private String meterType;

    private List<String> meters;

    private List<String> obisString;

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
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

    public String getMeterType() {
        return meterType;
    }

    public void setMeterType(String meterType) {
        this.meterType = meterType;
    }

    //    private UUID orgId;
//    private String meterType; // e.g., "MD" or "NonMD"
//    private List<Map<String, Object>> meters; // List of meter serial numbers
//    private List<Map<String, Object>> obisCode; // List of OBIS codes
//
//    public UUID getOrgId() {
//        return orgId;
//    }
//
//    public void setOrgId(UUID orgId) {
//        this.orgId = orgId;
//    }
//
//    public String getMeterType() {
//        return meterType;
//    }
//
//    public void setMeterType(String meterType) {
//        this.meterType = meterType;
//    }
//
//    public List<Map<String, Object>> getObisCode() {
//        return obisCode;
//    }
//
//    public void setObisCode(List<Map<String, Object>> obisCode) {
//        this.obisCode = obisCode;
//    }
//
//    public List<Map<String, Object>> getMeters() {
//        return meters;
//    }
//
//    public void setMeters(List<Map<String, Object>> meters) {
//        this.meters = meters;
//    }
}
