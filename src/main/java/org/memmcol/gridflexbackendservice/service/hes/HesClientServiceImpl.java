package org.memmcol.gridflexbackendservice.service.hes;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.memmcol.gridflexbackendservice.mapper.HesMapper;
import org.memmcol.gridflexbackendservice.model.hes.DashboardSummaryResponse;
import org.memmcol.gridflexbackendservice.model.hes.Event;
import org.memmcol.gridflexbackendservice.model.hes.Profile;
import org.memmcol.gridflexbackendservice.model.meter.SmartMeterInfo;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;


import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.memmcol.gridflexbackendservice.components.handleValidUser.handleUserValidation;

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

    @Autowired
    private HesMapper hesMapper;

    private final IMap<String, Object> hesTokenCache;

    public HesClientServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.hesTokenCache = hazelcastInstance.getMap("hesTokenCache");
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> communicationReport(int page, int size, String type, String search) {
        try {
            // Call async method (now returns Map<String, Object>)
            CompletableFuture<Map<String, Object>> communicationReportFuture =
                    asyncService.getAllCommunicationReportAsync(page, size, "lastSync", true, type, search);

            // Wait for completion
            CompletableFuture.allOf(communicationReportFuture).join();

            // Get the data from async response
            Map<String, Object> result = communicationReportFuture.join();

            // Wrap in your response format
            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Communication Report fetched successfully",
                    result
            );

        } catch (Exception exception) {
            genericHandler.logIncidentReport("fetching communication report service failed");
            genericHandler.logAndSaveException(exception, "fetching communication report");
            throw exception;
        }
    }

    @Override
    public Map<String, Object> profile(LocalDateTime startDate, LocalDateTime endDate, String meterNumber,
                                       String profile, String model,int page, int size, String search) {
        try {

            UserModel um = handleUserValidation();
            List<Profile> profiles;

            if(profile.equalsIgnoreCase("load-profile-one")) {
                profiles = hesMapper.getProfileChannelOne(startDate, endDate, meterNumber, model, um.getOrgId(), page, size);
            } else if(profile.equalsIgnoreCase("load-profile-two")) {
                profiles = hesMapper.getProfileChannelTwo(startDate, endDate, meterNumber, model, um.getOrgId(), page, size);
            } else if(profile.equalsIgnoreCase("daily-billing-profile")) {
                profiles = hesMapper.getDailyBillingProfile(startDate, endDate, meterNumber, model, um.getOrgId(), page, size);
            } else if(profile.equalsIgnoreCase("monthly-billing-profile")) {
                profiles = hesMapper.getMonthlyBillingProfile(startDate, endDate, meterNumber, model, um.getOrgId(), page, size);
            } else {
                throw new GlobalExceptionHandler.NotFoundException("Profile type not found");
            }

            // Normalize search text
            String searchLower = (search == null) ? "" : search.toLowerCase();

            // SEARCH ON ANY FIELD
            List<Profile> filteredProfiles = profiles.stream()
                    .filter(e -> searchLower.isEmpty() ||
                            (e.getMeterNumber() != null && e.getMeterNumber().toLowerCase().equalsIgnoreCase(searchLower)) ||
                            (e.getMeterModel() != null && e.getMeterModel().toLowerCase().equalsIgnoreCase(searchLower)) ||
                            (e.getReceivedAt() != null && e.getReceivedAt().toString().equalsIgnoreCase(searchLower)) ||
                            (e.getMeterHealthIndicator() != null && e.getMeterHealthIndicator().toLowerCase().equalsIgnoreCase(searchLower))
                    )
                    .collect(Collectors.toList());

            // Pagination logic
            int totalProfiles = filteredProfiles.size();
            List<Profile> paginatedProfiles;
            if (size == 0) {
                paginatedProfiles = filteredProfiles; // Return all users
            } else {
                int fromIndex = Math.min(page * size, totalProfiles);
                int toIndex = Math.min(fromIndex + size, totalProfiles);
                paginatedProfiles = filteredProfiles.subList(fromIndex, toIndex);
            }

            // Prepare response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedProfiles);
            response.put("totalData", totalProfiles);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", (int) Math.ceil((double) paginatedProfiles.size() / size));

            return ResponseMap.response(status.getSuccessCode(), "Meter event fetched successfully", response);

        } catch (Exception exception) {
            genericHandler.logIncidentReport("profile report service failed");
            genericHandler.logAndSaveException(exception, "profile report");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> event(LocalDateTime startDate, LocalDateTime endDate, String meterNumber, String eventTypeName, String model, String search, int page, int size) {
        try {
            UserModel um = handleUserValidation();
            List<Event> events;

            if(startDate == null || endDate == null) {
                events = new ArrayList<>();
            } else {
                events = hesMapper.getEvents(startDate, endDate, meterNumber, eventTypeName, model, page, size, um.getOrgId());
            }

            // Normalize search text
            String searchLower = (search == null) ? "" : search.toLowerCase();

            // SEARCH ON ANY FIELD
            List<Event> filteredEvents = events.stream()
                    .filter(e -> searchLower.isEmpty() ||
                            (e.getMeterNumber() != null && e.getMeterNumber().toLowerCase().equalsIgnoreCase(searchLower)) ||
                            (e.getEventName() != null && e.getEventName().toLowerCase().equalsIgnoreCase(searchLower)) ||
                            (e.getEventTypeName() != null && e.getEventTypeName().toLowerCase().equalsIgnoreCase(searchLower)) ||
                            (e.getEventTypeDesc() != null && e.getEventTypeDesc().toLowerCase().equalsIgnoreCase(searchLower)) ||
                            (e.getEventTime() != null && e.getEventTime().toString().equalsIgnoreCase(searchLower))
                    )
                    .collect(Collectors.toList());

            // Pagination logic
            int totalTariffs = filteredEvents.size();
            List<Event> paginatedEvents;
            if (size == 0) {
                paginatedEvents = filteredEvents; // Return all users
            } else {
                int fromIndex = Math.min(page * size, totalTariffs);
                int toIndex = Math.min(fromIndex + size, totalTariffs);
                paginatedEvents = filteredEvents.subList(fromIndex, toIndex);
            }

            // Prepare response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedEvents);
            response.put("totalData", totalTariffs);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", (int) Math.ceil((double) paginatedEvents.size() / size));

            return ResponseMap.response(status.getSuccessCode(), "Meter event fetched successfully", response);

        } catch (Exception exception) {
            genericHandler.logIncidentReport("event report service failed");
            genericHandler.logAndSaveException(exception, "event report");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> modelEventType() {
        try {

            List<Event> event_type = hesMapper.getEventType();

            List<SmartMeterInfo> model = hesMapper.getModel();

            Map<String, Object> response = new HashMap<>();
            response.put("event_types", event_type);
            response.put("models", model);

            return ResponseMap.response(status.getSuccessCode(), "Fetched successfully", response);

        } catch (Exception exception) {
            genericHandler.logIncidentReport("event report service failed");
            genericHandler.logAndSaveException(exception, "event report");
            throw exception;
        }
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