package com.hes.datacollection.quartz;

import com.hes.datacollection.model.SchedulerJobInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Thin wrapper around the Quartz Scheduler that handles all job lifecycle
 * operations triggered by the Data Collection Scheduler API.
 *
 * Every operation here is idempotent — safe to call on create and update.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuartzJobManager {

    private final Scheduler scheduler;

    // -------------------------------------------------------------------------
    // Schedule (create or update)
    // -------------------------------------------------------------------------

    /**
     * Registers or replaces a Quartz job+trigger based on the entity state.
     * Called after scheduler_job_info is persisted.
     */
    public void scheduleOrUpdate(SchedulerJobInfo job) {
        try {
            JobKey jobKey = buildJobKey(job);

            // If already exists, delete it so we can re-register cleanly
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
                log.info("Removed existing Quartz job {} for re-scheduling", jobKey);
            }

            JobDetail jobDetail = buildJobDetail(job, jobKey);
            Trigger   trigger   = buildTrigger(job, jobKey);

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Scheduled Quartz job {} with trigger {}", jobKey, trigger.getKey());

            // Honour initial paused state
            if (SchedulerJobInfo.STATUS_PAUSED.equals(job.getJobStatus())) {
                scheduler.pauseJob(jobKey);
                log.info("Job {} initially paused", jobKey);
            }

        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to schedule Quartz job for jobId=" + job.getJobId(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Pause
    // -------------------------------------------------------------------------

    public void pauseJob(SchedulerJobInfo job) {
        try {
            scheduler.pauseJob(buildJobKey(job));
            log.info("Paused Quartz job {}", buildJobKey(job));
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to pause job id=" + job.getJobId(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Resume
    // -------------------------------------------------------------------------

    public void resumeJob(SchedulerJobInfo job) {
        try {
            scheduler.resumeJob(buildJobKey(job));
            log.info("Resumed Quartz job {}", buildJobKey(job));
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to resume job id=" + job.getJobId(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    public void deleteJob(SchedulerJobInfo job) {
        try {
            JobKey key = buildJobKey(job);
            if (scheduler.checkExists(key)) {
                scheduler.deleteJob(key);
                log.info("Deleted Quartz job {}", key);
            }
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to delete job id=" + job.getJobId(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private JobKey buildJobKey(SchedulerJobInfo job) {
        return new JobKey(job.getJobName(), job.getJobGroup());
    }

    @SuppressWarnings("unchecked")
    private JobDetail buildJobDetail(SchedulerJobInfo job, JobKey jobKey) {
        Class<? extends Job> jobClass = resolveJobClass(job.getJobClass());

        return JobBuilder.newJob(jobClass)
                .withIdentity(jobKey)
                .withDescription(job.getDescription())
                // Pass the DB record id into the job context
                .usingJobData("jobId",     String.valueOf(job.getJobId()))
                .usingJobData("obisCodes", job.getObisCodes() != null ? job.getObisCodes() : "")
                .usingJobData("orgId",     job.getOrgId()    != null ? job.getOrgId().toString() : "")
                .storeDurably(false)
                .build();
    }

    private Trigger buildTrigger(SchedulerJobInfo job, JobKey jobKey) {
        TriggerKey triggerKey = TriggerKey.triggerKey(job.getJobName(), job.getJobGroup());

        if (Boolean.TRUE.equals(job.getCronJob()) && job.getCronExpression() != null) {
            // ---- Cron trigger (specific active days) ----------------------
            return TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .forJob(jobKey)
                    .withSchedule(
                            CronScheduleBuilder
                                    .cronSchedule(job.getCronExpression())
                                    .withMisfireHandlingInstructionDoNothing()
                    )
                    .startNow()
                    .build();
        } else {
            // ---- Simple repeat trigger (all days, fixed interval) ----------
            long intervalMs = job.getRepeatTime() != null ? job.getRepeatTime() : 1_800_000L; // 30 min default

            return TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .forJob(jobKey)
                    .withSchedule(
                            SimpleScheduleBuilder
                                    .simpleSchedule()
                                    .withIntervalInMilliseconds(intervalMs)
                                    .repeatForever()
                                    .withMisfireHandlingInstructionNextWithExistingCount()
                    )
                    .startAt(new Date(System.currentTimeMillis() + intervalMs)) // first fire after 1 interval
                    .build();
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Job> resolveJobClass(String className) {
        String target = (className != null && !className.isBlank())
                ? className
                : "com.hes.datacollection.quartz.DataCollectionJob"; // default

        try {
            return (Class<? extends Job>) Class.forName(target);
        } catch (ClassNotFoundException e) {
            log.warn("Job class '{}' not found, falling back to DataCollectionJob", target);
            return DataCollectionJob.class;
        }
    }
}
