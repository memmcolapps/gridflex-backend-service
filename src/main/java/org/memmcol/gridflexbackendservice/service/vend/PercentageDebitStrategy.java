package org.memmcol.gridflexbackendservice.service.vend;

import org.memmcol.gridflexbackendservice.mapper.VendMapper;
import org.memmcol.gridflexbackendservice.model.debt_setting.PercentageRange;
import org.memmcol.gridflexbackendservice.model.vend.DebtPaymentResult;
import org.memmcol.gridflexbackendservice.model.vend.MeterView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PercentageDebitStrategy {

    @Autowired
    private VendMapper vendMapper;

//    private final BigDecimal percentage;

//    public PercentageDebitStrategy(VendMapper vendMapper) {
//        this.vendMapper = vendMapper;
//    }

    public DebtPaymentResult processDebtsSequentially(List<MeterView> meters, BigDecimal paymentAmount, UUID orgId, UUID meterId) {

        List<MeterView> debitMeters = meters.stream()
                .filter(m -> "debit".equalsIgnoreCase(m.getAdjustmentType()))
                .sorted(Comparator.comparing(MeterView::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        if (debitMeters.isEmpty() || !hasUnpaidDebts(meters)) {
            return DebtPaymentResult.builder()
                    .totalDeducted(BigDecimal.ZERO)
                    .remainingPayment(paymentAmount != null ? paymentAmount : BigDecimal.ZERO)
                    .errorMessage(null)
                    .failedDebt(null)
                    .maximumVendable(BigDecimal.ZERO)
                    .debtPayments(new ArrayList<>())
                    .build();
        }

        BigDecimal remainingPayment = paymentAmount != null ? paymentAmount : BigDecimal.ZERO;
        BigDecimal totalDeducted = BigDecimal.ZERO;
        String errorMessage = null;
        MeterView failedDebt = null;
        BigDecimal maximumVendable = BigDecimal.ZERO;

        List<DebtPaymentResult.DebtPayment> debtPayments = new ArrayList<>();

        // Calculate percentage once before processing debts
        BigDecimal percentageValue = BigDecimal.ZERO;
        boolean hasPercentageDebt = debitMeters.stream()
                .anyMatch(d -> "percentage".equalsIgnoreCase(d.getDebitPaymentMode()));
        if (hasPercentageDebt) {
            PercentageRange pr = vendMapper.findPercentageByRange(orgId, meterId, paymentAmount);
            if (pr != null && pr.getPercentage() != null) {
                try {
                    percentageValue = new BigDecimal(pr.getPercentage());
                    if (percentageValue.compareTo(new BigDecimal("100")) > 0) {
                        percentageValue = new BigDecimal("100");
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        for (MeterView debt : debitMeters) {
            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal balance = debt.getBalanceAfterAdjustment() != null
                    ? debt.getBalanceAfterAdjustment()
                    : BigDecimal.ZERO;

            if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            String paymentMode = debt.getDebitPaymentMode();
            String paymentPlan = debt.getDebitPaymentPlan();
            BigDecimal deduction = BigDecimal.ZERO;

            if ("no-payment".equalsIgnoreCase(paymentMode)) {
                continue;
            }

            if ("monthly".equalsIgnoreCase(paymentMode)) {
                int currentYear = LocalDateTime.now().getYear();
                int currentMonth = LocalDateTime.now().getMonthValue();
                int paymentCount = vendMapper.countPaymentsThisMonth(meterId, debt.getCreditDebitAdjId(), currentYear, currentMonth, "debit");

                if (paymentCount > 0) {
                    continue;
                }

                int months = 1;
                try {
                    if (paymentPlan != null && !paymentPlan.isBlank()) {
                        months = Integer.parseInt(paymentPlan);
                    }
                } catch (NumberFormatException ignored) {}

                BigDecimal debitAmt = debt.getDebitAmount() != null ? debt.getDebitAmount() : balance;
                BigDecimal expectedMonthly = debitAmt.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);

                BigDecimal minimumRequired;

                // Compare balance with expectedMonthly
                if (balance.compareTo(expectedMonthly) < 0) {
                    // Balance is NOT >= expectedMonthly installment
                    // Minimum required = balance + 1000
                    minimumRequired = balance.add(new BigDecimal("1000"));

                    if (remainingPayment.compareTo(minimumRequired) < 0) {
                        errorMessage = "Minimum that can be vended is " + minimumRequired.toPlainString();
                        failedDebt = debt;
                        break;
                    }

                    // Deduct the balance (not expectedMonthly)
                    deduction = balance;
                } else {
                    // Balance is >= expectedMonthly installment
                    minimumRequired = expectedMonthly.add(new BigDecimal("1000"));

                    if (remainingPayment.compareTo(minimumRequired) < 0) {
                        errorMessage = "Minimum that can be vended is " + minimumRequired.toPlainString();
                        failedDebt = debt;
                        break;
                    }

                    // Deduct expectedMonthly
                    deduction = expectedMonthly;
                }

                maximumVendable = minimumRequired;

            } else if ("percentage".equalsIgnoreCase(paymentMode)) {
                // Use pre-calculated percentage (calculated once before the loop)
                if (percentageValue.compareTo(BigDecimal.ZERO) > 0) {
                    deduction = remainingPayment.multiply(percentageValue).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                    // CAP: Don't exceed balance (no over-payment)
                    if (deduction.compareTo(balance) > 0) {
                        deduction = balance;
                    }
                }

            } else {
                // One-off mode - validate payment must be debitAmount + 1000
                BigDecimal debitAmt = debt.getDebitAmount() != null ? debt.getDebitAmount() : balance;

                BigDecimal minimumRequired;
                if (balance.compareTo(debitAmt) < 0) {
                    minimumRequired = balance.add(new BigDecimal("1000"));
                    deduction = balance;
                } else {
                    minimumRequired = debitAmt.add(new BigDecimal("1000"));
                    deduction = debitAmt;
                }

                maximumVendable = minimumRequired;

                if (remainingPayment.compareTo(minimumRequired) < 0) {
                    errorMessage = "Minimum that can be vended is " + maximumVendable.toPlainString();
                    failedDebt = debt;
                    break;
                }
            }

            if (deduction.compareTo(BigDecimal.ZERO) > 0) {
                if (deduction.compareTo(remainingPayment) > 0) {
                    deduction = remainingPayment;
                }
                remainingPayment = remainingPayment.subtract(deduction);
                totalDeducted = totalDeducted.add(deduction);

                // Track the debt payment for settlement
                DebtPaymentResult.DebtPayment debtPayment = DebtPaymentResult.DebtPayment.builder()
                        .liabilityCauseId(null)
                        .liabilityName(debt.getLiabilityName())
                        .amountPaid(deduction)
                        .balanceBefore(balance)
                        .balanceAfter(balance.subtract(deduction))
                        .build();
                debtPayments.add(debtPayment);
            }
        }

        return DebtPaymentResult.builder()
                .totalDeducted(totalDeducted)
                .remainingPayment(remainingPayment)
                .errorMessage(errorMessage)
                .failedDebt(failedDebt)
                .maximumVendable(maximumVendable)
                .debtPayments(debtPayments)
                .build();
    }

    private static boolean hasUnpaidDebts(List<MeterView> meters) {
        return meters.stream()
                .filter(m -> "debit".equalsIgnoreCase(m.getAdjustmentType()))
                .anyMatch(m -> {
                    BigDecimal balance = m.getBalanceAfterAdjustment();
                    String status = m.getAdjustmentStatus();
                    boolean hasBalance = balance != null && balance.compareTo(BigDecimal.ZERO) > 0;
                    boolean isNotPaid = status == null || !"paid".equalsIgnoreCase(status);
                    return hasBalance && isNotPaid;
                });
    }

}


//package org.memmcol.gridflexbackendservice.service.vend;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//
//public class PercentageDebitStrategy implements PaymentStrategy {
//
//    private final BigDecimal percentage;
//
//    public PercentageDebitStrategy(BigDecimal percentage) {
//        this.percentage = percentage;
//    }
//
//    @Override
//    public BigDecimal calculateDebitToDeduct(BigDecimal totalDebit) {
//        if (totalDebit == null || totalDebit.compareTo(BigDecimal.ZERO) <= 0) {
//            return BigDecimal.ZERO;
//        }
//
//        if (percentage == null || percentage.compareTo(BigDecimal.ZERO) <= 0) {
//            return BigDecimal.ZERO;
//        }
//
//        BigDecimal percentValue = percentage.compareTo(new BigDecimal("100")) > 0
//                ? new BigDecimal("100")
//                : percentage;
//
//        return totalDebit.multiply(percentValue)
//                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
//    }
//
//    @Override
//    public BigDecimal calculateCreditUnits(BigDecimal totalCreditUnits) {
//        return totalCreditUnits != null ? totalCreditUnits : BigDecimal.ZERO;
//    }
//}
