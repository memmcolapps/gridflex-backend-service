package org.memmcol.gridflexbackendservice.service.tariff;

import org.memmcol.gridflexbackendservice.model.tariff.BulkApprovalRequest;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TariffService {
    Map<String, Object> createTariff(Tariff tariff);

//    Map<String, Object> manageTariffStatus(UUID tariffId, String approveStatus) throws MissingServletRequestParameterException;

//    Map<String, Object> getTariffs(int page, int size);

    Map<String, Object> getFilterTariffs(int page, int size, String tariffName, String tariffType,
                                         String tariffRate, String bandCode, Boolean status, String effectiveDate, String approveStatus, String type);

//    Map<String, Object> getUniqueTariffId();

    Map<String, Object> bulkApproveTariff(BulkApprovalRequest tariffIds);

    Map<String, Object> updateTariff(Tariff tariff);

    Map<String, Object> getTariff(UUID id, UUID tariffVersionId);

    Map<String, Object> approve(UUID tId, String approveStatus) throws MissingServletRequestParameterException;;

    Map<String, Object> changeStatus(UUID id, Boolean status);
}
