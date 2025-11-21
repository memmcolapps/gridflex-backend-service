package org.memmcol.gridflexbackendservice.model.hes;

import java.time.LocalDateTime;
import java.util.UUID;

public class MeterStreamEvent {

    private String meterNo;
    private LocalDateTime lastSeen;
    private String status;

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
}

