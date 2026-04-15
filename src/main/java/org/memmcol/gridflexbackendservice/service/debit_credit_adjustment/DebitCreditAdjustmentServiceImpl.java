package org.memmcol.gridflexbackendservice.service.debit_credit_adjustment;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.memmcol.gridflexbackendservice.mapper.DebitCreditAdjustmentMapper;
import org.memmcol.gridflexbackendservice.mapper.MeterMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.CreditDebitAdjustment;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditAdjust;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditPayment;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.MeterAndLiabilityCause;
import org.memmcol.gridflexbackendservice.model.debt_setting.LiabilityCause;
import org.memmcol.gridflexbackendservice.model.meter.Meter;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.service.audit.SafeAuditService;
import org.memmcol.gridflexbackendservice.service.tariff.TariffServiceImpl;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.util.GenericResp;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.memmcol.gridflexbackendservice.components.GenericHandler.capitalizeFirstLetter;
import static org.memmcol.gridflexbackendservice.components.HandleValidUser.handleUserValidation;

@Service
public class DebitCreditAdjustmentServiceImpl implements DebitCreditAdjustmentService {
    private static final Logger log = LoggerFactory.getLogger(TariffServiceImpl.class);

    @Autowired
    private GenericHandler genericHandler;

    @Autowired
    private DebitCreditAdjustmentMapper mapper;

    @Autowired
    private ResponseProperties status;

//    @Autowired
//    private AuditRepository auditRepository;

    @Autowired
    private SafeAuditService safeAuditService;

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;

    @Autowired
    private HttpServletRequest httpServletRequest;

    private final IMap<String, Object> debitCreditCache;
    //
    private final IMap<String, Object> auditCache;

    private String debit = "Debit Adjustment";

    private String credit = "Credit Adjustment";
    @Autowired
    private MeterMapper meterMapper;

    public DebitCreditAdjustmentServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.debitCreditCache = hazelcastInstance.getMap("debitCreditCache");
        this.auditCache = hazelcastInstance.getMap("auditCache");
    }

    @Transactional
    @Override
    public Map<String, Object> createDebitAdjustment(DebitCreditAdjust request) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            int result;

            UserModel um = handleUserValidation();
            UUID nodeId = um.getNodeInfo().getNodeId();
            String nodeType = um.getNodeInfo().getType();
            if(!nodeType.equalsIgnoreCase("Business hub")
                    && !nodeType.equalsIgnoreCase("Service center")){
                throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
            }

            if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0){
                throw new GlobalExceptionHandler.NotFoundException("Amount must be greater than zero");
            }

            if (request.getType() == null ||
                    (!request.getType().trim().equalsIgnoreCase("credit")
                            && !request.getType().trim().equalsIgnoreCase("debit"))) {
                throw new GlobalExceptionHandler.NotFoundException("Parameter type must be; type: 'credit' or 'debit'");
            }

            String statement = "adjustment newly created";

            String desc = Objects.equals(request.getType(), "debit") ? "Debit" + statement : "Credit" + statement;

            Meter meter = mapper.getMeterById(request.getMeterId());
            if (meter == null) {
                throw new GlobalExceptionHandler.NotFoundException("Meter not found or not assigned");
            }

            if(!meter.getNodeId().equals(nodeId)
                    && !meter.getServiceCenter().equals(nodeId)
                    && !meter.getRoot().equals(nodeId)){
                throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
            }

            LiabilityCause liabilityCause = mapper.getLiabilityCauseById(request.getLiabilityCauseId(), um.getOrgId());
            if (liabilityCause == null) {
                throw new GlobalExceptionHandler.NotFoundException("Liability cause not found");
            }

            DebitCreditAdjust AdjustResult = mapper.getDebitAdjustmentByMeterIdAndLiabilityCause(
                    request.getMeterId(),
                    um.getOrgId(),
                    request.getLiabilityCauseId(),
                    request.getType());

            if (AdjustResult != null) {

//                request.setBalance(liabilityResult.getBalance().add(request.getBalance()));

                int rows = mapper.addCreditDebitAdjustment(AdjustResult.getId(), request.getAmount(), request.getAmount());
                if (rows == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(request.getType()+" adjustment failed");
                }
                request.setBalance(AdjustResult.getBalance().add(request.getAmount()));
                request.setId(AdjustResult.getId());
            } else {
                request.setOrgId(um.getOrgId());
                request.setStatus("UNPAID");
                result = mapper.createDebitAdjustment(request);
                if(result == 0){
                    throw new GlobalExceptionHandler.NotFoundException(debit + " " + status.getRegFailureDesc());
                }
            }

            // 1. Save payment record
            DebitCreditPayment payment = new DebitCreditPayment();
            payment.setCreditDebitAdjId(request.getId());
            payment.setCredit(request.getType().equalsIgnoreCase("credit")
                    ? request.getAmount() : BigDecimal.ZERO);
            payment.setBalance(AdjustResult != null ? request.getBalance() : request.getAmount());
            payment.setDebt(request.getType().equalsIgnoreCase("credit")
                    ? BigDecimal.ZERO : request.getAmount());
            payment.setOrgId(um.getOrgId());

            int res = mapper.insertDebtCreditPayment(payment);
            if(res == 0){
                throw new GlobalExceptionHandler.NotFoundException("Debt Reconciliation" + status.getRegFailureDesc());
            }

            DebitCreditAdjust debitAdjustment = mapper.getDebitAdjustmentById(request.getId(), um.getOrgId());
            um.setPassword("");
//            handleAddCache(debitAdjustment);
            AuditLog auditLog = buildAuditLog(um, desc, "debit-credit", debitAdjustment, metadata);
            safeAuditService.saveAudit(auditLog);

            if(request.getType().equalsIgnoreCase("credit")){
                return ResponseMap.response(status.getSuccessCode(), credit + " " + status.getRegDesc(), "");
            } else {
                return ResponseMap.response(status.getSuccessCode(), debit + " " + status.getRegDesc(), "");
            }

        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Creating debit adjustment service failed");
            genericHandler.logAndSaveException(exception, "creating debit-credit adjustment");
            throw exception;
        }
    }

