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
import org.memmcol.gridflexbackendservice.service.band.BandServiceImpl;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
                .filter(m -> "Created".equalsIgnoreCase(m.getMeterStage()))
                .count();

        long allocated = filteredMeters.stream()
                .filter(m -> m.getNodeId() != null && m.getCustomerId() == null)
                .count();

        long assigned = filteredMeters.stream()
                .filter(m -> m.getCustomerId() != null)
                .count();

        long deactivated = filteredMeters.stream()
                .filter(m -> "Deactivated".equalsIgnoreCase(m.getStatus()))
                .count();

        long allocatedSummary = allocated + assigned;

        // Calculate percentages
        double inventoryPercent = (inventory * 100.0) / total;
        double allocatedPercent = (allocatedSummary * 100.0) / total;
        double assignedPercent = (assigned * 100.0) / total;
        double deactivatedPercent = (deactivated * 100.0) / total;

//        // Extract unique manufacturers
//        List<Manufacturer> uniqueManufacturers = meters.stream()
//                .map(Meter::getManufacturer)
//                .filter(Objects::nonNull)
//                .distinct() // make sure Manufacturer implements equals() and hashCode()
//                .collect(Collectors.toList());

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
        resp.put("assigned", String.format("%.2f", assignedPercent));
        resp.put("deactivated", String.format("%.2f", deactivatedPercent));

        Map<String, Object> card = new HashMap<>();
        card.put("totalMeter", filteredMeters.size());
        card.put("inventory", inventory);
        card.put("allocated", allocatedSummary);
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
    public Map<String, Object> vendingDashboard() {
        return Map.of();
    }

    @Override
    public Map<String, Object> billingDashboard() {
        return Map.of();
    }
}
