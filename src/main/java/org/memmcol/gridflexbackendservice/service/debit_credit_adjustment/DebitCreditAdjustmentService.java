package org.memmcol.gridflexbackendservice.service.debit_credit_adjustment;

import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditAdjustment;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface DebitCreditAdjustmentService {
    Map<String, Object> createDebitAdjustment(DebitCreditAdjustment debitAdjustment);
    
    Map<String, Object> getDebitAdjustment(String accountNumber);

    Map<String, Object> getDebitAdjustments(int page, int size, String customerId, String accountNumber, String customerName, String meterNumber, BigDecimal balance, String type);

    Map<String, Object> reconcileDebt(UUID debitCreditAdjustmentId, String amount);
}
