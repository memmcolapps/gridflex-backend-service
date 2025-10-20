package org.memmcol.gridflexbackendservice.service.dashboard;

import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.memmcol.gridflexbackendservice.mapper.DashboardMapper;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
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
public Map<String, Object> dataManagementDashboard() {
    try {
        UserModel um = handleUserValidation();

        List<Meter> meters = dashboardMapper.getMeters(um.getOrgId());

        // Calculate summary stats
        long inventory = meters.stream()
                .filter(m -> "Created".equalsIgnoreCase(m.getMeterStage()))
                .count();

        long allocated = meters.stream()
                .filter(m -> m.getNodeId() != null)
                .count();

        long assigned = meters.stream()
                .filter(m -> m.getNodeId() != null && m.getCustomerId() != null)
                .count();

        long deactivated = meters.stream()
                .filter(m -> "Deactivated".equalsIgnoreCase(m.getStatus()))
                .count();

        // Extract unique manufacturers
        List<Manufacturer> uniqueManufacturers = meters.stream()
                .map(Meter::getManufacturer)
                .filter(Objects::nonNull)
                .distinct() // make sure Manufacturer implements equals() and hashCode()
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
        metersInstalledByYearAndMonth.forEach((year, monthMap) -> {
            monthMap.forEach((month, count) -> {
                Map<String, Object> data = new HashMap<>();
                data.put("year", year);
                data.put("month", month);
                data.put("count", count);
                installedOverMonths.add(data);
            });
        });

        // Optionally sort results by year then by month order
        installedOverMonths.sort(Comparator
                .comparing((Map<String, Object> e) -> (Integer) e.get("year"))
                .thenComparing(e -> Month.valueOf(e.get("month").toString())));

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("totalMeter", meters.size());
        response.put("inventory", inventory);
        response.put("allocated", allocated);
        response.put("assigned", assigned);
        response.put("deactivated", deactivated);
        response.put("manufacturers", uniqueManufacturers);
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
