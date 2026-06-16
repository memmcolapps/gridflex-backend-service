package org.memmcol.gridflexbackendservice.service.vend;

import org.memmcol.gridflexbackendservice.mapper.VendMapper;
import org.memmcol.gridflexbackendservice.model.debt_setting.PercentageRange;
import org.memmcol.gridflexbackendservice.model.vend.CreditPaymentResult;
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
public class PercentageCreditStrategy {

    @Autowired
    private VendMapper vendMapper;

//    private final BigDecimal percentage;

//    public PercentageCreditStrategy(VendMapper vendMapper) {
//        this.vendMapper = vendMapper;
//    }

    public CreditPaymentResult processCreditsSequentially(List<MeterView> meters, BigDecimal paymentAmount, UUID orgId, UUID meterId) {
        List<MeterView> creditMeters = meters.stream()
                .filter(m -> "credit".equalsIgnoreCase(m.getAdjustmentType()))
                .sorted(Comparator.comparing(MeterView::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        if (creditMeters.isEmpty() || !hasUnpaidCredits(meters)) {
            return CreditPaymentResult.builder()
                    .totalCreditUnits(BigDecimal.ZERO)
                    .totalCreditDeducted(BigDecimal.ZERO)
                    .remainingPayment(paymentAmount)
                    .build();
        }

        BigDecimal remainingPayment = paymentAmount != null ? paymentAmount : BigDecimal.ZERO;
        BigDecimal totalCreditUnits = BigDecimal.ZERO;
        BigDecimal totalCreditDeducted = BigDecimal.ZERO;
        List<CreditPaymentResult.CreditPayment> creditPayments = new ArrayList<>();

        // Calculate percentage once before processing credits
        BigDecimal percentageValue = BigDecimal.ZERO;
        boolean hasPercentageCredit = creditMeters.stream()
                .anyMatch(c -> "percentage".equalsIgnoreCase(c.getCreditPaymentMode()));
        if (hasPercentageCredit) {
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

        for (MeterView credit : creditMeters) {
            BigDecimal balance = credit.getBalanceAfterAdjustment() != null
                    ? credit.getBalanceAfterAdjustment()
                    : BigDecimal.ZERO;

            if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            String paymentMode = credit.getCreditPaymentMode();
            String paymentPlan = credit.getCreditPaymentPlan();
            BigDecimal creditDeducted = BigDecimal.ZERO;
            BigDecimal creditUnits = BigDecimal.ZERO;

            if ("no-payment".equalsIgnoreCase(paymentMode)) {
                continue;
            }

            if ("monthly".equalsIgnoreCase(paymentMode)) {
                int currentYear = LocalDateTime.now().getYear();
                int currentMonth = LocalDateTime.now().getMonthValue();
                int paymentCount = vendMapper.countPaymentsThisMonth(meterId, credit.getCreditDebitAdjId(), currentYear, currentMonth, "credit");

                if (paymentCount > 0) {
                    continue;
                }

                int months = 1;
                try {
                    if (paymentPlan != null && !paymentPlan.isBlank()) {
                        months = Integer.parseInt(paymentPlan);
                    }
                } catch (NumberFormatException ignored) {}

                BigDecimal creditAmt = credit.getDebitAmount() != null ? credit.getDebitAmount() : balance;
                BigDecimal expectedMonthly = creditAmt.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);

                if (balance.compareTo(expectedMonthly) < 0) {
                    creditDeducted = balance;
                } else {
                    creditDeducted = expectedMonthly;
                }

            } else if ("percentage".equalsIgnoreCase(paymentMode)) {
                BigDecimal creditAmt = credit.getDebitAmount() != null ? credit.getDebitAmount() : balance;
                if (percentageValue.compareTo(BigDecimal.ZERO) > 0) {
                    creditDeducted = creditAmt.multiply(percentageValue)
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                    if (balance.compareTo(creditDeducted) < 0) {
                        creditDeducted = balance;
                    }
                }

            } else {
                BigDecimal creditAmt = credit.getDebitAmount() != null ? credit.getDebitAmount() : balance;
                if (balance.compareTo(creditAmt) < 0) {
                    creditDeducted = balance;
                } else {
                    creditDeducted = creditAmt;
                }
            }

            // Calculate credit units for display (creditDeducted / tariffRate will be done in main flow)
            // Store creditDeducted as totalCreditDeducted
            totalCreditDeducted = totalCreditDeducted.add(creditDeducted);

            // Also track credit units for legacy/display purposes
            creditUnits = creditDeducted;
            totalCreditUnits = totalCreditUnits.add(creditUnits);

            // Track credit payment for settlement
            BigDecimal balanceAfter = balance.subtract(creditDeducted);
            if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
                balanceAfter = BigDecimal.ZERO;
            }

            CreditPaymentResult.CreditPayment creditPayment = CreditPaymentResult.CreditPayment.builder()
                    .adjustmentId(credit.getCreditDebitAdjId())
                    .adjustmentName(credit.getLiabilityName())
                    .amountPaid(creditDeducted)
                    .balanceBefore(balance)
                    .balanceAfter(balanceAfter)
                    .build();
            creditPayments.add(creditPayment);
        }

        return CreditPaymentResult.builder()
                .totalCreditUnits(totalCreditUnits)
                .totalCreditDeducted(totalCreditDeducted)
                .remainingPayment(remainingPayment)
                .creditPayments(creditPayments)
                .build();
    }

    private static boolean hasUnpaidCredits(List<MeterView> meters) {
        return meters.stream()
                .filter(m -> "credit".equalsIgnoreCase(m.getAdjustmentType()))
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
//public class PercentageCreditStrategy implements PaymentStrategy {
//
//    private final BigDecimal percentage;
//
//    public PercentageCreditStrategy(BigDecimal percentage) {
//        this.percentage = percentage;
//    }
//
//    @Override
//    public BigDecimal calculateDebitToDeduct(BigDecimal totalDebit) {
//        return totalDebit != null ? totalDebit : BigDecimal.ZERO;
//    }
//
//    @Override
//    public BigDecimal calculateCreditUnits(BigDecimal totalCreditUnits) {
//        if (totalCreditUnits == null || totalCreditUnits.compareTo(BigDecimal.ZERO) <= 0) {
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
//        return totalCreditUnits.multiply(percentValue)
//                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
//    }
//}
