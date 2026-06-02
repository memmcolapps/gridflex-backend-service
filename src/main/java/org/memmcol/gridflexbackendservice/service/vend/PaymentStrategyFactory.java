//package org.memmcol.gridflexbackendservice.service.vend;
//
//public class PaymentStrategyFactory {
//
//    public static PaymentStrategy getStrategy(
//            String paymentMode,
//            String paymentType,
//            String paymentPlan
//    ) {
//
//        int months = 0;
//
//        try {
//            if (paymentPlan != null && !paymentPlan.isBlank()) {
//                months = Integer.parseInt(paymentPlan);
//            }
//        } catch (NumberFormatException ignored) {
//        }
//
//        if ("monthly".equalsIgnoreCase(paymentMode)) {
//
//            if ("credit".equalsIgnoreCase(paymentType)) {
//                return new MonthlyCreditStrategy(months);
//            }
//
//            return new MonthlyDebitStrategy(months);
//        }
//
//        if ("credit".equalsIgnoreCase(paymentType)) {
//            return new OneOffCreditStrategy();
//        }
//
//        return new OneOffDebitStrategy();
//    }
//}