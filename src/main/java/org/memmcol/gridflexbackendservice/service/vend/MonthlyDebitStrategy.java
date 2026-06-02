//package org.memmcol.gridflexbackendservice.service.vend;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//
//public class MonthlyDebitStrategy implements PaymentStrategy {
//
//    private final int months;
//
//    public MonthlyDebitStrategy(int months) {
//        this.months = months;
//    }
//
//    @Override
//    public BigDecimal calculateDebitToDeduct(BigDecimal totalDebit) {
//
//        if (totalDebit == null) return BigDecimal.ZERO;
//
//        if (months <= 0) return totalDebit;
//
//        return totalDebit.divide(
//                BigDecimal.valueOf(months),
//                2,
//                RoundingMode.HALF_UP
//        );
//    }
//
//    @Override
//    public BigDecimal calculateCreditUnits(BigDecimal totalCreditUnits) {
//        return totalCreditUnits != null ? totalCreditUnits : BigDecimal.ZERO;
//    }
//}
//
