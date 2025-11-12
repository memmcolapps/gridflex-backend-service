package org.memmcol.gridflexbackendservice.service.hes;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.memmcol.gridflexbackendservice.model.hes.DashboardSummaryResponse;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class HesClientServiceImpl implements HesService {
    @Autowired
    private ResponseProperties status;

//    @Autowired
//    private HesAuthServiceImpl auth;

    @Autowired
    private WebClient webClient;

    @Autowired
    private GenericHandler genericHandler;

    @Autowired
    private DashboardAsyncService asyncService;

    private final IMap<String, Object> hesTokenCache;

    public HesClientServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.hesTokenCache = hazelcastInstance.getMap("hesTokenCache");
    }

    public Map<String, Object> dashboard() {

        try {

            // Run all tasks asynchronously in parallel
            CompletableFuture<DashboardSummaryResponse.MeterSummary> meterSummaryFuture = asyncService.getMeterSummaryAsync();
            CompletableFuture<List<DashboardSummaryResponse.CommunicationLogPoint>> communicationLogsFuture = asyncService.getCommunicationLogsAsync();
            CompletableFuture<DashboardSummaryResponse.DataSchedulerRate> schedulerRateFuture = asyncService.getSchedulerRateAsync();
            CompletableFuture<List<DashboardSummaryResponse.CommunicationReportRow>> communicationReportFuture = asyncService.
                    getCommunicationReportAsync(0, 5, "lastSync", true);

            // Wait for all to complete
            CompletableFuture.allOf(
                    meterSummaryFuture,
                    communicationLogsFuture,
                    schedulerRateFuture,
                    communicationReportFuture
            ).join();
            DashboardSummaryResponse resp;
            // Combine results
            resp = new DashboardSummaryResponse(
                    meterSummaryFuture.join(),
                    communicationLogsFuture.join(),
                    schedulerRateFuture.join(),
                    communicationReportFuture.join()
            );

            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), resp);

        } catch (Exception exception) {
            genericHandler.logIncidentReport("fetching hes dashboard service failed");
            genericHandler.logAndSaveException(exception, "fetching hes dashboard");
            throw exception;
        }
    }

//    public Map<String, Object> dashboard() {
//        String token = auth.getAccessToken();
//
//        try {
//            Map<String, Object> resp = webClient.get()
//                    .uri("/api/dashboard/summary")
//                    .headers(h -> h.setBearerAuth(token))
//                    .retrieve()
//                    .onStatus(HttpStatusCode::isError,
//                            clientResponse -> clientResponse.bodyToMono(String.class)
//                                    .map(body -> new RuntimeException("HES service error: " + body))
//                    )
//                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
//                    .block();
//
//            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), resp);
//
//        } catch (WebClientResponseException webClientResponseException) {
//            genericHandler.logIncidentReport("fetching hes dashboard service failed");
//            genericHandler.logAndSaveException(webClientResponseException, "fetching hes dashboard");
//            // handles HTTP errors
//            throw webClientResponseException;
//        } catch (Exception exception) {
//            genericHandler.logIncidentReport("fetching hes dashboard service failed");
//            genericHandler.logAndSaveException(exception, "fetching hes dashboard");
//            throw exception;
//        }
//    }
}
