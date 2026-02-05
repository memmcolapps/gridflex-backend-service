package org.memmcol.gridflexbackendservice.model.debit_credit_adjustment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AdjustmentComputationResult {
    private UUID meterId;
    private BigDecimal initialAmount;
    private BigDecimal effectiveTendered;
    private BigDecimal totalDeduction;
    @Builder.Default
    private List<AdjustmentAllocation> allocations = new ArrayList<>();
}