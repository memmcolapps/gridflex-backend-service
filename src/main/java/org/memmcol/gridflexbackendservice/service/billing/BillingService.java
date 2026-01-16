package org.memmcol.gridflexbackendservice.service.billing;

import org.memmcol.gridflexbackendservice.model.billing.MeterReadingSheet;
import org.memmcol.gridflexbackendservice.model.user.MeterReadingDTO;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

public interface BillingService {

    Map<String, Object> getGenerateMeterReading(String assetId, String type, String meterClass);
    Map<String, Object> createMeterReading(MeterReadingSheet meterReadingSheet);
    Map<String, Object> getAllMeterReading(MeterReadingDTO selectItem,int page, int size);
    Map<String, Object> updateMeterCurrentReading(MeterReadingSheet meterReadingSheet);

    void calculateMonthlyConsumption(UUID meterId, LocalDate date);

    Map<String, Object> monthlyConsumption(int page, int size, String search, String month, Integer year);
}