//    @Transactional
//    public Map<String, Object> reconcileDebt(UUID meterId, UUID liabilityCauseId,
//                                             String amount) {
//
//        try {
//
//            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
//
//            UserModel um = handleUserValidation();
//
//            List<DebitCreditAdjust> debts = mapper.getDebitAdjustmentByMeterIdAndLcId(
//                    meterId, liabilityCauseId, um.getOrgId());
//
//            if (debts == null) {
//                throw new GlobalExceptionHandler.NotFoundException(
//                        "No outstanding debts found");
//            }
//
//            BigDecimal remainingPayment = new BigDecimal(amount);
//
//            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
//                throw new GlobalExceptionHandler.NotFoundException(
//                        "Payment must be greater than zero");
//            }
//
//            BigDecimal totalOutstanding = debts.stream()
//                    .map(DebitCreditAdjust::getBalance)
//                    .filter(Objects::nonNull)
//                    .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//            if (remainingPayment.compareTo(totalOutstanding) > 0) {
//                throw new GlobalExceptionHandler.NotFoundException(
//                        "Payment exceeds total outstanding debt ("+totalOutstanding+")");
//            }
//
//            // Loop through debts
//            for (DebitCreditAdjust debt : debts) {
//
//                if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
//                    break; // stop if no money left
//                }
//
//                BigDecimal currentBalance = debt.getBalance();
//
//                if (currentBalance.compareTo(BigDecimal.ZERO) <= 0) {
//                    continue; // skip already paid
//                }
//
//                BigDecimal paymentForThisDebt;
//
//                if (remainingPayment.compareTo(currentBalance) >= 0) {
//                    // Fully pay this debt
//                    paymentForThisDebt = currentBalance;
//                } else {
//                    // Partially pay this debt
//                    paymentForThisDebt = remainingPayment;
//                }
//
//                BigDecimal newBalance = currentBalance.subtract(paymentForThisDebt);
//
//                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
//                    newBalance = BigDecimal.ZERO; // hard safety guard
//                }
//
//                String newStatus = newBalance.compareTo(BigDecimal.ZERO) == 0
//                        ? "PAID"
//                        : "PARTIALLY_PAID";
//
//                DebitCreditPayment debitCreditPayment = mapper.getPaymentById(
//                        debt.getId(),
//                        um.getOrgId());
//
//                if (debitCreditPayment == null) {
//                    throw new GlobalExceptionHandler.NotFoundException("Debit adjustment payment not found");
//                }
//
//                // Insert payment record
//                DebitCreditPayment payment = new DebitCreditPayment();
//                payment.setParentId(debitCreditPayment.getId());
//                payment.setCreditDebitAdjId(debt.getId());
//                payment.setCredit(paymentForThisDebt);
//                payment.setBalance(newBalance);
//                payment.setDebt(BigDecimal.ZERO);
//                payment.setOrgId(um.getOrgId());
//
//                int res = mapper.insertDebtCreditPayment(payment);
//                if(res == 0){
//                    throw new GlobalExceptionHandler.NotFoundException("Debt Reconciliation" + status.getRegFailureDesc());
//                }
//
//                // Update debt record
//                int resp = mapper.updateReconciledDebt(
//                        debt.getId(),
//                        newBalance,
//                        newStatus
//                );
//                if(resp == 0){
//                    throw new GlobalExceptionHandler.NotFoundException("Balance" + status.getUpdateFailureDesc());
//                }
//
//                // Reduce remaining payment
//                remainingPayment = remainingPayment.subtract(paymentForThisDebt);
//
//                DebitCreditAdjust debitAdjustment = mapper.getDebitAdjustmentById(debt.getId(), um.getOrgId());
//                String desc = capitalizeFirstLetter(debitAdjustment.getLiabilityCause().getName())+" debt reconcile "+newStatus;
//                um.setPassword("");
//    //            handleAddCache(debitAdjustment);
//                AuditLog auditLog = buildAuditLog(um, desc, "debit-credit", debitAdjustment, metadata);
//                safeAuditService.saveAudit(auditLog);
////            return ResponseMap.response(status.getSuccessCode(), "Payment reconciliation successful", "");
////
////
//            }
//
//            return ResponseMap.response(
//                    status.getSuccessCode(),
//                    "Debt reconciliation successful",
//                    ""
//            );
//
//        } catch (Exception e) {
//            log.error("Reconciliation failed", e);
//            throw e;
//        }
//    }

    @Transactional
    @Override
    public Map<String, Object> reconcileDebt(
            UUID meterId, UUID liabilityCauseId, String amount) {

        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

            UserModel um = handleUserValidation();
            UUID nodeId = um.getNodeInfo().getNodeId();
            String nodeType = um.getNodeInfo().getType();
//            if(!nodeType.equalsIgnoreCase("Business hub")
//                    && !nodeType.equalsIgnoreCase("Service center")){
//                throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
//            }

            Meter meter = mapper.getMeterById(meterId);
            if (meter == null) {
                throw new GlobalExceptionHandler.NotFoundException("Meter not found or not assigned");
            }

            if(!meter.getNodeId().equals(nodeId)
                    && !meter.getServiceCenter().equals(nodeId)
                    && !meter.getRoot().equals(nodeId)){
                throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
            }

            DebitCreditAdjust debitCreditAdjust = mapper.getDebitAdjustmentByMeterIdAndLcId(
                    meterId, liabilityCauseId, um.getOrgId());
            if (debitCreditAdjust == null) {
                throw new GlobalExceptionHandler.NotFoundException(
                        "Debit Adjustment or liability cause not found or no debt available");
            }

            if (debitCreditAdjust.getStatus().equalsIgnoreCase("PAID")) {
                throw new GlobalExceptionHandler.NotFoundException("No outstanding adjustment debt");
            }

            // Convert strings to BigDecimal for precise monetary calculation
            BigDecimal currentBalance = debitCreditAdjust.getBalance();
            BigDecimal paymentAmount = new BigDecimal(amount);

            if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new GlobalExceptionHandler.NotFoundException("Payment amount must be greater than zero");
            }

            // Ensure we don't allow overpayment
            if (paymentAmount.compareTo(currentBalance) > 0) {
                throw new GlobalExceptionHandler.NotFoundException("Payment exceeds current balance");
            }

            BigDecimal returnBalance = debitCreditAdjust.getStatus().equalsIgnoreCase( "UNPAID")
                    || debitCreditAdjust.getStatus().equalsIgnoreCase( "PARTIALLY_PAID")
                    ? currentBalance.subtract(paymentAmount) : currentBalance;

            DebitCreditPayment debitCreditPayment = mapper.getPaymentById(debitCreditAdjust.getId(), um.getOrgId());

            if (debitCreditPayment == null) {
                throw new GlobalExceptionHandler.NotFoundException("Debit adjustment payment not found");
            }

            // 1. Save payment record
            DebitCreditPayment payment = new DebitCreditPayment();
            payment.setParentId(debitCreditPayment.getId());
            payment.setCreditDebitAdjId(debitCreditAdjust.getId());
            payment.setCredit(paymentAmount);
            payment.setBalance(returnBalance);
            payment.setDebt(BigDecimal.ZERO);
            payment.setOrgId(um.getOrgId());

            int res = mapper.insertDebtCreditPayment(payment);
            if(res == 0){
                throw new GlobalExceptionHandler.NotFoundException("Debt Reconciliation" + status.getRegFailureDesc());
            }
            // 2. Update penalty balance
            BigDecimal newBalance = currentBalance.subtract(paymentAmount);
            String newStatus = newBalance.compareTo(BigDecimal.ZERO) == 0 ? "PAID" : "PARTIALLY_PAID";

            // Persist the updated values
            int resp = mapper.updateReconciledDebt(debitCreditAdjust.getId(), newBalance, newStatus);
            if(resp == 0){
                throw new GlobalExceptionHandler.NotFoundException("Balance" + status.getUpdateFailureDesc());
            }

//            DebitCreditAdjust debitAdjustment = mapper.getDebitAdjustmentByMeterIdAndLcId(meterId, liabilityCauseId, um.getOrgId());
            DebitCreditAdjust debitAdjustment = mapper.getDebitAdjustmentById(debitCreditAdjust.getId(), um.getOrgId());
            String desc = capitalizeFirstLetter(debitAdjustment.getLiabilityCause().getName())+" debt reconcile "+newStatus;
            um.setPassword("");
