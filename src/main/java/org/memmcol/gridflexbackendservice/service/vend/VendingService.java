package org.memmcol.gridflexbackendservice.service.vend;

import org.memmcol.gridflexbackendservice.model.vend.*;

import java.util.Map;

public interface VendingService {
    Map<String, Object> createCreditToken(Transaction transaction);

    Map<String, Object> createKctToken(KctToken kctToken);

    Map<String, Object> createClearTamperToken(ClearTamper clearTamper);

    Map<String, Object> createClearCreditToken(ClearCredit clearCredit);

    Map<String, Object> createKctClearTamperToken(kctAndClearTamper kctAndClearTamper);

    Map<String, Object> createCompensationToken(KctToken kctToken);

    Map<String, Object> getAllToken();

    Map<String, Object> calculateCreditToken(CreditToken creditToken);
}
