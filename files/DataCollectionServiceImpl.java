package com.hes.datacollection.service.impl;

import com.hes.datacollection.dto.PagedResponse;
import com.hes.datacollection.dto.SyncScheduleRequest;
import com.hes.datacollection.dto.SyncScheduleResponse;
import com.hes.datacollection.exception.DuplicateScheduleException;
import com.hes.datacollection.exception.ResourceNotFoundException;
import com.hes.datacollection.mapper.DataCollectionMapper;
import com.hes.datacollection.model.SchedulerJobInfo;
import com.hes.datacollection.quartz.QuartzJobManager;
import com.hes.datacollection.repository.SchedulerJobInfoRepository;
import com.hes.datacollection.service.DataCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DataCollectionServiceImpl implements DataCollectionService {

    private final SchedulerJobInfoRepository repository;
    private final DataCollectionMapper       mapper;
    private final QuartzJobManager           quartzManager;

    // =========================================================================
    // CREATE — "Set Sync" button in modal
    // =========================================================================

    @Override
    public SyncScheduleResponse createSchedule(SyncScheduleRequest request) {
        log.info("Creating data collection schedule: '{}'", request.getEventProfileType());

        // Guard: no duplicate name within same org
        if (repository.existsByNameInOrgExcluding(
                request.getEventProfileType(), request.getOrgId(), -1L)) {
            throw new DuplicateScheduleException(request.getEventProfileType(),
                    "A schedule with this Event/Profile Type already exists.");
        }

        // 1. Persist to scheduler_job_info
        SchedulerJobInfo entity = mapper.toEntity(request);
        entity.setUpdatedAt(LocalDateTime.now());
        SchedulerJobInfo saved = repository.save(entity);

        // 2. Register with Quartz
        quartzManager.scheduleOrUpdate(saved);

        log.info("Created schedule jobId={} jobName='{}'", saved.getJobId(), saved.getJobName());
        return mapper.toResponse(saved);
    }

    // =========================================================================
    // UPDATE — editing an existing schedule via the modal
    // =========================================================================

    @Override
    public SyncScheduleResponse updateSchedule(Long jobId, SyncScheduleRequest request) {
        log.info("Updating data collection schedule jobId={}", jobId);

        SchedulerJobInfo entity = findOrThrow(jobId);

        // Guard: no name clash with another record
        if (repository.existsByNameInOrgExcluding(
                request.getEventProfileType(), request.getOrgId(), jobId)) {
            throw new DuplicateScheduleException(request.getEventProfileType(),
                    "Another schedule already uses this Event/Profile Type.");
        }

        // 1. Update entity fields
        mapper.updateEntity(entity, request);
        entity.setUpdatedAt(LocalDateTime.now());
        SchedulerJobInfo updated = repository.save(entity);

        // 2. Re-register with Quartz (replaces the old trigger)
        quartzManager.scheduleOrUpdate(updated);

        log.info("Updated schedule jobId={}", jobId);
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
    @Transactional(readOnly = true)
    public PagedResponse<SyncScheduleResponse> getAllSchedules(
            String search, String status, UUID orgId,
            int page, int size, String sortBy, String sortDir) {

        // Map UI status ("ACTIVE"/"PAUSED") → DB value ("SCHEDULED"/"PAUSED")
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
    // STATUS TOGGLE — three-dot action menu in the UI table
    // =========================================================================

    @Override
    public SyncScheduleResponse toggleStatus(Long jobId) {
        SchedulerJobInfo entity = findOrThrow(jobId);
        boolean isCurrentlyActive = SchedulerJobInfo.STATUS_SCHEDULED.equals(entity.getJobStatus());

        if (isCurrentlyActive) {
            entity.setJobStatus(SchedulerJobInfo.STATUS_PAUSED);
            entity.setUpdatedAt(LocalDateTime.now());
            repository.save(entity);
            quartzManager.pauseJob(entity);
            log.info("Paused schedule jobId={}", jobId);
        } else {
            entity.setJobStatus(SchedulerJobInfo.STATUS_SCHEDULED);
            entity.setUpdatedAt(LocalDateTime.now());
            repository.save(entity);
            quartzManager.resumeJob(entity);
            log.info("Resumed schedule jobId={}", jobId);
        }

        return mapper.toResponse(entity);
    }

    @Override
    public List<SyncScheduleResponse> bulkSetStatus(List<Long> jobIds, String targetStatus) {
        log.info("Bulk setting {} schedules to status '{}'", jobIds.size(), targetStatus);

        String dbStatus = SchedulerJobInfo.STATUS_SCHEDULED.equalsIgnoreCase(targetStatus)
                || "ACTIVE".equalsIgnoreCase(targetStatus)
                ? SchedulerJobInfo.STATUS_SCHEDULED
                : SchedulerJobInfo.STATUS_PAUSED;

        repository.bulkUpdateStatus(dbStatus, jobIds);

        List<SchedulerJobInfo> updated = repository.findAllById(jobIds);
        updated.forEach(job -> {
            if (SchedulerJobInfo.STATUS_SCHEDULED.equals(dbStatus)) {
                quartzManager.resumeJob(job);
            } else {
                quartzManager.pauseJob(job);
            }
        });

        return mapper.toResponseList(updated);
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    @Override
    public void deleteSchedule(Long jobId) {
        SchedulerJobInfo entity = findOrThrow(jobId);
        quartzManager.deleteJob(entity);   // unregister from Quartz first
        repository.delete(entity);
        log.info("Deleted schedule jobId={}", jobId);
    }

    @Override
    public void bulkDeleteSchedules(List<Long> jobIds) {
        log.info("Bulk deleting {} schedules", jobIds.size());
        List<SchedulerJobInfo> entities = repository.findAllById(jobIds);
        entities.forEach(quartzManager::deleteJob);
        repository.deleteAll(entities);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private SchedulerJobInfo findOrThrow(Long jobId) {
        return repository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", jobId));
    }

    /** UI shows "ACTIVE", DB stores "SCHEDULED" */
    private String mapToDbStatus(String uiStatus) {
        if (uiStatus == null) return null;
        return "ACTIVE".equalsIgnoreCase(uiStatus) ? SchedulerJobInfo.STATUS_SCHEDULED : uiStatus.toUpperCase();
    }

    /** Maps UI sort field names to entity field names */
    private String resolveSort(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "eventprofiletype", "name"   -> "name";
            case "status"                     -> "jobStatus";
            case "lasttruntime", "lastruntime"-> "lastRunTime";
            case "updatedat"                  -> "updatedAt";
            default                           -> "jobId";
        };
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