//            handleAddCache(debitAdjustment);
            AuditLog auditLog = buildAuditLog(um, desc, "debit-credit", debitAdjustment, metadata);
            safeAuditService.saveAudit(auditLog);
            return ResponseMap.response(status.getSuccessCode(), "Payment reconciliation successful", "");

        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Reconciliation dept service failed");
            genericHandler.logAndSaveException(exception, "reconcile dept");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getMeterAndLiabilityCause(String meterNumber, String accountNumber) {
        try {
            Meter meter = null;
            UserModel um = handleUserValidation();

            if (meterNumber == null && accountNumber == null) {
                throw new GlobalExceptionHandler.NotFoundException("At least one of meterNumber or accountNumber must be provided.");
            }

            if(meterNumber != null){
                meter = mapper.getMeterNumber(um.getOrgId(), meterNumber);
            }

            if(accountNumber != null){
                meter = mapper.getAccountNumber(um.getOrgId(), accountNumber);
            }

            List<LiabilityCause> liabilityCause = mapper.getLiabilityCause(um.getOrgId());

            if(liabilityCause == null){
                throw new GlobalExceptionHandler.NotFoundException("Liability cause not found");
            }

            MeterAndLiabilityCause meterAndLiabilityCause = new MeterAndLiabilityCause();
            meterAndLiabilityCause.setMeter(meter);
            meterAndLiabilityCause.setLiabilityCause(liabilityCause);

            return ResponseMap.response(status.getSuccessCode(),  "Meter " + status.getDesc(), meterAndLiabilityCause);
        } catch (Exception exception) {
            log.error("Error occurred while fetching feeder lines [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Fetching meter & liability causes service failed");
            genericHandler.logAndSaveException(exception, "fetch meter liability cause");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getDebitAdjustmentPaymentHistory(
            UUID meterId, UUID liabilityCauseId, String type) {

        try {

            UserModel um = handleUserValidation();

            if (!type.equalsIgnoreCase("debit") &&
                    !type.equalsIgnoreCase("credit")) {

                throw new GlobalExceptionHandler.NotFoundException(
                        "Type parameter (" + type + ") not supported"
                );
            }

            List<DebitCreditPayment> response =
                    mapper.FetchDebitCreditPaymentHistory(
                            meterId,
                            liabilityCauseId,
                            type,
                            um.getOrgId()
                    );

            return ResponseMap.response(
                    status.getSuccessCode(),
                    type + " " + status.getDesc(),
                    response
            );

        } catch (Exception exception) {

            log.error("Error occurred while fetching debit/credit payment history: {}",
                    exception.getMessage().trim(), exception);

            genericHandler.logIncidentReport("Fetching debit/credit payment history failed");
            genericHandler.logAndSaveException(exception, "fetch debit/credit payment history");

            throw exception;
        }
    }

//    @Override
//    public Map<String, Object> debitCreditAdjustmentBulkUpload(MultipartFile file) throws IOException {
//        UserModel user = handleUserValidation();
//        try {
//        // Determine file type
//        String filename = Optional.ofNullable(file.getOriginalFilename())
//                .orElseThrow(() -> new IOException("File has no name"));
//            UUID nodeId = user.getNodeInfo().getNodeId();
//            String nodeType = user.getNodeInfo().getType();
//
//            if(!nodeType.equalsIgnoreCase("Business hub")
//                    && !nodeType.equalsIgnoreCase("Service center")
//                    && !nodeType.equalsIgnoreCase("Region")){
//                throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
//            }
//
//        List<DebitCreditAdjust> debitCreditAdjusts;
//        if (filename.endsWith(".csv")) {
//            debitCreditAdjusts = processDebitCreditAdjustCsv(file.getInputStream());
//        } else if (filename.endsWith(".xlsx")) {
//            debitCreditAdjusts = processDebitCreditAdjustExcel(file.getInputStream());
//        } else {
//            throw new IOException("Unsupported file format. Only .csv or .xlsx allowed.");
//        }
//
//        Map<String, Object> result = bulkCreateDebitAdjustment(debitCreditAdjusts, user);
//
//
//        // --- Prepare payments for new adjustments only ---
////        List<DebitCreditAdjust> successfulAdjustments = (List<DebitCreditAdjust>) result.get("successfulAdjustments");
//
////        bulkInsertDebtCreditPayments(successfulAdjustments, user);
//
//        return result;
//
//    } catch (Exception e) {
//        log.error("Error in bulk upload: {}", e.getMessage(), e);
//        genericHandler.logIncidentReport("Debit-credit-adjustment bulk upload failed");
//        genericHandler.logAndSaveException(e, "Bulk upload debit-credit-adjustment failed");
//        throw new IOException("Bulk upload failed: " + e.getMessage());
//    }
//    }

////    @Override
//    public Map<String, Object> bulkCreateDebitAdjustment(List<DebitCreditAdjust> requests, UserModel user) {
//
//        UserModel um = handleUserValidation();
//        UUID nodeId = um.getNodeInfo().getNodeId();
//        String nodeType = um.getNodeInfo().getType();
//
//        if(!nodeType.equalsIgnoreCase("Business hub")
//                && !nodeType.equalsIgnoreCase("Service center")){
//            throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
//        }
//
//        if (requests == null || requests.isEmpty()) {
//            throw new IllegalArgumentException("Request list cannot be empty");
//        }
//
//        List<GenericResp> failedRecords = new ArrayList<>();
//        List<DebitCreditAdjust> validRecords = new ArrayList<>();
//
//        // ===============================
//        // STEP 1: BASIC VALIDATION
//        // ===============================
//        for (DebitCreditAdjust request : requests) {
//
//            if (request.getMeterId() == null) {
//                failedRecords.add(buildFailure(request, "MeterId is required"));
//                continue;
//            }
//
//            if (request.getLiabilityCauseId() == null) {
//                failedRecords.add(buildFailure(request, "LiabilityCauseId is required"));
//                continue;
//            }
//
//            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
//                failedRecords.add(buildFailure(request, "Amount must be greater than zero"));
//                continue;
//            }
//
//            if (request.getType() == null ||
//                    (!request.getType().equalsIgnoreCase("credit")
//                            && !request.getType().equalsIgnoreCase("debit"))) {
//                failedRecords.add(buildFailure(request, "Type must be credit or debit"));
//                continue;
//            }
//
//            validRecords.add(request);
//        }
//
//        if (validRecords.isEmpty()) {
//            return ResponseMap.response("131", "All records failed validation", failedRecords);
//        }
//
//        // ===============================
//        // STEP 2: BULK FETCH DATA
//        // ===============================
//        List<UUID> meterIds = validRecords.stream()
//                .map(DebitCreditAdjust::getMeterId)
//                .distinct()
//                .toList();
//
//        List<UUID> liabilityIds = validRecords.stream()
//                .map(DebitCreditAdjust::getLiabilityCauseId)
//                .distinct()
//                .toList();
//
//        List<String> types = validRecords.stream()
//                .map(r -> r.getType().toLowerCase())
//                .distinct()
//                .toList();
//
//        Map<UUID, Meter> meterMap = mapper.getMetersByIds(meterIds)
//                .stream()
//                .collect(Collectors.toMap(Meter::getId, m -> m));
//
//        Map<UUID, LiabilityCause> liabilityMap =
//                mapper.getLiabilityCausesByIds(liabilityIds, um.getOrgId())
//                        .stream()
//                        .collect(Collectors.toMap(LiabilityCause::getId, l -> l));
//
//        List<DebitCreditAdjust> existingList =
//                mapper.findExistingAdjustments(meterIds, um.getOrgId(), liabilityIds, types);
//
//        Map<String, DebitCreditAdjust> existingMap = existingList.stream()
//                .collect(Collectors.toMap(
//                        a -> a.getMeterId() + "_" + a.getLiabilityCauseId() + "_" + a.getType().toLowerCase(),
//                        a -> a
//                ));
//
//        // ===============================
//        // STEP 3: PROCESS RECORDS
//        // ===============================
//        List<DebitCreditAdjust> toInsert = new ArrayList<>();
//        List<DebitCreditPayment> payments = new ArrayList<>();
//
//        LocalDateTime now = LocalDateTime.now();
//
//        int successCount = 0;
//
//        for (DebitCreditAdjust request : validRecords) {
//
//            Meter meter = meterMap.get(request.getMeterId());
//            if (meter == null) {
//                failedRecords.add(buildFailure(request, "Meter not found"));
//                continue;
//            }
//
//            if(!meter.getNodeId().equals(nodeId)
//                    && !meter.getServiceCenter().equals(nodeId)
//                    && !meter.getRoot().equals(nodeId)){
//                failedRecords.add(buildFailure(request, "No permission for meter"));
//                continue;
//            }
//
//            LiabilityCause lc = liabilityMap.get(request.getLiabilityCauseId());
//            if (lc == null) {
//                failedRecords.add(buildFailure(request, "Liability cause not found"));
//                continue;
//            }
//
//            String key = request.getMeterId() + "_" + request.getLiabilityCauseId() + "_" + request.getType().toLowerCase();
//            DebitCreditAdjust existing = existingMap.get(key);
//
//            BigDecimal newBalance;
//
//            if (existing != null) {
//                // ===============================
//                // UPDATE EXISTING
//                // ===============================
//                int rows = mapper.addCreditDebitAdjustment(
//                        existing.getId(),
//                        request.getAmount(),
//                        request.getAmount()
//                );
//
//                if (rows == 0) {
//                    failedRecords.add(buildFailure(request, "Update failed"));
//                    continue;
//                }
//
//                newBalance = existing.getBalance().add(request.getAmount());
//                request.setId(existing.getId());
//
//            } else {
//                // ===============================
//                // CREATE NEW
//                // ===============================
//                request.setId(UUID.randomUUID());
//                request.setOrgId(um.getOrgId());
//                request.setStatus("UNPAID");
//                request.setCreatedAt(now);
//                request.setUpdatedAt(now);
//
//                toInsert.add(request);
//
//                newBalance = request.getAmount();
//            }
//
//            // ===============================
//            // PAYMENT CREATION
//            // ===============================
//            DebitCreditPayment payment = new DebitCreditPayment();
//            payment.setCreditDebitAdjId(request.getId());
//            payment.setCredit(request.getType().equalsIgnoreCase("credit")
//                    ? request.getAmount() : BigDecimal.ZERO);
//            payment.setDebt(request.getType().equalsIgnoreCase("debit")
//                    ? request.getAmount() : BigDecimal.ZERO);
//            payment.setBalance(newBalance);
//            payment.setOrgId(um.getOrgId());
//            payment.setCreatedAt(now);
//
//            payments.add(payment);
//
//            successCount++;
//        }
//
//        // ===============================
//        // STEP 4: BULK INSERT
//        // ===============================
//        if (!toInsert.isEmpty()) {
//            mapper.bulkInsertDebitCreditAdjust(toInsert);
//        }
//
//        if (!payments.isEmpty()) {
//            mapper.bulkInsertDebtCreditPayments(payments);
//        }
//
//        // ===============================
//        // STEP 5: RESPONSE
//        // ===============================
//        Map<String, Object> result = new HashMap<>();
//        result.put("totalRecords", requests.size());
//        result.put("successCount", successCount);
//        result.put("failedCount", failedRecords.size());
//        result.put("failedRecords", failedRecords);
//
//        if (!failedRecords.isEmpty()) {
//            return ResponseMap.response(
//                    "131",
//                    failedRecords.size() + " records failed",
//                    result
//            );
//        }
//
//        return ResponseMap.response(
//                status.getSuccessCode(),
//                successCount + " records processed successfully",
//                result
//        );
//    }

    @Transactional
    @Override
    public Map<String, Object> debitCreditAdjustmentBulkUpload(MultipartFile file) throws IOException {
        UserModel user = handleUserValidation();
        UUID nodeId = user.getNodeInfo().getNodeId();
        String nodeType = user.getNodeInfo().getType();

        if (!nodeType.equalsIgnoreCase("Business hub")
                && !nodeType.equalsIgnoreCase("Service center")
                && !nodeType.equalsIgnoreCase("Region")) {
            throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
        }

        String filename = Optional.ofNullable(file.getOriginalFilename())
                .orElseThrow(() -> new IOException("File has no name"));

        List<DebitCreditAdjust> adjustments;
        if (filename.endsWith(".csv")) {
            adjustments = processDebitCreditAdjustCsv(file.getInputStream());
        } else if (filename.endsWith(".xlsx")) {
            adjustments = processDebitCreditAdjustExcel(file.getInputStream());
        } else {
            throw new IOException("Unsupported file format. Only .csv or .xlsx allowed.");
        }

        return bulkCreateDebitAdjustment(adjustments, user);
    }

    public Map<String, Object> bulkCreateDebitAdjustment(List<DebitCreditAdjust> requests, UserModel user) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Request list cannot be empty");
        }

        List<GenericResp> failedRecords = new ArrayList<>();
        List<DebitCreditAdjust> validRecords = new ArrayList<>();

        // STEP 1: BASIC VALIDATION
        for (DebitCreditAdjust r : requests) {
            if (r.getMeterNumber() == null) {
                failedRecords.add(buildFailure(r, "Meter number is required"));
                continue;
            }
            if (r.getCode() == null) {
                failedRecords.add(buildFailure(r, "Liability cause code is required"));
                continue;
            }
            if (r.getAmount() == null || r.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                failedRecords.add(buildFailure(r, "Amount must be > 0"));
                continue;
            }
            if (r.getType() == null || (!r.getType().equalsIgnoreCase("credit") && !r.getType().equalsIgnoreCase("debit"))) {
                failedRecords.add(buildFailure(r, "Type must be credit or debit"));
                continue;
            }
            validRecords.add(r);
        }

        if (validRecords.isEmpty()) {
            return ResponseMap.response("131", "All records failed validation", failedRecords);
        }

        // STEP 2: MAP METER NUMBERS TO IDs
        List<String> meterNumbers = validRecords.stream()
                .map(r -> r.getMeterNumber().trim())  // trim spaces
                .distinct()
                .toList();

        Map<String, Meter> meterMap = mapper.getMetersByNumbers(meterNumbers)
                .stream()
                .collect(Collectors.toMap(
                        m -> m.getMeterNumber().trim(),  // trim DB values
                        m -> m
                ));

        for (DebitCreditAdjust r : validRecords) {
            String meterNumberKey = r.getMeterNumber().trim();
            Meter meter = meterMap.get(meterNumberKey);

            if (meter == null) {
                System.out.println("Meter not found for number: " + meterNumberKey);
                failedRecords.add(buildFailure(r, "Meter not found"));
                continue;  // skip this record
            }

            r.setMeterId(meter.getId()); // safe to set now
        }

//        successCount + " of " + totalRecords + " Meters uploaded successfully",

        validRecords.removeIf(r -> r.getMeterId() == null);
        if (validRecords.isEmpty()) {
            return ResponseMap.response("131", "All records failed meter mapping", failedRecords);
        }

        // STEP 3: FETCH LIABILITY CAUSES
        List<String> liabilityCodes = validRecords.stream()
                .map(DebitCreditAdjust::getCode)
                .distinct()
                .toList();

        List<LiabilityCause> liabilityCauses = mapper.getLiabilityCausesByIds(liabilityCodes, user.getOrgId());
        Map<String, LiabilityCause> liabilityMap = liabilityCauses.stream()
                .collect(Collectors.toMap(LiabilityCause::getCode, l -> l));

        for (DebitCreditAdjust r : validRecords) {
            LiabilityCause lc = liabilityMap.get(r.getCode());
            if (lc == null) {
                failedRecords.add(buildFailure(r, "Liability cause not found"));
                continue;
            }
            r.setLiabilityCauseId(lc.getId());  // Set UUID for DB insert
        }

        validRecords.removeIf(r -> r.getMeterId() == null || r.getLiabilityCauseId() == null);

        // STEP 4: PROCESS NEW/EXISTING ADJUSTMENTS
        LocalDateTime now = LocalDateTime.now();
        List<DebitCreditAdjust> toInsert = new ArrayList<>();
        List<DebitCreditPayment> payments = new ArrayList<>();

        List<UUID> meterIds = validRecords.stream().map(DebitCreditAdjust::getMeterId).distinct().toList();
        List<UUID> liabilityIds = validRecords.stream().map(DebitCreditAdjust::getLiabilityCauseId).distinct().toList();
        List<String> types = validRecords.stream().map(r -> r.getType().toLowerCase()).distinct().toList();

        List<DebitCreditAdjust> existingList = mapper.findExistingAdjustments(
                meterIds, user.getOrgId(), liabilityIds, types
        );

        Map<String, DebitCreditAdjust> existingMap = existingList.stream()
                .collect(Collectors.toMap(
                        a -> a.getMeterId() + "_" + a.getLiabilityCauseId() + "_" + a.getType().toLowerCase(),
                        a -> a
                ));

        System.out.println("existingMap: "+existingMap);

        int successCount = 0;

        List<DebitCreditAdjust> updates = new ArrayList<>();
        for (DebitCreditAdjust r : validRecords) {
            BigDecimal newBalance;
            String key = r.getMeterId() + "_" + r.getLiabilityCauseId() + "_" + r.getType().toLowerCase();
            DebitCreditAdjust existing = existingMap.get(key);
            if (existing != null) {
                DebitCreditAdjust upd = new DebitCreditAdjust();
                upd.setId(existing.getId());
                upd.setAmount(r.getAmount());
//                upd.setBalance(r.getType().equalsIgnoreCase("debit") ? r.getAmount() : BigDecimal.ZERO);
                upd.setUpdatedAt(LocalDateTime.now());
                updates.add(upd);

                r.setId(existing.getId());
//                r.setBalance(existing.getBalance().add(r.getAmount()));
                newBalance = existing.getBalance().add(r.getAmount());
            } else {
                // NEW ADJUSTMENT
                r.setId(UUID.randomUUID());
                r.setOrgId(user.getOrgId());
                r.setStatus("UNPAID");
                r.setCreatedAt(now);
                r.setUpdatedAt(now);
                r.setBalance(r.getAmount());
                toInsert.add(r);
                newBalance = r.getAmount();
            }

            // CREATE PAYMENT
            DebitCreditPayment p = new DebitCreditPayment();
            p.setCreditDebitAdjId(r.getId());
            p.setCredit(r.getType().equalsIgnoreCase("credit") ? r.getAmount() : BigDecimal.ZERO);
            p.setDebt(r.getType().equalsIgnoreCase("debit") ? r.getAmount() : BigDecimal.ZERO);
            p.setBalance(newBalance);
            p.setOrgId(user.getOrgId());
            p.setCreatedAt(now);
            payments.add(p);

            successCount++;
        }
//        for (DebitCreditAdjust r : validRecords) {
//            String key = r.getMeterId() + "_" + r.getLiabilityCauseId() + "_" + r.getType().toLowerCase();
//            DebitCreditAdjust existing = existingMap.get(key);
//            BigDecimal newBalance;
//            System.out.println("existing: "+existing);
//            System.out.println("id1: "+existing.getId());
//            System.out.println("lc1: "+existing.getLiabilityCauseId());
//
//            if (existing != null) {
//                System.out.println("id: "+existing.getId());
//                System.out.println("lc: "+existing.getLiabilityCauseId());
//                // UPDATE EXISTING
//                mapper.updateAdjustmentBalance(existing.getId(), r.getAmount(), r.getAmount(), r.getUpdatedAt());
//                r.setId(existing.getId());
//                newBalance = existing.getBalance().add(r.getAmount());
//            } else {
//                // NEW ADJUSTMENT
//                r.setId(UUID.randomUUID());
//                r.setOrgId(user.getOrgId());
//                r.setStatus("UNPAID");
//                r.setCreatedAt(now);
//                r.setUpdatedAt(now);
//                r.setBalance(r.getAmount());
//                toInsert.add(r);
//                newBalance = r.getAmount();
//            }
//
//            // CREATE PAYMENT
//            DebitCreditPayment p = new DebitCreditPayment();
//            p.setCreditDebitAdjId(r.getId());
//            p.setCredit(r.getType().equalsIgnoreCase("credit") ? r.getAmount() : BigDecimal.ZERO);
//            p.setDebt(r.getType().equalsIgnoreCase("debit") ? r.getAmount() : BigDecimal.ZERO);
//            p.setBalance(newBalance);
//            p.setOrgId(user.getOrgId());
//            p.setCreatedAt(now);
//            payments.add(p);
//
//            successCount++;
//        }

        // STEP 5: BULK INSERT
        if (!updates.isEmpty()) mapper.bulkUpdateAdjustmentBalance(updates);
        if (!toInsert.isEmpty()) mapper.bulkInsertDebitCreditAdjust(toInsert);
        if (!payments.isEmpty()) mapper.bulkInsertDebtCreditPayments(payments);

        // STEP 6: RESPONSE
        Map<String, Object> result = new HashMap<>();
        result.put("totalRecords", requests.size());
        result.put("successCount", successCount);
        result.put("failedCount", failedRecords.size());
        result.put("failedRecords", failedRecords);

        if (!failedRecords.isEmpty()) {
            return ResponseMap.response("131", failedRecords.size() + " of "+ requests.size() + " credit-debit adjustment upload failed", result);
        }
        return ResponseMap.response(status.getSuccessCode(), successCount + " of "+ requests.size() + " credit-debit adjustment uploaded successfully", result);
    }

