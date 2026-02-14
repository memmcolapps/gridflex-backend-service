package org.memmcol.gridflexbackendservice.service.vend;

import java.math.BigDecimal;

public class OneOffCreditStrategy implements PaymentStrategy {

    @Override
    public BigDecimal calculateDebitToDeduct(BigDecimal totalDebit) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateCreditUnits(BigDecimal totalCreditUnits) {
        return totalCreditUnits != null ? totalCreditUnits : BigDecimal.ZERO;
    }
}

