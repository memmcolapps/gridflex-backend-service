package org.memmcol.gridflexbackendservice.model.hes;

import java.io.Serializable;

public class Profile implements Serializable {
    private String meterSerial;
    private String meterNumber;
    private String meterModel;
    private String entryTimestamp;
    private String totalImportActiveEnergy;
    private String totalExportActiveEnergy;
    private String receiveAt;

    // Profile channel one
    private String meterHealthIndicator;
    private String totalInstantaneousActivePower;
    private String totalInstantaneousApparentPower;
    private String l1CurrentHarmonyThd;
    private String l2CurrentHarmonyThd;
    private String l3CurrentHarmonyThd;
    private String l1VoltageHarmonyThd;
    private String l2VoltageHarmonyThd;
    private String l3VoltageHarmonyThd;

    // Daily and Monthly billing profile
    private String totalAbsoluteActiveEnergy;
    private String exportActiveEnergy;
    private String importActiveEnergy;
    private String importReactiveEnergy;
    private String exportReactiveEnergy;
    private String remainingCreditAmount;
    private String importActiveMd;
    private String importActiveMdTime;
    private String t1ActiveEnergy;
    private String t2ActiveEnergy;
    private String t3ActiveEnergy;
    private String t4ActiveEnergy;
    private String totalActiveEnergy;
    private String totalApparentEnergy;
    private String t1TotalApparentEnergy;
    private String t2TotalApparentEnergy;
    private String t3TotalApparentEnergy;
    private String t4TotalApparentEnergy;
    private String activeMaximumDemand;
    private String totalApparentDemand;
    private String totalApparentDemandTime;

    public String getMeterSerial() {
        return meterSerial;
    }

