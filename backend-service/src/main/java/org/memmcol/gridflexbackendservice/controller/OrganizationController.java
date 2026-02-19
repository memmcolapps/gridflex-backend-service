package org.memmcol.gridflexbackendservice.controller;


import org.memmcol.gridflexbackendservice.model.user.Organization;
import org.memmcol.gridflexbackendservice.service.organization.OrganizationService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/organization/service")
public class OrganizationController {

    @Autowired
    private final OrganizationService organizationService;

    @Autowired
    private GlobalExceptionHandler exception;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PatchMapping("/update")
    public ResponseEntity<Map<String, Object>> updateOrganization(@RequestBody Organization organization) {
        try {
            Map<String, Object> result = organizationService.updateOrganization(organization);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/single")
    public ResponseEntity<Map<String, Object>> getOrganizationById(@RequestParam UUID id) {
        try {
            Map<String, Object> result = organizationService.getOrganizationById(id);
            return ResponseEntity.ok(result);
        }catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}


//    @PostMapping("/create")
//    public ResponseEntity<Map<String, Object>> createOrganization(@RequestBody Organization organization) {
//        try {
//            Map<String, Object> result = organizationService.addOrganization(organization);
//            return ResponseEntity.ok(result);
//        }catch (GlobalExceptionHandler.SQLServerException e) {
//            return handleException(e);
//        }
//    }


//    @GetMapping("/all")
//    public ResponseEntity<Map<String, Object>> getOrganization(
//            @RequestParam(value = "page", required = false,  defaultValue = "0") int page,
//            @RequestParam(value = "size", required = false,  defaultValue = "0") int size) {
//        try {
//            Map<String, Object> result = organizationService.getOrganization(page,size);
//            return ResponseEntity.ok(result);
//        }catch (GlobalExceptionHandler.SQLServerException e) {
//            return handleException(e);
//        }
//    }