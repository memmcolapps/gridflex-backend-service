package org.memmcol.gridflexbackendservice.components;

import org.memmcol.gridflexbackendservice.mapper.BillingMapper;
import org.memmcol.gridflexbackendservice.mapper.MeterMapper;
import org.memmcol.gridflexbackendservice.service.billing.BillingServiceImpl;
import org.memmcol.gridflexbackendservice.service.service_alert.ReportQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Component
public class UptimeScheduler {

    @Autowired
    private ReportQueryService service;

    @Autowired
    private BillingServiceImpl billingService;

    @Autowired
    private BillingMapper billingMapper;

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
}

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



