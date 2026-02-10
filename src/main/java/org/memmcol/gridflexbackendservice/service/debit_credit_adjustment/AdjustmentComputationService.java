package org.memmcol.gridflexbackendservice.service.debit_credit_adjustment;



import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.AdjustmentAllocation;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.AdjustmentComputationResult;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.CreditDebitAdjustment;
import org.memmcol.gridflexbackendservice.model.meter.PaymentMode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.memmcol.gridflexbackendservice.util.AdjustmentConstants.*;

public class AdjustmentComputationService {

    /**
     * Compute total adjustment impact on vending:
     * - DEBIT reduces amount available to vend
     * - CREDIT increases amount available to vend
     *
     * Also returns per-adjustment allocation amounts (for payment insertion + balance update).
     */
    public AdjustmentComputationResult compute(
            List<CreditDebitAdjustment> activeAdjustments,
            PaymentMode paymentMode,
            BigDecimal initialAmount
    ) {

        if (initialAmount == null) initialAmount = BigDecimal.ZERO;

        AdjustmentComputationResult result = AdjustmentComputationResult.builder()
                .initialAmount(initialAmount)
                .totalDeduction(BigDecimal.ZERO)
                .effectiveTendered(initialAmount)
                .build();

        if (activeAdjustments == null || activeAdjustments.isEmpty()) {
            return result;
        }

        // PaymentMode may be null => default safe behavior:
        // DEBIT => ONEOFF, CREDIT => ONEOFF
        for (CreditDebitAdjustment adj : activeAdjustments) {
            if (adj == null || adj.getBalance() == null) continue;

            BigDecimal balance = adj.getBalance();
            if (balance.compareTo(BigDecimal.ZERO) <= 0) continue;

            String type = normalize(adj.getType());
            boolean isDebit = TYPE_DEBIT.equalsIgnoreCase(type);
            boolean isCredit = TYPE_CREDIT.equalsIgnoreCase(type);

            if (!isDebit && !isCredit) {
                // Unknown type => ignore to avoid wrong financial postings
                continue;
            }

            String mode = resolveMode(paymentMode, type);
            String plan = resolvePlan(paymentMode, type);

            BigDecimal allocation = computeAllocation(mode, plan, adj, initialAmount);

            // Never allocate above balance
            if (allocation.compareTo(balance) > 0) {
                allocation = balance;
            }

            // Never allocate negative
            if (allocation.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Apply effect on vending amount
            if (isDebit) {
                // debit reduces vending amount
                result.setTotalDeduction(result.getTotalDeduction().add(allocation));
                result.setEffectiveTendered(result.getEffectiveTendered().subtract(allocation));
            } else {
                // credit increases vending amount
                result.setEffectiveTendered(result.getEffectiveTendered().add(allocation));
            }

            result.getAllocations().add(
                    AdjustmentAllocation.builder()
                            .adjustmentId(adj.getId())
                            .liabilityCauseId(adj.getLiabilityCauseId())
                            .adjustmentType(type)
                            .paymentMode(mode)
                            .allocatedAmount(allocation)
                            .build()
            );
        }

        // Prevent negative vending (important non-obvious rule)
        if (result.getEffectiveTendered().compareTo(BigDecimal.ZERO) < 0) {
            // clamp to zero (or throw exception based on your policy)
            result.setEffectiveTendered(BigDecimal.ZERO);
        }

        return result;
    }

    private BigDecimal computeAllocation(String mode, String plan, CreditDebitAdjustment adj, BigDecimal initialAmount) {
        BigDecimal balance = safe(adj.getBalance());

        switch (normalize(mode)) {
            case MODE_ONEOFF:
                // settle as much as possible (full outstanding)
                return balance;

            case MODE_MONTHLY:
                // Rule B: debit/months (fixed schedule)
                // For CREDIT, treat similarly: credit/months
                int months = parseInt(plan, 1);
                if (months <= 0) months = 1;

                BigDecimal base = safe(adj.getDebit()); // original amount
                if (base.compareTo(BigDecimal.ZERO) <= 0) {
                    // fallback: if debit column not reliable, monthly schedule based on current balance
                    base = balance;
                }

                return base.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);

            case MODE_PERCENTAGE:
                // percentage of initial amount, capped at 100
                BigDecimal percent = parseDecimal(plan, BigDecimal.ZERO);
                if (percent.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
                if (percent.compareTo(new BigDecimal("100")) > 0) {
                    percent = new BigDecimal("100");
                }
                return initialAmount.multiply(percent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            default:
                // safe fallback
                return balance;
        }
    }

    private String resolveMode(PaymentMode pm, String type) {
        if (pm == null) return MODE_ONEOFF;

        if (TYPE_DEBIT.equalsIgnoreCase(type) && TYPE_DEBIT.equalsIgnoreCase(pm.getPaymentType())) {
            return pm.getPaymentMode() == null ? MODE_ONEOFF : pm.getPaymentMode();
        }
        return pm.getPaymentMode() == null ? MODE_ONEOFF : pm.getPaymentMode();
    }

    private String resolvePlan(PaymentMode pm, String type) {
        if (pm == null) return null;

        if (TYPE_DEBIT.equalsIgnoreCase(type) && TYPE_DEBIT.equalsIgnoreCase(pm.getPaymentType())) {
            return pm.getPaymentPlan();
        }
        return pm.getPaymentPlan();
    }

    private String normalize(String s) {
        return s == null ? null : s.trim().toUpperCase();
    }

    private BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private int parseInt(String s, int def) {
        try {
            if (s == null) return def;
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private BigDecimal parseDecimal(String s, BigDecimal def) {
        try {
            if (s == null) return def;
            return new BigDecimal(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}