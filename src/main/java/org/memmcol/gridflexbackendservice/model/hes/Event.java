package org.memmcol.gridflexbackendservice.model.hes;

import jakarta.persistence.Id;
import org.apache.ibatis.annotations.Result;
import org.memmcol.gridflexbackendservice.model.meter.Meter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class Event implements Serializable {

    private int id;
    private String srcTable;
    private String meterNumber;
    private String meterModel;
    private String eventTypeId;
    private String eventCode;
    private LocalDateTime eventTime;
    private String currentThreshold;
    private String eventName;
    int criticalLevel;
    private LocalDateTime createdAt;
    private String event;
    private String eventType;

    private String rechargeAmountKwh;
    private String rechargeToken;

    private String manageTokenTypeCode;
    private String manageToken;
    private String mgtTokenTypeDescription;
    private String reasonDescription;
    private String reasonOfOperationCode;
    private String totalAbsoluteActiveKwh;
    private String balanceKwh;

    private EventType eventTypeModel;

    private Meter meter;

    public String getSrcTable() {
        return srcTable;
    }

    public void setSrcTable(String srcTable) {
        this.srcTable = srcTable;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMeterNumber() {
        return meterNumber;
    }

    public void setMeterNumber(String meterNumber) {
        this.meterNumber = meterNumber;
    }

    public String getMeterModel() {
        return meterModel;
    }

    public void setMeterModel(String meterModel) {
        this.meterModel = meterModel;
    }

    public String getEventTypeId() {
        return eventTypeId;
    }

    public void setEventTypeId(String eventTypeId) {
        this.eventTypeId = eventTypeId;
    }

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Meter getMeter() {
        return meter;
    }

    public void setMeter(Meter meter) {
        this.meter = meter;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public EventType getEventTypeModel() {
        return eventTypeModel;
    }

    public void setEventTypeModel(EventType eventTypeModel) {
        this.eventTypeModel = eventTypeModel;
    }

    public String getCurrentThreshold() {
        return currentThreshold;
    }

    public void setCurrentThreshold(String currentThreshold) {
        this.currentThreshold = currentThreshold;
    }

    public int getCriticalLevel() {
        return criticalLevel;
    }

    public void setCriticalLevel(int criticalLevel) {
        this.criticalLevel = criticalLevel;
    }

    public String getRechargeAmountKwh() {
        return rechargeAmountKwh;
    }

    public void setRechargeAmountKwh(String rechargeAmountKwh) {
        this.rechargeAmountKwh = rechargeAmountKwh;
    }

    public String getManageTokenTypeCode() {
        return manageTokenTypeCode;
    }

    public void setManageTokenTypeCode(String manageTokenTypeCode) {
        this.manageTokenTypeCode = manageTokenTypeCode;
    }

    public String getMgtTokenTypeDescription() {
        return mgtTokenTypeDescription;
    }

    public void setMgtTokenTypeDescription(String mgtTokenTypeDescription) {
        this.mgtTokenTypeDescription = mgtTokenTypeDescription;
    }

    public String getReasonDescription() {
        return reasonDescription;
    }

    public void setReasonDescription(String reasonDescription) {
        this.reasonDescription = reasonDescription;
    }

    public String getReasonOfOperationCode() {
        return reasonOfOperationCode;
    }

    public void setReasonOfOperationCode(String reasonOfOperationCode) {
        this.reasonOfOperationCode = reasonOfOperationCode;
    }

    public String getTotalAbsoluteActiveKwh() {
        return totalAbsoluteActiveKwh;
    }

    public void setTotalAbsoluteActiveKwh(String totalAbsoluteActiveKwh) {
        this.totalAbsoluteActiveKwh = totalAbsoluteActiveKwh;
    }

    public String getBalanceKwh() {
        return balanceKwh;
    }

    public void setBalanceKwh(String balanceKwh) {
        this.balanceKwh = balanceKwh;
    }

    public String getManageToken() {
        return manageToken;
    }

    public void setManageToken(String manageToken) {
        this.manageToken = manageToken;
    }

    public String getRechargeToken() {
        return rechargeToken;
    }

    public void setRechargeToken(String rechargeToken) {
        this.rechargeToken = rechargeToken;
    }
}
