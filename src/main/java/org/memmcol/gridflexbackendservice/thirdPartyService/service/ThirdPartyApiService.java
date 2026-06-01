package org.memmcol.gridflexbackendservice.thirdPartyService.service;

import java.util.Map;

public interface ThirdPartyApiService {

    Map<String, Object> odysseyMeterReading();

    Map<String, Object> odysseyPayment();
}
