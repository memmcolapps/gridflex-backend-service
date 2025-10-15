package org.memmcol.gridflexbackendservice.service.vend;

import org.memmcol.gridflexbackendservice.model.vend.*;

import java.util.Map;

public interface VendingService {
    Map<String, Object> createCreditToken(CreditToken creditToken);

    Map<String, Object> createKctToken(KctToken kctToken);

    Map<String, Object> createClearTamperToken(ClearTamper clearTamper);

    Map<String, Object> createClearCreditToken(ClearCredit clearCredit);

    Map<String, Object> createKctClearTamperToken(kctAndClearTamper kctAndClearTamper);

    Map<String, Object> createCompensationToken(KctToken kctToken);

    Map<String, Object> getAllToken(String meterNumber, String meterAccountNumber,
                                    String tariffName, String tokenType, String status,int page, int size);

    Map<String, Object> calculateCreditToken(CreditToken creditToken);
}
