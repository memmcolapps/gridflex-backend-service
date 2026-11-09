package org.memmcol.gridflexbackendservice.service.hes;

import org.memmcol.gridflexbackendservice.model.hes.BulkTriggerRequest;
import org.memmcol.gridflexbackendservice.model.hes.ClockSyncRequest;
import org.memmcol.gridflexbackendservice.model.hes.ProfileProcessRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface DlmsService {
    Map<String, Object> setClock(String serial, LocalDateTime dateTime);

    Map<String, Object> setCtpt(String serial,
                                long ctNumerator,
                                long ctDenominator,
                                long ptNumerator,
                                long ptDenominator);

    Map<String, Object> setApn(String serial, String apn);

    Map<String, Object> setIpPort(String serial, String ip, int port);

    Map<String, Object> readMeter(String serial, String type);

    Map<String, Object> setToken(String serial, String token);

    Map<String, Object> setRelayControl(String serial, boolean state);

    Map<String, Object> setRelayMode(String serial, int mode);

    Map<String, Object> profileProcess(ProfileProcessRequest request);

    Map<String, Object> setBulkMeterClock(ClockSyncRequest serial);

    Map<String, Object> getHesGroupLog();

    Map<String, Object> getHesMeterLog(Long jobId, String status, int page, int size, String sort);

    Map<String, Object> executeBulkJob(String jobGroup, String jobName, BulkTriggerRequest request);

    Map<String, Object> getBulkExecutionStatus(Long executionId);

    Map<String, Object> pauseJob(String jobGroup, String jobName);

    Map<String, Object> resumeJob(String jobGroup, String jobName);
}