//    public Map<String, Object> bulkCreateDebitAdjustment(List<DebitCreditAdjust> requests, UserModel user) {
//
//        UUID nodeId = user.getNodeInfo().getNodeId();
//        String nodeType = user.getNodeInfo().getType();
//
//        if(!nodeType.equalsIgnoreCase("Business hub")
//                && !nodeType.equalsIgnoreCase("Service center")){
//            throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
//        }
//
//        if (requests == null || requests.isEmpty()) {
//            throw new IllegalArgumentException("Request list cannot be empty");
//        }
//
//        List<GenericResp> failedRecords = new ArrayList<>();
//        List<DebitCreditAdjust> validRecords = new ArrayList<>();
//
//        // ===============================
//        // STEP 1: VALIDATE RAW INPUT (NOT IDs)
//        // ===============================
//        for (DebitCreditAdjust request : requests) {
//
//            if (isBlank(request.getMeterNumber())) {
//                failedRecords.add(buildFailure(request, "Meter number is required"));
//                continue;
//            }
//
//            if (isBlank(request.getCode())) {
//                failedRecords.add(buildFailure(request, "Code is required"));
//                continue;
//            }
//
//            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
//                failedRecords.add(buildFailure(request, "Amount must be greater than zero"));
//                continue;
//            }
//
//            if (request.getType() == null ||
//                    (!request.getType().equalsIgnoreCase("credit")
//                            && !request.getType().equalsIgnoreCase("debit"))) {
//                failedRecords.add(buildFailure(request, "Type must be credit or debit"));
//                continue;
//            }
//
//            validRecords.add(request);
//        }
//
//        if (validRecords.isEmpty()) {
//            return ResponseMap.response("131", "All records failed validation", failedRecords);
//        }
//
//        // ===============================
//        // STEP 2: BULK FETCH (IMPORTANT)
//        // ===============================
//        List<String> meterNumbers = validRecords.stream()
//                .map(DebitCreditAdjust::getMeterNumber)
//                .distinct()
//                .toList();
//
//        List<String> codes = validRecords.stream()
//                .map(DebitCreditAdjust::getCode)
//                .distinct()
//                .toList();
//
//        Map<String, UUID> meterMap = mapper.findMetersByNumbers(meterNumbers, user.getOrgId())
//                .stream()
//                .collect(Collectors.toMap(
//                        m -> (String) m.get("meter_number"),
//                        m -> (UUID) m.get("id")
//                ));
//
//        Map<String, UUID> liabilityMap = mapper.findLiabilityByCodes(codes, user.getOrgId())
//                .stream()
//                .collect(Collectors.toMap(
//                        l -> (String) l.get("code"),
//                        l -> (UUID) l.get("id")
//                ));
//
//        // ===============================
//        // STEP 3: MAP IDs
//        // ===============================
//        List<DebitCreditAdjust> finalValidList = new ArrayList<>();
//
//        for (DebitCreditAdjust request : validRecords) {
//
//            UUID meterId = meterMap.get(request.getMeterNumber());
//            if (meterId == null) {
//                failedRecords.add(buildFailure(request, "Meter not found"));
//                continue;
//            }
//
//            UUID liabilityId = liabilityMap.get(request.getCode());
//            if (liabilityId == null) {
//                failedRecords.add(buildFailure(request, "Liability code not found"));
//                continue;
//            }
//
//            request.setMeterId(meterId);
//            request.setLiabilityCauseId(liabilityId);
//            request.setOrgId(user.getOrgId());
//
//            finalValidList.add(request);
//        }
//
//        if (finalValidList.isEmpty()) {
//            return ResponseMap.response("131", "All records failed mapping", failedRecords);
//        }
//
//        // ===============================
//        // STEP 4: FETCH EXISTING
//        // ===============================
//        List<UUID> meterIds = finalValidList.stream().map(DebitCreditAdjust::getMeterId).distinct().toList();
//        List<UUID> liabilityIds = finalValidList.stream().map(DebitCreditAdjust::getLiabilityCauseId).distinct().toList();
//        List<String> types = finalValidList.stream().map(r -> r.getType().toLowerCase()).distinct().toList();
//
//        List<DebitCreditAdjust> existingList =
//                mapper.findExistingAdjustments(meterIds, user.getOrgId(), liabilityIds, types);
//
//        Map<String, DebitCreditAdjust> existingMap = existingList.stream()
//                .collect(Collectors.toMap(
//                        a -> a.getMeterId() + "_" + a.getLiabilityCauseId() + "_" + a.getType().toLowerCase(),
//                        a -> a
//                ));
//
//        // ===============================
//        // STEP 5: PROCESS
//        // ===============================
//        List<DebitCreditAdjust> toInsert = new ArrayList<>();
//        List<DebitCreditPayment> payments = new ArrayList<>();
//
//        LocalDateTime now = LocalDateTime.now();
//        int successCount = 0;
//
//        for (DebitCreditAdjust request : finalValidList) {
//
//            String key = request.getMeterId() + "_" +
//                    request.getLiabilityCauseId() + "_" +
//                    request.getType().toLowerCase();
//
//            DebitCreditAdjust existing = existingMap.get(key);
//            BigDecimal newBalance;
//
//            if (existing != null) {
//
//                int rows = mapper.addCreditDebitAdjustment(
//                        existing.getId(),
//                        request.getAmount(),
//                        request.getAmount()
//                );
//
//                if (rows == 0) {
//                    failedRecords.add(buildFailure(request, "Update failed"));
//                    continue;
//                }
//
//                newBalance = existing.getBalance().add(request.getAmount());
//                request.setId(existing.getId());
//
//            } else {
//
//                request.setId(UUID.randomUUID());
//                request.setStatus("UNPAID");
//                request.setCreatedAt(now);
//                request.setUpdatedAt(now);
//
//                toInsert.add(request);
//                newBalance = request.getAmount();
//            }
//
//            // PAYMENT
//            DebitCreditPayment payment = new DebitCreditPayment();
//            payment.setCreditDebitAdjId(request.getId());
//            payment.setCredit(request.getType().equalsIgnoreCase("credit") ? request.getAmount() : BigDecimal.ZERO);
//            payment.setDebt(request.getType().equalsIgnoreCase("debit") ? request.getAmount() : BigDecimal.ZERO);
//            payment.setBalance(newBalance);
//            payment.setOrgId(user.getOrgId());
//            payment.setCreatedAt(now);
//
//            payments.add(payment);
//            successCount++;
//        }
//
//        // ===============================
//        // STEP 6: SAVE
//        // ===============================
//        if (!toInsert.isEmpty()) {
//            mapper.bulkInsertDebitCreditAdjust(toInsert);
//        }
//
//        if (!payments.isEmpty()) {
//            mapper.bulkInsertDebtCreditPayments(payments);
//        }
//
//        // ===============================
//        // STEP 7: RESPONSE
//        // ===============================
//        Map<String, Object> result = new HashMap<>();
//        result.put("totalRecords", requests.size());
//        result.put("successCount", successCount);
//        result.put("failedCount", failedRecords.size());
//        result.put("failedRecords", failedRecords);
//
//        return ResponseMap.response(
//                failedRecords.isEmpty() ? status.getSuccessCode() : "131",
//                successCount + " processed, " + failedRecords.size() + " failed",
//                result
//        );
//    }

