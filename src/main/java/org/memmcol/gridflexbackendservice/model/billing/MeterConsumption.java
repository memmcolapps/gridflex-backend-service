package org.memmcol.gridflexbackendservice.model.billing;

import java.math.BigDecimal;

public class MeterConsumption {
    private BigDecimal consumption;
    private ConsumptionType type;

    public MeterConsumption(BigDecimal consumption, ConsumptionType type) {
        this.consumption = consumption;
        this.type = type;
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
}
