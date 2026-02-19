package com.hes.datacollection.service;

import com.hes.datacollection.dto.PagedResponse;
import com.hes.datacollection.dto.SyncScheduleRequest;
import com.hes.datacollection.dto.SyncScheduleResponse;
import com.hes.datacollection.exception.DuplicateScheduleException;
import com.hes.datacollection.exception.ResourceNotFoundException;
import com.hes.datacollection.mapper.DataCollectionMapper;
import com.hes.datacollection.model.ActiveDay;
import com.hes.datacollection.model.IntervalUnit;
import com.hes.datacollection.model.SchedulerJobInfo;
import com.hes.datacollection.quartz.CronExpressionBuilder;
import com.hes.datacollection.quartz.QuartzJobManager;
import com.hes.datacollection.repository.SchedulerJobInfoRepository;
import com.hes.datacollection.service.impl.DataCollectionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataCollectionService — Unit Tests")
class DataCollectionServiceImplTest {

    @Mock SchedulerJobInfoRepository repository;
    @Mock DataCollectionMapper        mapper;
    @Mock QuartzJobManager            quartzManager;

    @InjectMocks DataCollectionServiceImpl service;

    // ---- Test fixtures -------------------------------------------------------

    private SchedulerJobInfo activeEntity;
    private SchedulerJobInfo pausedEntity;
    private SyncScheduleRequest validRequest;
    private SyncScheduleResponse activeResponse;

    @BeforeEach
    void setUp() {
        activeEntity = SchedulerJobInfo.builder()
                .jobId(1L)
                .name("Standard Event Log")
                .jobName("Standard_Event_Log")
                .jobGroup(SchedulerJobInfo.GROUP_DATA_COLLECTION)
                .jobClass("com.hes.datacollection.quartz.DataCollectionJob")
                .cronJob(false)
                .repeatTime(1_800_000L)
                .repeatMinutes(30)
                .jobStatus(SchedulerJobInfo.STATUS_SCHEDULED)
                .obisCodes("")
                .build();

        pausedEntity = SchedulerJobInfo.builder()
                .jobId(2L)
                .name("Relay Control Log")
                .jobName("Relay_Control_Log")
                .jobGroup(SchedulerJobInfo.GROUP_DATA_COLLECTION)
                .cronJob(false)
                .repeatMinutes(30)
                .jobStatus(SchedulerJobInfo.STATUS_PAUSED)
                .obisCodes("")
                .build();

        validRequest = SyncScheduleRequest.builder()
                .eventProfileType("Standard Event Log")
                .timeInterval(30)
                .timeUnit(IntervalUnit.MINS)
                .activeDays(List.of(ActiveDay.values()))
                .build();

        activeResponse = SyncScheduleResponse.builder()
                .jobId(1L)
                .eventProfileType("Standard Event Log")
                .timeInterval(30)
                .timeUnit(IntervalUnit.MINS)
                .formattedInterval("30 mins")
                .formattedActiveDays("Repeat-Daily")
                .status("ACTIVE")
                .build();
    }

    // =========================================================================
    @Nested @DisplayName("createSchedule()")
    class CreateTests {

        @Test @DisplayName("Creates schedule and registers with Quartz on success")
        void create_success() {
            when(repository.existsByNameInOrgExcluding(any(), any(), anyLong())).thenReturn(false);
            when(mapper.toEntity(validRequest)).thenReturn(activeEntity);
            when(repository.save(any())).thenReturn(activeEntity);
            when(mapper.toResponse(activeEntity)).thenReturn(activeResponse);

            SyncScheduleResponse result = service.createSchedule(validRequest);

            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            verify(repository).save(activeEntity);
            verify(quartzManager).scheduleOrUpdate(activeEntity);
        }

        @Test @DisplayName("Throws DuplicateScheduleException when name already exists")
        void create_duplicate_throws() {
            when(repository.existsByNameInOrgExcluding(any(), any(), anyLong())).thenReturn(true);

            assertThatThrownBy(() -> service.createSchedule(validRequest))
                    .isInstanceOf(DuplicateScheduleException.class)
                    .hasMessageContaining("Standard Event Log");

            verify(repository, never()).save(any());
            verify(quartzManager, never()).scheduleOrUpdate(any());
        }
    }

    // =========================================================================
    @Nested @DisplayName("toggleStatus()")
    class ToggleTests {

        @Test @DisplayName("Pauses an ACTIVE schedule in DB and Quartz")
        void toggle_activeToPaused() {
            when(repository.findById(1L)).thenReturn(Optional.of(activeEntity));
            when(repository.save(any())).thenReturn(activeEntity);
            when(mapper.toResponse(any())).thenReturn(
                    SyncScheduleResponse.builder().status("PAUSED").build());

            SyncScheduleResponse result = service.toggleStatus(1L);

            assertThat(activeEntity.getJobStatus()).isEqualTo(SchedulerJobInfo.STATUS_PAUSED);
            verify(quartzManager).pauseJob(activeEntity);
            verify(quartzManager, never()).resumeJob(any());
        }

        @Test @DisplayName("Resumes a PAUSED schedule in DB and Quartz")
        void toggle_pausedToActive() {
            when(repository.findById(2L)).thenReturn(Optional.of(pausedEntity));
            when(repository.save(any())).thenReturn(pausedEntity);
            when(mapper.toResponse(any())).thenReturn(
                    SyncScheduleResponse.builder().status("ACTIVE").build());

            service.toggleStatus(2L);

            assertThat(pausedEntity.getJobStatus()).isEqualTo(SchedulerJobInfo.STATUS_SCHEDULED);
            verify(quartzManager).resumeJob(pausedEntity);
            verify(quartzManager, never()).pauseJob(any());
        }

