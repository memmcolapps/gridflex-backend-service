//package org.memmcol.gridflexbackendservice.controller;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.memmcol.gridflexbackendservice.model.hes.scheduler.dto.PagedResponse;
//import org.memmcol.gridflexbackendservice.model.hes.scheduler.dto.SyncScheduleRequest;
//import org.memmcol.gridflexbackendservice.model.hes.scheduler.dto.SyncScheduleResponse;
//import org.memmcol.gridflexbackendservice.service.hes.DataCollectionService;
//import org.memmcol.gridflexbackendservice.util.ResponseMap;
//import org.memmcol.gridflexbackendservice.util.StatusConstants;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//
///**
// * REST API for the HES Data Collection Scheduler UI.
// *
// * This controller provides CRUD operations over scheduler_job_info.
// * The actual job execution is handled by the AMI Core service.
// */
//@Slf4j
//@RestController
//@RequestMapping("/data-collection/schedules")
//@RequiredArgsConstructor
//@Tag(name = "Data Collection Scheduler",
//        description = "Manage meter sync intervals and communication schedules (HES/Controls)")
//public class DataCollectionController {
//
//    private final DataCollectionService service;
//    private final StatusConstants status;
//
//    // =========================================================================
//    // CREATE
//    // =========================================================================
//
//    @PostMapping("/create")
//    @Operation(summary = "Create a new sync schedule")
//    public Map<String, Object> createSchedule(@Valid @RequestBody SyncScheduleRequest request) {
//        try {
//            log.info("Creating data collection schedule: {}", request.getEventProfileType());
//
//            SyncScheduleResponse response = service.createSchedule(request);
//
//            return ResponseMap.response(
//                    status.getRegCode(),
//                    "Data Collection Schedule " + status.getRegDesc(),
//                    response
//            );
//        } catch (Exception exception) {
//            log.error("Error occurred while creating schedule: {}", exception.getMessage(), exception);
//            throw exception;
//        }
//    }
//
//    // =========================================================================
//    // READ
//    // =========================================================================
//
//    @GetMapping("/all/schedules")
//    @Operation(summary = "List all Data Collection schedules with search, filter, and pagination")
//    public Map<String, Object> getAllSchedules(
//
//            @Parameter(description = "Search by event/profile type, description, or OBIS codes")
//            @RequestParam(required = false) String search,
//
//            @Parameter(description = "Filter by status: ACTIVE or PAUSED")
//            @RequestParam(required = false) String status,
//
//            @Parameter(description = "Filter by organisation ID")
//            @RequestParam(required = false) UUID orgId,
//
//            @Parameter(description = "Zero-based page index")
//            @RequestParam(defaultValue = "0") int page,
//
//            @Parameter(description = "Rows per page")
//            @RequestParam(defaultValue = "10") int size,
//
//            @Parameter(description = "Field to sort by")
//            @RequestParam(defaultValue = "jobId") String sortBy,
//
//            @Parameter(description = "Sort direction: asc or desc")
//            @RequestParam(defaultValue = "asc") String sortDir) {
//
//        try {
//            log.info("Fetching all schedules - page: {}, size: {}, search: {}", page, size, search);
//
//            PagedResponse<SyncScheduleResponse> result =
//                    service.getAllSchedules(search, status, orgId, page, size, sortBy, sortDir);
//
//            return ResponseMap.response(
//                    this.status.getGetCode(),
//                    "Data Collection Schedules " + this.status.getGetDesc(),
//                    result
//            );
//        } catch (Exception exception) {
//            log.error("Error occurred while fetching schedules: {}", exception.getMessage(), exception);
//            throw exception;
//        }
//    }
//
//    @GetMapping("/{jobId}")
//    @Operation(summary = "Get a single sync schedule by job ID")
//    public Map<String, Object> getScheduleById(@PathVariable Long jobId) {
//        try {
//            log.info("Fetching schedule by jobId: {}", jobId);
//
//            SyncScheduleResponse response = service.getScheduleById(jobId);
//
//            return ResponseMap.response(
//                    status.getGetCode(),
//                    "Data Collection Schedule " + status.getGetDesc(),
//                    response
//            );
//        } catch (Exception exception) {
//            log.error("Error occurred while fetching schedule {}: {}", jobId, exception.getMessage(), exception);
//            throw exception;
//        }
//    }
//
//    // =========================================================================
//    // UPDATE
//    // =========================================================================
//
//    @PutMapping("/update/{jobId}")
//    @Operation(summary = "Update an existing sync schedule")
//    public Map<String, Object> updateSchedule(
//            @PathVariable Long jobId,
//            @Valid @RequestBody SyncScheduleRequest request) {
//
//        try {
//            log.info("Updating schedule jobId: {}", jobId);
//
//            SyncScheduleResponse response = service.updateSchedule(jobId, request);
//
//            return ResponseMap.response(
//                    status.getUpdateCode(),
//                    "Data Collection Schedule " + status.getUpdateDesc(),
//                    response
//            );
//        } catch (Exception exception) {
//            log.error("Error occurred while updating schedule {}: {}", jobId, exception.getMessage(), exception);
//            throw exception;
//        }
//    }
//
//    // =========================================================================
//    // STATUS TOGGLE
//    // =========================================================================
//
//    @PatchMapping("/{jobId}/toggle-status")
//    @Operation(summary = "Toggle schedule status between ACTIVE and PAUSED")
//    public Map<String, Object> toggleStatus(@PathVariable Long jobId) {
//        try {
//            log.info("Toggling status for schedule jobId: {}", jobId);
//
//            SyncScheduleResponse response = service.toggleStatus(jobId);
//
//            return ResponseMap.response(
//                    status.getUpdateCode(),
//                    "Data Collection Schedule Status " + status.getUpdateDesc(),
//                    response
//            );
//        } catch (Exception exception) {
//            log.error("Error occurred while toggling status for schedule {}: {}", jobId, exception.getMessage(), exception);
//            throw exception;
//        }
//    }
//
//    @PatchMapping("/bulk/status")
//    @Operation(summary = "Bulk update status for multiple schedules")
//    public Map<String, Object> bulkUpdateStatus(
//            @RequestParam List<Long> jobIds,
//            @Parameter(description = "Target status: ACTIVE or PAUSED")
//            @RequestParam String status) {
//
//        try {
//            log.info("Bulk updating status for {} schedules to: {}", jobIds.size(), status);
//
//            List<SyncScheduleResponse> responses = service.bulkSetStatus(jobIds, status);
//
//            return ResponseMap.response(
//                    this.status.getUpdateCode(),
//                    "Bulk Status " + this.status.getUpdateDesc() + " for " + responses.size() + " schedules",
//                    responses
//            );
//        } catch (Exception exception) {
//            log.error("Error occurred during bulk status update: {}", exception.getMessage(), exception);
//            throw exception;
//        }
//    }
//
//    // =========================================================================
//    // DELETE
//    // =========================================================================
//
//    @DeleteMapping("/delete/{jobId}")
//    @Operation(summary = "Delete a sync schedule")
//    public Map<String, Object> deleteSchedule(@PathVariable Long jobId) {
//        try {
//            log.info("Deleting schedule jobId: {}", jobId);
//
//            service.deleteSchedule(jobId);
//
//            return ResponseMap.response(
//                    status.getDelCode(),
//                    "Data Collection Schedule " + status.getDelDesc(),
//                    null
//            );
//        } catch (Exception exception) {
//            log.error("Error occurred while deleting schedule {}: {}", jobId, exception.getMessage(), exception);
//            throw exception;
//        }
//    }
//
//    @DeleteMapping("/delete/bulk")
//    @Operation(summary = "Bulk delete sync schedules")
//    public Map<String, Object> bulkDelete(@RequestParam List<Long> jobIds) {
//        try {
//            log.info("Bulk deleting {} schedules", jobIds.size());
//
//            service.bulkDeleteSchedules(jobIds);
//
//            return ResponseMap.response(
//                    status.getDelCode(),
//                    jobIds.size() + " Data Collection Schedules " + status.getDelDesc(),
//                    null
//            );
//        } catch (Exception exception) {
//            log.error("Error occurred during bulk delete: {}", exception.getMessage(), exception);
//            throw exception;
//        }
//    }
//}