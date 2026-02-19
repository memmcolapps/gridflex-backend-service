package org.memmcol.gridflexbackendservice.service.debit_credit_adjustment;

import lombok.RequiredArgsConstructor;
import org.memmcol.gridflexbackendservice.mapper.CreditDebitPaymentMapper;
import org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper;
import org.memmcol.gridflexbackendservice.mapper.PaymentModeMapper;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.AdjustmentAllocation;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.AdjustmentComputationResult;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.CreditDebitAdjustment;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditPayment;
import org.memmcol.gridflexbackendservice.model.meter.PaymentMode;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.memmcol.gridflexbackendservice.util.AdjustmentConstants.*;

@Service
@RequiredArgsConstructor
public class CreditDebitAdjustmentSettlementService {

    private final DebitCreditAdjustmentMapper adjustmentMapper;
    private final PaymentModeMapper paymentModeMapper;
    private final CreditDebitPaymentMapper paymentMapper;

    private final AdjustmentComputationService computationService = new AdjustmentComputationService();

    /**
     * Called during vending. This only COMPUTES.
     * It does NOT write payment or update balances.
     */
    public AdjustmentComputationResult computeAdjustmentImpact(UUID orgId, UUID meterId, BigDecimal initialAmount) {

        List<CreditDebitAdjustment> active = adjustmentMapper.findActiveForUpdate(orgId, meterId);

        PaymentMode paymentMode = paymentModeMapper.findActive(orgId, meterId);

        AdjustmentComputationResult result = computationService.compute(active, paymentMode, initialAmount);
        result.setMeterId(meterId);

        return result;
    }

    /**
     * Called ONLY after token generation succeeds.
     * Applies settlements + inserts payments.
     */
    public void settleAdjustments(UUID orgId,
                                  UUID transactionId,
                                  List<AdjustmentAllocation> allocations) {

        if (allocations == null || allocations.isEmpty()) return;

        for (AdjustmentAllocation alloc : allocations) {
            BigDecimal paid = alloc.getAllocatedAmount();
            if (paid == null || paid.compareTo(BigDecimal.ZERO) <= 0) continue;

            CreditDebitAdjustment adj = adjustmentMapper.findByIdForUpdate(orgId, alloc.getAdjustmentId());
            if (adj == null) continue;

            BigDecimal currentBal = adj.getBalance() == null ? BigDecimal.ZERO : adj.getBalance();
            BigDecimal newBal = currentBal.subtract(paid);

            if (newBal.compareTo(BigDecimal.ZERO) < 0) {
                newBal = BigDecimal.ZERO; // safety clamp
            }

            String newStatus;
            if (newBal.compareTo(BigDecimal.ZERO) == 0) {
                newStatus = STATUS_PAID;
            } else if (newBal.compareTo(currentBal) < 0) {
                newStatus = STATUS_PARTIAL;
            } else {
                newStatus = adj.getStatus() == null ? STATUS_UNPAID : adj.getStatus();
            }

            // update balance/status
            adjustmentMapper.updateBalanceAndStatus(orgId, adj.getId(), newBal, newStatus);

            // insert payment record
            DebitCreditPayment dcp = new DebitCreditPayment();
            dcp.setOrgId(orgId);
            dcp.setCreditDebitAdjId(adj.getId());
            dcp.setPaymentMode(alloc.getPaymentMode());
            dcp.setTransId(transactionId);
            dcp.setCredit(alloc.getAllocatedAmount());

            paymentMapper.insertPayment(dcp);
        }
    }
}
