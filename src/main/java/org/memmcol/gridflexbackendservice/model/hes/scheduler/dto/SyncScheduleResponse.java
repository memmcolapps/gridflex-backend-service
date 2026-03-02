package org.memmcol.gridflexbackendservice.model.hes.scheduler.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.memmcol.gridflexbackendservice.model.hes.scheduler.ActiveDay;
import org.memmcol.gridflexbackendservice.model.hes.scheduler.IntervalUnit;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
//@JsonInclude(JsonInclude.Include.NON_NULL)
public class SyncScheduleResponse {

    private Long jobId;

    // ---- UI table columns -----------------------------------------------
    private String eventProfileType;
    private Integer timeInterval;
    private IntervalUnit timeUnit;
    private String formattedInterval;      // "30 mins", "2 hrs"

    private List<ActiveDay> activeDays;
    private String formattedActiveDays;    // "Repeat-Daily", "Repeat-(Mon-Fri)"

    private String status;                 // "ACTIVE" or "PAUSED"

    // ---- Additional metadata --------------------------------------------
    private String jobName;
    private String jobGroup;
    private String jobClass;
    private boolean cronJob;
    private String cronExpression;
    private String obisCodes;
    private UUID orgId;
    private LocalDateTime lastRunTime;
    private LocalDateTime updatedAt;
}
