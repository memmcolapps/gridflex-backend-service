package org.memmcol.gridflexbackendservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.ibatis.annotations.Update;
import org.memmcol.gridflexbackendservice.model.hes.Cron;
import org.memmcol.gridflexbackendservice.model.hes.RealTimeReadRequest;
import org.memmcol.gridflexbackendservice.model.hes.Schedule;
import org.memmcol.gridflexbackendservice.service.hes.*;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/hes/service")
public class HesController {

    @Autowired
    private GlobalExceptionHandler exception;

    @Autowired
    private HesService hesService;

    @Autowired
    private DlmsService dlmsService;

    @Autowired
    private TenantMeterEmitterService emitterService;

    @Autowired
    HesServiceConsumer hesServiceConsumer;

    @GetMapping("/communication/report")
    public ResponseEntity<?> report(
            @RequestParam(value = "page", required = false,  defaultValue = "0") int page,
            @RequestParam(value = "size", required = false,  defaultValue = "0") int size,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "node", required = false) String node
    ) {
        try {
            Map<String, Object> result = hesService.communicationReport(page, size, type, search, node);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/communication/range/report")
    public ResponseEntity<?> communicationRangeReport(
            @RequestParam(value = "page", required = false,  defaultValue = "0") int page,
            @RequestParam(value = "size", required = false,  defaultValue = "0") int size,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate,
            @RequestParam(value = "meterNumber", required = false) List<String> meterNumber,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "node", required = false) String node
    ) {
        try {
            Map<String, Object> result = hesService.communicationRangeReport(page, size, startDate, endDate, type,search, meterNumber, node);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile(
            @RequestParam(value = "page", required = false,  defaultValue = "0") int page,
            @RequestParam(value = "size", required = false,  defaultValue = "0") int size,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate,
            @RequestParam(value = "meterNumber", required = false) List<String> meterNumber,
            @RequestParam(value = "profile", required = false) String profile,
            @RequestParam(value = "model", required = false) List<String> model,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "node") String node
    ) {
        try {
            Map<String, Object> result = hesService.profile(startDate, endDate, meterNumber, profile, model, page, size, search, node);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/event")
    public ResponseEntity<?> event(
            @RequestParam(value = "page", required = false,  defaultValue = "0") int page,
            @RequestParam(value = "size", required = false,  defaultValue = "0") int size,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate,
            @RequestParam(value = "meterNumber", required = false) List<String> meterNumber,
            @RequestParam(value = "eventTypeId", required = false) List<Long> eventTypeId,
            @RequestParam(value = "model", required = false) List<String> model,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "node") String node
    ) {
        try {
            Map<String, Object> result = hesService.event(startDate, endDate, meterNumber, eventTypeId, model, search, page, size, node);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/model")
    public ResponseEntity<?> modelEventType() {
        try {
            Map<String, Object> result = hesService.modelEventType();
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/data/schedule")
    public ResponseEntity<?> report(
            @RequestParam(value = "page", required = false,  defaultValue = "0") int page,
            @RequestParam(value = "size", required = false,  defaultValue = "0") int size,
            @RequestParam(value = "search", required = false) String search
    ) {
        try {
            Map<String, Object> result = hesService.scheduleData(page, size, search);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/meter-status/stream")
    public SseEmitter stream() {
        // In production, validate tenantId from JWT/session
        return emitterService.streamMeterStatus();
    }

    /**
     * Parameterized SSE endpoint: the user wants a filtered stream.
     * The controller will:
     *  - validate the caller's tenant (inside subscribeWithOrg)
     *  - connect the subscriber SSE emitter to the tenant registry
     *  - start the upstream filtered stream (on demand) so events become available
     *
     * POST /meter-stream/subscribe-params
     * Body: FilterStreamRequest JSON
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeWithParams(
            @RequestBody RealTimeReadRequest req,
            HttpServletRequest httpRequest) {
        // Create a tenant-scoped emitter (validate tenant inside)
//        SseEmitter emitter = emitterService.subscribe();

        // Start the parameterized upstream stream; it will forward to emitterService for this org
//        hesServiceConsumer.startParameterizedStream(req);

        return hesServiceConsumer.startParameterizedStream(req);
    }

    @PostMapping("/set/schedule")
    public ResponseEntity<?> setSchedule(
            @RequestParam String jobGroup,
            @RequestParam String jobName,
            @RequestParam String timeInterval,
            @RequestParam String unit
    ) {
        try {
            Map<String, Object> result = hesService.setSchedule(jobGroup, timeInterval, unit, jobName);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/set/cron")
    public ResponseEntity<?> setCron(
            @RequestBody Cron schedule) {
        try {
            String cron = CronBuilder.buildCron(schedule);
            Map<String, Object> result = hesService.setCron(
                    schedule.getJobGroup(), schedule.getJobName(), cron);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

//    @GetMapping("/profile-events")
//    public ResponseEntity<?> profileEvents() {
//        try {
//            Map<String, Object> result = hesService.profileEvents();
//            return ResponseEntity.ok(result);
//        } catch (GlobalExceptionHandler.SQLServerException e) {
//            return handleException(e);
//        }
//    }

    //---------- Test schedule Api-----------
    @PostMapping("/trigger")
    public ResponseEntity<?> triggerEvent(
            @RequestParam String jobGroup,
            @RequestParam String jobName
    ) {
        try {
            Map<String, Object> result = hesService.triggerEvent(jobGroup, jobName);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/dlms/set-clock")
    public ResponseEntity<?> setClock(
            @RequestParam String serial,
            @RequestParam
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime dateTime
    ) {
        try {
            Map<String, Object> result = dlmsService.setClock(serial, dateTime);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/dlms/set-ctpt")
    public ResponseEntity<?> setCtpt(
            @RequestParam String serial,
            @RequestParam long ctNumerator,
            @RequestParam long ctDenominator,
            @RequestParam long ptNumerator,
            @RequestParam long ptDenominator
    ) {
        try {
            Map<String, Object> result = dlmsService.setCtpt(serial, ctNumerator, ctDenominator, ptNumerator, ptDenominator);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/dlms/set-apn")
    public ResponseEntity<?> setApn(
            @RequestParam String serial,
            @RequestParam String apn
    ) {
        try {
            Map<String, Object> result = dlmsService.setApn(serial, apn);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/dlms/set-ip-port")
    public ResponseEntity<?> setIpPort(
            @RequestParam String serial,
            @RequestParam String ip,
            @RequestParam int port
    ) {
        try {
            Map<String, Object> result = dlmsService.setIpPort(serial, ip, port);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }


    @GetMapping("/meter-configuration")
    public ResponseEntity<?> meterConfiguration(
            @RequestParam(required = false) String meterNumber,
            @RequestParam(required = false) String simNo,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String meterClass,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String businessHub,
            @RequestParam(required = false) String manufacturer,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "0") int size) {
        try {
            Map<String, Object> result = hesService.meterConfiguration(
                    meterNumber, simNo, model, meterClass, category,
                    businessHub, manufacturer, status, page, size);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/profile-event-name")
    public ResponseEntity<?> profileEventsInfo(@RequestParam String type) {
        try {
            Map<String, Object> result = hesService.profileEventsInfo(type);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/dlms/read-meter")
    public ResponseEntity<?> readMeter(
            @RequestParam String serial,
            @RequestParam String type
    ) {
        try {
            Map<String, Object> result = dlmsService.readMeter(serial, type);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

//    /**
//     * Optional endpoint: start a parameterized stream without subscribing (e.g., start background forwarding).
//     */
//    @PostMapping("/start-param-only")
//    public String startParamOnly(@RequestBody RealTimeReadRequest req) {
//        hesServiceConsumer.startParameterizedStream(req);
//        return "started";
//    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
