package org.memmcol.gridflexbackendservice.service.meter;

import org.memmcol.gridflexbackendservice.model.meter.AssignMeterToCustomer;
import org.memmcol.gridflexbackendservice.model.meter.BulkApproveMeter;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.meter.PaymentMode;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.Map;
import java.util.UUID;

public interface MeterService {
    Map<String, Object> createMeter(Meter request);

    Map<String, Object> updateMeter(Meter request);

    Map<String, Object> getAllMeters(int page, int size, String meterNumber, String simNo, String manufacturer, String meterStage,
                                     String meterClass, String category, String status, String createdAt, String customerId, String type);

    Map<String, Object> getSingleMeter(UUID meterId, String meterNumber, String accountNumber, UUID meterVersionId, String versionMeterNumber);

    Map<String, Object> changeStatus(UUID meterId, String status, String reason) throws MissingServletRequestParameterException;

    Map<String, Object> getManufacturers();

//    Map<String, Object> bulkApproveMeter(BulkApproveMeter request);

    Map<String, Object> singleCustomer(String customerId);

//    Map<String, Object> assignMeterToCustomer(String accountNumber, String tariff, String customerId, UUID meterId, UUID cId, String feederLine, String transformer, String substation);

//    Map<String, Object> createVirtualMeter(Meter meter);

    Map<String, Object> allocateMeter(String meterNumber, String regionId);

    Map<String, Object> assignMeterToCustomer(AssignMeterToCustomer assignMeterToCustomer);

    Map<String, Object> migrate(PaymentMode paymentMode);

    Map<String, Object> approve(UUID meterVersionId, String approveState) throws MissingServletRequestParameterException;

    Map<String, Object> detachMeter(UUID meterNumber, String reason);
}
