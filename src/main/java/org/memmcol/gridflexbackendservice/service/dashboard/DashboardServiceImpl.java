package org.memmcol.gridflexbackendservice.service.dashboard;

import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.memmcol.gridflexbackendservice.mapper.DashboardMapper;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.model.vend.Transaction;
import org.memmcol.gridflexbackendservice.service.band.BandServiceImpl;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

import static org.memmcol.gridflexbackendservice.components.handleValidUser.handleUserValidation;

@Service
public class DashboardServiceImpl implements  DashboardService{
    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    @Autowired
    private DashboardMapper dashboardMapper;

    @Autowired
    private GenericHandler genericHandler;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private ResponseProperties status;

    @Override
    public Map<String, Object> dataManagementDashboard(String band, String year, String meterCategory) {
    try {
        UserModel um = handleUserValidation();

        List<Meter> meters = dashboardMapper.getMeters(um.getOrgId());

        List<Meter> filteredMeters = meters.stream()
                .filter(m -> band == null || band.isEmpty() || (m.getTariffInfo() != null &&
                        m.getTariffInfo().getBand() != null &&
                        band.equalsIgnoreCase(m.getTariffInfo().getBand().getName())))
                .filter(m -> {
                    if (year == null || year.isEmpty() || m.getCreatedAt() == null)
                        return true;

                    Instant instant = m.getCreatedAt().toInstant();
                    ZonedDateTime zoned = instant.atZone(ZoneId.systemDefault());
                    int meterYear = zoned.getYear();
                    return String.valueOf(meterYear).equals(year);
                })
                .filter(m -> meterCategory == null || meterCategory.isEmpty() || m.getMeterCategory().equalsIgnoreCase(meterCategory))
                .collect(Collectors.toList());

        int total = filteredMeters.size();
        if (total == 0) {
            total = 1;
        }

        // Calculate summary stats
        long inventory = filteredMeters.stream()
                .filter(m -> m.getNodeId() == null)
                .count();

        long allocated = filteredMeters.stream()
                .filter(m -> m.getNodeId() != null)
                .count();

        long assigned = filteredMeters.stream()
                .filter(m -> m.getCustomerId() != null)
                .count();

        long deactivated = filteredMeters.stream()
                .filter(m -> "Deactivated".equalsIgnoreCase(m.getStatus()))
                .count();

//        long inventory = created + pending_allocated;

        // Calculate percentages
        double inventoryPercent = (inventory * 100.0) / total;
        double allocatedPercent = (allocated * 100.0) / total;
//        double assignedPercent = (assigned * 100.0) / total;
//        double deactivatedPercent = (deactivated * 100.0) / total;

        // Manufacturer list & count summary
        Map<String, Long> manufacturerCounts = filteredMeters.stream()
                .filter(m -> m.getManufacturer() != null)
                .collect(Collectors.groupingBy(
                        m -> m.getManufacturer().getName(),
                        Collectors.counting()
                ));

        List<Map<String, Object>> manufacturersSummary = manufacturerCounts.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", entry.getKey());
                    map.put("totalMeters", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());

        // Group meters installed by year and month (handling java.util.Date safely)
        Map<Integer, Map<String, Long>> metersInstalledByYearAndMonth = meters.stream()
                .filter(m -> m.getCustomerId() != null && m.getUpdatedAt() != null)
                .collect(Collectors.groupingBy(
                        m -> {
                            Instant instant = m.getUpdatedAt().toInstant();
                            ZonedDateTime zoned = instant.atZone(ZoneId.systemDefault());
                            return zoned.getYear();
                        },
                        Collectors.groupingBy(
                                m -> {
                                    Instant instant = m.getUpdatedAt().toInstant();
                                    ZonedDateTime zoned = instant.atZone(ZoneId.systemDefault());
                                    return zoned.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH); // e.g. "October"
                                },
                                Collectors.counting()
                        )
                ));

        // Flatten into a list of objects with year, month, and count
        List<Map<String, Object>> installedOverMonths = new ArrayList<>();
        metersInstalledByYearAndMonth.forEach((yr, monthMap) -> {
            monthMap.forEach((month, count) -> {
                Map<String, Object> data = new HashMap<>();
                data.put("year", yr);
                data.put("month", month);
                data.put("count", count);
                installedOverMonths.add(data);
            });
        });

        // Optionally sort results by year then by month order
        installedOverMonths.sort(Comparator
                .comparing((Map<String, Object> e) -> (Integer) e.get("year"))
                .thenComparing(e -> Month.valueOf(e.get("month").toString())));

        Map<String, Object> resp = new HashMap<>();
        resp.put("inventory", String.format("%.2f", inventoryPercent));
        resp.put("allocated", String.format("%.2f", allocatedPercent));
//        resp.put("assigned", String.format("%.2f", assignedPercent));
//        resp.put("deactivated", String.format("%.2f", deactivatedPercent));

        Map<String, Object> card = new HashMap<>();
        card.put("totalMeter", filteredMeters.size());
        card.put("inventory", inventory);
        card.put("allocated", allocated);
        card.put("assigned", assigned);
        card.put("deactivated", deactivated);

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("cardData", card);
        response.put("percentData", resp);
        response.put("manufacturers", manufacturersSummary);
        response.put("installedOverMonths", installedOverMonths);

        return ResponseMap.response(status.getSuccessCode(), status.getDesc(), response);

    } catch (Exception exception) {
        log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
        genericHandler.logIncidentReport("fetching data management dashboard failed");
        genericHandler.logAndSaveException(exception, "fetch data management dashboard");
        throw exception;
    }
}


    @Override
    public Map<String, Object> vendingDashboard(String band, String year, String meterCategory) {
        try {
            UserModel um = handleUserValidation();
            List<Transaction> transactions = dashboardMapper.getVendingTransaction(um.getOrgId());

            // === Filter transactions ===
            List<Transaction> filteredTransaction = transactions.stream()
                    .filter(t -> band == null || band.isEmpty() || t.getBandName().equalsIgnoreCase(band))
                    .filter(t -> meterCategory == null || meterCategory.isEmpty() ||
                            t.getMeterCategory().equalsIgnoreCase(meterCategory))
                    .filter(t -> {
                        if (year == null || year.isEmpty() || t.getCreatedAt() == null)
                            return true;
                        ZonedDateTime zoned = t.getCreatedAt().toInstant().atZone(ZoneId.systemDefault());
                        return String.valueOf(zoned.getYear()).equals(year);
                    })
                    .collect(Collectors.toList());

            int total = filteredTransaction.size();
            if (total == 0) total = 1;

            // === Compute current and previous year ===
            int currentYear = year != null && !year.isEmpty()
                    ? Integer.parseInt(year)
                    : ZonedDateTime.now().getYear();
            int previousYear = currentYear - 1;

            // === Token counts ===
            long creditToken = filteredTransaction.stream()
                    .filter(m -> m.getTokenType().equalsIgnoreCase("credit-token"))
                    .count();

            long kct = filteredTransaction.stream()
                    .filter(m -> m.getTokenType().equalsIgnoreCase("kct"))
                    .count();

            long clearTamper = filteredTransaction.stream()
                    .filter(m -> m.getTokenType().equalsIgnoreCase("clear-tamper"))
                    .count();

            long clearCredit = filteredTransaction.stream()
                    .filter(m -> m.getTokenType().equalsIgnoreCase("clear-credit"))
                    .count();

            long kctClearTamper = filteredTransaction.stream()
                    .filter(m -> m.getTokenType().equalsIgnoreCase("kct-clear-tamper"))
                    .count();

            long compensation = filteredTransaction.stream()
                    .filter(m -> m.getTokenType().equalsIgnoreCase("compensation"))
                    .count();

            // === Token statuses ===
            long success = filteredTransaction.stream()
                    .filter(m -> m.getStatus().equalsIgnoreCase("Successful"))
                    .count();

            long pending = filteredTransaction.stream()
                    .filter(m -> m.getStatus().equalsIgnoreCase("Pending"))
                    .count();

            long fail = filteredTransaction.stream()
                    .filter(m -> m.getStatus().equalsIgnoreCase("Failed"))
                    .count();

            // === Card Totals (filtered by selected/current year) ===
            BigDecimal transactionSum = filteredTransaction.stream()
                    .map(Transaction::getInitialAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal unitCostSum = filteredTransaction.stream()
                    .map(Transaction::getUnitCost)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal vatSum = filteredTransaction.stream()
                    .map(Transaction::getVatAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalProfit = filteredTransaction.stream()
                    .filter(t -> "Successful".equalsIgnoreCase(t.getStatus()))
                    .map(Transaction::getFinalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // === Compute previous year totals ===
            List<Transaction> previousYearTransactions = transactions.stream()
                    .filter(t -> t.getCreatedAt() != null)
                    .filter(t -> {
                        int yr = t.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).getYear();
                        return yr == previousYear;
                    })
                    .collect(Collectors.toList());

            BigDecimal previousTransactionSum = previousYearTransactions.stream()
                    .map(Transaction::getInitialAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal previousUnitCostSum = previousYearTransactions.stream()
                    .map(Transaction::getUnitCost)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal previousVatAmountSum = previousYearTransactions.stream()
                    .map(Transaction::getVatAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal previousTotalProfit = previousYearTransactions.stream()
                    .filter(t -> "Successful".equalsIgnoreCase(t.getStatus()))
                    .map(Transaction::getFinalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // === Percentages ===
            double creditTokenPercent = (creditToken * 100.0) / total;
            double kctPercent = (kct * 100.0) / total;
            double clearTamperPercent = (clearTamper * 100.0) / total;
            double clearCreditPercent = (clearCredit * 100.0) / total;
            double kctClearTamperPercent = (kctClearTamper * 100.0) / total;
            double compensationPercent = (compensation * 100.0) / total;

            double successPercent = (success * 100.0) / total;
            double pendingPercent = (pending * 100.0) / total;
            double failPercent = (fail * 100.0) / total;

            // === Group transactions by year and month (for charts) ===
            Map<Integer, Map<String, Map<String, BigDecimal>>> transactionByYearAndMonth = transactions.stream()
                    .filter(m -> m.getCreatedAt() != null)
                    .collect(Collectors.groupingBy(
                            m -> m.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).getYear(),
                            Collectors.groupingBy(
                                    m -> m.getCreatedAt().toInstant().atZone(ZoneId.systemDefault())
                                            .getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                                    Collectors.collectingAndThen(Collectors.toList(), list -> {
                                        BigDecimal amountSum = list.stream()
                                                .map(Transaction::getInitialAmount)
                                                .filter(Objects::nonNull)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        BigDecimal costUnitSum = list.stream()
                                                .map(Transaction::getUnitCost)
                                                .filter(Objects::nonNull)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        BigDecimal vatAmountSum = list.stream()
                                                .map(Transaction::getVatAmount)
                                                .filter(Objects::nonNull)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        Map<String, BigDecimal> sums = new HashMap<>();
                                        sums.put("amountSum", amountSum);
                                        sums.put("costUnitSum", costUnitSum);
                                        sums.put("vatAmountSum", vatAmountSum);
                                        return sums;
                                    })
                            )
                    ));

            List<Map<String, Object>> transactionOverMonths = new ArrayList<>();
            transactionByYearAndMonth.forEach((yr, monthMap) -> {
                monthMap.forEach((mn, sums) -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("year", yr);
                    data.put("month", mn);
                    data.put("transactionSum", sums.get("amountSum"));
                    data.put("unitCostSum", sums.get("costUnitSum"));
                    data.put("vatAmountSum", sums.get("vatAmountSum"));
                    transactionOverMonths.add(data);
                });
            });

            transactionOverMonths.sort(Comparator
                    .comparing((Map<String, Object> e) -> (Integer) e.get("year"))
                    .thenComparing(e -> Month.valueOf(e.get("month").toString().toUpperCase())));

            // === Build response objects ===
            Map<String, Object> percentData = Map.of(
                    "creditToken", String.format("%.2f", creditTokenPercent),
                    "kctToken", String.format("%.2f", kctPercent),
                    "clearTamperToken", String.format("%.2f", clearTamperPercent),
                    "clearCreditToken", String.format("%.2f", clearCreditPercent),
                    "kctClearTamperToken", String.format("%.2f", kctClearTamperPercent),
                    "compensationToken", String.format("%.2f", compensationPercent)
            );

            Map<String, Object> cardData = Map.of(
                    "transactionSum", transactionSum,
                    "previousTransactionSum", previousTransactionSum,
                    "unitCostSum", unitCostSum,
                    "previousUnitCostSum", previousUnitCostSum,
                    "vatAmountSum", vatSum,
                    "previousVatAmountSum", previousVatAmountSum,
                    "totalProfit", totalProfit,
                    "previousTotalProfit", previousTotalProfit
            );

            Map<String, Object> transactionStatus = Map.of(
                    "success", successPercent,
                    "pending", pendingPercent,
                    "failed", failPercent
            );

            Map<String, Object> response = new HashMap<>();
            response.put("cardData", cardData);
            response.put("tokenDistribution", percentData);
            response.put("transactionStatus", transactionStatus);
            response.put("transactionOverMonths", transactionOverMonths);

            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), response);

        } catch (Exception exception) {
            log.error("Error occurred while fetching vending dashboard: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("fetching vending dashboard failed");
            genericHandler.logAndSaveException(exception, "fetch vending dashboard");
            throw exception;
        }
    }

//    @Override
//    public Map<String, Object> vendingDashboard(String band, String year, String meterCategory) {
//        try {
//            UserModel um = handleUserValidation();
//            List<Transaction> transactions = dashboardMapper.getVendingTransaction(um.getOrgId());
//
//            // === Filter transactions ===
//            List<Transaction> filteredTransaction = transactions.stream()
//                    .filter(t -> band == null || band.isEmpty() || t.getBandName().equalsIgnoreCase(band))
//                    .filter(t -> meterCategory == null || meterCategory.isEmpty() ||
//                            t.getMeterCategory().equalsIgnoreCase(meterCategory))
//                    .filter(t -> {
//                        if (year == null || year.isEmpty() || t.getCreatedAt() == null)
//                            return true;
//                        ZonedDateTime zoned = t.getCreatedAt().toInstant().atZone(ZoneId.systemDefault());
//                        return String.valueOf(zoned.getYear()).equals(year);
//                    })
//                    .collect(Collectors.toList());
//
//            int total = filteredTransaction.size();
//            if (total == 0) total = 1;
//
//            // === Compute current year and previous year ===
//            int currentYear = year != null && !year.isEmpty()
//                    ? Integer.parseInt(year)
//                    : ZonedDateTime.now().getYear();
//            int previousYear = currentYear - 1;
//
//            // === Token counts ===
//            long creditToken = filteredTransaction.stream()
//                    .filter(m -> m.getTokenType().equalsIgnoreCase("credit-token"))
//                    .count();
//
//            long kct = filteredTransaction.stream()
//                    .filter(m -> m.getTokenType().equalsIgnoreCase("kct"))
//                    .count();
//
//            long clearTamper = filteredTransaction.stream()
//                    .filter(m -> m.getTokenType().equalsIgnoreCase("clear-tamper"))
//                    .count();
//
//            long clearCredit = filteredTransaction.stream()
//                    .filter(m -> m.getTokenType().equalsIgnoreCase("clear-credit"))
//                    .count();
//
//            long kctClearTamper = filteredTransaction.stream()
//                    .filter(m -> m.getTokenType().equalsIgnoreCase("kct-clear-tamper"))
//                    .count();
//
//            long compensation = filteredTransaction.stream()
//                    .filter(m -> m.getTokenType().equalsIgnoreCase("compensation"))
//                    .count();
//
//            // === Token counts ===
//            long success = filteredTransaction.stream()
//                    .filter(m -> m.getStatus().equalsIgnoreCase("Completed"))
//                    .count();
//
//            long pending = filteredTransaction.stream()
//                    .filter(m -> m.getStatus().equalsIgnoreCase("Pending"))
//                    .count();
//
//            long fail = filteredTransaction.stream()
//                    .filter(m -> m.getStatus().equalsIgnoreCase("Failed"))
//                    .count();
//
//
//            // === Card Totals (filtered by year) ===
//            BigDecimal transactionSum = filteredTransaction.stream()
//                    .map(Transaction::getInitialAmount)
//                    .filter(Objects::nonNull)
//                    .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//            BigDecimal unitCostSum = filteredTransaction.stream()
//                    .map(Transaction::getUnitCost)
//                    .filter(Objects::nonNull)
//                    .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//            BigDecimal vatSum = filteredTransaction.stream()
//                    .map(Transaction::getVatAmount)
//                    .filter(Objects::nonNull)
//                    .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//            BigDecimal totalProfit = filteredTransaction.stream()
//                    .filter(t -> "Completed".equalsIgnoreCase(t.getStatus()))
//                    .map(Transaction::getFinalAmount)
//                    .filter(Objects::nonNull)
//                    .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//            // === Percentages ===
//            double creditTokenPercent = (creditToken * 100.0) / total;
//            double kctPercent = (kct * 100.0) / total;
//            double clearTamperPercent = (clearTamper * 100.0) / total;
//            double clearCreditPercent = (clearCredit * 100.0) / total;
//            double kctClearTamperPercent = (kctClearTamper * 100.0) / total;
//            double compensationPercent = (compensation * 100.0) / total;
//
//            double successPercent = (success * 100.0) / total;
//            double pendingPercent = (pending * 100.0) / total;
//            double failPercent = (fail * 100.0) / total;
//
//            // === Group transactions by year and month (for charts) ===
//            Map<Integer, Map<String, Map<String, BigDecimal>>> transactionByYearAndMonth = transactions.stream()
//                    .filter(m -> m.getCreatedAt() != null)
//                    .collect(Collectors.groupingBy(
//                            // Group by Year
//                            m -> m.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).getYear(),
//                            Collectors.groupingBy(
//                                    // Group by Month (e.g., "January")
//                                    m -> m.getCreatedAt().toInstant().atZone(ZoneId.systemDefault())
//                                            .getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
//                                    // For each month, compute sums
//                                    Collectors.collectingAndThen(Collectors.toList(), list -> {
//                                        BigDecimal amountSum = list.stream()
//                                                .map(Transaction::getInitialAmount)
//                                                .filter(Objects::nonNull)
//                                                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//                                        BigDecimal costUnitSum = list.stream()
//                                                .map(Transaction::getUnitCost)
//                                                .filter(Objects::nonNull)
//                                                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//                                        BigDecimal vatAmountSum = list.stream()
//                                                .map(Transaction::getVatAmount)
//                                                .filter(Objects::nonNull)
//                                                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//                                        Map<String, BigDecimal> sums = new HashMap<>();
//                                        sums.put("amountSum", amountSum);
//                                        sums.put("costUnitSum", costUnitSum);
//                                        sums.put("vatAmountSum", vatAmountSum);
//                                        return sums;
//                                    })
//                            )
//                    ));
//
//            List<Map<String, Object>> transactionOverMonths = new ArrayList<>();
//            transactionByYearAndMonth.forEach((yr, monthMap) -> {
//                monthMap.forEach((mn, sums) -> {
//                    Map<String, Object> data = new HashMap<>();
//                    data.put("year", yr);
//                    data.put("month", mn);
//                    data.put("transactionSum", sums.get("amountSum"));
//                    data.put("unitCostSum", sums.get("costUnitSum"));
//                    data.put("vatAmountSum", sums.get("vatAmountSum"));
//                    transactionOverMonths.add(data);
//                });
//            });
//
//            // === Sort by year and month chronologically ===
//            transactionOverMonths.sort(Comparator
//                    .comparing((Map<String, Object> e) -> (Integer) e.get("year"))
//                    .thenComparing(e -> Month.valueOf(e.get("month").toString().toUpperCase())));
//
//
//            // === Build response objects ===
//            Map<String, Object> percentData = Map.of(
//                    "creditToken", String.format("%.2f", creditTokenPercent),
//                    "kctToken", String.format("%.2f", kctPercent),
//                    "clearTamperToken", String.format("%.2f", clearTamperPercent),
//                    "clearCreditToken", String.format("%.2f", clearCreditPercent),
//                    "kctClearTamperToken", String.format("%.2f", kctClearTamperPercent),
//                    "compensationToken", String.format("%.2f", compensationPercent)
//            );
//
//            Map<String, Object> cardData = Map.of(
//                    "transactionSum", transactionSum,
//                    "previousTransactionSum", 0,
//                    "unitCostSum", unitCostSum,
//                    "previousUnitCostSum", 0,
//                    "vatAmountSum", vatSum,
//                    "previousVatAmountSum", 0,
//                    "totalProfit", totalProfit,
//                    "previousTotalProfit", 0
//            );
//
//            Map<String, Object> transactionStatus = Map.of(
//                    "success", successPercent,
//                    "pending", pendingPercent,
//                    "failed", failPercent
//            );
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("cardData", cardData);
//            response.put("tokenDistribution", percentData);
//            response.put("transactionStatus", transactionStatus);
//            response.put("transactionOverMonths", transactionOverMonths);
//
//            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), response);
//
//        } catch (Exception exception) {
//            log.error("Error occurred while fetching vending dashboard: {}", exception.getMessage(), exception);
//            genericHandler.logIncidentReport("fetching vending dashboard failed");
//            genericHandler.logAndSaveException(exception, "fetch vending dashboard");
//            throw exception;
//        }
//    }

    @Override
    public Map<String, Object> billingDashboard() {
        return Map.of();
    }
}
