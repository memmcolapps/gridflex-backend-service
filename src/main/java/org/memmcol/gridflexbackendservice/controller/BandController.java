package org.memmcol.gridflexbackendservice.controller;

import org.memmcol.gridflexbackendservice.model.Band;
import org.memmcol.gridflexbackendservice.service.band.BandService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/band/service")
public class BandController {

    @Autowired
    private BandService service;

    @Autowired private ResponseProperties status;

    @Autowired private GlobalExceptionHandler exception;

    @PostMapping("/create")
    public ResponseEntity<?> createBand(@RequestBody Band band) {
        try {
            Map<String, Object> result = service.createBand(band);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }

    }

    @PutMapping("/update")
    public ResponseEntity<?> updateBand(@RequestBody Band band) {
        try {
            Map<String, Object> result = service.updateBand(band);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllBands() {
        try {
            Map<String, Object> result = service.getBands();
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }


    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
