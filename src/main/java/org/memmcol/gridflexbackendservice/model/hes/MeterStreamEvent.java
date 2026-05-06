package org.memmcol.gridflexbackendservice.model.hes;

import java.time.LocalDateTime;
import java.util.UUID;

public class MeterStreamEvent {

    private String meterNo;
    private LocalDateTime lastSeen;
    private String status;
    private int statuscode;
    private String meterModel;
    private String obisString;
    private String timestamp;
    private String desc;
    private String value;
    private String statusmessage;

    public MeterStreamEvent(String system, LocalDateTime now, String connected) {
        this.meterNo = system;
        this.lastSeen = now;
        this.status = connected;
    }

    public String getMeterNo() {
        return meterNo;
    }

    public void setMeterNo(String meterNo) {
        this.meterNo = meterNo;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getStatuscode() {
        return statuscode;
    }

    public void setStatuscode(int statuscode) {
        this.statuscode = statuscode;
    }

    public String getMeterModel() {
        return meterModel;
    }

    public void setMeterModel(String meterModel) {
        this.meterModel = meterModel;
    }

    public String getObisString() {
        return obisString;
    }

    public void setObisString(String obisString) {
        this.obisString = obisString;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getStatusmessage() {
        return statusmessage;
    }

    public void setStatusmessage(String statusmessage) {
        this.statusmessage = statusmessage;
    }
}

