package org.memmcol.gridflexbackendservice.service.meter_reading_sheet;

import org.memmcol.gridflexbackendservice.model.meter.MeterReadingSheet;
import org.memmcol.gridflexbackendservice.model.user.MeterReadingDTO;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface MeterReadingSheetService {

    Map<String, Object> getGenerateMeterReading(String assetId, String type);
    Map<String, Object> createMeterReading(MeterReadingSheet meterReadingSheet);
    Map<String, Object> getAllMeterReading(MeterReadingDTO selectItem,int page, int size);
    Map<String, Object> updateMeterCurrentReading(UUID meterReadingId, BigDecimal currentReading);


}
