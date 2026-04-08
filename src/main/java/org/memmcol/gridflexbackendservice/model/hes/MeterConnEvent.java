package org.memmcol.gridflexbackendservice.model.hes;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.meter.Meter;

import java.io.Serializable;
import java.time.LocalDateTime;

public class MeterConnEvent implements Serializable {
    @Id
    private String meterNo;

    private String connectionType;

    private LocalDateTime onlineTime;

    private LocalDateTime offlineTime;

    private LocalDateTime updatedAt;

    private String businessName;

    private Meter meter;

    public String getMeterNo() {
        return meterNo;
    }

    public void setMeterNo(String meterNo) {
        this.meterNo = meterNo;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public LocalDateTime getOnlineTime() {
        return onlineTime;
    }

    public void setOnlineTime(LocalDateTime onlineTime) {
        this.onlineTime = onlineTime;
    }

    public LocalDateTime getOfflineTime() {
        return offlineTime;
    }

    public void setOfflineTime(LocalDateTime offlineTime) {
        this.offlineTime = offlineTime;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Meter getMeter() {
        return meter;
    }

    public void setMeter(Meter meter) {
        this.meter = meter;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }
}
