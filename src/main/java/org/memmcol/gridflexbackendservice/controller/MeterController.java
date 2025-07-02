package org.memmcol.gridflexbackendservice.controller;

import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.service.meter.MeterService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler.SQLServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/meter/service")
public class MeterController {
    @Autowired
    private MeterService service;

    @Autowired
    private GlobalExceptionHandler exception;

    @PostMapping("/create")
    public ResponseEntity<?> createMeter(@RequestBody Meter meter) {
        try {
            Map<String, Object> result = service.createMeter(meter);
            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

//    @PostMapping("/create/virtual")
//    public ResponseEntity<?> createVirtualMeter(@RequestBody Meter meter) {
//        try {
//            Map<String, Object> result = service.createVirtualMeter(meter);
//            return ResponseEntity.ok(result);
//        } catch (SQLServerException e) {
//            return handleException(e);
//        }
//    }

    @PutMapping("/update")
    public ResponseEntity<?> createUpdate(@RequestBody Meter meter) {
        try {
            Map<String, Object> result = service.updateMeter(meter);
            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/all-meters")
    public ResponseEntity<?> getAllMeters(
            @RequestParam(value = "page", required = false,  defaultValue = "0") int page,
            @RequestParam(value = "size", required = false,  defaultValue = "0") int size,
            @RequestParam(value = "meterNumber", required = false, defaultValue = "") String meterNumber,
            @RequestParam(value = "simNo", required = false, defaultValue = "") String simNo,
            @RequestParam(value = "manufacturer", required = false, defaultValue = "") String manufacturer,
            @RequestParam(value = "meterClass", required = false, defaultValue = "") String meterClass,
            @RequestParam(value = "category", required = false, defaultValue = "") String category,
            @RequestParam(value = "status", required = false, defaultValue = "") String status,
            @RequestParam(value = "createdAt", required = false, defaultValue = "") String createdAt
    ) {
        try {
            Map<String, Object> result = service.getAllMeters(page, size, meterNumber, simNo, manufacturer, meterClass, category, status, createdAt);
            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/single-meter")
    public ResponseEntity<?> getSingleMeter(
            @RequestParam(value = "meterId", required = false) UUID meterId,
            @RequestParam(value = "meterNumber", required = false) String meterNumber,
            @RequestParam(value = "accountNumber", required = false) String accountNumber

    ) {
        try {
            Map<String, Object> result = service.getSingleMeter(meterId, meterNumber, accountNumber);
            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/change-status")
    public ResponseEntity<Map<String, Object>> changeStatus(
            @RequestParam(required = true) UUID meterId,
            @RequestParam(value = "status", required = true) String status,
            @RequestParam(value = "reason", required = true) String reason
            ) {
        try {
            Map<String, Object> result =  service.changeStatus(meterId, status, reason);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        } catch (MissingServletRequestParameterException e) {
            throw new RuntimeException(e);
        }
    }

//    @PatchMapping("/bulk-approve")
//    public ResponseEntity<?> bulkApproveMeter(@RequestBody BulkApproveMeter request) {
//        try {
//            Map<String, Object> result = service.bulkApproveMeter(request);
//            return ResponseEntity.ok(result);
//        } catch (GlobalExceptionHandler.SQLServerException e) {
//            return handleException(e);
//        }
//    }

    @GetMapping("/manufacturers")
    public ResponseEntity<Map<String, Object>> getManufacturers() {

        try {
            Map<String, Object> result =  service.getManufacturers();

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/assign-meter")
    public ResponseEntity<Map<String, Object>> AssignMeter(
            @RequestParam UUID cId,
            @RequestParam UUID meterId,
            @RequestParam String customerId,
            @RequestParam String accountNumber,
            @RequestParam String tariff,
            @RequestParam String feederLine,
            @RequestParam String transformer,
            @RequestParam String substation) {
        try {
            Map<String, Object> result =  service.assignMeterToCustomer(accountNumber, tariff, customerId, meterId, cId, feederLine, transformer, substation);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/single-customer")
    public ResponseEntity<?> singleCustomer(
            @RequestParam(value = "customerId", required = true) String customerId
    ) {
        try {
            Map<String, Object> result = service.singleCustomer(customerId);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/allocate-meter")
    public ResponseEntity<?> allocatedMeter(
            @RequestParam(value = "meterId", required = true) UUID meterId,
            @RequestParam(value = "nodeId", required = true) UUID nodeId
    ) {
        try {
            Map<String, Object> result = service.allocateMeter(meterId, nodeId);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
