package org.memmcol.gridflexbackendservice.service.hes;


import jakarta.annotation.PostConstruct;
import org.memmcol.gridflexbackendservice.mapper.MeterMapper;
import org.memmcol.gridflexbackendservice.mapper.UserMapper;
import org.memmcol.gridflexbackendservice.model.hes.MeterStreamEvent;
import org.memmcol.gridflexbackendservice.model.hes.RealTimeReadRequest;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.UUID;

import static org.memmcol.gridflexbackendservice.components.handleValidUser.handleUserValidation;

@Service
public class HesServiceConsumer {

    @Autowired
    private WebClient webClient;

    @Autowired
    private MeterMapper meterMapper;

    @Autowired
    private TenantMeterEmitterService emitterService;
    private UserMapper userMapper;
//    @Autowired
//    private org.memmcol.gridflexbackendservice.components.handleValidUser handleValidUser;


//    public HesServiceConsumer(TenantMeterEmitterService emitterService,
//                                   MeterMapper meterMapper) {
//        this.webClient = WebClient.create("http://existing-service-host:8080"); // change host
//        this.emitterService = emitterService;
//        this.meterMapper = meterMapper;
//    }

    @PostConstruct
    public void startDefaultStream() {
        System.out.println("### Starting consumption of external SSE stream...");
        Flux<MeterStreamEvent> flux = webClient.get()
                .uri("/meter-status/stream") // endpoint of existing service
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(MeterStreamEvent.class)
                .doOnError(e -> System.out.println("### SSE connection failed 1: " + e.getMessage()))
                .retry();  // <- This ensures auto reconnect;

        flux.subscribe(event -> {
            UUID orgId = null;
            try {
                System.out.println("Events: " + event.getMeterNo());
                System.out.println("Status: " + event.getStatus());
                System.out.println("Last Seen: " + event.getLastSeen());

                System.out.println("### Received event → " + event);

                // 1) Handle SYSTEM events (no DB lookup)
                if (isSystemEvent(event)) {
                    System.out.println("SYSTEM event received: forwarding to all active subscribers in their own org.");
                    emitterService.forwardSystemEvent(event);
                    return;
                }

                // 2) NORMAL meter data (tenant-isolated)
                orgId = meterMapper.findOrgIdByMeterId(event.getMeterNo());
            } catch (Exception ex) {
                System.out.println("Meter lookup failed for 1 " + event.getMeterNo() + ": " + ex.getMessage());
            }

            if (orgId != null) {
                emitterService.forwardMeterData(orgId.toString(), event);
            } else {
                // unknown meter: optionally log or ignore
                System.out.println("Meter lookup failed for 2 " + event.getMeterNo());
            }
        });
    }

    private boolean isSystemEvent(MeterStreamEvent e) {
        return e.getMeterNo() != null &&
                e.getMeterNo().equalsIgnoreCase("SYSTEM");
    }


    /**
     * Start a parameterized upstream stream for the given request and forward filtered events to tenant subscribers.
     *
     * This method doesn't return a Flux; instead it pushes events to emitterService directly.
     * Optionally you can keep a reference to the subscription for cancellation.
     */
    public void startParameterizedStream(RealTimeReadRequest req) {
        System.out.println("### Starting PARAMETERIZED stream for org " + req.getOrgId());

        UserModel user = handleUserValidation();
        req.setOrgId(user.getOrgId());

        // Build a POST request to upstream filtered endpoint (or GET with query params per your upstream API)
        Flux<MeterStreamEvent> flux = webClient.post()
                .uri("/stream") // upstream parameterized endpoint
                .bodyValue(req)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(MeterStreamEvent.class)
                .doOnError(e -> System.out.println("### Param SSE failed: " + e.getMessage()))
                .retry();

        // Subscribe and forward filtered events
        flux.subscribe(event -> {
            // If upstream events include orgId we can trust it; else lookup via meterMapper:
            if (req.getOrgId() != null) {
                // Enforce isolation: only forward if event belongs to same org
                UUID orgId = null;
                try {
                    orgId = meterMapper.findOrgIdByMeterId(event.getMeterNo());
                } catch (Exception ex) {
                    System.out.println("Meter lookup failed for 3 " + event.getMeterNo() + ": " + ex.getMessage());
                    throw new GlobalExceptionHandler.NotFoundException("Organization does not exist");
//                    System.out.println("Lookup failed in param stream for " + event.getMeterNo());
                }

                if (orgId != null && orgId.equals(req.getOrgId())) {
                    emitterService.forwardMeterData(orgId.toString(), event);
                }
            } else {
                // no orgId requested — forward or broadcast
                System.out.println("Meter lookup failed for 4 " + event.getMeterNo());
                throw new GlobalExceptionHandler.NotFoundException("Organization does not exist");
            }
        });
    }
}
