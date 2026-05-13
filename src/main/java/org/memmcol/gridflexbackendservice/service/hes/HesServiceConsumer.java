package org.memmcol.gridflexbackendservice.service.hes;


import jakarta.annotation.PostConstruct;
import org.memmcol.gridflexbackendservice.mapper.MeterMapper;
import org.memmcol.gridflexbackendservice.mapper.UserMapper;
import org.memmcol.gridflexbackendservice.model.hes.MeterStreamEvent;
import org.memmcol.gridflexbackendservice.model.hes.RealTimeReadRequest;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.memmcol.gridflexbackendservice.components.HandleValidUser.handleUserValidation;

@Service
public class HesServiceConsumer {

    @Qualifier("realtimeWebClient")
    @Autowired
    private WebClient webClient;

    @Autowired
    private MeterMapper meterMapper;

    @Autowired
    private HesAuthServiceImpl auth;

    /**
     * Tenant emitters
     */
    private final Map<String, ArrayList<SseEmitter>> tenantEmitters =
            new ConcurrentHashMap<>();

    /**
     * CACHE:
     * meterNo -> orgId
     */
    private final Map<String, UUID> meterOrgCache =
            new ConcurrentHashMap<>();

    public SseEmitter streamMeterStatus() {

        UserModel user = handleUserValidation();
        UUID orgId = user.getOrgId();
        String orgKey = orgId.toString();
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        tenantEmitters.computeIfAbsent(orgKey, k -> new ArrayList<>()).add(emitter);

        emitter.onCompletion(() -> {
            tenantEmitters.get(orgKey).remove(emitter);
            System.out.println("### Client disconnected");
        });

        emitter.onTimeout(() -> {
            tenantEmitters.get(orgKey).remove(emitter);
            System.out.println("### SSE timeout");
            emitter.complete();
        });
        emitter.onError((ex) -> {
            tenantEmitters.get(user.getOrgId().toString()).remove(emitter);
            System.out.println("### SSE emitter error: "+ ex.getMessage());
        });

        /**
         * Consume downstream SSE
         */
        Flux<ServerSentEvent<MeterStreamEvent>> flux = webClient.get()
                .uri("/meter-status/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(
                        new ParameterizedTypeReference<ServerSentEvent<MeterStreamEvent>>() {}
                )
                .retryWhen(
                        Retry.fixedDelay(3, Duration.ofSeconds(2))
                )
                .doOnSubscribe(sub ->
                        System.out.println("### Connected to downstream SSE"))
                .doOnError(error -> {
                    System.out.println("### Downstream SSE ERROR: "
                            + error.getMessage());

                    emitter.completeWithError(error);
                })
                .doOnComplete(() -> {
                    System.out.println("### Downstream SSE completed");
                    emitter.complete();
                });

        /**
         * Subscribe to downstream events
         */
        flux.subscribe(event -> {

            try {

                MeterStreamEvent data = event.data();

                if (data == null) {
                    return;
                }
                String meterNo = data.getMeterNo();

                System.out.println("### RECEIVED EVENT: " + data.getMeterNo());

                /**
                 * CACHE LOOKUP
                 *
                 * If meterNo exists in cache:
                 *     use cached orgId
                 *
                 * Else:
                 *     query DB once
                 *     store in cache
                 */
                UUID meterOrgId = meterOrgCache.computeIfAbsent(
                        meterNo,
                        key -> meterMapper.findOrgIdByMeterId(key)
                );

                /**
                 * Skip meters not belonging to organization
                 */
                if (meterOrgId == null ||
                        !meterOrgId.equals(orgId)) {

                    System.out.println("### SKIPPED EVENT FOR METER: "
                            + data.getMeterNo());

                    return;
                }

                assert event.id() != null;
                assert event.event() != null;
                /**
                 * SEND ONLY ORGANIZATION EVENTS
                 */
                emitter.send(
                        SseEmitter.event()
                                .id(event.id())
                                .name(event.event())
                                .data(data)
                );
//                }

            } catch (Exception ex) {

                System.out.println("### EMITTER ERROR: "+ ex.getMessage());

                emitter.completeWithError(ex);
            }

        });

        emitter.onCompletion(() ->
                System.out.println("### Client disconnected"));

        emitter.onTimeout(() -> {
            System.out.println("### SSE timeout");
            emitter.complete();
        });

        emitter.onError(error -> {
            System.out.println("### SSE emitter error: "
                    + error.getMessage());
        });

        return emitter;
    }

