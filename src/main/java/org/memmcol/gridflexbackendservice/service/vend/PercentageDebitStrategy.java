package org.memmcol.gridflexbackendservice.service.vend;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PercentageDebitStrategy implements PaymentStrategy {

    private final BigDecimal percentage;

    public PercentageDebitStrategy(BigDecimal percentage) {
        this.percentage = percentage;
    }

    @Override
    public BigDecimal calculateDebitToDeduct(BigDecimal totalDebit) {
        if (totalDebit == null || totalDebit.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (percentage == null || percentage.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal percentValue = percentage.compareTo(new BigDecimal("100")) > 0
                ? new BigDecimal("100")
                : percentage;

        return totalDebit.multiply(percentValue)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateCreditUnits(BigDecimal totalCreditUnits) {
        return totalCreditUnits != null ? totalCreditUnits : BigDecimal.ZERO;
    }
}
