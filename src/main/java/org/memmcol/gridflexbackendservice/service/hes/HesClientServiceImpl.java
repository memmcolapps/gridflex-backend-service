package org.memmcol.gridflexbackendservice.service.hes;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.memmcol.gridflexbackendservice.mapper.HesMapper;
import org.memmcol.gridflexbackendservice.model.hes.*;
import org.memmcol.gridflexbackendservice.model.meter.SmartMeterInfo;
import org.memmcol.gridflexbackendservice.model.node.Node;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.model.vend.MeterView;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;


import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.memmcol.gridflexbackendservice.components.HandleValidUser.handleUserValidation;

@Service
public class HesClientServiceImpl implements HesService {
    @Autowired
    private ResponseProperties status;

    @Qualifier("realtimeWebClient")
    @Autowired
    private WebClient webClient;

    @Qualifier("hesWebClient")
    @Autowired
    private WebClient hesWebClient;

    @Qualifier("dlmsWriteOpsClient")
    @Autowired
    private WebClient dlmsWriteOpsClient;

    @Autowired
    private GenericHandler genericHandler;

//    @Autowired
//    private DashboardAsyncService asyncService;
    @Autowired
    private HesAuthServiceImpl auth;

    @Autowired
    private HesMapper hesMapper;

    private final IMap<String, Object> hesTokenCache;