        @Test @DisplayName("Throws ResourceNotFoundException for unknown jobId")
        void toggle_unknownId_throws() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.toggleStatus(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    // =========================================================================
    @Nested @DisplayName("deleteSchedule()")
    class DeleteTests {

        @Test @DisplayName("Removes from Quartz then from DB")
        void delete_success() {
            when(repository.findById(1L)).thenReturn(Optional.of(activeEntity));

            service.deleteSchedule(1L);

            // Quartz must be cleaned up BEFORE the DB row is removed
            var inOrder = inOrder(quartzManager, repository);
            inOrder.verify(quartzManager).deleteJob(activeEntity);
            inOrder.verify(repository).delete(activeEntity);
        }

        @Test @DisplayName("Throws when schedule not found")
        void delete_notFound() {
            when(repository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteSchedule(404L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    @Nested @DisplayName("getAllSchedules()")
    class ListTests {

        @Test @DisplayName("Returns paged response with correct metadata")
        void getAll_paged() {
            var dbPage = new PageImpl<>(List.of(activeEntity));
            when(repository.findDataCollectionSchedules(any(), any(), any(), any(Pageable.class)))
                    .thenReturn(dbPage);
            when(mapper.toResponseList(anyList())).thenReturn(List.of(activeResponse));

            PagedResponse<SyncScheduleResponse> result =
                    service.getAllSchedules(null, null, null, 0, 10, "jobId", "asc");

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.isFirst()).isTrue();
        }

        @Test @DisplayName("Maps 'ACTIVE' UI status to 'SCHEDULED' DB status")
        void getAll_mapsActiveStatusCorrectly() {
            var dbPage = new PageImpl<>(List.of(activeEntity));
            when(repository.findDataCollectionSchedules(any(), eq("SCHEDULED"), any(), any(Pageable.class)))
                    .thenReturn(dbPage);
            when(mapper.toResponseList(anyList())).thenReturn(List.of(activeResponse));

            service.getAllSchedules(null, "ACTIVE", null, 0, 10, "jobId", "asc");

            verify(repository).findDataCollectionSchedules(any(), eq("SCHEDULED"), any(), any(Pageable.class));
        }
    }
}


// =============================================================================
// CronExpressionBuilder — separate test class
// =============================================================================

package com.hes.datacollection.quartz;

import com.hes.datacollection.model.ActiveDay;
import com.hes.datacollection.model.IntervalUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CronExpressionBuilder — Unit Tests")
class CronExpressionBuilderTest {

    private final CronExpressionBuilder builder = new CronExpressionBuilder();

    @Test @DisplayName("All 7 days + MINS → simple repeat trigger")
    void allDaysMins_simpleRepeat() {
        var cfg = builder.build(30, IntervalUnit.MINS, List.of(ActiveDay.values()));

        assertThat(cfg.cronJob()).isFalse();
        assertThat(cfg.cronExpression()).isNull();
        assertThat(cfg.repeatTimeMs()).isEqualTo(1_800_000L);
        assertThat(cfg.repeatMinutes()).isEqualTo(30);
    }

    @Test @DisplayName("All 7 days + HRS → simple repeat trigger")
    void allDaysHrs_simpleRepeat() {
        var cfg = builder.build(2, IntervalUnit.HRS, List.of(ActiveDay.values()));

        assertThat(cfg.cronJob()).isFalse();
        assertThat(cfg.repeatTimeMs()).isEqualTo(7_200_000L);
        assertThat(cfg.repeatHours()).isEqualTo(2);
    }

    @Test @DisplayName("Mon-Fri + MINS → cron trigger with correct expression")
    void monFriMins_cronTrigger() {
        var days = List.of(ActiveDay.MON, ActiveDay.TUE, ActiveDay.WED, ActiveDay.THU, ActiveDay.FRI);
        var cfg  = builder.build(30, IntervalUnit.MINS, days);

        assertThat(cfg.cronJob()).isTrue();
        assertThat(cfg.cronExpression()).isEqualTo("0 0/30 * ? * MON,TUE,WED,THU,FRI *");
        assertThat(cfg.repeatTimeMs()).isNull();
    }

    @Test @DisplayName("Mon-Fri + HRS → cron trigger")
    void monFriHrs_cronTrigger() {
        var days = List.of(ActiveDay.MON, ActiveDay.TUE, ActiveDay.WED, ActiveDay.THU, ActiveDay.FRI);
        var cfg  = builder.build(2, IntervalUnit.HRS, days);

        assertThat(cfg.cronJob()).isTrue();
        assertThat(cfg.cronExpression()).isEqualTo("0 0 0/2 ? * MON,TUE,WED,THU,FRI *");
    }

    @Test @DisplayName("parseDaysFromCron returns all days for null cron (simple trigger)")
    void parseDays_null_returnsAll() {
        List<ActiveDay> days = builder.parseDaysFromCron(null);
        assertThat(days).hasSize(7);
    }

    @Test @DisplayName("parseDaysFromCron correctly extracts Mon-Fri")
    void parseDays_monFri() {
        List<ActiveDay> days = builder.parseDaysFromCron("0 0/30 * ? * MON,TUE,WED,THU,FRI *");
        assertThat(days).containsExactlyInAnyOrder(
                ActiveDay.MON, ActiveDay.TUE, ActiveDay.WED, ActiveDay.THU, ActiveDay.FRI);
    }
}
