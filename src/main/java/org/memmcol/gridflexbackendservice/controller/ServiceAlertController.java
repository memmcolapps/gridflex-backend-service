package org.memmcol.gridflexbackendservice.controller;

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
public class ServiceAlertController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceAlertController.class);

    @Autowired
    private ReportQueryService service;

    @Autowired
//    private UptimeReportRepository reportRepository;

//    private static final List<String> SERVICES = List.of("API-GATEWAY-SERVICE", "GRIDFLEX-BACKEND-SERVICE");

    @PostMapping("/alerts")
    public void receiveAlert(@RequestBody Map<String, Object> payload) {

//        logger.info(">>> Received alert: {}", payload);
        service.saveAlert(payload);
    }

//    @GetMapping("/reports/summary")
//    public ResponseEntity<Map<String, Object>> getSummary(
//            @RequestParam int year,
//            @RequestParam int month) {
//
//        YearMonth ym = YearMonth.of(year, month);
//        LocalDate startDate = ym.atDay(1);
//        LocalDate endDate = ym.atEndOfMonth();
//
//        // Fetch daily reports for both services
//        List<UptimeReport> dailyReports = reportRepository.findAll().stream()
//                .filter(r -> "DAILY".equals(r.getReportType())
//                        && r.getCreatedAt() != null
//                        && !r.getCreatedAt().isBefore(startDate)
//                        && !r.getCreatedAt().isAfter(endDate)
//                        && SERVICES.contains(r.getServiceName()))
//                .toList();
//
//        // Fetch monthly reports for both services
//        List<UptimeReport> monthlyReports = reportRepository.findAll().stream()
//                .filter(r -> "MONTHLY".equals(r.getReportType())
//                        && r.getMonth() != null
//                        && r.getMonth().equals(ym)
//                        && SERVICES.contains(r.getServiceName()))
//                .toList();
//
//        // Aggregate uptime/downtime across both services
//        long totalUp = monthlyReports.stream().mapToLong(UptimeReport::getUptimeMinutes).sum();
//        long totalDown = monthlyReports.stream().mapToLong(UptimeReport::getDowntimeMinutes).sum();
//        long total = totalUp + totalDown;
//
//        Map<String, Object> aggregated = new HashMap<>();
//        aggregated.put("services", SERVICES);
//        aggregated.put("uptimePercent", total == 0 ? 0 : (totalUp * 100.0 / total));
//        aggregated.put("downtimePercent", total == 0 ? 0 : (totalDown * 100.0 / total));
//        aggregated.put("uptimeMinutes", totalUp);
//        aggregated.put("downtimeMinutes", totalDown);
//
//        // Final response
//        Map<String, Object> response = new HashMap<>();
//        response.put("dailyReports", dailyReports);
//        response.put("monthlyReports", monthlyReports);
//        response.put("aggregatedSummary", aggregated);
//
//        return ResponseEntity.ok(response);
//    }

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


//    @GetMapping("/report/daily")
//    public ResponseEntity<List<UptimeReport>> getDailyReports(
////            @RequestParam String serviceName,
//            @RequestParam int year,
//            @RequestParam int month) {
//        return ResponseEntity.ok(queryService.getDailyReports(year, month));
//    }

//    @GetMapping("/report/monthly")
//    public ResponseEntity<List<UptimeReport>> getMonthlyReports(
////            @RequestParam String serviceName,
//            @RequestParam int year) {
//        return ResponseEntity.ok(queryService.getMonthlyReports(year));
//    }




//    @GetMapping("/burn-cpu")
//    public String burnCpu() {
//        long start = System.currentTimeMillis();
//        while (System.currentTimeMillis() - start < 30000) { // 30s busy loop
//            Math.sqrt(Math.random());
//        }
//        return "Done";
//    }
//
//    @GetMapping("/burn-memory")
//    public String burnMemory() {
//        for (int i = 0; i < 100; i++) {
//            memoryHog.add(new byte[10 * 1024 * 1024]); // allocate 10MB * 100 = 1GB
//            try { Thread.sleep(500); } catch (InterruptedException e) { }
//        }
//        return "Allocated ~1GB memory!";
////        for (int i = 0; i < 100; i++) {
////            memoryHog.add(new byte[10_000_000]); // 10MB each
////        }
////        return "Allocated ~1GB!";
//    }
//
//    @GetMapping("/slow")
//    public String slowEndpoint() throws InterruptedException {
//        Thread.sleep(5000); // 5s delay
//        return "Slow response!";
//    }








//
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/alerts")
//public class AlertController {
//
//    private final UptimeRepository repository;
//
//    public AlertController(UptimeRepository repository) {
//        this.repository = repository;
//    }
//
//    @PostMapping
//    public ResponseEntity<String> receiveAlert(@RequestBody Map<String, Object> alert) {
//        List<Map<String, Object>> alerts = (List<Map<String, Object>>) alert.get("alerts");
//
//        for (Map<String, Object> a : alerts) {
//            String status = (String) a.get("status"); // firing/resolved
//            Map<String, String> labels = (Map<String, String>) a.get("labels");
//            String alertName = labels.get("alertname");
//
//            if ("InstanceDown".equals(alertName) && "firing".equals(status)) {
//                repository.save(new UptimeLog(LocalDateTime.now(), false));
//            } else if ("InstanceUp".equals(alertName) && "firing".equals(status)) {
//                repository.save(new UptimeLog(LocalDateTime.now(), true));
//            }
//        }
//
//        return ResponseEntity.ok("Alert received");
//    }
//}
