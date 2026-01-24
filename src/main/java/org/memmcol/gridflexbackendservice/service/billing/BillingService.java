package org.memmcol.gridflexbackendservice.service.billing;

import org.memmcol.gridflexbackendservice.model.billing.FeederReadingSheet;
import org.memmcol.gridflexbackendservice.model.billing.MeterReadingSheet;
import org.memmcol.gridflexbackendservice.model.user.MeterReadingDTO;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface BillingService {

    Map<String, Object> getGenerateMeterReading(String assetId, String type, String meterClass);
    Map<String, Object> createMeterReading(MeterReadingSheet meterReadingSheet);
    Map<String, Object> getAllMeterReading(String search,int page, int size, String month, Integer year, String meterClass);
    Map<String, Object> updateMeterCurrentReading(MeterReadingSheet meterReadingSheet);

    void calculateMonthlyConsumption(UUID meterId, LocalDate date);

    Map<String, Object> monthlyConsumption(int page, int size, String search, String month, Integer year);

    Map<String, Object> energyImport(List<MeterReadingSheet> meterReadingSheet);

    Map<String, Object> monthlyConsumptionByFeeder(int page, int size, String search, String month, Integer year, UUID nodeId);

    Map<String, Object> generateMonthlyFeederReading(FeederReadingSheet feederReadingSheet);

    Map<String, Object> updateMonthlyFeederReading(FeederReadingSheet feederReadingSheet);
}