    public HesClientServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.hesTokenCache = hazelcastInstance.getMap("hesTokenCache");
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

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> profile(LocalDateTime startDate, LocalDateTime endDate, List<String> meterNumber,
                                       String profile, List<String> model,int page, int size, String search, String node) {
        try {

            UserModel um = handleUserValidation();
            List<Profile> profiles;

            if(startDate == null || endDate == null) {
                profiles = new ArrayList<>();
            } else if(profile.equalsIgnoreCase("load-profile-one")) {
                profiles = hesMapper.getProfileChannelOne(startDate, endDate, meterNumber, model, um.getOrgId(), page, size, node);
            } else if(profile.equalsIgnoreCase("load-profile-two")) {
                profiles = hesMapper.getProfileChannelTwo(startDate, endDate, meterNumber, model, um.getOrgId(), page, size, node);
            } else if(profile.equalsIgnoreCase("load-profile-one-household")) {
                profiles = hesMapper.getProfileChannelOneHouseHold(startDate, endDate, meterNumber, model, um.getOrgId(), page, size, node);
            } else if(profile.equalsIgnoreCase("load-profile-two-household")) {
                profiles = hesMapper.getProfileChannelTwoHouseHold(startDate, endDate, meterNumber, model, um.getOrgId(), page, size, node);
            } else if(profile.equalsIgnoreCase("load-profile-three-household")) {
                profiles = hesMapper.getProfileChannelThreeHouseHold(startDate, endDate, meterNumber, model, um.getOrgId(), page, size, node);
            } else if(profile.equalsIgnoreCase("daily-billing-profile")) {
                profiles = hesMapper.getDailyBillingProfile(startDate, endDate, meterNumber, model, um.getOrgId(), page, size, node);
            } else if(profile.equalsIgnoreCase("monthly-billing-profile")) {
                profiles = hesMapper.getMonthlyBillingProfile(startDate, endDate, meterNumber, model, um.getOrgId(), page, size, node);
            } else if(profile.equalsIgnoreCase("daily-billing-data-household")) {
                profiles = hesMapper.getDailyBillingDataHouseHold(startDate, endDate, meterNumber, model, um.getOrgId(), page, size, node);
            } else if(profile.equalsIgnoreCase("monthly-billing-data-household")) {
                profiles = hesMapper.getMonthlyBillingDataHouseHold(startDate, endDate, meterNumber, model, um.getOrgId(), page, size, node);
            }else if(profile.equalsIgnoreCase("daily-billing-energy-household")) {
                profiles = hesMapper.getDailyBillingEnergyHouseHold(startDate, endDate, meterNumber, model, um.getOrgId(), page, size, node);
            } else if(profile.equalsIgnoreCase("monthly-billing-energy-household")) {
                profiles = hesMapper.getMonthlyBillingEnergyHouseHold(startDate, endDate, meterNumber, model, um.getOrgId(), page, size, node);
            } else {
                throw new GlobalExceptionHandler.NotFoundException("Unsupported type");
            }

            // Normalize search text
            String searchLower = (search == null) ? "" : search.toLowerCase();

//            // SEARCH ON ANY FIELD
//            List<Profile> filteredProfiles = profiles.stream()
//                    .filter(e -> searchLower.isEmpty() ||
//                            (e.getMeterNumber() != null && e.getMeterNumber().toLowerCase().equalsIgnoreCase(searchLower)) ||
//                            (e.getMeterModel() != null && e.getMeterModel().toLowerCase().equalsIgnoreCase(searchLower)) ||
//                            (e.getReceivedAt() != null && e.getReceivedAt().toString().equalsIgnoreCase(searchLower)) ||
//                            (e.getMeterHealthIndicator() != null && e.getMeterHealthIndicator().toLowerCase().equalsIgnoreCase(searchLower))
//                    )
//                    .collect(Collectors.toList());
            List<Profile> filteredProfiles = profiles.stream()
                    .filter(e -> {
                        if (searchLower.isEmpty()) return true;

                        return contains(e.getMeterNumber(), searchLower)
                                || contains(e.getMeterModel(), searchLower)
                                || contains(e.getMeterHealthIndicator(), searchLower)
                                || containsDate(e.getReceivedAt(), searchLower)
                                || contains(e.getMeter() != null ? e.getMeter().getAccountNumber() : null, searchLower)
                                || containsNested(e, searchLower);
                    })
                    .collect(Collectors.toList());

            List<Profile> paginatedProfiles = filteredProfiles;
//            int totalProfiles = filteredProfiles.size();
            int totalProfiles = filteredProfiles.size();
//            List<Profile> paginatedProfiles;
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

    private boolean contains(String field, String search) {
        return field != null && field.toLowerCase().contains(search);
    }

    private boolean containsDate(LocalDateTime date, String search) {
        return date != null && date.toString().toLowerCase().contains(search);
    }

    private boolean containsNested(Profile e, String search) {
        if (e.getMeter() == null || e.getMeter().getFlatNode() == null) return false;

        var fn = e.getMeter().getFlatNode();

        return contains(fn.getFeederName(), search)
                || contains(fn.getRegionName(), search)
                || contains(fn.getBusinessName(), search)
                || contains(fn.getServiceName(), search);
    }

    private boolean containsEventTypeName(Event e, String search) {
        return e.getEventType() != null
                && e.getEventTypeModel().getName() != null
                && e.getEventTypeModel().getName().toLowerCase().contains(search);
    }

    private boolean containsEventTypeDescription(Event e, String search) {
        return e.getEventType() != null
                && e.getEventTypeModel().getDescription() != null
                && e.getEventTypeModel().getDescription().toLowerCase().contains(search);
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> event(LocalDateTime startDate, LocalDateTime endDate, List<String> meterNumber, List<Long> eventTypeId, List<String> model, String search, int page, int size, String node) {
        try {
            UserModel um = handleUserValidation();
            List<Event> events;

            if(startDate == null || endDate == null) {
                events = new ArrayList<>();
            } else {
                events = hesMapper.getEvents(startDate, endDate, meterNumber, eventTypeId, model, page, size, um.getOrgId(), node);
            }

            // Normalize search text
            String searchLower = (search == null) ? "" : search.toLowerCase();

            // FILTER (flexible + safe)
            List<Event> filteredEvents = events.stream()
                    .filter(e -> {
                        if (searchLower.isEmpty()) return true;

                        return contains(e.getMeterNumber(), searchLower)
                                || contains(e.getEventName(), searchLower)
//                                || containsEventTypeName(e, searchLower)
                                || containsEventTypeDescription(e, searchLower)
                                || containsDate(e.getEventTime(), searchLower);
                    })
                    .collect(Collectors.toList());

            // Pagination logic
            List<Event> paginatedEvents = filteredEvents;
            int totalEvents = filteredEvents.size();
            int totalProfiles = filteredEvents.size();
//            List<Profile> paginatedProfiles;
            if (size == 0) {
                paginatedEvents = paginatedEvents;
            } else {
                int fromIndex = Math.min(page * size, totalProfiles);
                int toIndex = Math.min(fromIndex + size, totalProfiles);
                paginatedEvents = paginatedEvents.subList(fromIndex, toIndex);
            }

            // Prepare response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedEvents);
            response.put("totalData", totalEvents);
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

            // Get user node info
            String userNodeType = um.getNodeInfo() != null ? um.getNodeInfo().getType() : null;
            UUID userNodeId = um.getNodeInfo() != null ? um.getNodeInfo().getNodeId() : null;

            // Build node map first
            Map<UUID, Node> nodeMap = new HashMap<>();
            for (Node node : flatList) {
                nodeMap.put(node.getId(), node);
            }

            // If user is root, return full tree
            List<Node> filteredFlatList = flatList;
            if (userNodeType != null && !userNodeType.equalsIgnoreCase("root") && userNodeId != null) {
                // Find user's node
                Node userNode = nodeMap.get(userNodeId);
                if (userNode != null) {
                    // Get allowed node IDs (ancestors + user node + descendants)
                    Set<UUID> allowedIds = new HashSet<>();
                    
                    // Add user's node
                    allowedIds.add(userNodeId);
                    
                    // Add all ancestors
                    UUID parentId = userNode.getParentId();
                    while (parentId != null) {
                        allowedIds.add(parentId);
                        Node parent = nodeMap.get(parentId);
                        parentId = (parent != null) ? parent.getParentId() : null;
                    }
                    
                    // Add all descendants - check parentId in flat list
                    Set<UUID> descendantIds = getDescendantIds(userNodeId, flatList);
                    allowedIds.addAll(descendantIds);
                    
                    // Filter flat list to only include allowed nodes
                    filteredFlatList = flatList.stream()
                            .filter(n -> allowedIds.contains(n.getId()))
                            .collect(Collectors.toList());
                }
            }

            // Re-initialize and build tree from filtered list
            Map<UUID, Node> filteredNodeMap = new HashMap<>();
            List<Node> roots = new ArrayList<>();

            for (Node node : filteredFlatList) {
                filteredNodeMap.put(node.getId(), node);
                node.setNodesTree(new ArrayList<>());
            }

            for (Node node : filteredFlatList) {
                if (node.getParentId() == null) {
                    roots.add(node);
                } else {
                    Node parent = filteredNodeMap.get(node.getParentId());
                    if (parent != null) {
                        parent.getNodesTree().add(node);
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

    private Set<UUID> getDescendantIds(UUID nodeId, List<Node> flatList) {
        Set<UUID> descendantIds = new HashSet<>();
        Map<UUID, Node> nodeMap = new HashMap<>();
        for (Node node : flatList) {
            nodeMap.put(node.getId(), node);
        }
        collectDescendants(nodeId, descendantIds, nodeMap, flatList);
        return descendantIds;
    }

    private void collectDescendants(UUID nodeId, Set<UUID> descendantIds, Map<UUID, Node> nodeMap, List<Node> flatList) {
        for (Node node : flatList) {
            if (node.getParentId() != null && node.getParentId().equals(nodeId)) {
                descendantIds.add(node.getId());
                collectDescendants(node.getId(), descendantIds, nodeMap, flatList);
            }
        }
    }

    @Transactional(readOnly = true)
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

    @Transactional
    @Override
    public Map<String, Object> scheduleData(int page, int size, String search) {
        try {
            handleUserValidation();
            List<Schedule> meterConnEvent = hesMapper.getScheduleData(page, size);

            // Normalize search text
            String searchLower = (search == null) ? "" : search.toLowerCase();

            // SEARCH ON ANY FIELD
            List<Schedule> filteredComm = meterConnEvent.stream()
                    .filter(e -> searchLower.isEmpty() ||
                            (e.getDescription() != null && e.getDescription().toLowerCase().equalsIgnoreCase(searchLower)) ||
                            (e.getInterfaceName() != null && e.getInterfaceName().equalsIgnoreCase(searchLower)) ||
                            (e.getJobGroup() != null && e.getJobGroup().toLowerCase().equalsIgnoreCase(searchLower)) ||
                            (e.getJobName() != null && e.getJobName().toLowerCase().equalsIgnoreCase(searchLower)) ||
                            (e.getLastRunTime() != null && e.getLastRunTime().toLowerCase().equalsIgnoreCase(searchLower)) ||
                            (e.getName() != null && e.getName().toLowerCase().equalsIgnoreCase(searchLower))
                    )
                    .collect(Collectors.toList());

            // Pagination logic
            int totalComm = filteredComm.size();
            List<Schedule> paginatedEvents;
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

        } catch (Exception exception) {
            genericHandler.logIncidentReport("data schedule service failed");
            genericHandler.logAndSaveException(exception, "data schedule report");
            throw exception;
        }

    }

    @Transactional
    @Override
    public Map<String, Object> setSchedule(String jobGroup, String timeInterval, String unit, String jobName) {
        String token = auth.getAccessToken();

        try {
            handleUserValidation();
            Schedule response = hesMapper.getProfileEvent(jobName);
            if(response == null){
                throw new GlobalExceptionHandler.NotFoundException("Job Name "+status.getNotFoundDesc());
            }
            if(!response.getJobGroup().trim().equals(jobGroup.trim())){
                throw new GlobalExceptionHandler.NotFoundException("Job group is wrong");
            }

            Map<String, Object> resp = hesWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quartz/{jobGroup}/{jobName}/interval/{unit}")
                            .queryParam(unit, timeInterval)
                            .build(jobGroup, jobName, unit)
                    )
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,    // <-- use lambda here
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(body -> new RuntimeException("set schedule service error: " + body))
                    )
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), resp);

        } catch (WebClientResponseException e) {
            genericHandler.logIncidentReport("set schedule service failed");
            genericHandler.logAndSaveException(e, "set schedule service");
            throw e;

        } catch (Exception e) {
            genericHandler.logIncidentReport("set schedule service failed");
            genericHandler.logAndSaveException(e, "set schedule");
            throw e;
        }
    }

//    @Override
//    public Map<String, Object> profileEvents() {
//        try {
//            handleUserValidation();
//            List<Schedule> resp = hesMapper.getProfileEvents();
//
//            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), resp);
//        } catch (Exception e){
//            throw e;
//        }
//    }

    @Transactional
    @Override
    public Map<String, Object> setCron(String jobGroup, String jobName, String cronExpression) {
        String token = auth.getAccessToken();

        try {
            handleUserValidation();

            Schedule response = hesMapper.getProfileEvent(jobName.trim());
            if(response == null){
                throw new GlobalExceptionHandler.NotFoundException("Job Name "+status.getNotFoundDesc());
            }
            if(!response.getJobGroup().trim().equals(jobGroup.trim())){
                throw new GlobalExceptionHandler.NotFoundException("Job group is wrong");
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("jobGroup", response.getJobGroup());
            requestBody.put("jobName", response.getJobName());
            requestBody.put("cron", cronExpression);

            Map<String, Object> resp = hesWebClient.post()
                    .uri("/quartz/interval/cron")
                    .headers(h -> {
                        h.setBearerAuth(token);
                        h.setContentType(MediaType.APPLICATION_JSON);
                    })
                    .bodyValue(requestBody) // JSON body
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(
                                            new RuntimeException("set schedule service error: " + body)
                                    ))
                    )
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), resp);

        } catch (WebClientResponseException e) {
            genericHandler.logIncidentReport("set cron service failed");
            genericHandler.logAndSaveException(e, "set cron service");
            throw e;

        } catch (Exception e) {
            genericHandler.logIncidentReport("set cron service failed");
            genericHandler.logAndSaveException(e, "set cron");
            throw e;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> triggerEvent(String jobGroup, String jobName) {
        String token = auth.getAccessToken();

        try {
            handleUserValidation();
            Schedule response = hesMapper.getProfileEvent(jobName);
            if(response == null){
                throw new GlobalExceptionHandler.NotFoundException("Job Name "+status.getNotFoundDesc());
            }
            if(!response.getJobGroup().trim().equals(jobGroup.trim())){
                throw new GlobalExceptionHandler.NotFoundException("Job group is wrong");
            }

            Map<String, Object> resp = hesWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quartz/trigger/{jobGroup}/{jobName}")
                            .build(jobGroup, jobName)
                    )
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,    // <-- use lambda here
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(body -> new RuntimeException("set schedule service error: " + body))
                    )
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), resp);

        } catch (WebClientResponseException e) {
//            genericHandler.logIncidentReport("test schedule service failed");
//            genericHandler.logAndSaveException(e, "test schedule service");
            throw e;

        } catch (Exception e) {
//            genericHandler.logIncidentReport("set schedule service failed");
//            genericHandler.logAndSaveException(e, "set schedule");
            throw e;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> meterConfiguration(String meterNumber, String simNo, String model, String meterClass, String category,
                                           String businessHub, String manufacturer, String meterStatus, int page, int size) {
        try {
            UserModel um = handleUserValidation();

            UUID orgId = um.getOrgId();
            List<MeterConnEvent> meterConfig = hesMapper.getMeterConfiguration(page, size, orgId);

            List<MeterConnEvent> filteredComm = meterConfig.stream()
                    .filter(e -> (meterNumber == null || e.getMeterNo() != null && e.getMeterNo().equalsIgnoreCase(meterNumber)) &&
                            (simNo == null || e.getMeter() != null && e.getMeter().getSimNumber() != null && e.getMeter().getSimNumber().equalsIgnoreCase(simNo)) &&
                            (model == null || e.getMeter() != null && e.getMeter().getSmartMeterInfo() != null && e.getMeter().getSmartMeterInfo().getMeterModel() != null && e.getMeter().getSmartMeterInfo().getMeterModel().equalsIgnoreCase(model)) &&
                            (meterClass == null || e.getMeter() != null && e.getMeter().getMeterClass() != null && e.getMeter().getMeterClass().equalsIgnoreCase(meterClass)) &&
                            (category == null || e.getMeter() != null && e.getMeter().getMeterCategory() != null && e.getMeter().getMeterCategory().equalsIgnoreCase(category)) &&
                            (businessHub == null || e.getBusinessName() != null && e.getBusinessName().equalsIgnoreCase(businessHub)) &&
                            (manufacturer == null || e.getMeter() != null && e.getMeter().getMeterManufacturerName() != null && e.getMeter().getMeterManufacturerName().equalsIgnoreCase(manufacturer)) &&
                            (meterStatus == null || e.getMeter() != null && e.getMeter().getSmartStatus() != null && e.getMeter().getSmartStatus().toString().equalsIgnoreCase(meterStatus))
                    )
                    .collect(Collectors.toList());

            int totalComm = filteredComm.size();
            List<MeterConnEvent> paginatedEvents;
            if (size == 0) {
                paginatedEvents = filteredComm;
            } else {
                int fromIndex = Math.min(page * size, totalComm);
                int toIndex = Math.min(fromIndex + size, totalComm);
                paginatedEvents = filteredComm.subList(fromIndex, toIndex);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedEvents);
            response.put("totalData", totalComm);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", size > 0 ? (int) Math.ceil((double) totalComm / size) : 0);
            return ResponseMap.response(this.status.getSuccessCode(), "Fetched successfully", response);

        }catch (Exception e){
            genericHandler.logIncidentReport("meterConfiguration failed");
            genericHandler.logAndSaveException(e, "meterConfiguration");
            throw e;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> profileEventsInfo(String type) {
        try {
            handleUserValidation();

            if (type.equalsIgnoreCase("profile")) {
            List<Schedule> profiles = hesMapper.getProfileEvents();

            for (Schedule p : profiles) {
                p.setProfileType(mapProfileKey(p));
            }

            return ResponseMap.response(
                    status.getSuccessCode(),
                    status.getDesc(),
                    profiles
            );
        } else if (type.equalsIgnoreCase("event")) {

                Object resp = hesMapper.getEventType();

                return ResponseMap.response(
                        status.getSuccessCode(),
                        status.getDesc(),
                        resp
                );

            } else {
                return ResponseMap.response(
                        status.getFailCode(),
                        "Type not supported. Use 'profile' or 'event'",
                        null
                );
            }

        } catch (Exception e) {
            throw e;
        }
    }

    private String mapProfileKey(Schedule p) {

//        if (p.getObisCode() == null) return null;
        if (p.getJobName() == null) return null;
        System.out.println(">>>>:"+p.getJobName());

        switch (p.getJobName()) {

            case "Channel1Job"://"1.0.99.1.0.255":
                return "load-profile-one";
            case "Channel2Job"://"1.0.99.2.0.255":
                return "load-profile-two";
            case "DailyBillingJob": //"0.0.98.2.0.255":
                return "daily-billing-profile";
            case "MonthlyBillingJob"://"0.0.98.1.0.255":
                return "monthly-billing-profile";
            case "MonthlyBillingDataHouseHoldJob": //"0.0.99.1.0.255":
                return "monthly-billing-data-household";
            case "MonthlyBillingEnergyHouseHoldJob": //"0.0.98.1.0.255":
                return "monthly-billing-energy-household";
            case "DailyBillingDataHouseHoldJob": //"0.0.99.2.0.255":
                return "daily-billing-data-household";
            case "DailyBillingEnergyHouseHoldJob": //"0.0.98.2.0.255":
                return "daily-billing-energy-household";
            case "Channel1JobHouseHold": //"0.1.99.1.0.255":
                return "load-profile-one-household";
            case "Channel2JobHouseHold": //"0.2.24.3.0.255":
                return "load-profile-two-household";
            default:
                return null;
        }
    }


    @Transactional
    @Override
    public Map<String, Object> getOnlineMeter(String type) {
        try {
            UserModel um = handleUserValidation();
            UUID orgId = um.getOrgId();
            String type2 = "";
            if(!type.equalsIgnoreCase("md") && !type.equalsIgnoreCase("Non-md")){
                throw new GlobalExceptionHandler.NotFoundException("Type not supported. Use 'md' or 'Non-md'.");
            }
            if(type.equalsIgnoreCase("Non-md")){
                type = "THREE-PHASE";
                type2 = "SINGLE-PHASE";
            }
            List<MeterView> resp = hesMapper.getOnlineMeter(orgId, type.toUpperCase(), type2);

            return ResponseMap.response(
               status.getSuccessCode(),
               status.getDesc(),
               resp
            );

        } catch (Exception e) {
            throw e;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> getObisMappingData(String type) {
        try {
            UserModel um = handleUserValidation();

            if(!type.equalsIgnoreCase("md") && !type.equalsIgnoreCase("Non-md")){
                throw new GlobalExceptionHandler.NotFoundException("Type not supported. Use 'md' or 'Non-md'.");
            }

            List<ObisMappingData> obis = hesMapper.getObisMappingData(type);

            // GROUP BY groupName
            Map<String, List<ObisMappingData>> groupedData =
                    obis.stream()
                            .collect(Collectors.groupingBy(
                                    item -> formatGroupName(item.getGroupName()),
                                    LinkedHashMap::new,
                                    Collectors.toList()
                            ));

            return ResponseMap.response(
                    status.getSuccessCode(),
                    status.getDesc(),
                    groupedData
            );

        } catch (Exception e) {
            throw e;
        }
    }

    private String formatGroupName(String name) {

        String[] parts = name.trim().split("\\s+");

        StringBuilder formatted = new StringBuilder(parts[0]);

        for (int i = 1; i < parts.length; i++) {

            formatted.append(
                    parts[i].substring(0, 1).toUpperCase()
                            + parts[i].substring(1)
            );
        }

        return formatted.toString();
    }

}