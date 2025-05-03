package org.memmcol.gridflexbackendservice.service.tariff;

import org.memmcol.gridflexbackendservice.model.BulkApprovalRequest;
import org.memmcol.gridflexbackendservice.model.Tariff;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface TariffService {
    Map<String, Object> createTariff(Tariff tariff);

    Map<String, Object> manageTariffStatus(Long tariffId, Boolean status, String approveStatus);

//    Map<String, Object> getTariffs(int page, int size);

    Map<String, Object> getFilterTariffs(String tariffName, String tariffIndex, String tariffType, String tariffRate, String bandCode, Boolean status, String effectiveDate, String approveStatus);

    Map<String, Object> getUniqueTariffId();

    Map<String, Object> bulkApproveTariff(BulkApprovalRequest request);
}
