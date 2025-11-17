package org.memmcol.gridflexbackendservice.service.hes;


import java.time.LocalDateTime;
import java.util.Map;

public interface HesService {
    Map<String, Object> dashboard();

    Map<String, Object> communicationReport(int page, int size, String type, String search);

    Map<String, Object> profile(LocalDateTime startDate, LocalDateTime endDate, String meterNumber, String profile, String model, int page, int size, String search);

    Map<String, Object> event(LocalDateTime startDate, LocalDateTime endDate, String meterNumber, String eventTypeName, String model, String search, int page, int size);

    Map<String, Object> modelEventType();

    Map<String, Object> communicationDailyReport(int page, int size, LocalDateTime startDate, LocalDateTime endDate, String type, String search, String meterNumber);

    Map<String, Object> communicationMonthlyReport(int page, int size, LocalDateTime startDate, LocalDateTime endDate);
}
