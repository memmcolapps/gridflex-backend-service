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


//    @GetMapping("/all-tariff")
//    public ResponseEntity<?> getAllTariff(
//            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
//            @RequestParam(value = "size", required = false, defaultValue = "0") int size
//            ) {
//        try {
//            Map<String, Object> result = service.getTariffs(page, size);
//            return ResponseEntity.ok(result);
//        } catch (GlobalExceptionHandler.SQLServerException e) {
//            return handleException(e);
//        }
//    }

    @GetMapping("/filter-tariff")
    public ResponseEntity<?> filterTariff(
            @RequestParam(value = "tariffName", required = false, defaultValue = "") String tariffName,
            @RequestParam(value = "tariffIndex", required = false, defaultValue = "") String tariffIndex,
            @RequestParam(value = "tariffType", required = false, defaultValue = "") String tariffType,
            @RequestParam(value = "tariffRate", required = false, defaultValue = "") String tariffRate,
            @RequestParam(value = "bandCode", required = false, defaultValue = "") String bandCode,
            @RequestParam(value = "status", required = false, defaultValue = "") Boolean status,
            @RequestParam(value = "effectiveDate", required = false, defaultValue = "") String effectiveDate,
            @RequestParam(value = "approveStatus", required = false, defaultValue = "") String approveStatus
    ) {
        try {
            Map<String, Object> result = service.getFilterTariffs(tariffName, tariffIndex, tariffType, tariffRate, bandCode, status, effectiveDate, approveStatus);
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
    public ResponseEntity<?> manageTariffStatus(
            @RequestParam (value = "tariffId", required = true) Long tariffId,
            @RequestParam (value = "status", required = false) Boolean status,
            @RequestParam (value = "approveStatus", required = false) String approveStatus) {
        try {
            Map<String, Object> result = service.manageTariffStatus(tariffId, status, approveStatus);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
