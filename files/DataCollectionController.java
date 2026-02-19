package com.hes.datacollection.controller;

import com.hes.datacollection.dto.ApiResponse;
import com.hes.datacollection.dto.PagedResponse;
import com.hes.datacollection.dto.SyncScheduleRequest;
import com.hes.datacollection.dto.SyncScheduleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.hes.datacollection.service.DataCollectionService;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/hes/data-collection/schedules")
@RequiredArgsConstructor
@Tag(name = "Data Collection Scheduler",
        description = "Set meter sync intervals and configure communication settings (HES/Controls)")
public class DataCollectionController {

    private final DataCollectionService service;

    // =========================================================================
    // CREATE — "Set Sync" button in the modal
    // POST /api/v1/hes/data-collection/schedules
    // =========================================================================

    @PostMapping
    @Operation(summary = "Create a new sync schedule",
            description = "Persists to scheduler_job_info and registers the job with Quartz")
    public ResponseEntity<ApiResponse<SyncScheduleResponse>> createSchedule(
            @Valid @RequestBody SyncScheduleRequest request) {

        SyncScheduleResponse response = service.createSchedule(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Sync schedule created and registered successfully"));
    }

    // =========================================================================
    // READ ALL — main table listing with search, filter, sort, pagination
    // GET /api/v1/hes/data-collection/schedules
    // =========================================================================

    @GetMapping
    @Operation(summary = "List all Data Collection schedules",
            description = "Supports the search bar, Filter button, Sort button, and Rows-per-page selector in the UI")
    public ResponseEntity<ApiResponse<PagedResponse<SyncScheduleResponse>>> getAllSchedules(

            @Parameter(description = "Search by event/profile type, description, or OBIS codes")
            @RequestParam(required = false) String search,

            @Parameter(description = "Filter by status: ACTIVE or PAUSED")
            @RequestParam(required = false) String status,

            @Parameter(description = "Filter by organisation ID")
            @RequestParam(required = false) UUID orgId,

            @Parameter(description = "Zero-based page index (default 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Rows per page — matches UI 'Rows per page' selector (default 10)")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Field to sort by")
            @RequestParam(defaultValue = "jobId") String sortBy,

            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "asc") String sortDir) {

        PagedResponse<SyncScheduleResponse> result =
                service.getAllSchedules(search, status, orgId, page, size, sortBy, sortDir);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // =========================================================================
    // READ ONE
    // GET /api/v1/hes/data-collection/schedules/{jobId}
    // =========================================================================

    @GetMapping("/{jobId}")
    @Operation(summary = "Get a single sync schedule by job ID")
    public ResponseEntity<ApiResponse<SyncScheduleResponse>> getScheduleById(
            @PathVariable Long jobId) {

        return ResponseEntity.ok(ApiResponse.success(service.getScheduleById(jobId)));
    }

    // =========================================================================
    // UPDATE — editing via the Set Sync modal
    // PUT /api/v1/hes/data-collection/schedules/{jobId}
    // =========================================================================

    @PutMapping("/{jobId}")
    @Operation(summary = "Update an existing sync schedule",
            description = "Updates scheduler_job_info and re-schedules the Quartz trigger")
    public ResponseEntity<ApiResponse<SyncScheduleResponse>> updateSchedule(
            @PathVariable Long jobId,
            @Valid @RequestBody SyncScheduleRequest request) {

        SyncScheduleResponse response = service.updateSchedule(jobId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Sync schedule updated successfully"));
    }

    // =========================================================================
    // TOGGLE STATUS — three-dot action menu "Pause" / "Resume" in the UI
    // PATCH /api/v1/hes/data-collection/schedules/{jobId}/toggle-status
    // =========================================================================

    @PatchMapping("/{jobId}/toggle-status")
    @Operation(summary = "Toggle schedule status between ACTIVE and PAUSED",
            description = "Updates job_status in scheduler_job_info and calls Quartz pauseJob/resumeJob")
    public ResponseEntity<ApiResponse<SyncScheduleResponse>> toggleStatus(
            @PathVariable Long jobId) {

        SyncScheduleResponse response = service.toggleStatus(jobId);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Schedule status changed to " + response.getStatus()));
    }

    // =========================================================================
    // BULK STATUS UPDATE
    // PATCH /api/v1/hes/data-collection/schedules/bulk/status
    // =========================================================================

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
    // DELETE — three-dot action menu "Delete" in the UI
    // DELETE /api/v1/hes/data-collection/schedules/{jobId}
    // =========================================================================

    @DeleteMapping("/{jobId}")
    @Operation(summary = "Delete a sync schedule",
            description = "Removes the record from scheduler_job_info and unregisters from Quartz")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(@PathVariable Long jobId) {
        service.deleteSchedule(jobId);
        return ResponseEntity.ok(ApiResponse.success(null, "Schedule deleted successfully"));
    }

    // =========================================================================
    // BULK DELETE
    // DELETE /api/v1/hes/data-collection/schedules/bulk
    // =========================================================================

    @DeleteMapping("/bulk")
    @Operation(summary = "Bulk delete sync schedules")
    public ResponseEntity<ApiResponse<Void>> bulkDelete(@RequestParam List<Long> jobIds) {
        service.bulkDeleteSchedules(jobIds);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Successfully deleted " + jobIds.size() + " schedules"));
    }
}
