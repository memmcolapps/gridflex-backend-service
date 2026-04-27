package org.memmcol.gridflexbackendservice.model.hes;

import java.time.LocalDateTime;

public class MeterLastCommunication {
    private String meterNo;
    private LocalDateTime lastOnline;

    public MeterLastCommunication() {}

    public MeterLastCommunication(String meterNo, LocalDateTime lastOnline) {
        this.meterNo = meterNo;
        this.lastOnline = lastOnline;
    }

    public String getMeterNo() {
        return meterNo;
    }

    public void setMeterNo(String meterNo) {
        this.meterNo = meterNo;
    }

    public LocalDateTime getLastOnline() {
        return lastOnline;
    }

    public void setLastOnline(LocalDateTime lastOnline) {
        this.lastOnline = lastOnline;
    }
}