///
//    private Map<String, Object> bulkInsertDebitCreditAdjust(List<DebitCreditAdjust> debitCreditAdjusts, UserModel user) {
//        Map<String, Object> result = new HashMap<>();
//        List<GenericResp> failedRecords = new ArrayList<>();
//
//        if (debitCreditAdjusts == null || debitCreditAdjusts.isEmpty()) {
//            throw new IllegalArgumentException("Debit Credit adjustment list cannot be empty");
//        }
//
//        int successCount = 0;
//
//        int batchSize = 500; // try 500–1000 for optimal JDBC performance
//        List<DebitCreditAdjust> successfulAdjustments = new ArrayList<>();
//        for (int i = 0; i < debitCreditAdjusts.size(); i += batchSize) {
//            int end = Math.min(i + batchSize, debitCreditAdjusts.size());
////            List<Customer> batch = customers.subList(i, end);
//            // IMPORTANT: create copy to avoid modifying original list
//            List<DebitCreditAdjust> batch = new ArrayList<>(debitCreditAdjusts.subList(i, end));
//            try {
//                insertBatchTransactional(batch, user,failedRecords);
//                successfulAdjustments.addAll(batch);
//                successCount += batch.size();
//                log.info("Batch {} processed successfully", (i / batchSize) + 1);
//
//            } catch (Exception e) {
//                log.warn("Batch {} failed. Retrying with sub-batches. Reason: {}",
//                        (i / batchSize) + 1, e.getMessage());
//
//                // Attempt smaller sub-batches to isolate failure
////                successCount += insertSubBatchTransactional(batch, user, failedRecords);
//            }
//        }
//
//        final int totalRecords = debitCreditAdjusts.size();
//
//        result.put("totalRecords", debitCreditAdjusts.size());
//        result.put("successCount", successCount);
//        result.put("failedCount", failedRecords.size());
//        result.put("failedRecords", failedRecords);
//
//        if (!failedRecords.isEmpty()) {
//            return ResponseMap.response(
//                    "131",
//                    failedRecords.size() + " of " + totalRecords + " debit or credit upload failed",
//                    result
//            );
//        }
//
//        return ResponseMap.response(
//                status.getSuccessCode(),
//                successCount + " of " + totalRecords + " debit or credit uploaded successfully",
//                result
//        );
//    }
//
//    private void bulkInsertDebtCreditPayments(List<DebitCreditAdjust> adjustments, UserModel user) {
//        if (adjustments == null || adjustments.isEmpty()) return;
//
//        LocalDateTime now = LocalDateTime.now();
//
//        List<DebitCreditPayment> payments = adjustments.stream().map(adj -> {
//            DebitCreditPayment p = new DebitCreditPayment();
//            p.setParentId(null);
//            p.setCreditDebitAdjId(adj.getId());
//            p.setCredit(adj.getType().equalsIgnoreCase("credit") ? adj.getAmount() : BigDecimal.ZERO);
//            p.setDebt(adj.getType().equalsIgnoreCase("debit") ? adj.getAmount() : BigDecimal.ZERO);
//            p.setBalance(adj.getAmount());
//            p.setOrgId(adj.getOrgId());
//            p.setCreatedAt(now);
//            return p;
//        }).toList();
//
//        mapper.bulkInsertDebtCreditPayments(payments);
//    }
//
//    private List<DebitCreditAdjust> insertBatchTransactional(List<DebitCreditAdjust> batch, UserModel user, List<GenericResp> failedRecords) {
//
////        prepareDebitCredits(batch, user, failedRecords);
//        // Validate & prepare debit credits; returns only valid records
//        List<DebitCreditAdjust> validRecords = prepareDebitCredits(batch, user, failedRecords);
//
//        if (validRecords.isEmpty()) return Collections.emptyList();
//
//        LocalDateTime now = LocalDateTime.now();
//
//        // --- Collect unique IDs for batch query ---
//        List<UUID> meterIds = validRecords.stream().map(DebitCreditAdjust::getMeterId).distinct().toList();
//        List<UUID> liabilityIds = validRecords.stream().map(DebitCreditAdjust::getLiabilityCauseId).distinct().toList();
//        List<String> types = validRecords.stream().map(DebitCreditAdjust::getType).distinct().toList();
//
//        // --- Fetch existing adjustments in bulk ---
//        List<DebitCreditAdjust> existingAdjustments = mapper.findExistingAdjustments(meterIds, user.getOrgId(), liabilityIds, types);
//
//        Map<String, DebitCreditAdjust> existingMap = existingAdjustments.stream()
//                .collect(Collectors.toMap(
//                        a -> a.getMeterId() + "_" + a.getLiabilityCauseId() + "_" + a.getType().toLowerCase(),
//                        a -> a
//                ));
//
//        List<DebitCreditAdjust> toUpdate = new ArrayList<>();
//        List<DebitCreditAdjust> toInsert = new ArrayList<>();
//        List<DebitCreditPayment> toInsertPayment = new ArrayList<>();
//
//        for (DebitCreditAdjust adjust : validRecords) {
//            String key = adjust.getMeterId() + "_" + adjust.getLiabilityCauseId() + "_" + adjust.getType().toLowerCase();
//            DebitCreditAdjust existing = existingMap.get(key);
//
//            if (existing != null) {
//                // Prepare for bulk update
//                existing.setAmount(adjust.getAmount()); // amount to add
//                existing.setUpdatedAt(now);
//                toUpdate.add(existing);
//            } else {
////                DebitCreditPayment payment = new DebitCreditPayment();
////                payment.setCreditDebitAdjId(request.getId());
////                payment.setCredit(request.getType().equalsIgnoreCase("credit")
////                        ? request.getAmount() : BigDecimal.ZERO);
////                payment.setBalance(AdjustResult != null ? request.getBalance() : request.getAmount());
////                payment.setDebt(request.getType().equalsIgnoreCase("credit")
////                        ? BigDecimal.ZERO : request.getAmount());
////                payment.setOrgId(um.getOrgId());
////
//                // Prepare for bulk insert
////                adjust.setId(UUID.randomUUID());
////                adjust.setCreatedAt(now);
////                adjust.setUpdatedAt(now);
//                toInsert.add(adjust);
//            }
//        }
//
//        // --- Bulk update existing adjustments ---
//        if (!toUpdate.isEmpty()) {
//            mapper.bulkUpdateAdjustments(toUpdate);
//        }
//
//        // --- Bulk insert new adjustments ---
//        if (!toInsert.isEmpty()) {
//            mapper.bulkInsertDebitCreditAdjust(toInsert);
//        }
//
//        // Audit
//        auditBatch(validRecords, user, "Adjustment created/updated in bulk");
//
//        List<DebitCreditAdjust> allProcessed = new ArrayList<>();
//        allProcessed.addAll(toInsert);
//        allProcessed.addAll(toUpdate);
//        return allProcessed;
//    }
//
//    private List<DebitCreditAdjust> prepareDebitCredits(
//            List<DebitCreditAdjust> batch,
//            UserModel user,
//            List<GenericResp> failedRecords) {
//
//        // Fetch meters and liability causes
//        List<String> meterNumbers = batch.stream().map(DebitCreditAdjust::getMeterNumber)
//                .filter(Objects::nonNull).distinct().toList();
//
//        List<String> codes = batch.stream().map(DebitCreditAdjust::getCode)
//                .filter(Objects::nonNull).distinct().toList();
//
//        Map<String, UUID> meterMap = mapper.findMetersByNumbers(meterNumbers, user.getOrgId()).stream()
//                .collect(Collectors.toMap(m -> (String) m.get("meter_number"), m -> (UUID) m.get("id")));
//
//        Map<String, UUID> liabilityMap = mapper.findLiabilityByCodes(codes, user.getOrgId()).stream()
//                .collect(Collectors.toMap(l -> (String) l.get("code"), l -> (UUID) l.get("id")));
//
//        List<DebitCreditAdjust> validAdjustments = new ArrayList<>();
//
//        for (DebitCreditAdjust adjust : batch) {
//
//            if (isBlank(adjust.getMeterNumber())) {
//                failedRecords.add(buildFailure(adjust, "Meter number is required")); continue;
//            }
//            if (isBlank(adjust.getCode())) {
//                failedRecords.add(buildFailure(adjust, "Code is required")); continue;
//            }
//            if (adjust.getAmount() == null || adjust.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
//                failedRecords.add(buildFailure(adjust, "Amount must be greater than zero")); continue;
//            }
//            if (isBlank(adjust.getType()) || (!adjust.getType().equalsIgnoreCase("credit") && !adjust.getType().equalsIgnoreCase("debit"))) {
//                failedRecords.add(buildFailure(adjust, "Type parameter not supported")); continue;
//            }
//
//            UUID meterId = meterMap.get(adjust.getMeterNumber());
//            UUID liabilityId = liabilityMap.get(adjust.getCode());
//
//            if (meterId == null) { failedRecords.add(buildFailure(adjust, "Meter not found")); continue; }
//            if (liabilityId == null) { failedRecords.add(buildFailure(adjust, "Liability code not found")); continue; }
//
//            System.out.println("meterId: "+meterId);
//            System.out.println("meterNumber: "+adjust.getMeterNumber());
//            // Set resolved IDs and system fields
//            adjust.setMeterId(meterId);
//            adjust.setLiabilityCauseId(liabilityId);
//            adjust.setOrgId(user.getOrgId());
//            adjust.setStatus("UNPAID");
//
//
//
//
//            validAdjustments.add(adjust);
//        }
//
//        return validAdjustments;
//    }
//
//    private int insertSubBatchTransactional(List<DebitCreditAdjust> batch, UserModel user, List<GenericResp> failedRecords) {
//        int successCount = 0;
//        int subBatchSize = 100;
//
//        for (int i = 0; i < batch.size(); i += subBatchSize) {
//            int end = Math.min(i + subBatchSize, batch.size());
////            List<Meter> subBatch = batch.subList(i, end);
//            List<DebitCreditAdjust> subBatch = new ArrayList<>(batch.subList(i, end));
//
//            try {
//                insertBatchTransactional(subBatch, user, failedRecords);
//                successCount += subBatch.size();
//            } catch (Exception e) {
//                log.warn("Sub-batch failed (size={}): {}", subBatch.size(), e.getMessage());
//
//                if (subBatch.size() > 50) {
//                    successCount += insertSinglesFallbackAsync(subBatch, user, failedRecords);
//                } else {
//                    successCount += insertSinglesFallback(subBatch, user, failedRecords);
//                }
//            }
//        }
//        return successCount;
//    }
//
//    private int insertSinglesFallback(List<DebitCreditAdjust> subBatch, UserModel user, List<GenericResp> failedRecords) {
//        List<CompletableFuture<Integer>> futures = new ArrayList<>();
//
//        for (DebitCreditAdjust debitCreditAdjust : subBatch) {
//            futures.add(insertSingleAsync(debitCreditAdjust, user, failedRecords));
//        }
//
//        // Wait for all tasks to complete
//        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//
//        // Sum successful inserts
//        return futures.stream().mapToInt(f -> f.join()).sum();
//    }
//
//    @Async("bulkUploadExecutor")
//    public CompletableFuture<Integer> insertSingleAsync(DebitCreditAdjust debitCreditAdjust, UserModel user, List<GenericResp> failedRecords) {
//        try {
//            insertSingleTransactional(debitCreditAdjust, user);
//            return CompletableFuture.completedFuture(1);
//        } catch (Exception e) {
//            String reason = extractErrorMessage(e);
//            GenericResp resp = new GenericResp();
//            resp.setId(debitCreditAdjust.getMeterNumber());
//            resp.setMessage("Debit-credit adjustment failed: "+reason);
//            resp.setData(debitCreditAdjust.getMeterNumber());
//
//            failedRecords.add(resp);
////            failedRecords.add(meter.getMeterNumber() + " (" + reason + ")");
//            log.warn("Async single insert failed for {}: {}", debitCreditAdjust.getMeterNumber(), reason);
//            return CompletableFuture.completedFuture(0);
//        }
//    }
//
//
//    private int insertSinglesFallbackAsync(List<DebitCreditAdjust> subBatch, UserModel user, List<GenericResp> failedRecords) {
//        int successCount = 0;
//
//        for (DebitCreditAdjust debitCreditAdjust : subBatch) {
//            try {
//                log.debug("Fallback single upload for adjustment: {}", debitCreditAdjust.getMeterNumber());
//                insertSingleTransactional(debitCreditAdjust, user);
//                successCount++;
//            } catch (Exception e) {
//                String reason = extractErrorMessage(e);
//                GenericResp resp = new GenericResp();
//                resp.setId(debitCreditAdjust.getMeterNumber());
//                resp.setMessage("Adjustment single save failed: "+reason);
//                resp.setData(debitCreditAdjust.getMeterNumber());
//
//                failedRecords.add(resp);
////                failedRecords.add(meter.getMeterNumber() + " (" + reason + ")");
//                log.warn("Adjustment {} failed individually: {}", debitCreditAdjust.getMeterNumber(), reason);
//            }
//        }
//
//        return successCount;
//    }
//
//    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
//    public void insertSingleTransactional(DebitCreditAdjust debitCreditAdjust, UserModel user) {
////        try {
//        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
//
//        // --- Step 1: Prepare core meter entity ---
//        debitCreditAdjust.setOrgId(user.getOrgId());
//
//        // --- Step 2: Insert into main + version tables ---
////        createDebitAdjustment(debitCreditAdjust);
//        mapper.createDebitAdjustment(debitCreditAdjust);
//
//        // --- Step 4: Audit logging ---
//        DebitCreditAdjust debitAdjustment = mapper.getDebitAdjustmentById(debitCreditAdjust.getId(), user.getOrgId());
////        Meter newMeter = mapper.findByIdVersion(debitCreditAdjust.getId(), user.getOrgId(), user.getNodeInfo().getNodeId());
//        AuditLog auditLog = buildAuditLog(user, "Adjustment created/updated in bulk-single","debit-credit", debitAdjustment, metadata);
//        safeAuditService.saveAudit(auditLog);
//
////        } catch (Exception e) {
////            log.error("Failed to insert meter {}: {}", meter.getMeterNumber(), e.getMessage(), e);
////            throw e; // rethrow so parent caller can track failure count
////        }
//    }

    private String extractErrorMessage(Exception e) {
        String message = e.getMessage();

        if (message == null) return "Unknown error";

        if (message.contains("violates not-null constraint")) {
            return "Missing required field — one or more mandatory columns are empty.";
        }
        if (message.contains("foreign key constraint")) {
            return "Invalid reference — linked data does not exist.";
        }
        if (message.contains("invalid input syntax")) {
            return "Invalid data type — check number or date format.";
        }

        // default fallback
        return message.split("\n")[0];
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private GenericResp buildFailure(DebitCreditAdjust adjust, String message) {

        GenericResp resp = new GenericResp();

        resp.setId(adjust.getMeterNumber());
        resp.setMessage(message);
        resp.setData(adjust);

        return resp;
    }

    private void auditBatch(List<DebitCreditAdjust> batch, UserModel user, String desc) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
        for (DebitCreditAdjust m : batch) {
            AuditLog auditLog = buildAuditLog(user, desc,"debit-credit ", m, metadata);
            safeAuditService.saveAudit(auditLog);
        }
    }

    private static List<DebitCreditAdjust> processDebitCreditAdjustExcel(InputStream inputStream) throws IOException {
        List<DebitCreditAdjust> debitCreditAdjusts = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip header row safely
            if (rows.hasNext()) {
                rows.next();
            }

            while (rows.hasNext()) {
                Row row = rows.next();
                DebitCreditAdjust debitCreditAdjust = new DebitCreditAdjust();

                debitCreditAdjust.setMeterNumber(getStringCellValue(row.getCell(0)).trim());
                debitCreditAdjust.setCode(getStringCellValue(row.getCell(1)).trim());
//                debitCreditAdjust.setAmount(BigDecimal.valueOf(Long.parseLong(String.valueOf(row.getCell(2)))));
                debitCreditAdjust.setAmount(getBigDecimalCellValue(row.getCell(2)));
                debitCreditAdjust.setType(getStringCellValue(row.getCell(3)).trim());


                debitCreditAdjusts.add(debitCreditAdjust);
            }
        }
        return debitCreditAdjusts;
    }

    private static BigDecimal getBigDecimalCellValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                return BigDecimal.valueOf(cell.getNumericCellValue());

            case STRING:
                String value = cell.getStringCellValue().trim();
                if (value.isEmpty()) return null;
                return new BigDecimal(value);

            case FORMULA:
                return BigDecimal.valueOf(cell.getNumericCellValue());

            default:
                return null;
        }
    }

    private static String getStringCellValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    private List<DebitCreditAdjust> processDebitCreditAdjustCsv(InputStream inputStream) throws IOException {
        List<DebitCreditAdjust> debitCreditAdjusts = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            for (CSVRecord record : csvParser) {
                DebitCreditAdjust debitCreditAdjust = new DebitCreditAdjust();
                debitCreditAdjust.setMeterNumber(record.get("Meter number".trim()));
                debitCreditAdjust.setCode(record.get("Liability cause code".trim().trim()));
                debitCreditAdjust.setAmount(BigDecimal.valueOf(Long.parseLong(record.get("Amount").trim())));
                debitCreditAdjust.setType(record.get("Type".trim()));

                debitCreditAdjusts.add(debitCreditAdjust);
            }
        }
        return debitCreditAdjusts;
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getDebitAdjustments(
            int page, int size, String type, String search, DebitCreditAdjust debitCreditAdjust) {
        try {

            UserModel um = handleUserValidation();
            UUID nodeId = um.getNodeInfo().getNodeId();

            List<Meter> allDebitCreditAdjustment;
            // Ideally, this should be a dynamic query in the mapper layer
            if(type.equalsIgnoreCase("debit")) {
                allDebitCreditAdjustment = mapper.GetDebitAdjustment(um.getOrgId(), page,size, nodeId);
            } else if (type.equalsIgnoreCase("credit")){
                allDebitCreditAdjustment = mapper.GetCreditAdjustment(um.getOrgId(), page,size, nodeId);
            } else {
                throw new GlobalExceptionHandler.NotFoundException("Type parameter (" + type + ") not supported");
            }

//            Meter meter = mapper.getMeterById(meterId);
//            if (meter == null) {
//                throw new GlobalExceptionHandler.NotFoundException("Meter not found or not assigned");
//            }
//
//            if(!meter.getNodeId().equals(nodeId)
//                    && !meter.getServiceCenter().equals(nodeId)
//                    && !meter.getRoot().equals(nodeId)){
//                throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
//            }


            List<Meter> filteredDebitCreditAdjustment =
                    allDebitCreditAdjustment.stream()
                            .filter(u ->
                                    search == null || search.isBlank()
                                            || (u.getAccountNumber() != null && u.getAccountNumber().toLowerCase().contains(search.toLowerCase()))
                                            || (u.getCustomerId() != null && u.getCustomerId().toLowerCase().contains(search.toLowerCase()))
                                            || (u.getMeterNumber() != null && u.getMeterNumber().toLowerCase().contains(search.toLowerCase()))
                                            || (u.getCustomer() != null && (
                                                    (u.getCustomer().getFirstname() != null && u.getCustomer().getFirstname().toLowerCase().contains(search.toLowerCase()))
                                                    || (u.getCustomer().getLastname() != null && u.getCustomer().getLastname().toLowerCase().contains(search.toLowerCase()))
                                            ))
                            )
                            .collect(Collectors.toList());



            // Pagination logic
            int totalDebitCreditAdjustment = filteredDebitCreditAdjustment.size();
            List<Meter> paginatedDebitCreditAdjustment;

            if (size <= 0) {
                paginatedDebitCreditAdjustment = filteredDebitCreditAdjustment; // Return all users
                page = 0;
            } else {
                int fromIndex = Math.min(page * size, totalDebitCreditAdjustment);
                int toIndex = Math.min(fromIndex + size, totalDebitCreditAdjustment);
                paginatedDebitCreditAdjustment = filteredDebitCreditAdjustment.subList(fromIndex, toIndex);
            }

            int totalPages = size <= 0 ? 1 : (int) Math.ceil((double) totalDebitCreditAdjustment / size);

            // Prepare response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedDebitCreditAdjustment);
            response.put("totalData", totalDebitCreditAdjustment);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", totalPages);

//            debitCreditCache.put(cacheKey, response);
            assert type != null;
            if(type.equalsIgnoreCase("credit")){
                return ResponseMap.response(status.getSuccessCode(),  credit + " "+status.getDesc(), response);
            } else {
                return ResponseMap.response(status.getSuccessCode(),  debit + " "+status.getDesc(), response);
            }

        } catch (Exception exception) {
            log.error("Error occurred while filtering tariffs: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Fetching debit adjustments service failed");
            genericHandler.logAndSaveException(exception, "fetch debit adjustments");
            throw exception;
        }
    }

