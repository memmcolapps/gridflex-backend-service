package org.memmcol.gridflexbackendservice.service.debit_credit_adjustment;

import lombok.RequiredArgsConstructor;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.memmcol.gridflexbackendservice.mapper.CreditDebitPaymentMapper;
import org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper;
import org.memmcol.gridflexbackendservice.mapper.PaymentModeMapper;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.AdjustmentAllocation;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.AdjustmentComputationResult;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.CreditDebitAdjustment;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditPayment;
import org.memmcol.gridflexbackendservice.model.meter.PaymentMode;

import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private ResponseProperties status;

    private final AdjustmentComputationService computationService = new AdjustmentComputationService();

    /**
     * Called during vending. This only COMPUTES.
     * It does NOT write payment or update balances.
     */
//    public AdjustmentComputationResult computeAdjustmentImpact(UUID orgId, UUID meterId, BigDecimal initialAmount) {
//
//        List<CreditDebitAdjustment> active = adjustmentMapper.findActiveForUpdate(orgId, meterId);
//
//        PaymentMode paymentMode = paymentModeMapper.findActive(orgId, meterId);
//
//        AdjustmentComputationResult result = computationService.compute(active, paymentMode, initialAmount);
//        result.setMeterId(meterId);
//
//        return result;
//    }

    /**
     * Called ONLY after token generation succeeds.
     * Applies settlements + inserts payments.
     */
