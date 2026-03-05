package org.memmcol.gridflexbackendservice.service.vend;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PercentageCreditStrategy implements PaymentStrategy {

    private final BigDecimal percentage;

    public PercentageCreditStrategy(BigDecimal percentage) {
        this.percentage = percentage;
    }

    @Override
    public BigDecimal calculateDebitToDeduct(BigDecimal totalDebit) {
        return totalDebit != null ? totalDebit : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateCreditUnits(BigDecimal totalCreditUnits) {
        if (totalCreditUnits == null || totalCreditUnits.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (percentage == null || percentage.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal percentValue = percentage.compareTo(new BigDecimal("100")) > 0
                ? new BigDecimal("100")
                : percentage;

        return totalCreditUnits.multiply(percentValue)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}
