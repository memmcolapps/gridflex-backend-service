package org.memmcol.gridflexbackendservice.thirdPartyService.service;

import java.util.Date;
import java.util.Map;

public interface ThirdPartyApiService {

    Map<String, Object> odysseyMeterReading(Date startDate, Date endDate, int offSet, int pageLimit);

    Map<String, Object> odysseyPayment(Date startDate, Date endDate);
}
