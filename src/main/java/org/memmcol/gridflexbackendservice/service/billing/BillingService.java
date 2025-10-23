package org.memmcol.gridflexbackendservice.service.billing;

import org.memmcol.gridflexbackendservice.model.meter.MeterReadingSheet;
import org.memmcol.gridflexbackendservice.model.user.MeterReadingDTO;

import java.util.Map;

public interface BillingService {

    Map<String, Object> getGenerateMeterReading(String assetId, String type, String meterClass);
    Map<String, Object> createMeterReading(MeterReadingSheet meterReadingSheet);
    Map<String, Object> getAllMeterReading(MeterReadingDTO selectItem,int page, int size);
    Map<String, Object> updateMeterCurrentReading(MeterReadingSheet meterReadingSheet);


}
