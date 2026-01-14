package org.memmcol.gridflexbackendservice.service.billing;

import org.memmcol.gridflexbackendservice.model.billing.ConsumptionType;
import org.memmcol.gridflexbackendservice.model.billing.MeterConsumption;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ConsumptionCalculator {

//    private static final int MAX_READING = 1_000_000;
    private static final BigDecimal MAX_READING = BigDecimal.valueOf(1_000_000);

    public MeterConsumption calculate(
            Meter meter,
//            BigDecimal oldReading,
            BigDecimal newReading,
            BigDecimal average,
            BigDecimal previousCumulative) {

        // FIXED (Virtual Meter)
        if (meter.getMeterType().equalsIgnoreCase("VIRTUAL")) {
            return new MeterConsumption(
                    BigDecimal.valueOf(Double.parseDouble(meter.getFixedEnergy())),
                    ConsumptionType.FIXED
            );
        }

        // ESTIMATE (Agent failed → current reading = 0)
        if (newReading != null && newReading.equals(BigDecimal.ZERO)) {
            return new MeterConsumption(
                    average,
                    ConsumptionType.ESTIMATE
            );
        }

        // ESTIMATE (Missing reading)
        if (newReading == null) {
            return new MeterConsumption(
                    average,
                    ConsumptionType.ESTIMATE
            );
        }

        // ACTUAL NORMAL
        if (newReading.compareTo(previousCumulative) > 0) {
            return new MeterConsumption(
                    newReading.subtract(previousCumulative),
                    ConsumptionType.ACTUAL_NORMAL
            );
        }

        // ACTUAL ROLLOVER
        if (newReading.compareTo(previousCumulative) < 0) {
            return new MeterConsumption(
                    newReading.add(MAX_READING.subtract(previousCumulative)),
                    ConsumptionType.ACTUAL_ROLLOVER
            );
        }

        return new MeterConsumption(BigDecimal.ZERO, ConsumptionType.MINIMUM);
    }
}

///-------------
//        // ACTUAL NORMAL
//        if (newReading > oldReading) {
//            return new MeterConsumption(
//                    (double) (newReading - oldReading),
////                    (double) (newReading - cummulativeReading),
//                    ConsumptionType.ACTUAL_NORMAL
//            );
//        }
//
//        // ACTUAL ROLLOVER
//        if (newReading < oldReading) {
//            return new MeterConsumption(
//                    (double) (newReading + (MAX_READING - oldReading)),
//                    ConsumptionType.ACTUAL_ROLLOVER
//            );
//        }

// MINIMUM