package org.memmcol.gridflexbackendservice.model.billing;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Id;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigDecimal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MeterReadingSheet implements Serializable {

    @Id
    private UUID id;

    private UUID meterId;
    private UUID nodeId;
    private String meterNumber;
    private UUID orgId;
    private String readingType;
    private BigDecimal lastReading;
    private BigDecimal currentReading;
    private BigDecimal previousReading;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate currentReadingDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate lastReadingDate;

    private String billMonth;
    private String billYear;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private String tariffId;
    private String tariffType;
    private String feederName;
    private String dssName;
    private String meterClass;
    private String type;

    private BigDecimal cumulativeReading;
    private BigDecimal averageConsumption;
    private BigDecimal consumption;
    private BigDecimal totalConsumption;
    private BigDecimal preConsumption;
    private LocalDate date;

    private BigDecimal consumptionPerMeter;
    private BigDecimal meterCount;

    public MeterReadingSheet() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMeterId() {
        return meterId;
    }

    public void setMeterId(UUID meterId) {
        this.meterId = meterId;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public String getMeterNumber() {
        return meterNumber;
    }

    public void setMeterNumber(String meterNumber) {
        this.meterNumber = meterNumber;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public String getReadingType() {
        return readingType;
    }

    public void setReadingType(String readingType) {
        this.readingType = readingType;
    }

    public BigDecimal getLastReading() {
        return lastReading;
    }

    public void setLastReading(BigDecimal lastReading) {
        this.lastReading = lastReading;
    }

    public BigDecimal getCurrentReading() {
        return currentReading;
    }

    public void setCurrentReading(BigDecimal currentReading) {
        this.currentReading = currentReading;
    }

    public LocalDate getCurrentReadingDate() {
        return currentReadingDate;
    }

    public java.math.BigDecimal getPreviousReading() {
        return previousReading;
    }

    public void setPreviousReading(java.math.BigDecimal previousReading) {
        this.previousReading = previousReading;
    }

    public void setCurrentReadingDate(LocalDate currentReadingDate) {
        this.currentReadingDate = currentReadingDate;
    }

    public LocalDate getLastReadingDate() {
        return lastReadingDate;
    }

    public void setLastReadingDate(LocalDate lastReadingDate) {
        this.lastReadingDate = lastReadingDate;
    }

    public String getBillMonth() {
        return billMonth;
    }

    public void setBillMonth(String billMonth) {
        this.billMonth = billMonth;
    }

    public String getBillYear() {
        return billYear;
    }

    public void setBillYear(String billYear) {
        this.billYear = billYear;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTariffId() {
        return tariffId;
    }

    public void setTariffId(String tariffId) {
        this.tariffId = tariffId;
    }

    public String getTariffType() {
        return tariffType;
    }

    public void setTariffType(String tariffType) {
        this.tariffType = tariffType;
    }

    public String getFeederName() {
        return feederName;
    }

    public void setFeederName(String feederName) {
        this.feederName = feederName;
    }

    public String getDssName() {
        return dssName;
    }

    public void setDssName(String dssName) {
        this.dssName = dssName;
    }

    public String getMeterClass() {
        return meterClass;
    }

    public void setMeterClass(String meterClass) {
        this.meterClass = meterClass;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getCumulativeReading() {
        return cumulativeReading;
    }

    public void setCumulativeReading(BigDecimal cumulativeReading) {
        this.cumulativeReading = cumulativeReading;
    }

    public BigDecimal getAverageConsumption() {
        return averageConsumption;
    }

    public void setAverageConsumption(BigDecimal averageConsumption) {
        this.averageConsumption = averageConsumption;
    }

    public BigDecimal getConsumption() {
        return consumption;
    }

    public void setConsumption(BigDecimal consumption) {
        this.consumption = consumption;
    }

    public BigDecimal getPreConsumption() {
        return preConsumption;
    }

    public BigDecimal getTotalConsumption() {
        return totalConsumption;
    }

    public void setTotalConsumption(BigDecimal totalConsumption) {
        this.totalConsumption = totalConsumption;
    }

    public void setPreConsumption(BigDecimal preConsumption) {
        this.preConsumption = preConsumption;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getConsumptionPerMeter() {
        return consumptionPerMeter;
    }

    public void setConsumptionPerMeter(BigDecimal consumptionPerMeter) {
        this.consumptionPerMeter = consumptionPerMeter;
    }

    public BigDecimal getMeterCount() {
        return meterCount;
    }

    public void setMeterCount(BigDecimal meterCount) {
        this.meterCount = meterCount;
    }
}

