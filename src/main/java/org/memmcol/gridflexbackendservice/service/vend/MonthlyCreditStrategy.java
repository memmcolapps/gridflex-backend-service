package org.memmcol.gridflexbackendservice.service.vend;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MonthlyCreditStrategy implements PaymentStrategy {

    private final int months;

    public MonthlyCreditStrategy(int months) {
        this.months = months;
    }

    @Override
    public BigDecimal calculateDebitToDeduct(BigDecimal totalDebit) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateCreditUnits(BigDecimal totalCreditUnits) {

        if (totalCreditUnits == null) return BigDecimal.ZERO;

        if (months <= 0) return totalCreditUnits;

        return totalCreditUnits.divide(
                BigDecimal.valueOf(months),
                3,
                RoundingMode.HALF_UP
        );
    }
}

