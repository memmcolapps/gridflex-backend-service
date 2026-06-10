package org.memmcol.gridflexbackendservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.memmcol.gridflexbackendservice.service.service_alert.ReportQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

@RestController
@RequestMapping("/service")
@Tag(name = "Service Alert", description = "Service Alert Management APIs")
public class ServiceAlertController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceAlertController.class);

    @Autowired
    private ReportQueryService service;

    @PostMapping("/alerts")
    public void receiveAlert(@RequestBody Map<String, Object> payload) {
        service.saveAlert(payload);
    }

    // Manually trigger daily report for yesterday
    @PostMapping("/trigger/daily")
    public ResponseEntity<String> triggerDaily() {
        service.calculateDailyReport("GRIDFLEX-BACKEND-SERVICE", LocalDate.now().minusDays(1));
        service.calculateDailyReport("API-GATEWAY-SERVICE", LocalDate.now().minusDays(1));
        return ResponseEntity.ok("Daily report triggered successfully");
    }

    // Manually trigger monthly report for last month
    @PostMapping("/trigger/monthly")
    public ResponseEntity<String> triggerMonthly() {
        service.calculateMonthlyReport("GRIDFLEX-BACKEND-SERVICE", YearMonth.now().minusMonths(1));
        service.calculateMonthlyReport("API-GATEWAY-SERVICE", YearMonth.now().minusMonths(1));
        return ResponseEntity.ok("Monthly report triggered successfully");
    }

}

