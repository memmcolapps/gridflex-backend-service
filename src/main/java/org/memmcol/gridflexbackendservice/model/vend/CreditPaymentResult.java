package org.memmcol.gridflexbackendservice.model.vend;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CreditPaymentResult {
    private BigDecimal totalCreditUnits;
    private BigDecimal totalCreditDeducted;
    private BigDecimal remainingPayment;
    
    @Builder.Default
    private List<CreditPayment> creditPayments = new ArrayList<>();
    
    @Data
    @Builder
    public static class CreditPayment {
        private UUID adjustmentId;
        private String adjustmentName;
        private BigDecimal amountPaid;
        private BigDecimal balanceBefore;
        private BigDecimal balanceAfter;
    }
}
