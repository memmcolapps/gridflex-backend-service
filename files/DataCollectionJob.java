package com.hes.datacollection.quartz;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Default Quartz Job executed on each scheduled fire for a Data Collection schedule.
 *
 * This is the execution entry-point. Extend or replace with your actual
 * meter data collection logic (DLMS/COSEM calls, AMI head-end communication, etc.).
 *
 * Available context from JobDataMap:
 *   - jobId     : scheduler_job_info.job_id (String)
 *   - obisCodes : pipe-separated OBIS codes (String)
 *   - orgId     : UUID of the organisation  (String)
 */
@Slf4j
@Component
public class DataCollectionJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getMergedJobDataMap();

        String jobId     = data.getString("jobId");
        String obisCodes = data.getString("obisCodes");
        String orgId     = data.getString("orgId");

        log.info("[DataCollectionJob] FIRED at {} | jobId={} | orgId={} | obisCodes={}",
                LocalDateTime.now(), jobId, orgId, obisCodes);

        try {
            // ------------------------------------------------------------------
            // TODO: Replace with actual meter data collection logic:
            //
            //   1. Resolve meter list for the orgId + OBIS codes
            //   2. Connect to AMI / HES head-end
            //   3. Issue DLMS/COSEM read requests for each OBIS code
            //   4. Persist readings to profile_reading_energy / profile_channel_* tables
            //   5. Update scheduler_job_info.last_run_time
            // ------------------------------------------------------------------

            log.info("[DataCollectionJob] Completed successfully for jobId={}", jobId);

        } catch (Exception e) {
            log.error("[DataCollectionJob] Failed for jobId={}: {}", jobId, e.getMessage(), e);
            // Wrap in JobExecutionException so Quartz can handle retries
            throw new JobExecutionException(e, false); // false = don't refire immediately
        }
    }
}
