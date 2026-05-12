package org.memmcol.gridflexbackendservice.service.hes;

import java.time.LocalDateTime;
import java.util.Map;

public interface DlmsService {
    Map<String, Object> setClock(String serial, LocalDateTime dateTime);

    Map<String, Object> setCtpt(String serial,
                                long ctNumerator,
                                long ctDenominator,
                                long ptNumerator,
                                long ptDenominator);

    Map<String, Object> setApn(String serial, String apn);

    Map<String, Object> setIpPort(String serial, String ip, int port);

    Map<String, Object> readMeter(String serial, String type);

    Map<String, Object> setToken(String serial, String token);

    Map<String, Object> setRelayControl(String serial, boolean state);

    Map<String, Object> setRelayMode(String serial, int mode);

}
