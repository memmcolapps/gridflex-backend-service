package org.memmcol.gridflexbackendservice.service.hes;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface HesService {
//    Map<String, Object> dashboard();

    Map<String, Object> communicationReport(int page, int size, String type, String search, String node);

    Map<String, Object> profile(LocalDateTime startDate, LocalDateTime endDate, List<String> meterNumber,
                                String profile, String model, int page, int size, String search, String unit);

    Map<String, Object> event(LocalDateTime startDate, LocalDateTime endDate, List<String> meterNumber, String eventTypeName, String model, String search, int page, int size, String unit);

    Map<String, Object> modelEventType();

    Map<String, Object> communicationRangeReport(int page, int size, LocalDateTime startDate, LocalDateTime endDate, String type, String search, List<String> meterNumber, String node);

    Map<String, Object> scheduleData(int page, int size, String search);

    Map<String, Object> setSchedule(String jobGroup, String timeInterval, String unit, String jobName);

    Map<String, Object> profileEvents();

    Map<String, Object> setCron(String jobGroup, String jobName, String cronExpression);

    Map<String, Object> triggerEvent(String jobGroup, String jobName);

    Map<String, Object> meterConfiguration(String meterNumber, String simNo, String model, String meterClass, String category,
            String businessHub, String manufacturer, String meterStatus, int page, int size);

    Map<String, Object> profileEventsInfo(String type);
//    Map<String, Object> communicationMonthlyReport(int page, int size, LocalDateTime startDate, LocalDateTime endDate);
}
