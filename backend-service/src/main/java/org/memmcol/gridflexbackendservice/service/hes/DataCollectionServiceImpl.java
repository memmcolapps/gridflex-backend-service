package org.memmcol.gridflexbackendservice.service.hes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.memmcol.gridflexbackendservice.mapper.DataCollectionMapper;
import org.memmcol.gridflexbackendservice.model.hes.Schedule;
import org.memmcol.gridflexbackendservice.model.hes.scheduler.SchedulerJobInfo;
import org.memmcol.gridflexbackendservice.model.hes.scheduler.dto.PagedResponse;
import org.memmcol.gridflexbackendservice.model.hes.scheduler.dto.SyncScheduleRequest;
import org.memmcol.gridflexbackendservice.model.hes.scheduler.dto.SyncScheduleResponse;
import org.memmcol.gridflexbackendservice.model.hes.scheduler.exception.DuplicateScheduleException;
import org.memmcol.gridflexbackendservice.model.hes.scheduler.exception.ResourceNotFoundException;
import org.memmcol.gridflexbackendservice.repository.SchedulerJobInfoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Data Collection Scheduler service implementation.
 *
 * ARCHITECTURE NOTE:
 * This service does NOT manage Quartz directly. We only perform CRUD operations
 * on scheduler_job_info. The AMI Core service (which has Quartz embedded) polls
 * this table and automatically:
 *   - Schedules new jobs when rows are inserted
 *   - Updates triggers when rows are updated
 *   - Pauses jobs when job_status = 'PAUSED'
 *   - Removes jobs when rows are deleted
 *
 * This separation keeps the HES UI layer stateless and lets AMI Core own
 * all DLMS/COSEM communication and Quartz lifecycle management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DataCollectionServiceImpl implements DataCollectionService {

    private final SchedulerJobInfoRepository repository;
    private final DataCollectionMapper mapper;

    // =========================================================================
    // CREATE
    // =========================================================================

    @Override
    public SyncScheduleResponse createSchedule( SyncScheduleRequest request) {
        log.info("Creating data collection schedule: '{}'", request.getEventProfileType());

        // Guard: no duplicate name within same org
        if (repository.existsDuplicateName(request.getEventProfileType(), request.getOrgId(), -1L)) {
            throw new DuplicateScheduleException(
                    request.getEventProfileType(),
                    "A schedule with this Event/Profile Type already exists.");
        }

        SchedulerJobInfo entity = mapper.toEntity(request);
        entity.setUpdatedAt(LocalDateTime.now());
        SchedulerJobInfo saved = repository.save(entity);

        log.info("Created schedule jobId={} — AMI Core will pick up this job automatically",
                saved.getJobId());
        return mapper.toResponse(saved);
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    @Override
    public SyncScheduleResponse updateSchedule(Long jobId, SyncScheduleRequest request) {
        log.info("Updating data collection schedule jobId={}", jobId);

        SchedulerJobInfo entity = findOrThrow(jobId);

        // Guard: no name clash with another record
        if (repository.existsDuplicateName(request.getEventProfileType(), request.getOrgId(), jobId)) {
            throw new DuplicateScheduleException(
                    request.getEventProfileType(),
                    "Another schedule already uses this Event/Profile Type.");
        }

        mapper.updateEntity(entity, request);
        entity.setUpdatedAt(LocalDateTime.now());
        SchedulerJobInfo updated = repository.save(entity);

        log.info("Updated schedule jobId={} — AMI Core will refresh the trigger", jobId);
        return mapper.toResponse(updated);
    }

    // =========================================================================
    // READ
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public SyncScheduleResponse getScheduleById(Long jobId) {
        return mapper.toResponse(findOrThrow(jobId));
    }

    @Override
    public PagedResponse<SyncScheduleResponse> getAllSchedules( String search, Schedule.ScheduleStatus status, int page, int size, String sortBy, String sortDir ) {
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<SyncScheduleResponse> getAllSchedules(
            String search, String status, UUID orgId,
            int page, int size, String sortBy, String sortDir) {

        String dbStatus = mapToDbStatus(status);

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(resolveSort(sortBy)).descending()
                : Sort.by(resolveSort(sortBy)).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<SchedulerJobInfo> resultPage = repository.findDataCollectionSchedules(
                blankToNull(search), dbStatus, orgId, pageable);

        return PagedResponse.<SyncScheduleResponse>builder()
                .content(mapper.toResponseList(resultPage.getContent()))
                .page(resultPage.getNumber())
                .size(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .first(resultPage.isFirst())
                .last(resultPage.isLast())
                .build();
    }

    // =========================================================================
    // STATUS TOGGLE (pause / resume)
    // =========================================================================

    @Override
    public SyncScheduleResponse toggleStatus(Long jobId) {
        SchedulerJobInfo entity = findOrThrow(jobId);
        boolean isActive = SchedulerJobInfo.STATUS_SCHEDULED.equals(entity.getJobStatus());

        String newStatus = isActive
                ? SchedulerJobInfo.STATUS_PAUSED
                : SchedulerJobInfo.STATUS_SCHEDULED;

        entity.setJobStatus(newStatus);
        entity.setUpdatedAt(LocalDateTime.now());
        SchedulerJobInfo saved = repository.save(entity);

        log.info("Toggled schedule jobId={} to status '{}' — AMI Core will {}",
                jobId, newStatus, isActive ? "pause" : "resume");

        return mapper.toResponse(saved);
    }

    @Override
    public List<SyncScheduleResponse> bulkToggleStatus( List<Long> ids, Schedule.ScheduleStatus targetStatus ) {
        return List.of();
    }

    @Override
    public List<SyncScheduleResponse> bulkSetStatus(List<Long> jobIds, String targetStatus) {
        log.info("Bulk setting {} schedules to status '{}'", jobIds.size(), targetStatus);

        String dbStatus = "ACTIVE".equalsIgnoreCase(targetStatus) ||
                SchedulerJobInfo.STATUS_SCHEDULED.equalsIgnoreCase(targetStatus)
                ? SchedulerJobInfo.STATUS_SCHEDULED
                : SchedulerJobInfo.STATUS_PAUSED;

        repository.bulkUpdateStatus(dbStatus, jobIds);
        List<SchedulerJobInfo> updated = repository.findAllById(jobIds);

        log.info("Bulk update complete — AMI Core will adjust {} jobs accordingly", updated.size());
        return mapper.toResponseList(updated);
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    @Override
    public void deleteSchedule(Long jobId) {
        SchedulerJobInfo entity = findOrThrow(jobId);
        repository.delete(entity);
        log.info("Deleted schedule jobId={} — AMI Core will unregister this job", jobId);
    }

    @Override
    public void bulkDeleteSchedules(List<Long> jobIds) {
        log.info("Bulk deleting {} schedules", jobIds.size());
        List<SchedulerJobInfo> entities = repository.findAllById(jobIds);
        repository.deleteAll(entities);
        log.info("Bulk delete complete — AMI Core will clean up");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private SchedulerJobInfo findOrThrow(Long jobId) {
        return repository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", jobId));
    }

    /** UI: "ACTIVE" → DB: "SCHEDULED" */
    private String mapToDbStatus(String uiStatus) {
        if (uiStatus == null) return null;
        return "ACTIVE".equalsIgnoreCase(uiStatus)
                ? SchedulerJobInfo.STATUS_SCHEDULED
                : uiStatus.toUpperCase();
    }

    private String resolveSort(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "eventprofiletype", "name" -> "name";
            case "status" -> "jobStatus";
            case "lastruntime" -> "lastRunTime";
            case "updatedat" -> "updatedAt";
            default -> "jobId";
        };
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}