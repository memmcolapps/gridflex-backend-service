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
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
            Map<String, Object> result = service.createUpdate(meter);
            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }


    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
