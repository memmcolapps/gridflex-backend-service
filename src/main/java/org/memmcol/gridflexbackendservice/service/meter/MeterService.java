package org.memmcol.gridflexbackendservice.service.meter;

import org.memmcol.gridflexbackendservice.model.meter.*;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.model.vend.MeterView;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface MeterService {
    Map<String, Object> createMeter(Meter request);

    Map<String, Object> updateMeter(Meter request);

    Map<String, Object> getAllMeters(int page, int size, String meterNumber, String simNo, String manufacturer, String meterStage,
                                     String meterClass, String category, String status, String createdAt, String customerId, String type);

    Map<String, Object> getSingleMeter(UUID meterId, String meterNumber, String accountNumber, UUID meterVersionId, String versionMeterNumber, String cin);

    Map<String, Object> changeStatus(UUID meterId, Boolean status, String reason) throws MissingServletRequestParameterException;

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

    Map<String, Object> continueAssignMeter(MeterView meterView);

    Map<String, Object> bulkUpload(MultipartFile file) throws IOException;

    ///
    Map<String, Object> bulkAllocateMeters(List<MeterRequest> allocations, UserModel user);

    Map<String, Object> bulkApproval(List<MeterRequest> meterNumber) throws IOException;

//    Map<String, Object> bulkApproveMeters(List<Meter> meters, UserModel user);

    Map<String, Object> bulkAllocate(MultipartFile file) throws IOException;
}
