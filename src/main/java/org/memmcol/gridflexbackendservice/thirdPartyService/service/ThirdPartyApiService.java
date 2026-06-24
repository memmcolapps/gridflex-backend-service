package org.memmcol.gridflexbackendservice.thirdPartyService.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

public interface ThirdPartyApiService {

    Map<String, Object> odysseyMeterReading(LocalDateTime startDate, LocalDateTime endDate, int offSet, int pageLimit);

    Map<String, Object> odysseyPayment(LocalDateTime startDate, LocalDateTime endDate, String id, int offSet, int pageLimit);
}
