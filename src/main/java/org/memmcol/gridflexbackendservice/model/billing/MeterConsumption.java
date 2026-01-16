package org.memmcol.gridflexbackendservice.model.billing;

import java.math.BigDecimal;

public class MeterConsumption {
    private BigDecimal consumption;
    private ConsumptionType type;
    private BigDecimal cumulativeReading;

    public MeterConsumption(BigDecimal consumption, ConsumptionType type, BigDecimal cumulativeReading) {
        this.consumption = consumption;
        this.type = type;
        this.cumulativeReading = cumulativeReading;
    }

    public BigDecimal getConsumption() {
        return consumption;
    }

    public void setConsumption(BigDecimal consumption) {
        this.consumption = consumption;
    }

    public ConsumptionType getType() {
        return type;
    }

    public void setType(ConsumptionType type) {
        this.type = type;
    }

    public BigDecimal getCumulativeReading() {
        return cumulativeReading;
    }

    public void setCumulativeReading(BigDecimal cumulativeReading) {
        this.cumulativeReading = cumulativeReading;
    }
}
