package org.memmcol.gridflexbackendservice.service.hes;

import org.memmcol.gridflexbackendservice.model.hes.DlmsBulkRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface DlmsService {
    Map<String, Object> setClock(DlmsBulkRequest.SetClockRequest request);

    Map<String, Object> setCtpt(DlmsBulkRequest.SetCtptRequest request);

    Map<String, Object> setApn(DlmsBulkRequest.SetApnRequest request);

    Map<String, Object> setIpPort(DlmsBulkRequest.SetIpPortRequest request);
}
