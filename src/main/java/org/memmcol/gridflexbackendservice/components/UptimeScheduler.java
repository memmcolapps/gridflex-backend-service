package org.memmcol.gridflexbackendservice.components;

import java.time.LocalDate;
import java.time.YearMonth;

import org.memmcol.gridflexbackendservice.service.service_alert.ReportQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UptimeScheduler {

    private static final Logger logger = LoggerFactory.getLogger(UptimeScheduler.class);

    private final ReportQueryService reportQueryService;

    public UptimeScheduler(ReportQueryService reportQueryService) {
        this.reportQueryService = reportQueryService;
    }

    /**
     * Runs every day at 00:05 AM.
     * Generates report for yesterday.
     */
    @Scheduled(cron = "0 5 0 * * *")
    public void generateDailyReports() {

        LocalDate yesterday = LocalDate.now().minusDays(1);

        logger.info("Starting daily report generation for {}", yesterday);

        reportQueryService.calculateDailyReport("GRIDFLEX-BACKEND-SERVICE", yesterday);
        reportQueryService.calculateDailyReport("API-GATEWAY-SERVICE", yesterday);

        logger.info("Daily report generation completed.");
    }

    /**
     * Runs on the 1st day of every month at 00:10 AM.
     * Generates report for the previous month.
     */
    @Scheduled(cron = "0 10 0 1 * *")
    public void generateMonthlyReports() {

        YearMonth previousMonth = YearMonth.now().minusMonths(1);

        logger.info("Starting monthly report generation for {}", previousMonth);

        reportQueryService.calculateMonthlyReport("GRIDFLEX-BACKEND-SERVICE", previousMonth);
        reportQueryService.calculateMonthlyReport("API-GATEWAY-SERVICE", previousMonth);

        logger.info("Monthly report generation completed.");
    }

}



//@Component
//public class UptimeScheduler {
//
//    @Autowired
//    private ReportQueryService service;
//
//    @Autowired
//    private BillingServiceImpl billingService;
//
//    @Autowired
//    private BillingMapper billingMapper;

//    @Scheduled(cron = "0 5 0 * * *") // every day at 00:05
//    public void daily() {
//        service.calculateDailyReport("GRIDFLEX-BACKEND-SERVICE", LocalDate.now().minusDays(1));
//        service.calculateDailyReport("API-GATEWAY-SERVICE", LocalDate.now().minusDays(1));
//    }
//
//    @Scheduled(cron = "0 10 0 1 * *") // every 1st of month at 00:10
//    public void monthly() {
//        service.calculateMonthlyReport("GRIDFLEX-BACKEND-SERVICE", YearMonth.now().minusMonths(1));
//        service.calculateMonthlyReport("API-GATEWAY-SERVICE", YearMonth.now().minusMonths(1));
//    }

//    @Scheduled(cron = "0 0 2 1 * ?")
//    public void run() {
//
//        YearMonth billingMonth = YearMonth.now().minusMonths(1);
//        List<UUID> meterIds = billingMapper.findAllMeterIds();
//
//        for (UUID meterId : meterIds) {
//            billingService.calculateMonthlyConsumption(meterId, billingMonth);
//        }
//    }
//}

//@Component
//public class UptimeScheduler {
//
//    @Autowired
//    private ReportQueryService service;
//
//    // Run every 1 minute (instead of daily)
//    @Scheduled(cron = "0 * * * * *")
//    public void daily() {
//        System.out.println(">>> Running DAILY report scheduler at " + java.time.LocalDateTime.now());
//        service.calculateDailyReport("GRIDFLEX-BACKEND-SERVICE", LocalDate.now().minusDays(1));
//        service.calculateDailyReport("API-GATEWAY-SERVICE", LocalDate.now().minusDays(1));
//    }
//
//    // Run every 5 minutes (instead of monthly)
//    @Scheduled(cron = "0 */2 * * * *")
//    public void monthly() {
//        System.out.println(">>> Running MONTHLY report scheduler at " + java.time.LocalDateTime.now());
//        service.calculateMonthlyReport("GRIDFLEX-BACKEND-SERVICE", YearMonth.now().minusMonths(1));
//        service.calculateMonthlyReport("API-GATEWAY-SERVICE", YearMonth.now().minusMonths(1));
//    }
//}



