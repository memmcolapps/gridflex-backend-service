//package org.memmcol.gridflexbackendservice.model.hes.scheduler.dto;
//
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.memmcol.gridflexbackendservice.model.hes.scheduler.ActiveDay;
//import org.memmcol.gridflexbackendservice.model.hes.scheduler.IntervalUnit;
//
//import javax.validation.constraints.NotBlank;
//import javax.validation.constraints.NotEmpty;
//import javax.validation.constraints.NotNull;
//import javax.validation.constraints.Positive;
//import java.util.List;
//import java.util.UUID;
//
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class SyncScheduleRequest {
//
//    @NotBlank(message = "Event/Profile type is required")
//    private String eventProfileType;
//
//    @NotNull(message = "Time interval is required")
//    @Positive(message = "Time interval must be positive")
//    private Integer timeInterval;
//
//    @NotNull(message = "Time unit is required")
//    private IntervalUnit timeUnit;
//
//    @NotEmpty(message = "At least one active day must be selected")
//    private List<ActiveDay> activeDays;
//
//    private String obisCodes;
//    private UUID orgId;
//
//    /**
//     * Job class that AMI Core will instantiate (optional).
//     * Defaults to AMI Core's standard DataCollectionJob if omitted.
//     */
//    private String jobClass;
//}