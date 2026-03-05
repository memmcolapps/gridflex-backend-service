package org.memmcol.gridflexbackendservice.service.vend;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CreditPaymentResult {
    private BigDecimal totalCreditUnits;
    private BigDecimal remainingPayment;
}
