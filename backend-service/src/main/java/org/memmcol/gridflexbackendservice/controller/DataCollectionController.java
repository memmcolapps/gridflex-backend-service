package org.memmcol.gridflexbackendservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.memmcol.gridflexbackendservice.model.hes.scheduler.dto.ApiResponse;
import org.memmcol.gridflexbackendservice.model.hes.scheduler.dto.PagedResponse;
import org.memmcol.gridflexbackendservice.model.hes.scheduler.dto.SyncScheduleRequest;
import org.memmcol.gridflexbackendservice.model.hes.scheduler.dto.SyncScheduleResponse;
import org.memmcol.gridflexbackendservice.service.hes.DataCollectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

/**
 * REST API for the HES Data Collection Scheduler UI.
 *
 * This controller provides CRUD operations over scheduler_job_info.
 * The actual job execution is handled by the AMI Core service.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/hes/data-collection/schedules")
@RequiredArgsConstructor
@Tag(name = "Data Collection Scheduler",
        description = "Manage meter sync intervals and communication schedules (HES/Controls)")
public class DataCollectionController {

    private final DataCollectionService service;

    // =========================================================================
    // CREATE
    // =========================================================================

    @PostMapping
    @Operation(summary = "Create a new sync schedule",
            description = "Inserts a row into scheduler_job_info. AMI Core will pick it up and schedule the Quartz job.")
    public ResponseEntity<ApiResponse<SyncScheduleResponse>> createSchedule(
            @Valid @RequestBody SyncScheduleRequest request) {

        SyncScheduleResponse response = service.createSchedule(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Sync schedule created successfully"));
    }

    // =========================================================================
    // READ
    // =========================================================================

    @GetMapping
    @Operation(summary = "List all Data Collection schedules with search, filter, and pagination")
    public ResponseEntity<ApiResponse<PagedResponse<SyncScheduleResponse>>> getAllSchedules(

            @Parameter(description = "Search by event/profile type, description, or OBIS codes")
            @RequestParam(required = false) String search,

            @Parameter(description = "Filter by status: ACTIVE or PAUSED")
            @RequestParam(required = false) String status,

            @Parameter(description = "Filter by organisation ID")
            @RequestParam(required = false) UUID orgId,

            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Rows per page")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Field to sort by")
            @RequestParam(defaultValue = "jobId") String sortBy,

            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "asc") String sortDir) {

        PagedResponse<SyncScheduleResponse> result =
                service.getAllSchedules(search, status, orgId, page, size, sortBy, sortDir);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get a single sync schedule by job ID")
    public ResponseEntity<ApiResponse<SyncScheduleResponse>> getScheduleById(
            @PathVariable Long jobId) {

        return ResponseEntity.ok(ApiResponse.success(service.getScheduleById(jobId)));
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    @PutMapping("/{jobId}")
    @Operation(summary = "Update an existing sync schedule",
            description = "Updates scheduler_job_info. AMI Core will detect the change and refresh the Quartz trigger.")
    public ResponseEntity<ApiResponse<SyncScheduleResponse>> updateSchedule(
            @PathVariable Long jobId,
            @Valid @RequestBody SyncScheduleRequest request) {

        SyncScheduleResponse response = service.updateSchedule(jobId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Sync schedule updated successfully"));
    }

    // =========================================================================
    // STATUS TOGGLE
    // =========================================================================

    @PatchMapping("/{jobId}/toggle-status")
    @Operation(summary = "Toggle schedule status between ACTIVE and PAUSED",
            description = "Updates job_status in scheduler_job_info. AMI Core will pause/resume the job accordingly.")
    public ResponseEntity<ApiResponse<SyncScheduleResponse>> toggleStatus(
            @PathVariable Long jobId) {

        SyncScheduleResponse response = service.toggleStatus(jobId);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Schedule status changed to " + response.getStatus()));
    }

    @PatchMapping("/bulk/status")
    @Operation(summary = "Bulk update status for multiple schedules")
    public ResponseEntity<ApiResponse<List<SyncScheduleResponse>>> bulkUpdateStatus(
            @RequestParam List<Long> jobIds,
            @Parameter(description = "Target status: ACTIVE or PAUSED")
            @RequestParam String status) {

        List<SyncScheduleResponse> responses = service.bulkSetStatus(jobIds, status);
        return ResponseEntity.ok(ApiResponse.success(responses,
                "Bulk status updated to " + status + " for " + responses.size() + " schedules"));
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    @DeleteMapping("/{jobId}")
    @Operation(summary = "Delete a sync schedule",
            description = "Removes the row from scheduler_job_info. AMI Core will unregister the Quartz job.")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule( @PathVariable Long jobId) {
        service.deleteSchedule(jobId);
        return ResponseEntity.ok(ApiResponse.success(null, "Schedule deleted successfully"));
    }

    @DeleteMapping("/bulk")
    @Operation(summary = "Bulk delete sync schedules")
    public ResponseEntity<ApiResponse<Void>> bulkDelete(@RequestParam List<Long> jobIds) {
        service.bulkDeleteSchedules(jobIds);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Successfully deleted " + jobIds.size() + " schedules"));
    }
}