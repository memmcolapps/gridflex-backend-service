package org.memmcol.gridflexbackendservice.service.hes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface DlmsService {
    Map<String, Object> setClock(List<String> serials, LocalDateTime dateTime);

    Map<String, Object> setCtpt(List<String> serials,
                                long ctNumerator,
                                long ctDenominator,
                                long ptNumerator,
                                long ptDenominator);

    Map<String, Object> setApn(List<String> serials, String apn);

    Map<String, Object> setIpPort(List<String> serials, String ip, int port);
}
