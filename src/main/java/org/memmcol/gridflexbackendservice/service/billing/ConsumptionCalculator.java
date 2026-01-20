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
            BigDecimal oldReading,
            BigDecimal newReading,
            BigDecimal average,
            BigDecimal previousCumulative,
            BigDecimal energyConsumption
//            BigDecimal newCumulative
    ) {

        if(energyConsumption == null){
            energyConsumption = BigDecimal.ZERO;
        }
        // FIXED (Virtual Meter)
        if (meter.getMeterType().equalsIgnoreCase("VIRTUAL")) {
            previousCumulative = previousCumulative.add(energyConsumption);
            return new MeterConsumption(
                    BigDecimal.valueOf(Double.parseDouble(meter.getFixedEnergy())),
                    ConsumptionType.FIXED,
                    previousCumulative);
        }

        // ESTIMATE (Agent failed → current reading = 0)
        if (newReading != null && newReading.equals(BigDecimal.ZERO)) {
            BigDecimal consumption = average;
            previousCumulative = previousCumulative.add(energyConsumption);
            return new MeterConsumption(
                    consumption,
                    ConsumptionType.ESTIMATE,
                    previousCumulative
            );
        }

        // ESTIMATE (Missing reading)
        if (newReading == null) {
            BigDecimal consumption = average;
            previousCumulative = previousCumulative.add(energyConsumption);
            return new MeterConsumption(
                    consumption,
                    ConsumptionType.ESTIMATE,
                    previousCumulative
            );
        }

        // ACTUAL NORMAL
        if (newReading.compareTo(previousCumulative) > 0) {
            previousCumulative = previousCumulative.add(energyConsumption);
            BigDecimal consumption = newReading.subtract(previousCumulative);
            return new MeterConsumption(
                    consumption,
                    ConsumptionType.ACTUAL_NORMAL,
                    previousCumulative
            );
        }

        // ACTUAL ROLLOVER
        if (newReading.compareTo(previousCumulative) < 0) {

            previousCumulative = previousCumulative.add(energyConsumption);
            System.out.println("wwwww1: "+previousCumulative);
            System.out.println("newReading>>>: "+newReading);
//            BigDecimal consumption = consumption
            BigDecimal consumption = newReading.add(MAX_READING.subtract(previousCumulative));
            System.out.println("wwwww2: "+consumption);
//            previousCumulative = newReading.add(MAX_READING.subtract(previousCumulative));
            previousCumulative = newReading;
//            previousCumulative = previousCumulative.add(energyConsumption);
            return new MeterConsumption(
                    consumption,
                    ConsumptionType.ACTUAL_ROLLOVER,
                    previousCumulative
            );
        }

        return new MeterConsumption(BigDecimal.ZERO, ConsumptionType.MINIMUM, previousCumulative);
    }


    public MeterConsumption virtualCalculate(
            Meter meter,
//            BigDecimal oldReading,
            BigDecimal newReading,
            BigDecimal average,
            BigDecimal previousCumulative,
            BigDecimal energyConsumption
//            BigDecimal newCumulative
    ) {

        if(energyConsumption == null){
            energyConsumption = BigDecimal.ZERO;
        }
//        // FIXED (Virtual Meter)
//        if (meter.getMeterType().equalsIgnoreCase("VIRTUAL")) {
//            previousCumulative = previousCumulative.add(energyConsumption);
//            return new MeterConsumption(
//                    BigDecimal.valueOf(Double.parseDouble(meter.getFixedEnergy())),
//                    ConsumptionType.FIXED,
//                    previousCumulative);
//        }

        // ESTIMATE (Agent failed → current reading = 0)
        if (newReading != null) {
            BigDecimal consumption = average;
            previousCumulative = previousCumulative.add(energyConsumption);
            return new MeterConsumption(
                    newReading,
                    ConsumptionType.ESTIMATE,
                    previousCumulative
            );
        }

//        // ESTIMATE (Missing reading)
//        if (newReading == null) {
//            BigDecimal consumption = average;
//            previousCumulative = previousCumulative.add(energyConsumption);
//            return new MeterConsumption(
//                    consumption,
//                    ConsumptionType.ESTIMATE,
//                    previousCumulative
//            );
//        }

//        // ACTUAL NORMAL
//        if (newReading.compareTo(previousCumulative) > 0) {
//            previousCumulative = previousCumulative.add(energyConsumption);
//            BigDecimal consumption = newReading.subtract(previousCumulative);
//            return new MeterConsumption(
//                    consumption,
//                    ConsumptionType.ACTUAL_NORMAL,
//                    previousCumulative
//            );
//        }
//
//        // ACTUAL ROLLOVER
//        if (newReading.compareTo(previousCumulative) < 0) {
//
//            previousCumulative = previousCumulative.add(energyConsumption);
//            System.out.println("wwwww1: "+previousCumulative);
//            System.out.println("newReading>>>: "+newReading);
////            BigDecimal consumption = consumption
//            BigDecimal consumption = newReading.add(MAX_READING.subtract(previousCumulative));
//            System.out.println("wwwww2: "+consumption);
////            previousCumulative = newReading.add(MAX_READING.subtract(previousCumulative));
//            previousCumulative = newReading;
////            previousCumulative = previousCumulative.add(energyConsumption);
//            return new MeterConsumption(
//                    consumption,
//                    ConsumptionType.ACTUAL_ROLLOVER,
//                    previousCumulative
//            );
//        }

        return new MeterConsumption(BigDecimal.ZERO, ConsumptionType.MINIMUM, previousCumulative);
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