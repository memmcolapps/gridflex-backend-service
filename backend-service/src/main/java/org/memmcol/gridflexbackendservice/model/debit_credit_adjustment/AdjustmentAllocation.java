package org.memmcol.gridflexbackendservice.model.debit_credit_adjustment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class AdjustmentAllocation {
    private UUID adjustmentId;
    private UUID liabilityCauseId;
    private String adjustmentType; // DEBIT or CREDIT
    private String paymentMode;    // ONEOFF/MONTHLY/PERCENTAGE
    private BigDecimal allocatedAmount;
}
