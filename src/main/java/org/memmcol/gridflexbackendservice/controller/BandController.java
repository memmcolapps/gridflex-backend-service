package org.memmcol.gridflexbackendservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.memmcol.gridflexbackendservice.model.band.Band;
import org.memmcol.gridflexbackendservice.service.band.BandService;
import org.memmcol.gridflexbackendservice.exception.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.List;
@RestController
@RequestMapping("/band/service")
@Tag(name = "Band", description = "Band Management APIs")
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

    @GetMapping("/all")
    public ResponseEntity<?> getAllBands(
            @RequestParam(value = "type", required = false, defaultValue = "") String type,
            @RequestParam(value = "search", required = false, defaultValue = "") String search,
            @RequestParam(value = "sort", required = false, defaultValue = "") String sort
    ) {
        try {
            Map<String, Object> result = service.getBands(type, search, sort);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/single")
    public ResponseEntity<?> getSingleBand(
            @RequestParam(value = "bandId", required = false) UUID bandId,
            @RequestParam(value = "bandVersionId", required = false) UUID bandVersionId) {
        try {
            Map<String, Object> result = service.getBand(bandId, bandVersionId);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }


    @PutMapping("/approve")
    public ResponseEntity<?> approve(
            @RequestParam UUID bandId,
            @RequestParam String approveStatus) {
        try {
            Map<String, Object> result = service.approve(bandId, approveStatus);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        } catch (MissingServletRequestParameterException e) {
            throw new RuntimeException(e);
        }
    }

    @PutMapping("/bulk-approve")
    public ResponseEntity<?> bulkApprove(
            @RequestBody List<Band> band) {
        try {
            Map<String, Object> result = service.bulkApprove(band);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/change-state")
    public ResponseEntity<?> changeState(
            @RequestParam UUID bandId,
            @RequestParam Boolean status) {
        try {
            Map<String, Object> result = service.changeStatus(bandId, status);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/clear-cache")
    public ResponseEntity<?> clearCache() {
        try {
            Map<String, Object> result = service.clearCache();
            return ResponseEntity.ok(result);

        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }


    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