    public SseEmitter startParameterizedStream(RealTimeReadRequest req) {

        UserModel user = handleUserValidation();
        UUID orgId = user.getOrgId();
        req.setOrgId(orgId);

        // Validate ownership ONCE up-front instead of hitting the DB per event.
        // Any meter not owned by this org is stripped from the upstream request.
        List<String> requestedMeters = req.getMeters() == null ? List.of() : req.getMeters();
        Set<String> allowedMeters = new HashSet<>(requestedMeters.size());
        for (String meter : requestedMeters) {
            UUID owner = meterOrgCache.computeIfAbsent(meter, meterMapper::findOrgIdByMeterId);
            if (owner != null && owner.equals(orgId)) {
                allowedMeters.add(meter);
            }
        }
        if (allowedMeters.isEmpty()) {
            throw new RuntimeException("No requested meters belong to this organization");
        }
        req.setMeters(new ArrayList<>(allowedMeters));

        System.out.println("### Starting PARAMETERIZED stream for org " + orgId
                + " meters=" + allowedMeters.size());

        // Finite timeout so a dead stream eventually closes the client connection.
        SseEmitter emitter = new SseEmitter(Duration.ofSeconds(140).toMillis());

        // Use ServerSentEvent<String> so event names ("reading"/"heartbeat"/"warning"/"completed")
        // and raw JSON payloads pass through verbatim — without forcing each event payload into
        // MeterStreamEvent, which only matches "reading".
        Flux<ServerSentEvent<String>> flux = webClient.post()
                .uri("/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {});

        flux.subscribe(
                sse -> {
                    try {
                        String eventName = sse.event() == null ? "message" : sse.event();
                        String data = sse.data();
                        if (data == null) {
                            return;
                        }
                        emitter.send(SseEmitter.event()
                                .name(eventName)
                                .data(data, MediaType.APPLICATION_JSON));
                    } catch (Exception ex) {
                        System.out.println("### Forward error: " + ex.getMessage());
                        emitter.completeWithError(ex);
                    }
                },
                err -> {
                    System.out.println("### Param SSE failed: " + err.getMessage());
                    emitter.completeWithError(err);
                },
                () -> {
                    System.out.println("### Param SSE completed");
                    emitter.complete();
                }
        );

        return emitter;
    }

}


//    @Autowired
//    private org.memmcol.gridflexbackendservice.components.HandleValidUser handleValidUser;


//    public HesServiceConsumer(TenantMeterEmitterService emitterService,
//                                   MeterMapper meterMapper) {
//        this.webClient = WebClient.create("http://existing-service-host:8080"); // change host
//        this.emitterService = emitterService;
//        this.meterMapper = meterMapper;
//    }

//    @PostConstruct
//    public void startDefaultStream() {
//        System.out.println("### Starting consumption of external SSE stream...");
//        Flux<MeterStreamEvent> flux = webClient.get()
//                .uri("/meter-status/stream") // endpoint of existing service
//                .accept(MediaType.TEXT_EVENT_STREAM)
//                .retrieve()
//                .bodyToFlux(MeterStreamEvent.class)
//                .doOnError(e -> System.out.println("### SSE connection failed 1: " + e.getMessage()))
//                .retry();  // <- This ensures auto reconnect;
//
//        flux.subscribe(event -> {
//            UUID orgId = null;
//            try {
//                System.out.println("Events: " + event.getMeterNo());
//                System.out.println("Status: " + event.getStatus());
//                System.out.println("Last Seen: " + event.getLastSeen());
//
//                System.out.println("### Received event → " + event);
//
//                // 1) Handle SYSTEM events (no DB lookup)
//                if (isSystemEvent(event)) {
//                    System.out.println("SYSTEM event received: forwarding to all active subscribers in their own org.");
//                    emitterService.forwardSystemEvent(event);
//                    return;
//                }
//
//                // 2) NORMAL meter data (tenant-isolated)
//                orgId = meterMapper.findOrgIdByMeterId(event.getMeterNo());
//            } catch (Exception ex) {
//                System.out.println("Meter lookup failed for 1 " + event.getMeterNo() + ": " + ex.getMessage());
//            }
//
//            if (orgId != null) {
//                emitterService.forwardMeterData(orgId.toString(), event);
//            } else {
//                // unknown meter: optionally log or ignore
//                System.out.println("Meter lookup failed for 2 " + event.getMeterNo());
//            }
//        });
//    }
//
//    private boolean isSystemEvent(MeterStreamEvent e) {
//        return e.getMeterNo() != null &&
//                e.getMeterNo().equalsIgnoreCase("SYSTEM");
//    }


/**
 * Start a parameterized upstream stream for the given request and forward filtered events to tenant subscribers.
 *
 * This method doesn't return a Flux; instead it pushes events to emitterService directly.
 * Optionally you can keep a reference to the subscription for cancellation.
 */

//    public SseEmitter startParameterizedStream(RealTimeReadRequest req, String token) {
//
//        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
//
//        System.out.println("token-: " + token);
//
//        Flux<MeterStreamEvent> flux = webClient.post()
//                .uri("/stream")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.TEXT_EVENT_STREAM) // ✅ FIX
////                .accept(MediaType.TEXT_EVENT_STREAM)
////                .headers(headers -> {
////                    if (token != null) {
////                        headers.set("Authorization", token); // optional
////                    }
////                })
//                .bodyValue(req)
//                .retrieve()
//                .bodyToFlux(MeterStreamEvent.class)
//                .doOnError(error -> {
//                    System.out.println("❌ SSE Error: " + error.getMessage());
//                    emitter.completeWithError(error);
//                });
//
//        flux.subscribe(event -> {
//            try {
//                emitter.send(SseEmitter.event().data(event));
//            } catch (Exception e) {
//                emitter.completeWithError(e);
//            }
//        });
//
//        return emitter;
//    }




///------------------------------------------
//    public SseEmitter startParameterizedStream(RealTimeReadRequest req) {
//        System.out.println("### Starting PARAMETERIZED stream for org " + req.getOrgId());
////        String token = auth.getAccessToken();
//        UserModel user = handleUserValidation();
//        req.setOrgId(user.getOrgId());
//
//        // Build a POST request to upstream filtered endpoint (or GET with query params per your upstream API)
//        Flux<MeterStreamEvent> flux = webClient.post()
//                .uri("/stream") // upstream parameterized endpoint
////                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
//                .bodyValue(req)
//                .accept(MediaType.TEXT_EVENT_STREAM)
//                .retrieve()
//                .bodyToFlux(MeterStreamEvent.class)
//                .doOnError(e -> System.out.println("### Param SSE failed: " + e.getMessage()))
//                .retry();
//
//        // Subscribe and forward filtered events
//        flux.subscribe(event -> {
//            // If upstream events include orgId we can trust it; else lookup via meterMapper:
//            if (req.getOrgId() != null) {
//                // Enforce isolation: only forward if event belongs to same org
//                UUID orgId = null;
//                try {
//                    orgId = meterMapper.findOrgIdByMeterId(event.getMeterNo());
//                } catch (Exception ex) {
//                    System.out.println("Meter lookup failed for 3 " + event.getMeterNo() + ": " + ex.getMessage());
//                    throw new GlobalExceptionHandler.NotFoundException("Organization does not exist");
////                    System.out.println("Lookup failed in param stream for " + event.getMeterNo());
//                }
//
//                if (orgId != null && orgId.equals(req.getOrgId())) {
//                    emitterService.forwardMeterData(orgId.toString(), event);
//                }
//            } else {
//                // no orgId requested — forward or broadcast
//                System.out.println("Meter lookup failed for 4 " + event.getMeterNo());
//                throw new GlobalExceptionHandler.NotFoundException("Organization does not exist");
//            }
//        });
//    }
