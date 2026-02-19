package org.memmcol.gridflexbackendservice.model.debit_credit_adjustment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.memmcol.gridflexbackendservice.util.AdjustmentConstants;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditDebitAdjustment {

    private UUID id;
    private UUID meterId;
    private UUID liabilityCauseId;

    /**
     * Original amount for the adjustment (as per your schema column name "debit").
     * Note: even if type=CREDIT, this column still exists; business logic will use it
     * only when needed (MONTHLY installment = debit/months).
     */
    private BigDecimal debit;

    /**
     * Remaining amount outstanding.
     */
    private BigDecimal balance;

    /**
     * PAID / UNPAID / PARTIAL
     */
    private String status;

    /**
     * CREDIT / DEBIT
     */
    private String type;

    private UUID orgId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Convenience typed accessors
    private AdjustmentConstants adjustmentEnums;
}
