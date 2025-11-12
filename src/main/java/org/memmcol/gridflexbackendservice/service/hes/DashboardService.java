//package org.memmcol.gridflexbackendservice.service.hes;
//
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.memmcol.gridflexbackendservice.model.hes.DashboardSummaryResponse;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class DashboardService {
//    private final DashboardAsyncService asyncService;
//
//    // ======================
//    // Main method to call from controller
//    // ======================
//    public DashboardSummaryResponse getDashboardSummary() {
//
//        // Run all tasks asynchronously in parallel
//        CompletableFuture<DashboardSummaryResponse.MeterSummary> meterSummaryFuture = asyncService.getMeterSummaryAsync();
//        CompletableFuture<List<DashboardSummaryResponse.CommunicationLogPoint>> communicationLogsFuture = asyncService.getCommunicationLogsAsync();
//        CompletableFuture<DashboardSummaryResponse.DataSchedulerRate> schedulerRateFuture = asyncService.getSchedulerRateAsync();
//        CompletableFuture<List<DashboardSummaryResponse.CommunicationReportRow>> communicationReportFuture = asyncService.
//                getCommunicationReportAsync(0, 5, "lastSync", true, "", "");
//
//        // Wait for all to complete
//        CompletableFuture.allOf(
//                meterSummaryFuture,
//                communicationLogsFuture,
//                schedulerRateFuture,
//                communicationReportFuture
//        ).join();
//
//        // Combine results
//        return new DashboardSummaryResponse(
//                meterSummaryFuture.join(),
//                communicationLogsFuture.join(),
//                schedulerRateFuture.join(),
//                communicationReportFuture.join()
//        );
//    }
//
//
//}
//
