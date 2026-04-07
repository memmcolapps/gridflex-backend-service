package org.memmcol.gridflexbackendservice.service.debit_credit_adjustment;

import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditAdjust;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface DebitCreditAdjustmentService {
    Map<String, Object> createDebitAdjustment(DebitCreditAdjust debitAdjustment);

    Map<String, Object> getDebitAdjustment(UUID meterId, String type);

    Map<String, Object> getDebitAdjustments(int page, int size, String type, String search, DebitCreditAdjust debitCreditAdjust);

    Map<String, Object> reconcileDebt(UUID meterId, UUID liabilityCauseId, String amount);

    Map<String, Object> getMeterAndLiabilityCause(String meterNumber, String accountNumber);

    Map<String, Object> getDebitAdjustmentPaymentHistory( UUID meterId, UUID liabilityCauseId, String type );

    Map<String, Object> debitCreditAdjustmentBulkUpload(MultipartFile file) throws IOException;
}