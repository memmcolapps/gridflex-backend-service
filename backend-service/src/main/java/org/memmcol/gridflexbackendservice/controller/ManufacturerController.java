package org.memmcol.gridflexbackendservice.controller;

import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.service.manufacturer.ManufacturerService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler.SQLServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/manufacturer/service")
public class ManufacturerController {

    @Autowired
    private GlobalExceptionHandler exception;

    @Autowired private ManufacturerService service;

    @PostMapping("/create")
    public ResponseEntity<?> createManufacturer(@RequestBody Manufacturer request) {
        try {
            Map<String, Object> result = service.createManufacturer(request);
            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateManufacturer(@RequestBody Manufacturer request) {
        try {
            Map<String, Object> result = service.updateManufacturer(request);
            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

//    @PatchMapping("/change-state")
//    public ResponseEntity<?> createManufacturer(@RequestParam UUID id, @RequestParam Boolean status) {
//        try {
//            Map<String, Object> result = service.manageManufacturerState(id, status);
//            return ResponseEntity.ok(result);
//        } catch (SQLServerException e) {
//            return handleException(e);
//        }
//    }

    @GetMapping("/single")
    public ResponseEntity<?> getManufacturers(@RequestParam UUID id) {
        try {
            Map<String, Object> result = service.getManufacturer(id);
            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getManufacturers(
            @RequestParam(value = "name", required = false, defaultValue = "") String name,
            @RequestParam(value = "manufacturerId", required = false, defaultValue = "") String manufacturerId,
            @RequestParam(value = "contactPerson", required = false, defaultValue = "") String contactPerson,
//            @RequestParam(value = "state", required = false, defaultValue = "") String state,
            @RequestParam(value = "createdAt", required = false, defaultValue = "") String createdAt
    ) {
        try {
            Map<String, Object> result = service.getManufacturers(name, manufacturerId, contactPerson, createdAt);
            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }


    private ResponseEntity<Map<String, Object>> handleException(SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