//    public void settleAdjustments(UUID orgId,
//                                  UUID transactionId,
//                                  List<AdjustmentAllocation> allocations) {
//
//        if (allocations == null || allocations.isEmpty()) return;
//
//        for (AdjustmentAllocation alloc : allocations) {
//            BigDecimal paid = alloc.getAllocatedAmount();
//            if (paid == null || paid.compareTo(BigDecimal.ZERO) <= 0) continue;
//
//            CreditDebitAdjustment adj = adjustmentMapper.findByIdForUpdate(orgId, alloc.getAdjustmentId());
//            if (adj == null) continue;
//
//            BigDecimal currentBal = adj.getBalance() == null ? BigDecimal.ZERO : adj.getBalance();
//            BigDecimal newBal = currentBal.subtract(paid);
//
//            if (newBal.compareTo(BigDecimal.ZERO) < 0) {
//                newBal = BigDecimal.ZERO; // safety clamp
//            }
//
//            String newStatus;
//            if (newBal.compareTo(BigDecimal.ZERO) == 0) {
//                newStatus = STATUS_PAID;
//            } else if (newBal.compareTo(currentBal) < 0) {
//                newStatus = STATUS_PARTIAL;
//            } else {
//                newStatus = adj.getStatus() == null ? STATUS_UNPAID : adj.getStatus();
//            }
//
//            // update balance/status
//            adjustmentMapper.updateBalanceAndStatus(orgId, adj.getId(), newBal, newStatus);
//
//            // insert payment record
//            DebitCreditPayment dcp = new DebitCreditPayment();
//            dcp.setOrgId(orgId);
//            dcp.setCreditDebitAdjId(adj.getId());
//            dcp.setPaymentMode(alloc.getPaymentMode());
//            dcp.setTransId(transactionId);
//            dcp.setCredit(alloc.getAllocatedAmount());
//
//            paymentMapper.insertPayment(dcp);
//        }
//    }

    /**
     * Settle debts sequentially after token generation.
     * Updates the balance and inserts payment records for each debt processed.
     */
    public void settleDebtsSequentially(UUID orgId, UUID meterId, UUID transactionId,
                                        List<SettledDebt> settledDebts) {
        if (settledDebts == null || settledDebts.isEmpty()) return;

        // Get all active debit adjustments for this meter
        List<CreditDebitAdjustment> activeDebits = adjustmentMapper.findActiveForUpdate(orgId, meterId)
                .stream()
                .filter(adj -> "debit".equalsIgnoreCase(adj.getType()))
                .collect(java.util.stream.Collectors.toList());

        for (SettledDebt debt : settledDebts) {
            BigDecimal paid = debt.getAmountPaid();
            if (paid == null || paid.compareTo(BigDecimal.ZERO) <= 0) continue;

            // Find matching adjustment based on balance
            CreditDebitAdjustment matchingAdj = activeDebits.stream()
                    .filter(adj -> adj.getBalance() != null && adj.getBalance().compareTo(debt.getBalanceBefore()) == 0)
                    .findFirst()
                    .orElse(null);

            // If no exact match, try to find any unpaid adjustment
            if (matchingAdj == null) {
                matchingAdj = activeDebits.stream()
                        .filter(adj -> "UNPAID".equalsIgnoreCase(adj.getStatus()) || "PARTIALLY_PAID".equalsIgnoreCase(adj.getStatus()))
                        .findFirst()
                        .orElse(null);
            }

            if (matchingAdj == null) continue;

            BigDecimal currentBal = matchingAdj.getBalance() == null ? BigDecimal.ZERO : matchingAdj.getBalance();
            BigDecimal newBal = currentBal.subtract(paid);

            if (newBal.compareTo(BigDecimal.ZERO) < 0) {
                newBal = BigDecimal.ZERO;
            }

            String newStatus;
            if (newBal.compareTo(BigDecimal.ZERO) == 0) {
                newStatus = STATUS_PAID;
            } else if (newBal.compareTo(currentBal) < 0) {
                newStatus = STATUS_PARTIAL;
            } else {
                newStatus = matchingAdj.getStatus() == null ? STATUS_UNPAID : matchingAdj.getStatus();
            }

            DebitCreditPayment debitCreditPayment = adjustmentMapper.getPaymentById(matchingAdj.getId(), orgId);
            if (debitCreditPayment == null) {
                throw new GlobalExceptionHandler.NotFoundException("Debit adjustment payment not found");
            }

            // update balance/status
            int respAdjust = adjustmentMapper.updateBalanceAndStatus(matchingAdj.getId(), orgId, newBal, newStatus);
            if(respAdjust == 0){
                throw new GlobalExceptionHandler.NotFoundException("Balance " + status.getUpdateFailureDesc());
            }

            // insert payment record
            DebitCreditPayment dcp = new DebitCreditPayment();
            dcp.setParentId(debitCreditPayment.getId());
            dcp.setCreditDebitAdjId(matchingAdj.getId());
            dcp.setBalance(newBal);
            dcp.setDebt(BigDecimal.ZERO);
            dcp.setOrgId(orgId);
            dcp.setTransId(transactionId);
            dcp.setCredit(paid);

            int respAdjustPayment = paymentMapper.insertPayment(dcp);
            if(respAdjustPayment == 0){
                throw new GlobalExceptionHandler.NotFoundException("Failed to pay debit adjustment");
            }
        }
    }

    /**
     * Settle credits sequentially after token generation.
     * Updates the balance and inserts payment records for each credit processed.
     */
    public void settleCreditsSequentially(UUID orgId, UUID meterId, UUID transactionId,
                                         List<SettledCredit> settledCredits) {
        if (settledCredits == null || settledCredits.isEmpty()) return;

        // Get all active credit adjustments for this meter
        List<CreditDebitAdjustment> activeCredits = adjustmentMapper.findActiveForUpdate(orgId, meterId)
                .stream()
                .filter(adj -> "credit".equalsIgnoreCase(adj.getType()))
                .collect(java.util.stream.Collectors.toList());

        for (SettledCredit credit : settledCredits) {
            BigDecimal paid = credit.getAmountPaid();
            if (paid == null || paid.compareTo(BigDecimal.ZERO) <= 0) continue;

            // Find matching adjustment based on balance
            CreditDebitAdjustment matchingAdj = activeCredits.stream()
                    .filter(adj -> adj.getBalance() != null && adj.getBalance().compareTo(credit.getBalanceBefore()) == 0)
                    .findFirst()
                    .orElse(null);

            // If no exact match, try to find any unpaid adjustment
            if (matchingAdj == null) {
                matchingAdj = activeCredits.stream()
                        .filter(adj -> "UNPAID".equalsIgnoreCase(adj.getStatus()) || "PARTIALLY_PAID".equalsIgnoreCase(adj.getStatus()))
                        .findFirst()
                        .orElse(null);
            }

            if (matchingAdj == null) continue;

            BigDecimal currentBal = matchingAdj.getBalance() == null ? BigDecimal.ZERO : matchingAdj.getBalance();
            BigDecimal newBal = currentBal.subtract(paid);

            if (newBal.compareTo(BigDecimal.ZERO) < 0) {
                newBal = BigDecimal.ZERO;
            }

            String newStatus;
            if (newBal.compareTo(BigDecimal.ZERO) == 0) {
                newStatus = STATUS_PAID;
            } else if (newBal.compareTo(currentBal) < 0) {
                newStatus = STATUS_PARTIAL;
            } else {
                newStatus = matchingAdj.getStatus() == null ? STATUS_UNPAID : matchingAdj.getStatus();
            }

            DebitCreditPayment debitCreditPayment = adjustmentMapper.getPaymentById(matchingAdj.getId(), orgId);
            if (debitCreditPayment == null) {
                throw new GlobalExceptionHandler.NotFoundException("Credit adjustment payment not found");
            }

            // update balance/status
            int respAdjust = adjustmentMapper.updateBalanceAndStatus(matchingAdj.getId(), orgId, newBal, newStatus);
            if(respAdjust == 0){
                throw new GlobalExceptionHandler.NotFoundException("Balance " + status.getUpdateFailureDesc());
            }

            // insert payment record
            DebitCreditPayment dcp = new DebitCreditPayment();
            dcp.setParentId(debitCreditPayment.getId());
            dcp.setCreditDebitAdjId(matchingAdj.getId());
            dcp.setBalance(newBal);
            dcp.setCredit(BigDecimal.ZERO);
            dcp.setDebt(paid);
            dcp.setOrgId(orgId);
            dcp.setTransId(transactionId);

            int respAdjustPayment = paymentMapper.insertPayment(dcp);
            if(respAdjustPayment == 0){
                throw new GlobalExceptionHandler.NotFoundException("Failed to pay credit adjustment");
            }
        }
    }

    /**
     * Helper class to hold settled debt information
     */
    @lombok.Data
    @lombok.Builder
    public static class SettledDebt {
        private BigDecimal amountPaid;
        private BigDecimal balanceBefore;
    }

    /**
     * Helper class to hold settled credit information
     */
    @lombok.Data
    @lombok.Builder
    public static class SettledCredit {
        private BigDecimal amountPaid;
        private BigDecimal balanceBefore;
    }
}