    public void setMeterSerial(String meterSerial) {
        this.meterSerial = meterSerial;
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

    public String getEntryTimestamp() {
        return entryTimestamp;
    }

    public void setEntryTimestamp(String entryTimestamp) {
        this.entryTimestamp = entryTimestamp;
    }

    public String getTotalImportActiveEnergy() {
        return totalImportActiveEnergy;
    }

    public void setTotalImportActiveEnergy(String totalImportActiveEnergy) {
        this.totalImportActiveEnergy = totalImportActiveEnergy;
    }

    public String getTotalExportActiveEnergy() {
        return totalExportActiveEnergy;
    }

    public void setTotalExportActiveEnergy(String totalExportActiveEnergy) {
        this.totalExportActiveEnergy = totalExportActiveEnergy;
    }

    public String getReceiveAt() {
        return receiveAt;
    }

    public void setReceiveAt(String receiveAt) {
        this.receiveAt = receiveAt;
    }

    public String getMeterHealthIndicator() {
        return meterHealthIndicator;
    }

    public void setMeterHealthIndicator(String meterHealthIndicator) {
        this.meterHealthIndicator = meterHealthIndicator;
    }

    public String getTotalInstantaneousActivePower() {
        return totalInstantaneousActivePower;
    }

    public void setTotalInstantaneousActivePower(String totalInstantaneousActivePower) {
        this.totalInstantaneousActivePower = totalInstantaneousActivePower;
    }

    public String getTotalInstantaneousApparentPower() {
        return totalInstantaneousApparentPower;
    }

    public void setTotalInstantaneousApparentPower(String totalInstantaneousApparentPower) {
        this.totalInstantaneousApparentPower = totalInstantaneousApparentPower;
    }

    public String getL1CurrentHarmonyThd() {
        return l1CurrentHarmonyThd;
    }

    public void setL1CurrentHarmonyThd(String l1CurrentHarmonyThd) {
        this.l1CurrentHarmonyThd = l1CurrentHarmonyThd;
    }

    public String getL2CurrentHarmonyThd() {
        return l2CurrentHarmonyThd;
    }

    public void setL2CurrentHarmonyThd(String l2CurrentHarmonyThd) {
        this.l2CurrentHarmonyThd = l2CurrentHarmonyThd;
    }

    public String getL3CurrentHarmonyThd() {
        return l3CurrentHarmonyThd;
    }

    public void setL3CurrentHarmonyThd(String l3CurrentHarmonyThd) {
        this.l3CurrentHarmonyThd = l3CurrentHarmonyThd;
    }

    public String getL1VoltageHarmonyThd() {
        return l1VoltageHarmonyThd;
    }

    public void setL1VoltageHarmonyThd(String l1VoltageHarmonyThd) {
        this.l1VoltageHarmonyThd = l1VoltageHarmonyThd;
    }

    public String getL2VoltageHarmonyThd() {
        return l2VoltageHarmonyThd;
    }

    public void setL2VoltageHarmonyThd(String l2VoltageHarmonyThd) {
        this.l2VoltageHarmonyThd = l2VoltageHarmonyThd;
    }

    public String getL3VoltageHarmonyThd() {
        return l3VoltageHarmonyThd;
    }

    public void setL3VoltageHarmonyThd(String l3VoltageHarmonyThd) {
        this.l3VoltageHarmonyThd = l3VoltageHarmonyThd;
    }

    public String getTotalAbsoluteActiveEnergy() {
        return totalAbsoluteActiveEnergy;
    }

    public void setTotalAbsoluteActiveEnergy(String totalAbsoluteActiveEnergy) {
        this.totalAbsoluteActiveEnergy = totalAbsoluteActiveEnergy;
    }

    public String getExportActiveEnergy() {
        return exportActiveEnergy;
    }

    public void setExportActiveEnergy(String exportActiveEnergy) {
        this.exportActiveEnergy = exportActiveEnergy;
    }

    public String getImportActiveEnergy() {
        return importActiveEnergy;
    }

    public void setImportActiveEnergy(String importActiveEnergy) {
        this.importActiveEnergy = importActiveEnergy;
    }

    public String getImportReactiveEnergy() {
        return importReactiveEnergy;
    }

    public void setImportReactiveEnergy(String importReactiveEnergy) {
        this.importReactiveEnergy = importReactiveEnergy;
    }

    public String getExportReactiveEnergy() {
        return exportReactiveEnergy;
    }

    public void setExportReactiveEnergy(String exportReactiveEnergy) {
        this.exportReactiveEnergy = exportReactiveEnergy;
    }

    public String getRemainingCreditAmount() {
        return remainingCreditAmount;
    }

    public void setRemainingCreditAmount(String remainingCreditAmount) {
        this.remainingCreditAmount = remainingCreditAmount;
    }

    public String getImportActiveMd() {
        return importActiveMd;
    }

    public void setImportActiveMd(String importActiveMd) {
        this.importActiveMd = importActiveMd;
    }

    public String getImportActiveMdTime() {
        return importActiveMdTime;
    }

    public void setImportActiveMdTime(String importActiveMdTime) {
        this.importActiveMdTime = importActiveMdTime;
    }

    public String getT1ActiveEnergy() {
        return t1ActiveEnergy;
    }

    public void setT1ActiveEnergy(String t1ActiveEnergy) {
        this.t1ActiveEnergy = t1ActiveEnergy;
    }

    public String getT2ActiveEnergy() {
        return t2ActiveEnergy;
    }

    public void setT2ActiveEnergy(String t2ActiveEnergy) {
        this.t2ActiveEnergy = t2ActiveEnergy;
    }

    public String getT3ActiveEnergy() {
        return t3ActiveEnergy;
    }

    public void setT3ActiveEnergy(String t3ActiveEnergy) {
        this.t3ActiveEnergy = t3ActiveEnergy;
    }

    public String getT4ActiveEnergy() {
        return t4ActiveEnergy;
    }

    public void setT4ActiveEnergy(String t4ActiveEnergy) {
        this.t4ActiveEnergy = t4ActiveEnergy;
    }

    public String getTotalActiveEnergy() {
        return totalActiveEnergy;
    }

    public void setTotalActiveEnergy(String totalActiveEnergy) {
        this.totalActiveEnergy = totalActiveEnergy;
    }

    public String getTotalApparentEnergy() {
        return totalApparentEnergy;
    }

    public void setTotalApparentEnergy(String totalApparentEnergy) {
        this.totalApparentEnergy = totalApparentEnergy;
    }

    public String getT1TotalApparentEnergy() {
        return t1TotalApparentEnergy;
    }

    public void setT1TotalApparentEnergy(String t1TotalApparentEnergy) {
        this.t1TotalApparentEnergy = t1TotalApparentEnergy;
    }

    public String getT2TotalApparentEnergy() {
        return t2TotalApparentEnergy;
    }

    public void setT2TotalApparentEnergy(String t2TotalApparentEnergy) {
        this.t2TotalApparentEnergy = t2TotalApparentEnergy;
    }

    public String getT3TotalApparentEnergy() {
        return t3TotalApparentEnergy;
    }

    public void setT3TotalApparentEnergy(String t3TotalApparentEnergy) {
        this.t3TotalApparentEnergy = t3TotalApparentEnergy;
    }

    public String getT4TotalApparentEnergy() {
        return t4TotalApparentEnergy;
    }

    public void setT4TotalApparentEnergy(String t4TotalApparentEnergy) {
        this.t4TotalApparentEnergy = t4TotalApparentEnergy;
    }

    public String getActiveMaximumDemand() {
        return activeMaximumDemand;
    }

    public void setActiveMaximumDemand(String activeMaximumDemand) {
        this.activeMaximumDemand = activeMaximumDemand;
    }

    public String getTotalApparentDemand() {
        return totalApparentDemand;
    }

    public void setTotalApparentDemand(String totalApparentDemand) {
        this.totalApparentDemand = totalApparentDemand;
    }

    public String getTotalApparentDemandTime() {
        return totalApparentDemandTime;
    }

    public void setTotalApparentDemandTime(String totalApparentDemandTime) {
        this.totalApparentDemandTime = totalApparentDemandTime;
    }
}
