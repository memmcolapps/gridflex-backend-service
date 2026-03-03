package org.memmcol.gridflexbackendservice.service.debit_credit_adjustment;

import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditAdjust;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface DebitCreditAdjustmentService {
    Map<String, Object> createDebitAdjustment(DebitCreditAdjust debitAdjustment);

    Map<String, Object> getDebitAdjustment(UUID meterId, String type);

    Map<String, Object> getDebitAdjustments(int page, int size, String customerId, String accountNumber, String customerName, String meterNumber, BigDecimal balance, String type);

    Map<String, Object> reconcileDebt(UUID meterId, UUID liabilityCauseId, String amount);

    Map<String, Object> getMeterAndLiabilityCause(String meterNumber, String accountNumber);

    Map<String, Object> getDebitAdjustmentPaymentHistory( UUID meterId, UUID liabilityCauseId, String type );
}