package org.memmcol.gridflexbackendservice.service.vend;

import lombok.Builder;
import lombok.Data;
import org.memmcol.gridflexbackendservice.model.vend.MeterView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class DebtPaymentResult {
    private BigDecimal totalDeducted;
    private BigDecimal remainingPayment;
    private String errorMessage;
    private MeterView failedDebt;
    private BigDecimal maximumVendable;
    
    @Builder.Default
    private List<DebtPayment> debtPayments = new ArrayList<>();
    
    @Data
    @Builder
    public static class DebtPayment {
        private UUID adjustmentId;
        private UUID liabilityCauseId;
        private String liabilityName;
        private BigDecimal amountPaid;
        private BigDecimal balanceBefore;
        private BigDecimal balanceAfter;
    }
}