//    @Transactional(readOnly = true)
//    @Override
//    public Map<String, Object> getDebitAdjustments(
//            int page, int size, String customerId, String accountNumber,
//            String customerName, String meterNumber, BigDecimal balance, String type) {
//        try {
//            String db;
//            if("credit".equals(type) ){
//                db = credit;
//            } else if("debit".equalsIgnoreCase(type)){
//                db = debit;
//            } else {
//                throw new GlobalExceptionHandler.NotFoundException("Type parameter not found, use credit or debit instead");
//            }
//            UserModel um = handleUserValidation();
//
//            // Build a unique cache key
//            StringBuilder cacheKeyBuilder = new StringBuilder("debitCreditAdjustments_"+um.getOrgId());
//            if (customerId != null && !customerId.isEmpty()) cacheKeyBuilder.append("_customerId_").append(customerId);
//            if (accountNumber != null && !accountNumber.isEmpty()) cacheKeyBuilder.append("_accountNumber_").append(accountNumber);
//            if (customerName != null && !customerName.isEmpty()) cacheKeyBuilder.append("_customerName_").append(customerName);
//            if (meterNumber != null && !meterNumber.isEmpty()) cacheKeyBuilder.append("_meterNumber_").append(meterNumber);
//            if (type != null && !type.isEmpty()) cacheKeyBuilder.append("_type_").append(type);
//            if (balance != null) cacheKeyBuilder.append("_balance_").append(balance);
//            cacheKeyBuilder.append("_page_").append(page);
//            cacheKeyBuilder.append("_size_").append(size);
//
//            String cacheKey = cacheKeyBuilder.toString();
//
//            // Return from cache if available
////            Object cachedDebitCreditAdjustment = debitCreditCache.get(cacheKey);
////            if (cachedDebitCreditAdjustment != null) {
////                return ResponseMap.response(status.getSuccessCode(), "Cached " + db + status.getDesc(), cachedDebitCreditAdjustment);
////            }
//
//            List<DebitCreditAdjust> allDebitCreditAdjustment;
//            // Ideally, this should be a dynamic query in the mapper layer
//
//            allDebitCreditAdjustment = mapper.GetDebitCreditAdjustment(um.getOrgId(), type, page,size);
//
//            List<DebitCreditAdjust> filteredDebitCreditAdjustment = allDebitCreditAdjustment.stream()
//                    .filter(t -> customerId == null || customerId.isEmpty() ||
//                            t.getMeter().stream().anyMatch(m -> m.getCustomerId().equalsIgnoreCase(customerId)))
//                    .filter(t -> accountNumber == null || accountNumber.isEmpty() ||
//                            t.getMeter().stream().anyMatch(m -> m.getAccountNumber().equalsIgnoreCase(accountNumber)))
//                    .filter(t -> customerName == null || customerName.isEmpty() ||
//                            t.getMeter().stream().anyMatch(m -> m.getCustomer().getFirstname().equalsIgnoreCase(customerName)))
//                    .filter(t -> meterNumber == null || meterNumber.isEmpty() ||
//                            t.getMeter().stream().anyMatch(m -> m.getMeterNumber().equalsIgnoreCase(meterNumber)))
//                    .filter(t -> balance == null || t.getBalance().equals(balance))
//                    .collect(Collectors.toList());
//
//
//            // Pagination logic
//            int totalDebitCreditAdjustment = filteredDebitCreditAdjustment.size();
//            List<DebitCreditAdjust> paginatedDebitCreditAdjustment;
//
//            if (size <= 0) {
//                paginatedDebitCreditAdjustment = filteredDebitCreditAdjustment; // Return all users
//                page = 0;
//            } else {
//                int fromIndex = Math.min(page * size, totalDebitCreditAdjustment);
//                int toIndex = Math.min(fromIndex + size, totalDebitCreditAdjustment);
//                paginatedDebitCreditAdjustment = filteredDebitCreditAdjustment.subList(fromIndex, toIndex);
//            }
//
//            int totalPages = size <= 0 ? 1 : (int) Math.ceil((double) totalDebitCreditAdjustment / size);
//
//            // Prepare response with pagination metadata
//            Map<String, Object> response = new HashMap<>();
//            response.put("data", paginatedDebitCreditAdjustment);
//            response.put("totalData", totalDebitCreditAdjustment);
//            response.put("page", page);
//            response.put("size", size);
//            response.put("totalPages", totalPages);
//
////            debitCreditCache.put(cacheKey, response);
//            assert type != null;
//            if(type.equalsIgnoreCase("credit")){
//                return ResponseMap.response(status.getSuccessCode(),  credit + " "+status.getDesc(), response);
//            } else {
//                return ResponseMap.response(status.getSuccessCode(),  debit + " "+status.getDesc(), response);
//            }
//
//        } catch (Exception exception) {
//            log.error("Error occurred while filtering tariffs: {}", exception.getMessage().trim(), exception);
//            genericHandler.logIncidentReport("Fetching debit adjustments service failed");
//            genericHandler.logAndSaveException(exception, "fetch debit adjustments");
//            throw exception;
//        }
//    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getDebitAdjustment(UUID meterId, String type) {
        try {
            UserModel um = handleUserValidation();
            UUID nodeId = um.getNodeInfo().getNodeId();

            List<DebitCreditAdjust> result = mapper.getDebitAdjustmentByMeterId(meterId, um.getOrgId(), type, nodeId);

            if(result == null) {
                throw new GlobalExceptionHandler.NotFoundException(debit + " " + status.getNotFoundDesc());
            }

            return ResponseMap.response(status.getSuccessCode(), debit + " " + status.getDesc(), result);
        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Fetching debit adjustment service failed");
            genericHandler.logAndSaveException(exception, "fetch debit adjustment");
            throw exception;
        }
    }

    private AuditLog buildAuditLog(UserModel creator, String description, String type, Object createdEntity, Map<String, String> metadata) {
        AuditLog log = new AuditLog();
        log.setCreator(creator);
        log.setDescription(description);
        log.setType(type);
        log.setDebitCreditAdjust(createdEntity instanceof DebitCreditAdjust ? (DebitCreditAdjust) createdEntity : null);
        log.setIpAddress(metadata.get("ipAddress"));
        log.setUserAgent(metadata.get("userAgent"));
        log.setEndpoint(metadata.get("endpoint"));
        log.setHttpMethod(metadata.get("httpMethod"));
        return log;
    }

    private void handleAddCache(DebitCreditAdjust debitCreditAdjustment) {

        debitCreditCache.remove(debitCreditAdjustment.getId().toString()+"_"+debitCreditAdjustment.getOrgId());
        for (String key : auditCache.keySet()) {
            if (key.startsWith("grid_flex_audit_log_page_")) {
                auditCache.remove(key);
            }
        }
        for (String key : debitCreditCache.keySet()) {
            if (key.startsWith("debitCreditAdjustments_"+debitCreditAdjustment.getOrgId())) {
                debitCreditCache.remove(key);
            }
        }
        debitCreditCache.put(debitCreditAdjustment.getId().toString()+"_"+debitCreditAdjustment.getOrgId(), debitCreditAdjustment);  // Cache updated or deleted entity
    }

}