package org.memmcol.gridflexbackendservice.model.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

public class MeterReadingDTO {
    private String name;
    private String meterNumber;
    private UUID orgId;
    private String tariffType;
    private String month;
    private String readingType;
    private BigDecimal lastReading;
    private BigDecimal currentReading;
    private String year;
    private String address;
    private String feederName;
    private String businessName;
    private String serviceName;
    private String dssName;
    private String regionName;
    private String meterClass;
    private String meterClass2;

    // getters and setters
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public String getTariffType() {
        return tariffType;
    }

    public void setTariffType(String tariffType) {
        this.tariffType = tariffType;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
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

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public void setMeterNumber(String meterNumber) {
        this.meterNumber = meterNumber;
    }

    public String getMeterNumber() {
        return meterNumber;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public String getFeederName() {
        return feederName;
    }

    public void setFeederName(String feederName) {
        this.feederName = feederName;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getDssName() {
        return dssName;
    }

    public void setDssName(String dssName) {
        this.dssName = dssName;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getMeterClass() {
        return meterClass;
    }

    public void setMeterClass(String meterClass) {
        this.meterClass = meterClass;
    }

    public String getMeterClass2() {
        return meterClass2;
    }

    public void setMeterClass2(String meterClass2) {
        this.meterClass2 = meterClass2;
    }
}
