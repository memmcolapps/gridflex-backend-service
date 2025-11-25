package org.memmcol.gridflexbackendservice.service.hes;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.memmcol.gridflexbackendservice.mapper.HesMapper;
import org.memmcol.gridflexbackendservice.mapper.NodeMapper;
import org.memmcol.gridflexbackendservice.model.hes.*;
import org.memmcol.gridflexbackendservice.model.meter.SmartMeterInfo;
import org.memmcol.gridflexbackendservice.model.node.Node;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.memmcol.gridflexbackendservice.components.handleValidUser.handleUserValidation;

@Service
public class HesClientServiceImpl implements HesService {
    @Autowired
    private ResponseProperties status;

    @Autowired
    private WebClient webClient;

    @Autowired
    private GenericHandler genericHandler;

//    @Autowired
//    private DashboardAsyncService asyncService;

    @Autowired
    private HesMapper hesMapper;

    private final IMap<String, Object> hesTokenCache;

    public HesClientServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.hesTokenCache = hazelcastInstance.getMap("hesTokenCache");
    }

//    @Transactional(readOnly = true)
//    public Map<String, Object> dashboard() {
//
//        try {
//
//            // Run all tasks asynchronously in parallel
//            CompletableFuture<DashboardSummaryResponse.MeterSummary> meterSummaryFuture = asyncService.getMeterSummaryAsync();
//            CompletableFuture<List<DashboardSummaryResponse.CommunicationLogPoint>> communicationLogsFuture =
//                    asyncService.getCommunicationLogsAsync();
//            CompletableFuture<DashboardSummaryResponse.DataSchedulerRate> schedulerRateFuture = asyncService.getSchedulerRateAsync();
//            CompletableFuture<List<DashboardSummaryResponse.CommunicationReportRow>> communicationReportFuture = asyncService.
//                    getCommunicationReportAsync(0, 5, "lastSync", true);
//
//            // Wait for all to complete
//            CompletableFuture.allOf(
//                    meterSummaryFuture,
//                    communicationLogsFuture,
//                    schedulerRateFuture,
//                    communicationReportFuture
//            ).join();
//            DashboardSummaryResponse resp;
//            // Combine results
//            resp = new DashboardSummaryResponse(
//                    meterSummaryFuture.join(),
//                    communicationLogsFuture.join(),
//                    schedulerRateFuture.join(),
//                    communicationReportFuture.join()
//            );
//
//            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), resp);
//
//        } catch (Exception exception) {
//            genericHandler.logIncidentReport("fetching hes dashboard service failed");
//            genericHandler.logAndSaveException(exception, "fetching hes dashboard");
//            throw exception;
//        }
//    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {

        try {
            UserModel user = handleUserValidation();

            // METER SUMMARY
            int total = hesMapper.countAll(user.getOrgId());
            int online = hesMapper.getActiveMeterCount("ONLINE");
            int offline = Math.max(total - online, 0);
//            int failedCommands = 0;

            DashboardSummaryResponse.MeterSummary meterSummary =
                    new DashboardSummaryResponse.MeterSummary(total, online, offline);

            // COMMUNICATION LOGS (last 24 hours)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime fromTime = now.minusHours(24);

            List<MeterConnEvent> recentEvents = hesMapper.findRecentEvents(fromTime);

            List<DashboardSummaryResponse.CommunicationLogPoint> communicationLogs = new ArrayList<>();
            for (int i = 4; i <= 24; i += 4) {

                LocalDateTime start = now.minusHours(i);
                LocalDateTime end = now.minusHours(i - 4);

                long count = recentEvents.stream()
                        .filter(e -> e.getOnlineTime() != null &&
                                (e.getOnlineTime().isAfter(start) && e.getOnlineTime().isBefore(end)))
                        .count();

                communicationLogs.add(new DashboardSummaryResponse.CommunicationLogPoint(i + " hrs", (int) count));
            }

            // COMMUNICATION REPORT
            List<MeterConnEvent> commReport = hesMapper.getCommReport(user.getOrgId());

            List<DashboardSummaryResponse.CommunicationReportRow> communicationReport =
                    commReport.stream()
                            .map(e -> new DashboardSummaryResponse.CommunicationReportRow(
                                    e.getMeterNo(),
                                    e.getMeter().getSmartMeterInfo().getMeterModel(),
                                    e.getConnectionType(),
                                    e.getUpdatedAt()
                            ))
                            .toList();

            // COMMUNICATION EVENTS
            List<Event> eventReport = hesMapper.getEventsReport(user.getOrgId());

            List<DashboardSummaryResponse.EventLogs> eventsReport =
                    eventReport.stream()
                            .map(e -> new DashboardSummaryResponse.EventLogs(
                                    e.getMeterNumber(),
                                    e.getMeterModel(),
                                    e.getEventTypeId(),
                                    e.getEventCode(),
                                    e.getEventTime(),
                                    e.getCurrentThreshold(),
                                    e.getEventName(),
                                    e.getCreatedAt(),
                                    e.getEventType().getName(),
                                    e.getEventType().getObisCode(),
                                    e.getEventType().getDescription()
                            ))
                            .toList();


            // FINAL RESPONSE
            DashboardSummaryResponse resp = new DashboardSummaryResponse(
                    meterSummary,
                    communicationLogs,
                    eventsReport,
                    communicationReport
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
    public Map<String, Object> communicationReport(int page, int size, String type, String search,String node) {
        try {

            UserModel um = handleUserValidation();
            List<MeterConnEvent> meterConnEvent;

            if("MD".equalsIgnoreCase(type)){
                meterConnEvent = hesMapper.getCommunicationReport(page, size, um.getOrgId(), type, node);
            } else if("Non-MD".equalsIgnoreCase(type)) {
                String type1 = "three-phase";
                String type2 = "single-phase";
                meterConnEvent = hesMapper.getCommunicationNonMDReport(page, size, um.getOrgId(), type1, type2, "", node);
            } else {
                String type1 = "three-phase";
                String type2 = "single-phase";
                String type3 = "MD";
                meterConnEvent = hesMapper.getCommunicationNonMDReport(page, size, um.getOrgId(), type1, type2, type3, node);
            }

            // Normalize search text
            String searchLower = (search == null) ? "" : search.toLowerCase();

            // SEARCH ON ANY FIELD
            List<MeterConnEvent> filteredComm = meterConnEvent.stream()
                    .filter(e -> searchLower.isEmpty() ||
                            (e.getMeterNo() != null && e.getMeterNo().toLowerCase().equalsIgnoreCase(searchLower)) ||
                            (e.getUpdatedAt() != null && e.getUpdatedAt().toString().equalsIgnoreCase(searchLower)) ||
                            (e.getConnectionType() != null && e.getConnectionType().toLowerCase().equalsIgnoreCase(searchLower))
                    )
                    .collect(Collectors.toList());

            // Pagination logic
            int totalComm = filteredComm.size();
            List<MeterConnEvent> paginatedEvents;
            if (size == 0) {
                paginatedEvents = filteredComm; // Return all users
            } else {
                int fromIndex = Math.min(page * size, totalComm);
                int toIndex = Math.min(fromIndex + size, totalComm);
                paginatedEvents = filteredComm.subList(fromIndex, toIndex);
            }

            // Prepare response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedEvents);
            response.put("totalData", totalComm);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", (int) Math.ceil((double) paginatedEvents.size() / size));
            return ResponseMap.response(status.getSuccessCode(), "Fetched successfully", response);
//            // Call async method (now returns Map<String, Object>)
//            CompletableFuture<Map<String, Object>> communicationReportFuture =
//                    asyncService.getAllCommunicationReportAsync(page, size, "lastSync", true, type, search);
//
//            // Wait for completion
//            CompletableFuture.allOf(communicationReportFuture).join();
//
//            // Get the data from async response
//            Map<String, Object> result = communicationReportFuture.join();
//
//            // Wrap in your response format
//            return ResponseMap.response(
//                    status.getSuccessCode(),
//                    "Communication Report fetched successfully",
//                    result
//            );

        } catch (Exception exception) {
            genericHandler.logIncidentReport("fetching communication report service failed");
            genericHandler.logAndSaveException(exception, "fetching communication report");
            throw exception;
        }
    }

    @Override
    public Map<String, Object> profile(LocalDateTime startDate, LocalDateTime endDate, String meterNumber,
                                       String profile, String model,int page, int size, String search, String node) {
        try {

            UserModel um = handleUserValidation();
            List<Profile> profiles;

            if(startDate == null || endDate == null) {
                profiles = new ArrayList<>();
            } else if(profile.equalsIgnoreCase("load-profile-one")) {
                profiles = hesMapper.getProfileChannelOne(startDate, endDate, meterNumber, model, um.getOrgId(), page, size, node);
            } else if(profile.equalsIgnoreCase("load-profile-two")) {
                profiles = hesMapper.getProfileChannelTwo(startDate, endDate, meterNumber, model, um.getOrgId(), page, size, node);
            } else if(profile.equalsIgnoreCase("daily-billing-profile")) {
                profiles = hesMapper.getDailyBillingProfile(startDate, endDate, meterNumber, model, um.getOrgId(), page, size, node);
            } else if(profile.equalsIgnoreCase("monthly-billing-profile")) {
                profiles = hesMapper.getMonthlyBillingProfile(startDate, endDate, meterNumber, model, um.getOrgId(), page, size, node);
            } else {
                throw new GlobalExceptionHandler.NotFoundException("Unsupported type");
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

            return ResponseMap.response(status.getSuccessCode(), "Meter profile fetched successfully", response);

        } catch (Exception exception) {
            genericHandler.logIncidentReport("profile report service failed");
            genericHandler.logAndSaveException(exception, "profile report");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> event(LocalDateTime startDate, LocalDateTime endDate, String meterNumber, String eventTypeName, String model, String search, int page, int size, String node) {
        try {
            UserModel um = handleUserValidation();
            List<Event> events;

            if(startDate == null || endDate == null) {
                events = new ArrayList<>();
            } else {
                events = hesMapper.getEvents(startDate, endDate, meterNumber, eventTypeName, model, page, size, um.getOrgId(), node);
            }

            // Normalize search text
            String searchLower = (search == null) ? "" : search.toLowerCase();

            // SEARCH ON ANY FIELD
            List<Event> filteredEvents = events.stream()
                    .filter(e -> searchLower.isEmpty() ||
                            (e.getMeterNumber() != null && e.getMeterNumber().toLowerCase().equalsIgnoreCase(searchLower)) ||
                            (e.getEventName() != null && e.getEventName().toLowerCase().equalsIgnoreCase(searchLower)) ||
                            (e.getEventType().getName() != null && e.getEventType().getName().toLowerCase().equalsIgnoreCase(searchLower)) ||
                            (e.getEventType().getDescription() != null && e.getEventType().getDescription().toLowerCase().equalsIgnoreCase(searchLower)) ||
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

            UserModel um = handleUserValidation();

            // Fetch all event type
            List<EventType> event_type = hesMapper.getEventType();

            // Fetch all model
            List<SmartMeterInfo> model = hesMapper.getModel(um.getOrgId());

            // Fetch all nodes
            List<Node> flatList =  hesMapper.getAllNode(um.getOrgId());
            if(flatList == null || flatList.isEmpty()){
                return ResponseMap.response(status.getSuccessCode(), status.getDesc(), flatList);
            }
            Map<UUID, Node> nodeMap = new HashMap<>();
            List<Node> roots = new ArrayList<>();

            // Map nodes by ID
            for (Node node : flatList) {
                nodeMap.put(node.getId(), node);
                node.setNodesTree(new ArrayList<>()); // Initialize children list
            }

            // Reconstruct the tree
            for (Node node : flatList) {
                if (node.getParentId() == null) {
                    roots.add(node); // Add root nodes to the list
                } else {
                    Node parent = nodeMap.get(node.getParentId());
                    if (parent != null) {
                        parent.getNodesTree().add(node); // Add as a child to the parent
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("event_types", event_type);
            response.put("models", model);
            response.put("nodes", roots);

            return ResponseMap.response(status.getSuccessCode(), "Fetched successfully", response);

        } catch (Exception exception) {
            genericHandler.logIncidentReport("profile event filter service failed");
            genericHandler.logAndSaveException(exception, "profile event filter");
            throw exception;
        }
    }

    @Override
    public Map<String, Object> communicationRangeReport(int page, int size, LocalDateTime startDate, LocalDateTime endDate, String type, String search, List<String> meterNumber,String node) {

        try {
            UserModel um = handleUserValidation();
            List<MeterConnEvent> meterConnEvent = hesMapper.getRangeCommunicationReport(page, size, startDate, endDate, um.getOrgId(), type, meterNumber, node);

            // Normalize search text
            String searchLower = (search == null) ? "" : search.toLowerCase();

            // SEARCH ON ANY FIELD
            List<MeterConnEvent> filteredComm = meterConnEvent.stream()
                    .filter(e -> searchLower.isEmpty() ||
                            (e.getMeterNo() != null && e.getMeterNo().toLowerCase().equalsIgnoreCase(searchLower)) ||
                            (e.getUpdatedAt() != null && e.getUpdatedAt().toString().equalsIgnoreCase(searchLower)) ||
                            (e.getConnectionType() != null && e.getConnectionType().toLowerCase().equalsIgnoreCase(searchLower))
                    )
                    .collect(Collectors.toList());

            // Pagination logic
            int totalComm = filteredComm.size();
            List<MeterConnEvent> paginatedEvents;
            if (size == 0) {
                paginatedEvents = filteredComm; // Return all users
            } else {
                int fromIndex = Math.min(page * size, totalComm);
                int toIndex = Math.min(fromIndex + size, totalComm);
                paginatedEvents = filteredComm.subList(fromIndex, toIndex);
            }

            // Prepare response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedEvents);
            response.put("totalData", totalComm);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", (int) Math.ceil((double) paginatedEvents.size() / size));
            return ResponseMap.response(status.getSuccessCode(), "Fetched successfully", response);
        }  catch (Exception exception) {
            genericHandler.logIncidentReport("communication daily/monthly report service failed");
            genericHandler.logAndSaveException(exception, "communication daily/monthly report");
            throw exception;
        }

    }

    @Override
    public SseEmitter subscribe() {
        return null;
    }

}

//            // SCHEDULER RATE
//            long active = hesMapper.countByJobStatusIgnoreCase("COMPLETED") +
//                    hesMapper.countByJobStatusIgnoreCase("RUNNING");
//
//            long paused = hesMapper.countByJobStatusIgnoreCase("PAUSED");
//
//            double sum = active + paused;
//            double activePercent = sum > 0 ? (active / sum) * 100.0 : 0;
//            double pausedPercent = sum > 0 ? (paused / sum) * 100.0 : 0;
//
//            DashboardSummaryResponse.DataSchedulerRate schedulerRate =
//                    new DashboardSummaryResponse.DataSchedulerRate(activePercent, pausedPercent);

// COMM REPORT
///---------------
//            // Run all tasks asynchronously in parallel
//            CompletableFuture<DashboardSummaryResponse.MeterSummary> meterSummaryFuture = asyncService.getMeterSummaryAsync();
//            CompletableFuture<List<DashboardSummaryResponse.CommunicationLogPoint>> communicationLogsFuture = asyncService.getCommunicationLogsAsync();
//            CompletableFuture<DashboardSummaryResponse.DataSchedulerRate> schedulerRateFuture = asyncService.getSchedulerRateAsync();
//            CompletableFuture<List<DashboardSummaryResponse.CommunicationReportRow>> communicationReportFuture = asyncService.
//                    getCommunicationReportAsync(0, 5, "lastSync", true);
//
//            // Wait for all to complete
//            CompletableFuture.allOf(
//                    meterSummaryFuture,
//                    communicationLogsFuture,
//                    schedulerRateFuture,
//                    communicationReportFuture
//            ).join();
//            DashboardSummaryResponse resp;
//            // Combine results
//            resp = new DashboardSummaryResponse(
//                    meterSummaryFuture.join(),
//                    communicationLogsFuture.join(),
//                    schedulerRateFuture.join(),
//                    communicationReportFuture.join()
//            );
///-----------------
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