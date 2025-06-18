package org.memmcol.gridflexbackendservice.service.meter;

import org.memmcol.gridflexbackendservice.model.meter.BulkApproveMeter;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.Map;
import java.util.UUID;

public interface MeterService {
    Map<String, Object> createMeter(Meter request);

    Map<String, Object> updateMeter(Meter request);

    Map<String, Object> getAllMeters(int page, int size, String meterNumber, String simNo, String manufacturer, String meterClass, String category, String status, String createdAt);

    Map<String, Object> getSingleMeter(UUID meterId);

    Map<String, Object> changeStatus(UUID meterId, String status, String reason) throws MissingServletRequestParameterException;

    Map<String, Object> getManufacturers();

//    Map<String, Object> bulkApproveMeter(BulkApproveMeter request);

    Map<String, Object> singleCustomer(String customerId);

    Map<String, Object> assignMeterToCustomer(String accountNumber, String tariff, String customerId, UUID meterId, UUID cId, String feederLine, String transformer, String substation);

//    Map<String, Object> createVirtualMeter(Meter meter);

    Map<String, Object> allocateMeter(UUID meterNumber, UUID nodeId);
}
