package org.memmcol.gridflexbackendservice.controller;

import org.memmcol.gridflexbackendservice.model.Band;
import org.memmcol.gridflexbackendservice.model.Tariff;
import org.memmcol.gridflexbackendservice.service.band.BandService;
import org.memmcol.gridflexbackendservice.service.tariff.TariffService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/tariff/service")
public class TariffController {

    @Autowired
    private TariffService service;

    @Autowired
    private GlobalExceptionHandler exception;

    @PostMapping("/create")
    public ResponseEntity<?> createTariff(@RequestBody Tariff tariff) {
        try {
            Map<String, Object> result = service.createTariff(tariff);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }


    @GetMapping("/all-tariff")
    public ResponseEntity<?> getAllTariff(
            @RequestParam(required = true) int page,
            @RequestParam(required = true) int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
            ) {
        try {
            Map<String, Object> result = service.getTariffs(page, size, startDate, endDate);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/filter-tariff")
    public ResponseEntity<?> filterTariff(
            @RequestParam(required = true) int page,
            @RequestParam(required = true) int size,
            @RequestParam(required = true) String filter
    ) {
        try {
            Map<String, Object> result = service.getFilterTariffs(filter, page, size);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/filter/unique-id")
    public ResponseEntity<?> UniqueTariffId() {
        try {
            Map<String, Object> result = service.getUniqueTariffId();
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/change-state")
    public ResponseEntity<?> disableTariff(@RequestParam Long tariffId, @RequestParam Boolean status) {
        try {
            Map<String, Object> result = service.disableTariff(tariffId, status);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
