package org.memmcol.gridflexbackendservice.model.audit;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Date;

@Document(collection = "uptime_reports")
public class UptimeReport implements Serializable {
    @Id
    private String id;

    private String serviceName;

    private String reportType; // "DAILY" or "MONTHLY"

    private LocalDate createdAt;     // used when DAILY
    private String month;    // used when MONTHLY

    private double uptimePercent;
    private double downtimePercent;
    private long uptimeMinutes;
    private long downtimeMinutes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    //    public YearMonth getMonth() {
//        return month;
//    }
//
//    public void setMonth(YearMonth month) {
//        this.month = month;
//    }

    public double getUptimePercent() {
        return uptimePercent;
    }

    public void setUptimePercent(double uptimePercent) {
        this.uptimePercent = uptimePercent;
    }

    public double getDowntimePercent() {
        return downtimePercent;
    }

    public void setDowntimePercent(double downtimePercent) {
        this.downtimePercent = downtimePercent;
    }

    public long getUptimeMinutes() {
        return uptimeMinutes;
    }

    public void setUptimeMinutes(long uptimeMinutes) {
        this.uptimeMinutes = uptimeMinutes;
    }

    public long getDowntimeMinutes() {
        return downtimeMinutes;
    }

    public void setDowntimeMinutes(long downtimeMinutes) {
        this.downtimeMinutes = downtimeMinutes;
    }
}

