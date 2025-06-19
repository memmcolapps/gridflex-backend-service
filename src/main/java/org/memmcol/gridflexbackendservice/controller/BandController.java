package org.memmcol.gridflexbackendservice.controller;

import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.service.band.BandService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/band/service")
public class BandController {

    @Autowired
    private BandService service;

    @Autowired
    private GlobalExceptionHandler exception;

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

    @GetMapping("/all-band")
    public ResponseEntity<?> getAllBands(
            @RequestParam(value = "type", required = false, defaultValue = "") String type
    ) {
        try {
            Map<String, Object> result = service.getBands(type);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/single-band")
    public ResponseEntity<?> getSingleBand(@RequestParam UUID bandId) {
        try {
            Map<String, Object> result = service.getBand(bandId);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }


    @PatchMapping("/approve")
    public ResponseEntity<?> manageBandState(
            @RequestParam UUID bandId,
            @RequestParam String approveStatus) {
        try {
            Map<String, Object> result = service.manageBandState(bandId, approveStatus);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        } catch (MissingServletRequestParameterException e) {
            throw new RuntimeException(e);
        }
    }


    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
