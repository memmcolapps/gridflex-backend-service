package org.memmcol.gridflexbackendservice.model.meter;

import jakarta.persistence.Id;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MeterReadingSheet implements Serializable {

    @Id
    private UUID id;

    private UUID meterId;
    private String meterNumber;
    private UUID orgId;
    private UUID tariffId;
    private UUID nodeId;
    private String readingType;
    private BigDecimal lastReading;
    private BigDecimal currentReading;
    private LocalDateTime currentReadingDate;
    private LocalDateTime lastReadingDate;
    private String billMonth;
    private String billYear;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String tariffType;
    private String name;

    // Getters and Setters
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

    public void setMeterNumber(String meterNumber) {
        this.meterNumber = meterNumber;
    }

    public String getMeterNumber() {
        return meterNumber;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public UUID getTariffId() {
        return tariffId;
    }

    public void setTariffId(UUID tariffId) {
        this.tariffId = tariffId;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
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

    public LocalDateTime getCurrentReadingDate() {
        return currentReadingDate;
    }

    public void setCurrentReadingDate(LocalDateTime currentReadingDate) {
        this.currentReadingDate = currentReadingDate;
    }

    public LocalDateTime getLastReadingDate() {
        return lastReadingDate;
    }

    public void setLastReadingDate(LocalDateTime lastReadingDate) {
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

    public String getTariffType() {
        return tariffType;
    }

    public void setTariffType(String tariffType) {
        this.tariffType = tariffType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

