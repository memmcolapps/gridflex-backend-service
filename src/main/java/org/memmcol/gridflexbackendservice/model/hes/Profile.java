package org.memmcol.gridflexbackendservice.model.hes;

import org.memmcol.gridflexbackendservice.model.meter.Meter;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Profile implements Serializable {
    private String orgId;
    private String meterNumber;
    private String meterModel;
    private String entryTimestamp;
    private String totalImportActiveEnergy;
    private String totalExportActiveEnergy;
    private LocalDateTime receivedAt;

    // Profile channel one
    private String meterHealthIndicator;
    private String instantaneousVoltageL1;
    private String instantaneousVoltageL2;
    private String instantaneousVoltageL3;
    private String instantaneousCurrentL1;
    private String instantaneousCurrentL2;
    private String instantaneousCurrentL3;
    private String instantaneousActivePower;
    private String instantaneousReactiveImport;
    private String instantaneousReactiveExport;
    private String instantaneousPowerFactor;
    private String instantaneousApparentPower;
    private String instantaneousNetFrequency;

    // Profile channel Two
    private String activeEnergyImport;
    private String importActiveEnergyImportRate1;
    private String importActiveEnergyImportRate2;
    private String importActiveEnergyImportRate3;
    private String importActiveEnergyImportRate4;
    private String importActiveEnergyCombinedTotal;
    private String reactiveEnergyImport;
    private String reactiveEnergyExport;
    private String apparentEnergyImport;
    private String apparentEnergyExport;
    private String voltAngleL1L2;
    private String voltAngleL1L3;

    // Profile channel Three
    private String activePowerL1;
    private String activePowerL2;
    private String activePowerL3;
    private String powerFactorL1;
    private String powerFactorL2;
    private String powerFactorL3;
    private String gridFrequency;

    // Daily and Monthly billing profile
    private String totalAbsoluteActiveEnergy;
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
    private String activeMaximumDemandTime;
    private String totalApparentDemand;
    private String totalApparentDemandTime;

    // Profile channel one / daily / monthly data & energy household
    private String activeEnergyImportOngrid;
    private String activeEnergyImportOffgrid;
    private String activeEnergyExport;
    private String activeEnergyExportOngrid;
    private String activeEnergyExportOffgrid;

    // Profile channel two household
    private String voltageL1;
    private String voltageL2;
    private String currentL1;
    private String currentL2;
    private String currentL3;

    // Daily / Monthly billing data house
    private String creditOngrid;
    private String creditffgrid;

    private Meter meter;

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
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

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getMeterHealthIndicator() {
        return meterHealthIndicator;
    }

    public void setMeterHealthIndicator(String meterHealthIndicator) {
        this.meterHealthIndicator = meterHealthIndicator;
    }

    public String getInstantaneousVoltageL1() {
        return instantaneousVoltageL1;
    }

    public void setInstantaneousVoltageL1(String instantaneousVoltageL1) {
        this.instantaneousVoltageL1 = instantaneousVoltageL1;
    }

    public String getInstantaneousVoltageL2() {
        return instantaneousVoltageL2;
    }

    public void setInstantaneousVoltageL2(String instantaneousVoltageL2) {
        this.instantaneousVoltageL2 = instantaneousVoltageL2;
    }

    public String getInstantaneousVoltageL3() {
        return instantaneousVoltageL3;
    }

    public void setInstantaneousVoltageL3(String instantaneousVoltageL3) {
        this.instantaneousVoltageL3 = instantaneousVoltageL3;
    }

    public String getInstantaneousCurrentL1() {
        return instantaneousCurrentL1;
    }

    public void setInstantaneousCurrentL1(String instantaneousCurrentL1) {
        this.instantaneousCurrentL1 = instantaneousCurrentL1;
    }

    public String getInstantaneousCurrentL2() {
        return instantaneousCurrentL2;
    }

    public void setInstantaneousCurrentL2(String instantaneousCurrentL2) {
        this.instantaneousCurrentL2 = instantaneousCurrentL2;
    }

    public String getInstantaneousCurrentL3() {
        return instantaneousCurrentL3;
    }

    public void setInstantaneousCurrentL3(String instantaneousCurrentL3) {
        this.instantaneousCurrentL3 = instantaneousCurrentL3;
    }

    public String getInstantaneousActivePower() {
        return instantaneousActivePower;
    }

    public void setInstantaneousActivePower(String instantaneousActivePower) {
        this.instantaneousActivePower = instantaneousActivePower;
    }

    public String getInstantaneousReactiveImport() {
        return instantaneousReactiveImport;
    }

    public void setInstantaneousReactiveImport(String instantaneousReactiveImport) {
        this.instantaneousReactiveImport = instantaneousReactiveImport;
    }

    public String getInstantaneousReactiveExport() {
        return instantaneousReactiveExport;
    }

    public void setInstantaneousReactiveExport(String instantaneousReactiveExport) {
        this.instantaneousReactiveExport = instantaneousReactiveExport;
    }

    public String getInstantaneousPowerFactor() {
        return instantaneousPowerFactor;
    }

    public void setInstantaneousPowerFactor(String instantaneousPowerFactor) {
        this.instantaneousPowerFactor = instantaneousPowerFactor;
    }

    public String getInstantaneousApparentPower() {
        return instantaneousApparentPower;
    }

    public void setInstantaneousApparentPower(String instantaneousApparentPower) {
        this.instantaneousApparentPower = instantaneousApparentPower;
    }

    public String getInstantaneousNetFrequency() {
        return instantaneousNetFrequency;
    }

    public void setInstantaneousNetFrequency(String instantaneousNetFrequency) {
        this.instantaneousNetFrequency = instantaneousNetFrequency;
    }

    public String getActiveEnergyImport() {
        return activeEnergyImport;
    }

    public void setActiveEnergyImport(String activeEnergyImport) {
        this.activeEnergyImport = activeEnergyImport;
    }

    public String getImportActiveEnergyImportRate1() {
        return importActiveEnergyImportRate1;
    }

    public void setImportActiveEnergyImportRate1(String importActiveEnergyImportRate1) {
        this.importActiveEnergyImportRate1 = importActiveEnergyImportRate1;
    }

    public String getImportActiveEnergyImportRate2() {
        return importActiveEnergyImportRate2;
    }

    public void setImportActiveEnergyImportRate2(String importActiveEnergyImportRate2) {
        this.importActiveEnergyImportRate2 = importActiveEnergyImportRate2;
    }

    public String getImportActiveEnergyImportRate3() {
        return importActiveEnergyImportRate3;
    }

    public void setImportActiveEnergyImportRate3(String importActiveEnergyImportRate3) {
        this.importActiveEnergyImportRate3 = importActiveEnergyImportRate3;
    }

    public String getImportActiveEnergyImportRate4() {
        return importActiveEnergyImportRate4;
    }

    public void setImportActiveEnergyImportRate4(String importActiveEnergyImportRate4) {
        this.importActiveEnergyImportRate4 = importActiveEnergyImportRate4;
    }

    public String getImportActiveEnergyCombinedTotal() {
        return importActiveEnergyCombinedTotal;
    }

    public void setImportActiveEnergyCombinedTotal(String importActiveEnergyCombinedTotal) {
        this.importActiveEnergyCombinedTotal = importActiveEnergyCombinedTotal;
    }

    public String getReactiveEnergyImport() {
        return reactiveEnergyImport;
    }

    public void setReactiveEnergyImport(String reactiveEnergyImport) {
        this.reactiveEnergyImport = reactiveEnergyImport;
    }

    public String getReactiveEnergyExport() {
        return reactiveEnergyExport;
    }

    public void setReactiveEnergyExport(String reactiveEnergyExport) {
        this.reactiveEnergyExport = reactiveEnergyExport;
    }

    public String getApparentEnergyImport() {
        return apparentEnergyImport;
    }

    public void setApparentEnergyImport(String apparentEnergyImport) {
        this.apparentEnergyImport = apparentEnergyImport;
    }

    public String getApparentEnergyExport() {
        return apparentEnergyExport;
    }

    public void setApparentEnergyExport(String apparentEnergyExport) {
        this.apparentEnergyExport = apparentEnergyExport;
    }

    public String getTotalAbsoluteActiveEnergy() {
        return totalAbsoluteActiveEnergy;
    }

    public void setTotalAbsoluteActiveEnergy(String totalAbsoluteActiveEnergy) {
        this.totalAbsoluteActiveEnergy = totalAbsoluteActiveEnergy;
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

    public String getActiveMaximumDemandTime() {
        return activeMaximumDemandTime;
    }

    public void setActiveMaximumDemandTime(String activeMaximumDemandTime) {
        this.activeMaximumDemandTime = activeMaximumDemandTime;
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

    public String getActiveEnergyImportOngrid() {
        return activeEnergyImportOngrid;
    }

    public void setActiveEnergyImportOngrid(String activeEnergyImportOngrid) {
        this.activeEnergyImportOngrid = activeEnergyImportOngrid;
    }

    public String getActiveEnergyImportOffgrid() {
        return activeEnergyImportOffgrid;
    }

    public void setActiveEnergyImportOffgrid(String activeEnergyImportOffgrid) {
        this.activeEnergyImportOffgrid = activeEnergyImportOffgrid;
    }

    public String getActiveEnergyExport() {
        return activeEnergyExport;
    }

    public void setActiveEnergyExport(String activeEnergyExport) {
        this.activeEnergyExport = activeEnergyExport;
    }

    public String getActiveEnergyExportOngrid() {
        return activeEnergyExportOngrid;
    }

    public void setActiveEnergyExportOngrid(String activeEnergyExportOngrid) {
        this.activeEnergyExportOngrid = activeEnergyExportOngrid;
    }

    public String getActiveEnergyExportOffgrid() {
        return activeEnergyExportOffgrid;
    }

    public void setActiveEnergyExportOffgrid(String activeEnergyExportOffgrid) {
        this.activeEnergyExportOffgrid = activeEnergyExportOffgrid;
    }

    public String getVoltageL1() {
        return voltageL1;
    }

    public void setVoltageL1(String voltageL1) {
        this.voltageL1 = voltageL1;
    }

    public String getVoltageL2() {
        return voltageL2;
    }

    public void setVoltageL2(String voltageL2) {
        this.voltageL2 = voltageL2;
    }

    public String getCurrentL1() {
        return currentL1;
    }

    public void setCurrentL1(String currentL1) {
        this.currentL1 = currentL1;
    }

    public String getCurrentL2() {
        return currentL2;
    }

    public void setCurrentL2(String currentL2) {
        this.currentL2 = currentL2;
    }

    public String getCurrentL3() {
        return currentL3;
    }

    public void setCurrentL3(String currentL3) {
        this.currentL3 = currentL3;
    }

    public String getCreditOngrid() {
        return creditOngrid;
    }

    public void setCreditOngrid(String creditOngrid) {
        this.creditOngrid = creditOngrid;
    }

    public String getCreditffgrid() {
        return creditffgrid;
    }

    public void setCreditffgrid(String creditffgrid) {
        this.creditffgrid = creditffgrid;
    }

    public Meter getMeter() {
        return meter;
    }

    public void setMeter(Meter meter) {
        this.meter = meter;
    }

    public String getVoltAngleL1L2() {
        return voltAngleL1L2;
    }

    public void setVoltAngleL1L2(String voltAngleL1L2) {
        this.voltAngleL1L2 = voltAngleL1L2;
    }

    public String getVoltAngleL1L3() {
        return voltAngleL1L3;
    }

    public void setVoltAngleL1L3(String voltAngleL1L3) {
        this.voltAngleL1L3 = voltAngleL1L3;
    }

    public String getActivePowerL1() {
        return activePowerL1;
    }

    public void setActivePowerL1(String activePowerL1) {
        this.activePowerL1 = activePowerL1;
    }

    public String getActivePowerL2() {
        return activePowerL2;
    }

    public void setActivePowerL2(String activePowerL2) {
        this.activePowerL2 = activePowerL2;
    }

    public String getActivePowerL3() {
        return activePowerL3;
    }

    public void setActivePowerL3(String activePowerL3) {
        this.activePowerL3 = activePowerL3;
    }

    public String getPowerFactorL1() {
        return powerFactorL1;
    }

    public void setPowerFactorL1(String powerFactorL1) {
        this.powerFactorL1 = powerFactorL1;
    }

    public String getPowerFactorL2() {
        return powerFactorL2;
    }

    public void setPowerFactorL2(String powerFactorL2) {
        this.powerFactorL2 = powerFactorL2;
    }

    public String getPowerFactorL3() {
        return powerFactorL3;
    }

    public void setPowerFactorL3(String powerFactorL3) {
        this.powerFactorL3 = powerFactorL3;
    }

    public String getGridFrequency() {
        return gridFrequency;
    }

    public void setGridFrequency(String gridFrequency) {
        this.gridFrequency = gridFrequency;
    }
}
