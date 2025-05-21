package org.memmcol.gridflexbackendservice.controller;

import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.memmcol.gridflexbackendservice.service.customer.CustomerService;
import org.memmcol.gridflexbackendservice.service.meter.MeterService;
import org.memmcol.gridflexbackendservice.service.tariff.TariffService;
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
    public ResponseEntity<?> getAllMeters(@RequestParam(required = true) UUID orgId) {
        try {
            Map<String, Object> result = service.getAllMeters(orgId);
            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/single-meters")
    public ResponseEntity<?> getAllMeters(@RequestParam(required = true) UUID orgId, @RequestParam(required = true) UUID meterId) {
        try {
            Map<String, Object> result = service.getSingleMeter(orgId, meterId);
            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }


    @GetMapping("/all-feeder-lines")
    public ResponseEntity<Map<String, Object>> fetchAllFeederLines(@RequestParam(required = true) UUID orgId) {

        try {
            Map<String, Object> result =  service.fetchAllFeederLines(orgId);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }
    @GetMapping("/all-transformers")
    public ResponseEntity<Map<String, Object>> fetchAllTransformers(@RequestParam(required = true) UUID orgId) {

        try {
            Map<String, Object> result =  service.fetchAllTransformers(orgId);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/all-substations")
    public ResponseEntity<Map<String, Object>> fetchAllSubstations(@RequestParam(required = true) UUID orgId) {

        try {
            Map<String, Object> result =  service.fetchAllSubstations(orgId);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/change-status")
    public ResponseEntity<Map<String, Object>> changeStatus(
            @RequestParam(required = true) UUID orgId,
            @RequestParam(required = true) UUID meterId,
            @RequestParam(required = true) Boolean status,
            @RequestParam (value = "approveStatus", required = false) String approveStatus
            ) {

        try {
            Map<String, Object> result =  service.changeStatus(orgId, meterId, status, approveStatus);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        } catch (MissingServletRequestParameterException e) {
            throw new RuntimeException(e);
        }
    }





    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
