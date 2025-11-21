package org.memmcol.gridflexbackendservice.service.hes;

import org.memmcol.gridflexbackendservice.model.hes.MeterStreamEvent;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.memmcol.gridflexbackendservice.components.handleValidUser.handleUserValidation;

@Service
public class TenantMeterEmitterService {

    private final Map<String, List<SseEmitter>> tenantEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe() {
        UserModel user = handleUserValidation();
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        tenantEmitters.computeIfAbsent(user.getOrgId().toString(), k -> new ArrayList<>()).add(emitter);

        emitter.onCompletion(() -> tenantEmitters.get(user.getOrgId().toString()).remove(emitter));
        emitter.onTimeout(() -> tenantEmitters.get(user.getOrgId().toString()).remove(emitter));
        emitter.onError((ex) -> tenantEmitters.get(user.getOrgId().toString()).remove(emitter));

        return emitter;
    }

    public void forwardMeterData(UUID tenantId, MeterStreamEvent data) {
        List<SseEmitter> emitters = tenantEmitters.get(tenantId.toString());
        if (emitters != null) {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("meter-update")
                            .data(data));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }
        }
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

