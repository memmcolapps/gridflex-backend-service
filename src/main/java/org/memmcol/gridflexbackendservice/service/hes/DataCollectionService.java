//package org.memmcol.gridflexbackendservice.service.hes;
//
//import org.memmcol.gridflexbackendservice.model.hes.Schedule;
//import org.memmcol.gridflexbackendservice.model.hes.scheduler.dto.PagedResponse;
//import org.memmcol.gridflexbackendservice.model.hes.scheduler.dto.SyncScheduleRequest;
//import org.memmcol.gridflexbackendservice.model.hes.scheduler.dto.SyncScheduleResponse;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.UUID;
//
//public interface DataCollectionService {
//
//    /**
//     * Create a new sync schedule (Set Sync Schedule modal - "Set Sync" button).
//     */
//    SyncScheduleResponse createSchedule( SyncScheduleRequest request);
//
//    /**
//     * Update an existing sync schedule.
//     */
//    SyncScheduleResponse updateSchedule(Long id, SyncScheduleRequest request);
//
//    /**
//     * Get a single schedule by ID.
//     */
//    SyncScheduleResponse getScheduleById(Long id);
//
//    /**
//     * Get all schedules with pagination, optional search and status filter.
//     * Supports the search bar and filter/sort controls in the UI.
//     *
//     * @param search   free-text filter (meter no., account no., event type)
//     * @param status   optional status filter (ACTIVE / PAUSED)
//     * @param page     zero-based page index
//     * @param size     page size (default 10 per "Rows per page" selector in UI)
//     * @param sortBy   field name to sort by
//     * @param sortDir  "asc" or "desc"
//     */
//    PagedResponse<SyncScheduleResponse> getAllSchedules(
//            String search,
//            Schedule.ScheduleStatus status,
//            int page,
//            int size,
//            String sortBy,
//            String sortDir
//    );
//
//    @Transactional(readOnly = true)
//    PagedResponse<SyncScheduleResponse> getAllSchedules(
//            String search, String status, UUID orgId,
//            int page, int size, String sortBy, String sortDir );
//
//    /**
//     * Toggle status of a single schedule (Active ↔ Paused).
//     */
//    SyncScheduleResponse toggleStatus(Long id);
//
//    /**
//     * Bulk toggle status for multiple schedules.
//     */
//    List<SyncScheduleResponse> bulkToggleStatus(List<Long> ids, Schedule.ScheduleStatus targetStatus);
//
//    List<SyncScheduleResponse> bulkSetStatus( List<Long> jobIds, String targetStatus );
//
//    /**
//     * Delete a schedule by ID.
//     */
//    void deleteSchedule(Long id);
//
//    /**
//     * Bulk delete schedules.
//     */
//    void bulkDeleteSchedules(List<Long> ids);
//}
