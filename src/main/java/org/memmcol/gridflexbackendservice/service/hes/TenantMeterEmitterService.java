package org.memmcol.gridflexbackendservice.service.hes;

import org.memmcol.gridflexbackendservice.mapper.MeterMapper;
import org.memmcol.gridflexbackendservice.model.hes.MeterStreamEvent;
import org.memmcol.gridflexbackendservice.model.hes.RealTimeReadRequest;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.memmcol.gridflexbackendservice.components.HandleValidUser.handleUserValidation;

@Service
public class TenantMeterEmitterService {

    @Qualifier("realtimeWebClient")
    @Autowired
    private WebClient webClient;

    @Autowired
    private MeterMapper meterMapper;

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

//    private final Map<String, List<SseEmitter>> tenantEmitters = new ConcurrentHashMap<>();

    // Track which orgs already received system event
    private final Set<String> systemEventDeliveredTo = ConcurrentHashMap.newKeySet();

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

    /**
     * OPTIONAL:
     * Clear cache manually if needed
     */
    public void clearMeterOrgCache() {

        meterOrgCache.clear();

        System.out.println("### Meter org cache cleared");
    }


    /**
     * OPTIONAL:
     * Remove single meter from cache
     */
    public void evictMeter(String meterNo) {

        meterOrgCache.remove(meterNo);

        System.out.println("### Removed meter from cache: "
                + meterNo);
    }

//    public SseEmitter subscribe() {
//        UserModel user = handleUserValidation();
//        String orgId = user.getOrgId().toString();
//
//        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
//        tenantEmitters.computeIfAbsent(orgId, k -> new ArrayList<>()).add(emitter);
//
//        emitter.onCompletion(() -> tenantEmitters.get(orgId).remove(emitter));
//        emitter.onTimeout(() -> tenantEmitters.get(orgId).remove(emitter));
//        emitter.onError((ex) -> tenantEmitters.get(user.getOrgId().toString()).remove(emitter));
//
//        // Send initial message ONLY IF system event was never sent to this org
//        if (!systemEventDeliveredTo.contains(orgId)) {
//            try {
//                emitter.send(SseEmitter.event()
//                        .id("SYSTEM")
//                        .name("meter-status")
//                        .data(new MeterStreamEvent("SYSTEM", LocalDateTime.now(), "CONNECTED"))); //
//                systemEventDeliveredTo.add(orgId); // mark delivered
//            } catch (IOException e) {
//                emitter.completeWithError(e);
//                System.out.println("exception: " + e.getMessage());
//            }
//        }
//
////        // Send initial OK payload
////        try {
////            emitter.send(SseEmitter.event()
////                    .name("subscribed")
////                    .data("Connected to meter stream"));
////        } catch (IOException ignored) {}
//
//        return emitter;
//    }
//
//    /** Forward meter-specific or system-generated events */
//    public void forwardMeterData(String orgId, MeterStreamEvent data) {
//        List<SseEmitter> emitters = tenantEmitters.get(orgId);
//        if (emitters == null) return;
//
//        for (SseEmitter emitter : emitters) {
//            try {
//                emitter.send(SseEmitter.event()
//                        .id(data.getMeterNo())
//                        .name("meter-data")
//                        .data(data));
//            } catch (IOException e) {
//                emitter.completeWithError(e);
//            }
//        }
//    }


//    /** SYSTEM event forwarded ONCE per org */
//    public void forwardSystemEvent(MeterStreamEvent data) {
//        for (String orgId : tenantEmitters.keySet()) {
//
//            // Skip if already delivered
//            if (systemEventDeliveredTo.contains(orgId)) continue;
//
//            forwardMeterData(orgId, data);
//
//            // Mark delivered
//            systemEventDeliveredTo.add(orgId);
//        }
//    }

//    /** Forward SYSTEM events to ALL subscribed orgs */
//    public void forwardSystemEvent(MeterStreamEvent data) {
//        for (String orgId : tenantEmitters.keySet()) {
//            forwardMeterData(orgId, data);
//        }
//    }

//    private final Map<String, List<SseEmitter>> tenantEmitters = new ConcurrentHashMap<>();
//
//    public SseEmitter subscribe() {
//        UserModel user = handleUserValidation();
//        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
//        tenantEmitters.computeIfAbsent(user.getOrgId().toString(), k -> new ArrayList<>()).add(emitter);
//
//        emitter.onCompletion(() -> tenantEmitters.get(user.getOrgId().toString()).remove(emitter));
//        emitter.onTimeout(() -> tenantEmitters.get(user.getOrgId().toString()).remove(emitter));
//        emitter.onError((ex) -> tenantEmitters.get(user.getOrgId().toString()).remove(emitter));
//
//        System.out.println("Subscribed to tenant: " + emitter);
//        return emitter;
//    }
//
//    public void forwardMeterData(UUID tenantId, MeterStreamEvent data) {
//        List<SseEmitter> emitters = tenantEmitters.get(tenantId.toString());
//        if (emitters != null) {
//            for (SseEmitter emitter : emitters) {
//                try {
//                    emitter.send(SseEmitter.event()
//                            .name("meter-update")
//                            .data(data));
//                } catch (IOException e) {
//                    emitter.completeWithError(e);
//                }
//            }
//        }
//    }

    public void forwardSystemEventToAllTenants(MeterStreamEvent event) {
        tenantEmitters.forEach((orgId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(
                            SseEmitter.event()
                                    .name("system-status")
                                    .data(event)
                    );
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }
        });
    }


//    /**
//     * Subscribe with explicit orgId (used by subscribeWithParams controller).
//     * Validates that requestor is same org.
//     */
//    public SseEmitter subscribeWithOrg(UUID requestedOrgId) {
//        UserModel user = handleUserValidation();
//        if (!user.getOrgId().equals(requestedOrgId)) {
//            throw new SecurityException("Tenant mismatch");
//        }
//
//        UUID orgIdStr = requestedOrgId;
//        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
//
//        tenantEmitters
//                .computeIfAbsent(orgIdStr.toString(), k -> new CopyOnWriteArrayList<>())
//                .add(emitter);
//
//        emitter.onCompletion(() -> tenantEmitters.get(orgIdStr.toString()).remove(emitter));
//        emitter.onTimeout(() -> tenantEmitters.get(orgIdStr.toString()).remove(emitter));
//        emitter.onError((ex) -> tenantEmitters.get(orgIdStr.toString()).remove(emitter));
//
//        return emitter;
//    }
}

