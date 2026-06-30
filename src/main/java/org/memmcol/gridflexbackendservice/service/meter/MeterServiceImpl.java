package org.memmcol.gridflexbackendservice.service.meter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.mapper.*;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.customer.Customer;
import org.memmcol.gridflexbackendservice.model.debit_credit_adjustment.DebitCreditAdjustVersion;
import org.memmcol.gridflexbackendservice.model.hes.ObisMapping;
import org.memmcol.gridflexbackendservice.model.manufacturer.Manufacturer;
import org.memmcol.gridflexbackendservice.model.meter.*;
import org.memmcol.gridflexbackendservice.model.node.NodeSummary;
import org.memmcol.gridflexbackendservice.model.node.RegionBhubServiceCenter;
import org.memmcol.gridflexbackendservice.model.node.SubStationTransformerFeederLine;
import org.memmcol.gridflexbackendservice.model.tariff.Tariff;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.model.vend.MeterView;
import org.memmcol.gridflexbackendservice.service.audit.SafeAuditService;
import org.memmcol.gridflexbackendservice.service.hes.HesAuthServiceImpl;
import org.memmcol.gridflexbackendservice.util.GenericResp;
import org.memmcol.gridflexbackendservice.exception.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.HandlePermission;
import org.memmcol.gridflexbackendservice.util.LicenceFileUtil;
import org.memmcol.gridflexbackendservice.model.licence.Licence;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.memmcol.gridflexbackendservice.components.GenericHandler.capitalizeFirstLetter;
import static org.memmcol.gridflexbackendservice.components.HandleValidUser.handleUserValidation;

@Service
public class MeterServiceImpl implements MeterService {
    private static final Logger log = LoggerFactory.getLogger(MeterServiceImpl.class);

    @Autowired
    private ResponseProperties status;

    @Value("${file.base-address}"+"${app.base-path}")
    private String uploadDir;

    @Value("${gridflex.data.dir}")
    private String dataDir;

//    @Autowired
//    private AuditRepository auditRepository;

    @Autowired
    private SafeAuditService safeAuditService;

    @Autowired
    private NodeMapper nodeMapper;

    @Autowired
    private HesAuthServiceImpl auth;

    @Autowired
    private MeterMapper hesMapper;

    @Qualifier("dlmsWriteOpsClient")
    @Autowired
    private WebClient dlmsWriteOpsClient;

    @Autowired
    private MeterMapper meterMapper;

    @Autowired
    private TariffMapper tariffMapper;

    @Autowired
    private GenericHandler genericHandler;

    @Autowired
    private HttpServletRequest httpServletRequest;

    private String meterName = "Meter";

    private final IMap<String, Object> meterCache;

    private final IMap<String, Object> auditCache;

    @Autowired
    private CustomerMapper customerMapper;


    public MeterServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.meterCache = hazelcastInstance.getMap("meterCache");
        this.auditCache = hazelcastInstance.getMap("auditCache");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Map<String, Object> createMeter(Meter request) {
        try {

            handlePayloadCheck(request);
            // --- Step 1: Context & Validation ---
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel user = handleUserValidation();
            UUID nodeId = user.getNodeInfo().getNodeId();
            String nodeType = user.getNodeInfo().getType();

            if(!nodeType.equalsIgnoreCase("Region")
                    && !nodeType.equalsIgnoreCase("Root")
            ){
                throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
            }

            validateMeterRequest(request, user);

            resolveNodeHierarchy(request, nodeId, user.getOrgId());

            // --- Licence Meter Limit Check ---
            checkLicenceMeterLimit(user.getOrgId(), 1);

            // --- Step 2: Insert Meter + Versions ---
            int result1 = meterMapper.insertMeter(request);
            request.setMeterId(request.getId());
            int result2 = meterMapper.insertMeterVersion(request);
            if (result1 == 0 || result2 == 0) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getRegFailureDesc());
            }
            if ("md".trim().equalsIgnoreCase(request.getMeterClass())) {
                insertMDMeterInfo(request, user);
            }
            if (Boolean.TRUE.equals(request.getSmartStatus())) {
                insertSmartMeterInfo(request, user);
            }

            // --- Step 3: Fetch created meter & Audit ---
            Meter newMeter = meterMapper.findByIdVersion(request.getId(), request.getOrgId(), nodeId);
            AuditLog auditLog = buildAuditLog(user, "Meter created", meterName, newMeter, metadata, "");
            safeAuditService.saveAudit(auditLog);


            return ResponseMap.response(status.getSuccessCode(), meterName + " " + status.getRegDesc(), "");

        } catch (Exception ex) {
            log.error("Error creating meter: {}", ex.getMessage(), ex);
            genericHandler.logIncidentReport("Creating meter service failed");
            genericHandler.logAndSaveException(ex, "creating meter");
            throw ex;
        }
    }

    private void resolveNodeHierarchy(Meter request, UUID startNodeId, UUID orgId) {

        UUID currentNodeId = startNodeId;
        Set<UUID> visited = new HashSet<>();

        while (currentNodeId != null) {

            if (!visited.add(currentNodeId)) {
                throw new IllegalStateException("Circular hierarchy detected");
            }

            NodeSummary node = nodeMapper.getNodeByNodeId(currentNodeId, orgId);
            if (node == null) break;

            String type = node.getType() == null ? "" : node.getType().toLowerCase();

            switch (type) {
//                case "business hub":
//                    System.out.println("bbbhhh:: "+node.getNodeId());
//                    if(bhubId.equals(node.getNodeId())){
//                        request.setNodeId(node.getNodeId());
//                    } else {
//                        throw new GlobalExceptionHandler
//                                .NotFoundException("Feeder does not belong to the bushiness hub meter is allocated");
//                    }
//
//                    break;
                case "service center":
                    request.setServiceCenter(node.getNodeId());
                    break;
                case "region":
                    request.setRegion(node.getNodeId());
                    break;
//                case "substation":
//                    request.setSubstation(node.getNodeId());
//                    break;
                case "root":
                    request.setRoot(node.getNodeId());
                    break;
            }

            currentNodeId = node.getParentId();
        }
    }

    private void handlePayloadCheck(Meter request) {

        if(request.getMeterNumber() == null || request.getMeterNumber().isBlank()) {
            throw new GlobalExceptionHandler.NotFoundException("Meter number field is required");
        }
        if(request.getSimNumber() == null || request.getSimNumber().isBlank()) {
            throw new GlobalExceptionHandler.NotFoundException("Sim number field is required");
        }
        if(request.getMeterClass() == null || request.getMeterClass().isBlank()) {
            throw new GlobalExceptionHandler.NotFoundException("Meter class field is required");
        }
        if(request.getMeterManufacturer() == null || request.getMeterManufacturer().equals("")) {
            throw new GlobalExceptionHandler.NotFoundException("Meter class field is required");
        }
        if(request.getMeterType() == null || request.getMeterType().isBlank()) {
            throw new GlobalExceptionHandler.NotFoundException("Meter class field is required");
        }
        if(request.getOldSgc() == null || request.getOldSgc().isBlank()) {
            throw new GlobalExceptionHandler.NotFoundException("Old sgc field is required");
        }

        if(request.getNewSgc() == null || request.getNewSgc().isBlank()) {
            throw new GlobalExceptionHandler.NotFoundException("Old sgc field is required");
        }
        if(request.getOldKrn() == null || request.getOldKrn().isBlank()) {
            throw new GlobalExceptionHandler.NotFoundException("Old krn field is required");
        }
        if(request.getNewKrn() == null || request.getNewKrn().isBlank()) {
            throw new GlobalExceptionHandler.NotFoundException("New krn field is required");
        }
        if(request.getOldTariffIndex() == null) {
            throw new GlobalExceptionHandler.NotFoundException("Old tariff index field is required");
        }
        if(request.getNewTariffIndex() == null) {
            throw new GlobalExceptionHandler.NotFoundException("New tariff index field is required");
        }

//        if(!request.getMeterClass().toLowerCase().contains("single phase")
//                && !request.getMeterClass().toLowerCase().contains("singlephase")
//                && !request.getMeterClass().toLowerCase().contains("three phase")
//                && !request.getMeterClass().toLowerCase().contains("threephase")
//                && !request.getMeterClass().toLowerCase().contains("md")){
//            throw new GlobalExceptionHandler.NotFoundException("Meter class is not supported");
//        }
//        if (request.getMeterClass().toLowerCase().contains("single phase")
//                && request.getMeterClass().toLowerCase().contains("singlephase")){
//            request.setMeterClass("Single-Phase");
//        }
//
//        if (request.getMeterClass().toLowerCase().contains("three phase")
//                && request.getMeterClass().toLowerCase().contains("threephase")){
//            request.setMeterClass("Three-Phase");
//        }

        if(request.getSmartStatus()) {
            if(request.getSmartMeterInfo() == null){
                throw new GlobalExceptionHandler.NotFoundException("Smart meter info field is required");
            }

            if(request.getSmartMeterInfo().getMeterModel() == null || request.getSmartMeterInfo().getMeterModel().isBlank()){
                throw new GlobalExceptionHandler.NotFoundException("Meter model field is required");
            }
            if(request.getSmartMeterInfo().getProtocol() == null
                    || request.getSmartMeterInfo().getProtocol().isBlank()){
                throw new GlobalExceptionHandler.NotFoundException("Protocol field is required");
            }
            if(request.getSmartMeterInfo().getAuthentication() == null
                    || request.getSmartMeterInfo().getAuthentication().isBlank()){
                throw new GlobalExceptionHandler.NotFoundException("Authentication field is required");
            }
            if(request.getSmartMeterInfo().getPassword() == null
                    || request.getSmartMeterInfo().getPassword().isBlank()){
                throw new GlobalExceptionHandler.NotFoundException("Password field is required");
            }
        }


        if(request.getMeterClass().equalsIgnoreCase("md")) {
            if(request.getMdMeterInfo() == null){
                throw new GlobalExceptionHandler.NotFoundException("MD meter info field is required");
            }

            if(request.getMdMeterInfo().getCtRatioNum() == null || request.getMdMeterInfo().getCtRatioNum().isBlank()){
                throw new GlobalExceptionHandler.NotFoundException("CT ratio Numerator field is required");
            }
            if(request.getMdMeterInfo().getCtRatioDenom() == null || request.getMdMeterInfo().getCtRatioDenom().isBlank()){
                throw new GlobalExceptionHandler.NotFoundException("CT ratio Denominator field is required");
            }
            if(request.getMdMeterInfo().getVoltRatioDenom() == null || request.getMdMeterInfo().getVoltRatioDenom().isBlank()){
                throw new GlobalExceptionHandler.NotFoundException("Volt ratio Denominator field is required");
            }
            if(request.getMdMeterInfo().getVoltRatioNum() == null || request.getMdMeterInfo().getVoltRatioNum().isBlank()){
                throw new GlobalExceptionHandler.NotFoundException("Volt ratio Numerator field is required");
            }
//            if(request.getMdMeterInfo().getMultiplier() == null || request.getMdMeterInfo().getMultiplier().isBlank()){
//                throw new GlobalExceptionHandler.NotFoundException("Multiplier field is required");
//            }

            if(request.getMdMeterInfo().getMeterRating() == null || request.getMdMeterInfo().getMeterRating().isBlank()){
                throw new GlobalExceptionHandler.NotFoundException("Meter rating field is required");
            }
            if(request.getMdMeterInfo().getInitialReading() == null || request.getMdMeterInfo().getInitialReading().isBlank()){
                throw new GlobalExceptionHandler.NotFoundException("Initial reading field is required");
            }
            if(request.getMdMeterInfo().getDial() == null || request.getMdMeterInfo().getDial().isBlank()){
                throw new GlobalExceptionHandler.NotFoundException("Dial field is required");
            }
        }
    }

    private void validateMeterRequest(Meter request, UserModel user) {
        Manufacturer manufacturer = meterMapper.getMeterManufacturer(request.getMeterManufacturer());
        if (manufacturer == null) {
            throw new GlobalExceptionHandler.NotFoundException("Meter manufacturer not found");
        }

        Meter existing = meterMapper.getMeter(user.getOrgId(), null, request.getMeterNumber().trim(), null, null, request.getSimNumber(), user.getNodeInfo().getNodeId());
        if (existing != null) {
            throw new GlobalExceptionHandler.NotFoundException("Meter Number ("+existing.getMeterNumber()+" or Sim Number "+existing.getSimNumber()+") "+status.getExistDesc());
        }
//        if (existing.getSimNumber().equalsIgnoreCase(request.getSimNumber())){
//            throw new GlobalExceptionHandler.NotFoundException("Sim Number "+status.getExistDesc());
//        }

        String clazz = request.getMeterClass();
        String category = request.getMeterCategory();
        String type = request.getMeterType();

        if (category.equalsIgnoreCase("prepaid")){
            request.setMeterCategory("Prepaid");
        }

        if (type.equalsIgnoreCase("Electricity")){
            request.setMeterType("Electricity");
        }

        if (clazz.equalsIgnoreCase("single phase")
                || clazz.equalsIgnoreCase("singlephase")
                || clazz.equalsIgnoreCase("single-phase")){
            request.setMeterClass("Single-Phase");
        }

        if (clazz.equalsIgnoreCase("three phase")
                || clazz.equalsIgnoreCase("threephase")
                || clazz.equalsIgnoreCase("three-phase")){
            request.setMeterClass("Three-Phase");
        }

        if (!clazz.equalsIgnoreCase("md") &&
                !clazz.equalsIgnoreCase("single-phase") &&
                !clazz.equalsIgnoreCase("three-phase")) {
            throw new GlobalExceptionHandler.NotFoundException(
                    "Meter class not supported");
//            throw new GlobalExceptionHandler.NotFoundException(
//                    "Meter class must be one of: MD, single-phase, or three-phase");
        }

        if (!request.getMeterCategory().equalsIgnoreCase("Prepaid")) {
            throw new GlobalExceptionHandler.NotFoundException(
                    "Meter category not supported");
        }

        if (!request.getMeterType().equalsIgnoreCase("Electricity")) {
            throw new GlobalExceptionHandler.NotFoundException(
                    "Meter type not supported");
        }

        // Default states
        request.setStatus("Active");
        request.setMeterStage("Pending-created");
        request.setOrgId(user.getOrgId());
        request.setType("NON-VIRTUAL");
        request.setDescription(capitalizeFirstLetter("Newly Added"));
        request.setCreatedBy(user.getId());
    }

    private void insertMDMeterInfo(Meter request, UserModel user) {

        if (request.getMdMeterInfo().getLongitude() != null) {
            double longitude = Double.parseDouble(request.getMdMeterInfo().getLongitude());
            if (longitude < -180 || longitude > 180) {
                throw new IllegalArgumentException(
                        "Longitude must be between -180 and 180"
                );
            }
        }

        if (request.getMdMeterInfo().getLatitude() != null) {
            double latitude = Double.parseDouble(request.getMdMeterInfo().getLongitude());
            if (latitude < -90 || latitude > 90) {
                throw new IllegalArgumentException(
                        "Latitude must be between -90 and 90"
                );
            }
        }

        request.getMdMeterInfo().setMeterId(request.getId());
        request.getMdMeterInfo().setOrgId(user.getOrgId());
        request.getMdMeterInfo().setCreatedBy(user.getId());
        double ctRatioNumerator = Double.parseDouble(request.getMdMeterInfo().getCtRatioNum());
        double ctRatioDenominator = Double.parseDouble(request.getMdMeterInfo().getCtRatioDenom());
        double vtRatioNumerator = Double.parseDouble(request.getMdMeterInfo().getVoltRatioNum());
        double vtRatioDenominator = Double.parseDouble(request.getMdMeterInfo().getVoltRatioDenom());
        double multiplier = (ctRatioNumerator / ctRatioDenominator) * (vtRatioNumerator / vtRatioDenominator);
        BigDecimal rounded = BigDecimal.valueOf(multiplier).setScale(2 , RoundingMode.HALF_UP);
        request.getMdMeterInfo().setMultiplier(rounded.toString());
        request.getMdMeterInfo().setMeterStage("Pending-created");
        request.getMdMeterInfo().setDescription("Newly Added");

        int inserted = meterMapper.insertMDMeterInfoVersion(request.getMdMeterInfo());
        if (inserted == 0) {
            throw new GlobalExceptionHandler.NotFoundException(meterName + " MD data " + status.getRegFailureDesc());
        }
    }

    private void insertSmartMeterInfo(Meter request, UserModel user) {
        request.getSmartMeterInfo().setMeterId(request.getId());
        request.getSmartMeterInfo().setOrgId(user.getOrgId());
        request.getSmartMeterInfo().setCreatedBy(user.getId());
        request.getSmartMeterInfo().setMeterStage("Pending-created");
        request.getSmartMeterInfo().setDescription("Newly Added");
//        request.getSmartMeterInfo().setPassword(passwordEncoder.encode(request.getSmartMeterInfo().getPassword()));

        int inserted = meterMapper.insertSmartMeterInfoVersion(request.getSmartMeterInfo());
        if (inserted == 0) {
            throw new GlobalExceptionHandler.NotFoundException(meterName + " Smart data " + status.getRegFailureDesc());
        }
    }

    @Transactional
    @Override
    public Map<String, Object> updateMeter(Meter request) {
        try {
            // Gather client metadata
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

            // Validate user and set organization ID
            UserModel user = handleUserValidation();
            request.setOrgId(user.getOrgId());
            UUID nodeId = user.getNodeInfo().getNodeId();
            String nodeType = user.getNodeInfo().getType();

            HandlePermission.perm(nodeType);

            System.out.print("nodeId: "+nodeId);

            // Fetch existing meter and version
            Meter existingMeter = meterMapper.getMeterByIdForUpdate(request.getId(), user.getOrgId(), nodeId);
            if (existingMeter == null) {
                throw new GlobalExceptionHandler.NotFoundException("Meter not found or You do not have permission to edit");
            }
            // Insert or update meter version
            int result;
            if (existingMeter.getMeterStage().contains("Pending")
                    || existingMeter.getStatus().contains("Pending")
                    || existingMeter.getMeterStage().equalsIgnoreCase("Assign-edited")) {
                throw new GlobalExceptionHandler.NotFoundException("Meter ("+existingMeter.getMeterNumber()+ ") have a pending state that needs to be cleared");
            }
            if(existingMeter.getStatus().equalsIgnoreCase("Deactivated")){
                throw new GlobalExceptionHandler.NotFoundException(existingMeter.getMeterNumber()+ " is deactivated and cannot be edited");
            }

            if(existingMeter.getMeterStage().equalsIgnoreCase("Created")) {
                handleCreatedMeter(existingMeter, nodeType, request, user);
            }

            if(existingMeter.getMeterStage().equalsIgnoreCase("Unassigned")){
//                throw new GlobalExceptionHandler.NotFoundException("Editing allocated meter is not supported");
                handleAllocatedMeter(existingMeter, nodeType, request, user, nodeId);
            }


            String MDDesc = "";
            String SmartDesc = "";
            String MeterDesc = "";
            String meterStage = "Pending-edited";
            // Prepare meter update data
            request.setType("NON-VIRTUAL");
            request.setStatus("Active");
            MeterDesc = buildChangeDescription(existingMeter, request);
            request.setDescription("Meter edited");
            request.setCreatedBy(user.getId());

            request.setOrgId(user.getOrgId());
            request.setMeterId(existingMeter.getMeterId());
            request.setCreatedBy(user.getId());
            request.setCustomerId(existingMeter.getCustomerId());
            request.setMeterId(existingMeter.getId());

            if(existingMeter.getMeterStage().equalsIgnoreCase("Assigned")){
                meterStage = "Assign-edited";
                handleAssignedMeter(existingMeter, nodeType, request, user, meterStage);
                request.setDescription(meterStage);
            }

            request.setMeterStage(meterStage);
            int res = meterMapper.updateMeter(meterStage, request.getId(), request.getUpdatedAt(), request.getStatus());
            result = meterMapper.insertMeterVersion(request);
            if (result == 0 || res == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());

            // Handle MD meter-specific logic
            if (request.getMdMeterInfo() != null
                    && request.getMeterClass().equalsIgnoreCase("md")){
                MDDesc = handleMDMeterInfo(request, nodeType, existingMeter, user, meterStage);
            }

            // Handle smart meter-specific logic
            if (request.getSmartMeterInfo() != null && request.getSmartStatus()){
                SmartDesc = handleSmartMeterInfo(request, nodeType, existingMeter, user, meterStage);
            }

            if(request.getPaymentMode() != null
                    && "Prepaid".equalsIgnoreCase(request.getMeterCategory())
                    && existingMeter.getMeterStage().equalsIgnoreCase("Assigned")){
                handlePaymentMode(request, nodeType, existingMeter, user);
            }


            String desc = MeterDesc + "," + MDDesc + ","+ SmartDesc;

            // Fetch updated meter and log audit
            Meter updatedMeter = meterMapper.findByIdVersion(request.getId(), user.getOrgId(), nodeId);

            AuditLog auditLog = buildAuditLog(user, desc, meterName, updatedMeter, metadata, "");
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(), meterName + " " + status.getUpdateDesc(), "");
        } catch (Exception ex) {
            log.error("Error updating meter: {}", ex.getMessage(), ex);
            genericHandler.logIncidentReport("Editing meter service failed");
            genericHandler.logAndSaveException(ex, "editing meter");
            throw ex;
        }
    }


    private void handleAssignedMeter(Meter existingMeter, String nodeType, Meter request, UserModel user, String meterStage) {
        if(!nodeType.equalsIgnoreCase("Root")
                && !nodeType.equalsIgnoreCase("Region")
                && !nodeType.equalsIgnoreCase("Business hub")
                 && !nodeType.equalsIgnoreCase("Service center")){
            throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
        }
            Meter m = meterMapper.getMeterDuplicateCin(
                    user.getOrgId(),
                    request.getAccountNumber(),
                    request.getCin());

            if(m != null && !m.getId().equals(existingMeter.getId())){
                throw new GlobalExceptionHandler.NotFoundException(
                        "Cin or account number is already assigned to this meter " +
                                "("+ m.getMeterNumber()+")");
            }
            // Validate DSS
            SubStationTransformerFeederLine dss = meterMapper.verifyDss(request.getDssAssetId(), user.getOrgId());
            if (dss == null) {
                throw new GlobalExceptionHandler.NotFoundException("DSS " + status.getNotFoundDesc());
            }

            // Validate feeder line
            SubStationTransformerFeederLine feederLine = meterMapper.verifyFeeder(request.getFeederAssetId(), user.getOrgId());
            if (feederLine == null) {
                throw new GlobalExceptionHandler.NotFoundException("Feeder line " + status.getNotFoundDesc());
            }

            resolveBulkNodeHierarchy(existingMeter, feederLine.getParentId(), user.getOrgId(), existingMeter.getNodeId());

//            RegionBhubServiceCenter regionBhubServiceCenter = meterMapper.verifyBhub(feederLine.getParentId(), user.getOrgId());
//            if (regionBhubServiceCenter == null){
//                throw new GlobalExceptionHandler.NotFoundException("Feeder does not belong to the bushiness hub meter is allocated");
//            }

            if(!dss.getParentId().equals(feederLine.getNodeId())){
                throw new GlobalExceptionHandler.NotFoundException("DSS ("+ request.getAssetId() +") " +
                        "provided does not belong to the feeder line ("+request.getFeederAssetId()+")");
            }

            System.out.println("feeder: "+existingMeter.getFeeder());
        System.out.println("dss: "+existingMeter.getDss());
            request.setMeterStage(meterStage);
            request.setCin(request.getCin());
            request.setAccountNumber(request.getAccountNumber());
            request.setRoot(existingMeter.getRoot());
            request.setRegion(existingMeter.getRegion());
            request.setServiceCenter(existingMeter.getServiceCenter());
            request.setSubstation(existingMeter.getSubstation());
            request.setNodeId(existingMeter.getNodeId());
            request.setFeeder(existingMeter.getFeeder());
            request.setDss(existingMeter.getDss());
            request.setMeterNumber(existingMeter.getMeterNumber());
            request.setSimNumber(existingMeter.getSimNumber());
            request.setMeterManufacturer(existingMeter.getMeterManufacturer());
            request.setOldTariffIndex(existingMeter.getOldTariffIndex());
            request.setNewTariffIndex(existingMeter.getNewTariffIndex());
            request.setNewKrn(existingMeter.getNewKrn());
            request.setOldKrn(existingMeter.getOldKrn());
            request.setNewSgc(existingMeter.getNewSgc());
            request.setOldSgc(existingMeter.getOldSgc());
            request.setMeterType(existingMeter.getMeterType());
            request.setMeterClass(existingMeter.getMeterClass());
            request.setMeterCategory(existingMeter.getMeterCategory());
            request.setSmartStatus(existingMeter.getSmartStatus());
            request.setMeterManufacturer(existingMeter.getMeterManufacturer());

            Tariff tariff = tariffMapper.getApproveTariff(request.getTariff());
            if(tariff == null){
                throw new GlobalExceptionHandler.NotFoundException("Tariff is either not found, not approved or deactivated");
            }
            request.setTariff(tariff.getId());
    }

    private String handleSmartMeterInfo(Meter request, String nodeType, Meter existingMeter, UserModel user, String meterStage) {
        String SmartDesc = "";
        if (nodeType.equalsIgnoreCase("Region")
                || nodeType.equalsIgnoreCase("Root")
                || nodeType.equalsIgnoreCase("Business hub")) {
            UUID meterId = request.getId();
            request.getSmartMeterInfo().setMeterId(meterId);
            request.getSmartMeterInfo().setOrgId(user.getOrgId());
            request.getSmartMeterInfo().setMeterStage(meterStage);
            request.getSmartMeterInfo().setCreatedBy(user.getId());

            if(existingMeter.getSmartMeterInfo() == null){
                SmartMeterInfo smart = new SmartMeterInfo();
                smart.setMeterModel(request.getSmartMeterInfo().getMeterModel());
                smart.setProtocol(request.getSmartMeterInfo().getProtocol());
                smart.setAuthentication(request.getSmartMeterInfo().getAuthentication());
                smart.setPassword(request.getSmartMeterInfo().getPassword());

                existingMeter.setSmartMeterInfo(smart);
            }
            SmartDesc = buildSmartMeterInfoChangeDescription(existingMeter.getSmartMeterInfo(), request.getSmartMeterInfo());
            request.getSmartMeterInfo().setDescription("Pending edited");
            int mdResult2 = meterMapper.insertSmartMeterInfoVersion(request.getSmartMeterInfo());
            if (mdResult2 == 0) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " MD data " + status.getUpdateFailureDesc());
            }
        }
        return SmartDesc;
    }

    private String handleMDMeterInfo(Meter request, String nodeType, Meter existingMeter, UserModel user, String meterStage) {
        String MDDesc = "";
        if (nodeType.equalsIgnoreCase("Business hub")
                || nodeType.equalsIgnoreCase("Root")
                || nodeType.equalsIgnoreCase("Region")
        ) {
            UUID meterId = request.getId();
            request.getMdMeterInfo().setMeterId(meterId);
            request.getMdMeterInfo().setOrgId(user.getOrgId());
            request.getMdMeterInfo().setMeterStage(meterStage);
            request.getMdMeterInfo().setCreatedBy(user.getId());
            double ctRatioNumerator = Double.parseDouble(request.getMdMeterInfo().getCtRatioNum());
            double ctRatioDenominator = Double.parseDouble(request.getMdMeterInfo().getCtRatioDenom());
            double vtRatioNumerator = Double.parseDouble(request.getMdMeterInfo().getVoltRatioNum());
            double vtRatioDenominator = Double.parseDouble(request.getMdMeterInfo().getVoltRatioDenom());
            double multiplier = (ctRatioNumerator / ctRatioDenominator) * (vtRatioNumerator / vtRatioDenominator);
            BigDecimal rounded = BigDecimal.valueOf(multiplier).setScale(2, RoundingMode.HALF_UP);

            if(existingMeter.getMdMeterInfo() == null) {
                MDMeterInfo md = new MDMeterInfo();

                md.setCtRatioNum(String.valueOf(ctRatioNumerator));
                md.setCtRatioDenom(String.valueOf(ctRatioDenominator));
                md.setVoltRatioNum(String.valueOf(vtRatioNumerator));
                md.setVoltRatioDenom(String.valueOf(vtRatioDenominator));
                md.setMultiplier(rounded.toString());
                md.setMeterRating(request.getMdMeterInfo().getMeterRating());
                md.setInitialReading(request.getMdMeterInfo().getInitialReading());
                md.setDial(request.getMdMeterInfo().getDial());
                md.setLatitude(request.getMdMeterInfo().getLatitude());
                md.setLongitude(request.getMdMeterInfo().getLongitude());

                existingMeter.setMdMeterInfo(md);
            }
            MDDesc = buildMDMeterInfoChangeDescription(existingMeter.getMdMeterInfo(), request.getMdMeterInfo());
            request.getMdMeterInfo().setDescription("Pending edited");
            request.getMdMeterInfo().setMultiplier(rounded.toString());

            int mdResult2 = meterMapper.insertMDMeterInfoVersion(request.getMdMeterInfo());
            if (mdResult2 == 0) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " MD data " + status.getUpdateFailureDesc());
            }
        }
        return MDDesc;
    }

    private void handlePaymentMode(Meter request, String nodeType, Meter existingMeter, UserModel user) {
        if (nodeType.equalsIgnoreCase("Region")
                || nodeType.equalsIgnoreCase("Root")
                || nodeType.equalsIgnoreCase("Business hub")
        ) {
            UUID meterId = request.getId();
            var payment = request.getPaymentMode();

            payment.setMeterId(meterId);
            payment.setOrgId(user.getOrgId());
            payment.setCreatedBy(user.getId());
            payment.setDescription("Assign edited");
            payment.setMeterStage("Assign-edited");

//                String paymentType = payment.getPaymentType();
            String creditPaymentMode = payment.getCreditPaymentMode();
            String creditPaymentPlan = payment.getCreditPaymentPlan();

            String debitPaymentMode = payment.getDebitPaymentMode();
            String debitPaymentPlan = payment.getDebitPaymentPlan();

            // Validate payment mode
            if ((debitPaymentMode == null || creditPaymentMode.isBlank()) && (creditPaymentMode == null || creditPaymentMode.isBlank())) {
                throw new GlobalExceptionHandler.NotFoundException("Payment mode is required");
            }

            assert debitPaymentMode != null;
            if (debitPaymentMode.equalsIgnoreCase("one-off") ||
                    debitPaymentMode.equalsIgnoreCase("percentage")) {

                payment.setDebitPaymentPlan("");

            } else if (creditPaymentMode.equalsIgnoreCase("one-off") ||
                    creditPaymentMode.equalsIgnoreCase("percentage")) {

                payment.setCreditPaymentPlan("");

            } else if (debitPaymentMode.equalsIgnoreCase("monthly")) {

                if (debitPaymentPlan == null || debitPaymentPlan.isBlank()) {
                    throw new GlobalExceptionHandler.NotFoundException("Debit payment monthly plan is required");
                }

            } else if (creditPaymentMode.equalsIgnoreCase("monthly")) {

                if (creditPaymentPlan == null || creditPaymentPlan.isBlank()) {
                    throw new GlobalExceptionHandler.NotFoundException("Credit payment monthly plan is required");
                }
            } else if (creditPaymentMode.equalsIgnoreCase("non")) {

                payment.setCreditPaymentPlan("");

            } else if (debitPaymentMode.equalsIgnoreCase("non")) {

                payment.setDebitPaymentPlan("");

            }
            else {
                throw new GlobalExceptionHandler.NotFoundException("Payment mode is not supported");
            }

            payment.setStatus(true);

            int resp = meterMapper.assignPaymentModeWhenMigrationToPrepaid(payment);
            if (resp == 0) {
                throw new GlobalExceptionHandler.NotFoundException(
                        meterName + " Payment mode " + status.getUpdateFailureDesc());
            }

            if(request.getMeterAssignLocation() != null){
                var location =  request.getMeterAssignLocation();
                location.setMeterId(meterId);
                location.setOrgId(user.getOrgId());
                location.setCreatedBy(user.getId());
                location.setDescription("Assign edited");
                location.setMeterStage("Assign-edited");

                int locationAssignResult = meterMapper.assignVerMeterToLocation(location);

                if (locationAssignResult == 0) {
                    throw new GlobalExceptionHandler.NotFoundException("Meter assignment to location failed");
                }
            }
        } else {
            throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
        }
    }

    private void handleAllocatedMeter(Meter existingMeter, String nodeType, Meter request, UserModel user, UUID nodeId) {
        if(!nodeType.equalsIgnoreCase("Root")
                && !nodeType.equalsIgnoreCase("Region")){
            throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
        }
        // check if operator exist
            Meter meter = meterMapper.findByMeterNumber(request.getMeterNumber(), request.getOrgId());

            if(meter != null && !meter.getId().equals(existingMeter.getId())) {
                throw new GlobalExceptionHandler.NotFoundException("Meter number (" + request.getMeterNumber() + ") already exist");
            }

            Manufacturer isManufacturer = meterMapper.findManufacturerById(
                    request.getMeterManufacturer(), user.getOrgId());
            if (isManufacturer == null){
                throw new GlobalExceptionHandler.NotFoundException("Manufacturer " +status.getNotFoundDesc());
            }
            request.setRoot(existingMeter.getRoot());
            request.setRegion(existingMeter.getRegion());
            request.setMeterStage("Pending-edited");
            request.setNodeId(existingMeter.getNodeId());
            request.setDss(existingMeter.getDss());
            request.setCin(existingMeter.getCin());
            request.setAccountNumber(existingMeter.getAccountNumber());
            request.setTariff(existingMeter.getTariff());
//            request.setMeterManufacturer(existingMeter.getMeterManufacturer());
    }

    private void handleCreatedMeter(Meter existingMeter, String nodeType, Meter request, UserModel user) {
        if(!nodeType.equalsIgnoreCase("Root")
                && !nodeType.equalsIgnoreCase("Region") ){
            throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
        }
            Meter meter = meterMapper.findByMeterNumber(request.getMeterNumber(), request.getOrgId());

            if(meter != null && !meter.getId().equals(existingMeter.getId())) {
                throw new GlobalExceptionHandler.NotFoundException("Meter number (" + request.getMeterNumber() + ") already exist");
            }

            Manufacturer isManufacturer = meterMapper.findManufacturerById(
                    request.getMeterManufacturer(), user.getOrgId());
            if (isManufacturer == null){
                throw new GlobalExceptionHandler.NotFoundException("Manufacturer " +status.getNotFoundDesc());
            }
            request.setRoot(existingMeter.getRoot());
            request.setRegion(existingMeter.getRegion());
            request.setMeterStage("Pending-edited");
            request.setNodeId(existingMeter.getNodeId());
            request.setDss(existingMeter.getDss());
            request.setCin(existingMeter.getCin());
            request.setAccountNumber(existingMeter.getAccountNumber());
            request.setTariff(existingMeter.getTariff());
//            request.setMeterManufacturer(existingMeter.getMeterManufacturer());
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getAllMeters(
            int page, int size, String search, String meterNumber, String simNo, String manufacturer, String meterStage,
            String meterClass, String category, String state, String createdAt, String customerId, String type,
            String sortBy, String sortDirection) {
        try {

            UserModel um = handleUserValidation();

            UUID nodeId = um.getNodeInfo().getNodeId();
            String nodeType = um.getNodeInfo().getType();

            // Build a unique cache key
            StringBuilder cacheKeyBuilder = new StringBuilder("users_"+um.getOrgId());
            if (search != null && !search.isEmpty()) cacheKeyBuilder.append("_search_").append(search);
            if (meterNumber != null && !meterNumber.isEmpty()) cacheKeyBuilder.append("_meterNumber_").append(meterNumber);
            if (simNo != null && !simNo.isEmpty()) cacheKeyBuilder.append("_simNo_").append(simNo);
            if (meterStage != null && !meterStage.isEmpty()) cacheKeyBuilder.append("_meterStage_").append(meterStage);
            if (manufacturer != null && !manufacturer.isEmpty()) cacheKeyBuilder.append("_manufacturer_").append(manufacturer);
            if (meterClass != null && !meterClass.isEmpty()) cacheKeyBuilder.append("_meterClass_").append(meterClass);
            if (category != null && !category.isEmpty()) cacheKeyBuilder.append("_category_").append(category);
            if (state != null && !state.isEmpty()) cacheKeyBuilder.append("_state_").append(state);
            if (createdAt != null && !createdAt.isEmpty()) cacheKeyBuilder.append("_createdAt_").append(createdAt);
            if (customerId != null && !customerId.isEmpty()) cacheKeyBuilder.append("_customerId_").append(customerId);
            if (sortBy != null && !sortBy.isEmpty()) cacheKeyBuilder.append("_sortBy_").append(sortBy);
            if (sortDirection != null && !sortDirection.isEmpty()) cacheKeyBuilder.append("_sortDirection_").append(sortDirection);
            cacheKeyBuilder.append("_page_").append(page);
            cacheKeyBuilder.append("_size_").append(size);

            String cacheKey = cacheKeyBuilder.toString();

            // Return from cache if available
            Object cachedUser = meterCache.get(cacheKey);
            if (cachedUser != null) {
                return ResponseMap.response(status.getSuccessCode(), "Cached Meters " + status.getDesc(), cachedUser);
            }

            if(nodeType.equalsIgnoreCase("Service center")){
                nodeId = nodeMapper.getParentNode(um.getOrgId(), nodeId);
            }
            List<Meter> meters;
            List<NodeSummary> result;
            if(nodeType.equalsIgnoreCase("Region")
                    || nodeType.equalsIgnoreCase("Root")){
                // Fetch all users
                if (type.trim().equalsIgnoreCase("pending-state")) {
                    meters = meterMapper.getMetersVersion(um.getOrgId(), page, size, nodeId);
                } else if (type.trim().equalsIgnoreCase("inventory")) {
                    meters = meterMapper.getInventoryMeters(um.getOrgId(), page, size, nodeId);
                } else if (type.trim().equalsIgnoreCase("allocated")) {
                    meters = meterMapper.getAllocatedMeters(um.getOrgId(), page, size, nodeId);
                } else if (type.trim().equalsIgnoreCase("assigned")) {
                    meters = meterMapper.getAssignedMeters(um.getOrgId(),  page, size, nodeId);
                } else if (type.trim().equalsIgnoreCase("virtual")) {
                    meters = meterMapper.getAssignedVirtualMeters(um.getOrgId(), page, size, nodeId);
                } else {
                    meters = meterMapper.getMeters(um.getOrgId(), page, size, nodeId);
                }
            } else {
                // Fetch all users
                if (type.trim().equalsIgnoreCase("pending-state")) {
                    meters = meterMapper.getMetersVersionNode(um.getOrgId(), page, size, nodeId);
                } else if (type.trim().equalsIgnoreCase("inventory")) {
                    meters = List.of();
                } else if (type.trim().equalsIgnoreCase("allocated")) {
                    meters = meterMapper.getAllocatedMetersNode(um.getOrgId(), page, size, nodeId);
                } else if (type.trim().equalsIgnoreCase("assigned")) {
                    meters = meterMapper.getAssignedMetersNode(um.getOrgId(),  page, size, nodeId);
                } else if (type.trim().equalsIgnoreCase("virtual")) {
                    meters = meterMapper.getAssignedVirtualMetersNode(um.getOrgId(), page, size, nodeId);
                } else {
                    meters = meterMapper.getMetersNode(um.getOrgId(), page, size, nodeId);
                }
            }

            // Apply filtering
            Stream<Meter> meterStream = meters.stream();
            if (meterNumber != null && !meterNumber.isEmpty()) {
                meterStream = meterStream.filter(u -> containsIgnoreCase(u.getMeterNumber(), meterNumber));
            }

            if (simNo != null && !simNo.isEmpty()) {
                meterStream = meterStream.filter(u -> containsIgnoreCase(u.getSimNumber(), simNo));
            }

            if (meterStage != null && !meterStage.isEmpty()) {
                meterStream = meterStream.filter(u -> u.getMeterStage() != null && u.getMeterStage().equalsIgnoreCase(meterStage));
            }

            if (meterClass != null && !meterClass.isEmpty()) {
                meterStream = meterStream.filter(u -> u.getMeterClass() != null && u.getMeterClass().equalsIgnoreCase(meterClass));
            }

            if (category != null && !category.isEmpty()) {
                meterStream = meterStream.filter(u -> u.getMeterCategory() != null && u.getMeterCategory().equalsIgnoreCase(category));
            }

            if (manufacturer != null && !manufacturer.isEmpty()) {
                meterStream = meterStream.filter(u ->
                        containsIgnoreCase(u.getMeterManufacturerName(), manufacturer)
                                || (u.getMeterManufacturer() != null && containsIgnoreCase(u.getMeterManufacturer().toString(), manufacturer))
                                || (u.getManufacturer() != null && containsIgnoreCase(u.getManufacturer().getName(), manufacturer))
                                || (u.getManufacturer() != null && containsIgnoreCase(u.getManufacturer().getManufacturerId(), manufacturer)));
            }

            if (state != null && !state.isEmpty()) {
                meterStream = meterStream.filter(u -> u.getStatus() != null && u.getStatus().equalsIgnoreCase(state));
            }

            if (customerId != null && !customerId.isEmpty()) {
                meterStream = meterStream.filter(u -> containsIgnoreCase(u.getCustomerId(), customerId));
            }

            if (createdAt != null && !createdAt.isEmpty()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate date = LocalDate.parse(createdAt, formatter);
                meterStream = meterStream.filter(u -> {
                    if (u.getCreatedAt() == null) return false;
                    return !u.getCreatedAt()
//                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .isBefore(date);
                });
            }

            if (search != null && !search.trim().isEmpty()) {
                meterStream = meterStream.filter(u -> meterMatchesSearch(u, search));
            }

            List<Meter> filteredMeters = meterStream.toList();
            if (type.equalsIgnoreCase("assigned") || type.equalsIgnoreCase("pending-state")) {
                filteredMeters.forEach(meter -> {
                    if (meter.getImage() != null) {
                        meter.setImage(uploadDir + meter.getImage());
                    }
                });
            }

            filteredMeters = sortMeters(filteredMeters, sortBy, sortDirection);

            // Pagination logic
            int totalMeters = filteredMeters.size();
            List<Meter> paginatedMeters = filteredMeters;

             if (size <= 0) {
                 paginatedMeters = filteredMeters;
                 page = 0;
             } else {
                 int fromIndex = Math.min(page * size, totalMeters);
                 int toIndex = Math.min(fromIndex + size, totalMeters);
                 paginatedMeters = filteredMeters.subList(fromIndex, toIndex);
             }

            int totalPages = size <= 0 ? 1 : (int) Math.ceil((double) totalMeters / size);

            // Prepare response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedMeters);
            response.put("totalData", totalMeters);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", totalPages);


//            userCache.put(cacheKey, response);

            return ResponseMap.response(status.getSuccessCode(), meterName + "s " + status.getDesc(), response);

        } catch (Exception exception) {
            log.error("Error filtering / fetching users: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Fetching meter service failed");
            genericHandler.logAndSaveException(exception, "fetching meter");
            throw exception;
        }
    }

    private boolean containsIgnoreCase(String value, String term) {
        return value != null && term != null
                && value.toLowerCase(Locale.ROOT).contains(term.trim().toLowerCase(Locale.ROOT));
    }

    private boolean meterMatchesSearch(Meter meter, String search) {
        return containsIgnoreCase(meter.getMeterNumber(), search)
                || containsIgnoreCase(meter.getSimNumber(), search)
                || containsIgnoreCase(meter.getAccountNumber(), search)
                || containsIgnoreCase(meter.getCin(), search)
                || containsIgnoreCase(meter.getMeterClass(), search)
                || containsIgnoreCase(meter.getMeterCategory(), search)
                || containsIgnoreCase(meter.getMeterStage(), search)
                || containsIgnoreCase(meter.getStatus(), search)
                || containsIgnoreCase(meter.getMeterManufacturerName(), search)
                || (meter.getMeterManufacturer() != null && containsIgnoreCase(meter.getMeterManufacturer().toString(), search))
                || (meter.getManufacturer() != null && containsIgnoreCase(meter.getManufacturer().getName(), search))
                || (meter.getManufacturer() != null && containsIgnoreCase(meter.getManufacturer().getManufacturerId(), search));
    }

    private List<Meter> sortMeters(List<Meter> meters, String sortBy, String sortDirection) {
        String normalizedSortBy = sortBy == null || sortBy.trim().isEmpty()
                ? "createdAt"
                : sortBy.trim();
        boolean descending = sortDirection == null || sortDirection.equalsIgnoreCase("desc");

        Comparator<Meter> comparator;
        switch (normalizedSortBy.toLowerCase(Locale.ROOT)) {
            case "createdat", "dateadded" -> comparator = Comparator.comparing(
                    Meter::getCreatedAt,
                    nullableComparableComparator(descending)
            );
            case "updatedat" -> comparator = Comparator.comparing(
                    Meter::getUpdatedAt,
                    nullableComparableComparator(descending)
            );
            case "oldtariffindex" -> comparator = Comparator.comparing(
                    Meter::getOldTariffIndex,
                    nullableComparableComparator(descending)
            );
            case "newtariffindex" -> comparator = Comparator.comparing(
                    Meter::getNewTariffIndex,
                    nullableComparableComparator(descending)
            );
            default -> comparator = Comparator.comparing(
                    meter -> meterSortValue(meter, normalizedSortBy),
                    nullableStringComparator(descending)
            );
        }

        return meters.stream().sorted(comparator).toList();
    }

    private <T extends Comparable<? super T>> Comparator<T> nullableComparableComparator(boolean descending) {
        Comparator<T> valueComparator = descending ? Comparator.reverseOrder() : Comparator.naturalOrder();
        return Comparator.nullsLast(valueComparator);
    }

    private Comparator<String> nullableStringComparator(boolean descending) {
        Comparator<String> valueComparator = descending
                ? String.CASE_INSENSITIVE_ORDER.reversed()
                : String.CASE_INSENSITIVE_ORDER;
        return Comparator.nullsLast(valueComparator);
    }

    private String meterSortValue(Meter meter, String sortBy) {
        return switch (sortBy.toLowerCase(Locale.ROOT)) {
            case "meternumber" -> meter.getMeterNumber();
            case "simnumber", "simno" -> meter.getSimNumber();
            case "manufacturer", "metermanufacturer", "metermanufacturername" ->
                    meter.getManufacturer() != null ? meter.getManufacturer().getName() : meter.getMeterManufacturerName();
            case "meterclass" -> meter.getMeterClass();
            case "category", "metercategory" -> meter.getMeterCategory();
            case "metertype", "type" -> meter.getMeterType();
            case "meterstage" -> meter.getMeterStage();
            case "status" -> meter.getStatus();
            case "accountnumber" -> meter.getAccountNumber();
            case "customerid" -> meter.getCustomerId();
            case "cin" -> meter.getCin();
            default -> meter.getCreatedAt() == null ? null : meter.getCreatedAt().toString();
        };
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getSingleMeter(UUID meterId, String meterNumber, String accountNumber, UUID meterVersionId, String versionMeterNumber, String cin) {
        try {
            Meter meter = null;
            UserModel um = handleUserValidation();
            UUID nodeId = um.getNodeInfo().getNodeId();

            if (meterId == null && meterNumber == null && accountNumber == null && meterVersionId == null && versionMeterNumber == null) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException("At least one of meterId, meterNumber, or accountNumber must be provided.");
            }

//            Object cachedUser = meterCache.get(meterId.toString()+"_"+um.getOrgId());

//            if (cachedUser != null) {
//                return ResponseMap.response(status.getSuccessCode(), "Cached " + meterName + " " + status.getDesc(), cachedUser);
//            }

            if(meterNumber != null){
                meter = meterMapper.getMeter(um.getOrgId(), meterId, meterNumber, accountNumber, cin, "", nodeId);
                if (meter == null){
                    return ResponseMap.response(status.getFailCode(),  "Meter Number " + status.getNotFoundDesc(), "");
                }
            }

            if(accountNumber != null){
                meter = meterMapper.getMeter(um.getOrgId(), meterId, meterNumber, accountNumber, cin, "", nodeId);
                if (meter == null){
                    return ResponseMap.response(status.getFailCode(),  "Account Number " + status.getNotFoundDesc(), "");
                }
            }

            if(meterId != null){
                meter = meterMapper.getMeter(um.getOrgId(), meterId, meterNumber, accountNumber, cin, "", nodeId);
                if (meter == null){
                    return ResponseMap.response(status.getFailCode(),  "Meter ID " + status.getNotFoundDesc(), "");
                }
            }

            if(cin != null){
                meter = meterMapper.getMeter(um.getOrgId(), meterId, meterNumber, accountNumber, cin, "", nodeId);
                if (meter == null){
                    return ResponseMap.response(status.getFailCode(),  "CIN " + status.getNotFoundDesc(), "");
                }
            }

            if(versionMeterNumber != null){
                meter = meterMapper.getVersionMeter(um.getOrgId(), meterVersionId, versionMeterNumber, cin);
                if (meter == null){
                    return ResponseMap.response(status.getFailCode(),  "Meter Version Number " + status.getNotFoundDesc(), "");
                }
            }

            if(meterVersionId != null){
                meter = meterMapper.getVersionMeter(um.getOrgId(), meterVersionId, versionMeterNumber, cin);
                if (meter == null){
                    return ResponseMap.response(status.getFailCode(),  "Meter Version ID " + status.getNotFoundDesc(), "");
                }
            }

//            handleAddCache(meter);

            return ResponseMap.response(status.getSuccessCode(),  meterName + " " + status.getDesc(), meter);
        } catch (Exception exception) {
            log.error("Error occurred while fetching feeder lines [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Editing meter service failed");
            genericHandler.logAndSaveException(exception, "fetching meter");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> changeStatus(UUID meterId, Boolean state, String reason) throws MissingServletRequestParameterException {
        int result;
        try {
            // Gather client metadata
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel user = handleUserValidation();
            UUID nodeId = user.getNodeInfo().getNodeId();
            String nodeType = user.getNodeInfo().getType();
            Meter meterById = meterMapper.findById(meterId, user.getOrgId(),nodeId);

            if(meterById == null) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getNotFoundDesc());
            }
            if((!meterById.getNodeId().equals(nodeId)
                    || !meterById.getRegion().equals(nodeId)
                    || !meterById.getServiceCenter().equals(nodeId)
                    || !meterById.getRoot().equals(nodeId))
                    && !nodeType.equalsIgnoreCase("Business hub")
                    && !nodeType.equalsIgnoreCase("Service center")
                    && !nodeType.equalsIgnoreCase("Region")
                    && !nodeType.equalsIgnoreCase("Root")){
                throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
            }

            if(state && meterById.getMeterStage().equalsIgnoreCase("Assigned")){
                Tariff tariff = tariffMapper.getApproveTariff(meterById.getTariff());
                if(tariff == null){
                    throw new GlobalExceptionHandler.NotFoundException("Tariff is either not found, not approved or deactivated");
                }
            }

            if(state){
                Meter m = meterMapper.getMeterCin(user.getOrgId(), meterById.getAccountNumber(), meterById.getCin());
                if(m != null){
                    throw new GlobalExceptionHandler.NotFoundException(
                            "Activation failed because "+m.getMeterNumber()+
                                    " meter is active with the same CIN or account number");
                }
            }

            if(meterById.getMeterStage().contains("Pending") || meterById.getStatus().contains("Pending")){
                throw new GlobalExceptionHandler.NotFoundException("Meter have a pending state that needs to be cleared");
            }
            if(meterById.getStatus().contains("Deactivated") && !state){
                throw new GlobalExceptionHandler.NotFoundException("Meter already deactivated");
            }
            if(meterById.getStatus().contains("Active") && state){
                throw new GlobalExceptionHandler.NotFoundException("Meter already activated");
            }

            meterById.setStatus("Pending-"+(state ? "activated" : "deactivated"));
            meterById.setCreatedBy(user.getId());
            meterById.setMeterId(meterById.getId());
            meterById.setReason(reason);

            String changeDescription = buildChangeStatusDescription(meterById, state);
            meterById.setDescription(state ? "Meter Activated" : "Meter Deactivated");


            result = meterMapper.insertMeterVersion(meterById);
            if(result == 0){
                throw new GlobalExceptionHandler.NotFoundException("Meter " + status.getUpdateDesc());
            }

            int u = meterMapper.updateMeter(meterById.getMeterStage(), meterById.getId(), meterById.getUpdatedAt(), meterById.getStatus());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException("Meter" + (state ? " activated " : " deactivated ")+ "failed");
            Meter meter = meterMapper.getMeter(user.getOrgId(), meterById.getMeterId(), null, null, null, "",nodeId);
            user.setPassword("");
//            handleAddCache(newTariff);
            AuditLog auditLog = buildAuditLog(user, changeDescription, meterName, meter, metadata, reason);
            safeAuditService.saveAudit(auditLog);
            return ResponseMap.response(status.getSuccessCode(), meterName + (state ? " activated ": " deactivated ")+"successfully", "");

        } catch (Exception exception) {
            log.error("Error occurred while changing user status [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Editing meter service failed");
            genericHandler.logAndSaveException(exception, "changing meter state");
            throw exception;
        }
    }

    private String buildChangeStatusDescription(Meter oldMeter, Boolean status) {
        StringBuilder changes = new StringBuilder("Edited meter ");
        String oldState = oldMeter.getStatus().trim().equalsIgnoreCase("Active") ? "activated" : "deactivated";
        String newState = status ? "activated" : "deactivated";
        if (!Objects.equals(oldMeter.getStatus(), newState)) {
            changes.append(String.format("status: '%s' → '%s' ", oldState, newState));
        }

        return changes.toString();
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getManufacturers() {
        try {

            UserModel um = handleUserValidation();

            // Get all manufacturers
            List<Manufacturer> manufacturers = meterMapper.getManufacturers(um.getOrgId());

            return ResponseMap.response(status.getSuccessCode(),  status.getDesc(), manufacturers);
        } catch (Exception exception) {
            log.error("Error occurred while fetching feeder lines [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("fetching manufacturer service failed");
            genericHandler.logAndSaveException(exception, "fetching manufacturers");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> singleCustomer(String customerId) {
        try {

            UserModel um = handleUserValidation();
            String virtualMeterNo = handleGetVirtualMeter();
            String accountNumber = handleGetAccountNumber();
            UUID nodeId = um.getNodeInfo().getNodeId();
            String nodeType = um.getNodeInfo().getType();

            // check if customer exist
            Customer isCustomer = meterMapper.findByCustomerId(customerId.trim(), um.getOrgId());
            if (isCustomer == null) {
                throw new GlobalExceptionHandler.NotFoundException("Customer is either not found");
            }

            if(nodeType.equalsIgnoreCase("Service center")){
                nodeId = nodeMapper.getParentNode(um.getOrgId(), nodeId);
            }

//            if(nodeType == null
//                    || !nodeType.equalsIgnoreCase("Root")
//                    && !nodeType.equalsIgnoreCase("Business hub")
//                    && !nodeType.equalsIgnoreCase("Service center")
//                    && !nodeType.equalsIgnoreCase("Region")){
//                throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
//            }

            if ((isCustomer.getNodeId() == null || !isCustomer.getNodeId().equals(nodeId)) &&
                    (isCustomer.getServiceCenter() == null || !isCustomer.getServiceCenter().equals(nodeId)) &&
                    (isCustomer.getRoot() == null || !isCustomer.getRoot().equals(nodeId)) &&
                    (isCustomer.getRegion() == null || !isCustomer.getRegion().equals(nodeId))) {
                throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
            }

            List<Tariff> allTariffs = tariffMapper.GetTariffs(um.getOrgId());

            Map<String, Object> response = new HashMap<>();
            response.put("customer", isCustomer);
            response.put("GeneratedAccountNumber", accountNumber);
            response.put("GeneratedVirtualMeterNo", virtualMeterNo);
            response.put("tariffs", allTariffs);

            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), response);
        } catch (Exception exception) {
            log.error("Error occurred while fetching customer [ACTION]: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Fetching customer in meter service failed");
            genericHandler.logAndSaveException(exception, "fetching customer in meter");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> assignMeterToCustomer(AssignMeterToCustomer request, MultipartFile image) {
        try {
            // Gather client metadata
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel user = handleUserValidation();
            UUID nodeId = user.getNodeInfo().getNodeId();
            String nodeType = user.getNodeInfo().getType();

            Meter meterStatus = meterMapper.hasAssignedMeter(user.getOrgId(), request.getMeterNumber());
//            boolean meterStatus = meterMapper.hasAssignedMeter(user.getOrgId(), request.getMeterNumber());
            if(meterStatus == null) throw new GlobalExceptionHandler.NotFoundException("Meter "+status.getNotFoundDesc());

            if(nodeType.equalsIgnoreCase("Service center")){
                nodeId = nodeMapper.getParentNode(user.getOrgId(), nodeId);
            }

            if((!nodeId.equals(meterStatus.getNodeId()) && !nodeType.equalsIgnoreCase("Business hub"))
                    && (!nodeId.equals(meterStatus.getRoot()) && !nodeType.equalsIgnoreCase("Root"))
                    && (!nodeId.equals(meterStatus.getRegion()) && !nodeType.equalsIgnoreCase("Region"))
                    && (!nodeId.equals(meterStatus.getServiceCenter()) && !nodeType.equalsIgnoreCase("Service center"))) {
                throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
            }
            // Validate DSS
            SubStationTransformerFeederLine dss = meterMapper.verifyDss(request.getDssAssetId(), user.getOrgId());
            if (dss == null) {
                throw new GlobalExceptionHandler.NotFoundException("DSS " + status.getNotFoundDesc());
            }

            // Validate feeder line
            SubStationTransformerFeederLine feederLine = meterMapper.verifyFeeder(request.getFeederAssetId(), user.getOrgId());
            if (feederLine == null) {
                throw new GlobalExceptionHandler.NotFoundException("Feeder line " + status.getNotFoundDesc());
            }

//            RegionBhubServiceCenter regionBhubServiceCenter = meterMapper.verifyBhub(feederLine.getParentId(), user.getOrgId());
//            if (regionBhubServiceCenter == null){
//                throw new GlobalExceptionHandler.NotFoundException("Feeder does not belong to the bushiness hub meter is allocated");
//            }

            if(!dss.getParentId().equals(feederLine.getNodeId())){
                throw new GlobalExceptionHandler.NotFoundException("DSS ("+ request.getDssAssetId() +") " +
                        "provided does not belong to the feeder line ("+request.getFeederAssetId()+")");
            }

            Tariff tariff = tariffMapper.getApproveTariff(request.getTariffId());
            if(tariff == null){
                throw new GlobalExceptionHandler.NotFoundException("Tariff is either not found, not approved or deactivated");
            }

            Customer customer = meterMapper.getByCustomer(request.getCustomerId(), meterStatus.getRegion(), user.getOrgId(), meterStatus.getNodeId());
            if(customer == null) throw new GlobalExceptionHandler.NotFoundException("Customer not found or does not belong to this region/business hub");

            request.setOrgId(user.getOrgId());
            request.setCreatedBy(user.getId());

            if(request.getMeterClass() == null) {

                System.out.println("meter number: "+meterStatus.getMeterNumber());
                System.out.println("nodeId: "+nodeId);
                // Validate main meter record
                Meter mainMeter = meterMapper.getMeter(user.getOrgId(), null, request.getMeterNumber(), null, null, request.getSimNumber(), nodeId);
                if (mainMeter == null) {
                    throw new GlobalExceptionHandler.NotFoundException("Meter " + status.getNotFoundDesc()+" or user does not belong to the business hub meter is allocated");
                }

                if (mainMeter.getMeterStage().contains("Pending") || mainMeter.getStatus().contains("Pending")) {
                    throw new GlobalExceptionHandler.NotFoundException("Meter has a pending record that needs to be cleared");
                }

                if (mainMeter.getStatus().contains("Deactivated")) {
                    throw new GlobalExceptionHandler.NotFoundException("Deactivated meter can not be assign");
                }

                // Validate node assignment
                if (mainMeter.getNodeId() == null) {
                    throw new GlobalExceptionHandler.NotFoundException(request.getMeterNumber() + " meter has not been allocated");
                }

                request.setOldKrn(mainMeter.getOldKrn());
                request.setNewKrn(mainMeter.getNewKrn());
                request.setOldSgc(mainMeter.getOldSgc());
                request.setNewSgc(mainMeter.getNewSgc());
                request.setType("NON-VIRTUAL");
                request.setOldTariffIndex(mainMeter.getOldTariffIndex());
                request.setNewTariffIndex(mainMeter.getNewTariffIndex());
                request.setMeterType(mainMeter.getMeterType());
                request.setMeterClass(mainMeter.getMeterClass());
                request.setMeterCategory(mainMeter.getMeterCategory());
                request.setSmartStatus(mainMeter.getSmartStatus());
                request.setMeterManufacturer(mainMeter.getMeterManufacturer());
                request.setMeterType(mainMeter.getMeterType());
                request.setMeterId(mainMeter.getId());
                request.setSimNumber(mainMeter.getSimNumber());
//                request.setMeterModel(mainMeter.getMeterModel());
            } else {
                request.setType("VIRTUAL");
            }

            Meter m = meterMapper.getMeterDuplicateCin(user.getOrgId(), request.getAccountNumber(), request.getCin());
            if(m != null ) {
                Map<String, Object> result = new HashMap<>();
                result.put("meter", m);
                throw new GlobalExceptionHandler.PartialFailureException(
                        "Meter already assigned to cin or account number",
                        result
                );
            }

//            request.setNodeId(feederLine.getNodeId());
            request.setFeeder(feederLine.getNodeId());
            request.setDss(dss.getNodeId());
            request.setOrgId(user.getOrgId());
            request.setCreatedBy(user.getId());

            System.out.println("bhub:: "+meterStatus.getNodeId());
            // --- Step 9: Resolve hierarchy upward ---
            resolveHierarchy(request, feederLine.getNodeId(), user.getOrgId(), meterStatus.getNodeId(), meterStatus.getRegion(), meterStatus.getRoot());

            handleMeterAssign(request);

            Meter meter = meterMapper.getVersionMeter(user.getOrgId(), null, request.getMeterNumber(), null);
            String description = "Meter assigned to customer " + request.getCustomerId();

            AuditLog auditLog = buildAuditLog(user, description, meterName, meter, metadata, "");
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(), "Meter assigned successfully", "");

        } catch (Exception exception) {
            log.error("Error occurred during meter assignment: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Assigning meter service failed");
            genericHandler.logAndSaveException(exception, "assigning meter");
            throw exception;
        }
    }

    /**
     * Resolve hierarchy upward from the starting node (feeder)
     */
    private void resolveHierarchy(AssignMeterToCustomer request, UUID startNodeId, UUID orgId, UUID bhubId, UUID region, UUID root) {

        UUID currentNodeId = startNodeId;
        Set<UUID> visited = new HashSet<>();

        while (currentNodeId != null) {

            if (!visited.add(currentNodeId)) {
                throw new IllegalStateException("Circular hierarchy detected");
            }

            NodeSummary node = nodeMapper.getNodeByNodeId(currentNodeId, orgId);
            if (node == null) break;

            String type = node.getType() == null ? "" : node.getType().toLowerCase();

            switch (type) {
                case "business hub":
                    if(bhubId.equals(node.getNodeId())){
                        request.setNodeId(node.getNodeId());
                    } else {
                        throw new GlobalExceptionHandler
                                .NotFoundException("Feeder does not belong to the bushiness hub meter is allocated");
                    }
                    break;
                case "service center":
                    request.setServiceCenter(node.getNodeId());
                    break;
                case "region":
                    if(!region.equals(node.getNodeId())){
                        throw new GlobalExceptionHandler.NotFoundException(
                                "Meter does not belong to this region"
                        );
                    } else {
                        request.setRegion(node.getNodeId());
                    }

                    break;
                case "substation":
                    request.setSubstation(node.getNodeId());
                    break;
                case "root":
                    if(!root.equals(node.getNodeId())){
                        throw new GlobalExceptionHandler.NotFoundException(
                                "Meter does not belong to this root"
                        );
                    } else {
                        request.setRoot(node.getNodeId());
                    }

                    break;
            }

            currentNodeId = node.getParentId();
        }
    }

    private void handleMeterAssign(AssignMeterToCustomer request){

        // Assign meter to customer
        request.setDescription("Meter Assigned");
        request.setMeterStage("Pending-assigned");
        request.setStatus("Active");
        int customerAssignResult;
        int customerAssignResult1;
        if(request.getType().equalsIgnoreCase("NON-VIRTUAL")){
            request.setType("NON-VIRTUAL");
            customerAssignResult = meterMapper.assignedMeterToCustomer(request.getMeterStage(), request.getStatus(), request.getMeterId(), request.getUpdatedAt());
            customerAssignResult1 = meterMapper.assignedVersionMeterToCustomer(request);
            if(customerAssignResult == 0 || customerAssignResult1 == 0)
                throw new GlobalExceptionHandler.NotFoundException("Assigning meter to customer failed");

            // Handle prepaid meter assignment
            if ("prepaid".equalsIgnoreCase(request.getMeterCategory())) {
                request.setDescription("Payment mode assigned");

                //checking for one-off

//                System.out.print(">>>>PaymentMode: "+request.getPaymentMode());

                String creditPaymentPlan = request.getCreditPaymentPlan();
                String creditPaymentMode = request.getCreditPaymentMode();
                String debitPaymentPlan = request.getDebitPaymentPlan();
                String debitPaymentMode = request.getDebitPaymentMode();

                if ((debitPaymentMode == null || creditPaymentMode.isBlank()) && (creditPaymentMode == null || creditPaymentMode.isBlank())) {
                    throw new GlobalExceptionHandler.NotFoundException("Payment mode is required");
                }

                assert debitPaymentMode != null;
                if (debitPaymentMode.equalsIgnoreCase("one-off") ||
                        debitPaymentMode.equalsIgnoreCase("percentage")) {

                    request.setDebitPaymentPlan("");

                } else if (creditPaymentMode.equalsIgnoreCase("one-off") ||
                        creditPaymentMode.equalsIgnoreCase("percentage")) {

                    request.setCreditPaymentPlan("");

                } else if (debitPaymentMode.equalsIgnoreCase("monthly")) {

                    if (debitPaymentPlan == null || debitPaymentPlan.isBlank()) {
                        throw new GlobalExceptionHandler.NotFoundException("Debit payment monthly plan is required");
                    }

                } else if (creditPaymentMode.equalsIgnoreCase("monthly")) {

                    if (creditPaymentPlan == null || creditPaymentPlan.isBlank()) {
                        throw new GlobalExceptionHandler.NotFoundException("Credit payment monthly plan is required");
                    }
                } else if (creditPaymentMode.equalsIgnoreCase("no-payment")) {

                    request.setCreditPaymentPlan("");

                } else if (debitPaymentMode.equalsIgnoreCase("no-payment")) {

                    request.setDebitPaymentPlan("");

                }
                else {
                    throw new GlobalExceptionHandler.NotFoundException("Payment mode is not supported");
                }

//                request.setStatus(true);
                int paymentModeResult = meterMapper.assignPaymentModeVersion(request);

                if (paymentModeResult == 0) {
                    throw new GlobalExceptionHandler.NotFoundException("Payment mode assignment failed");
                }

            }
        } else {
            request.setType("VIRTUAL");
            request.setMeterCategory("Postpaid");
            request.setSmartStatus(false);
            request.setSimNumber("VIRTUAL");
            request.setMeterType("Electricity");
            customerAssignResult = meterMapper.insertVirtualVersionMeterToCustomer(request);
            request.setMeterId(request.getId());
            customerAssignResult1 = meterMapper.assignedVirtualVersionMeterToCustomer(request);

            if(customerAssignResult == 0 || customerAssignResult1 == 0)
                throw new GlobalExceptionHandler.NotFoundException("Assigning virtual meter to customer failed");
        }

        if(request.getDebitCreditAdjust() != null){

            DebitCreditAdjustVersion debitCreditAdjustVersion =
                    meterMapper.getDebitAdjustmentByOldVersion(request.getDebitCreditAdjust().get(0).getMeterId());

            if(debitCreditAdjustVersion != null){
                throw new GlobalExceptionHandler.NotFoundException("Meter have a pending state that needs to be cleared");
            }

            int res = meterMapper.insertDebitCreditAdjVersion(request.getDebitCreditAdjust().get(0).getMeterId(), request.getMeterId(), request.getOrgId(), request.getCreatedAt(), true);
            if (res == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Debit credit adjustment update failed");
            }
        }

        request.setMeterId(request.getMeterId());
        int locationAssignResult = meterMapper.assignVersionMeterToLocation(request);

        if (locationAssignResult == 0) {
            throw new GlobalExceptionHandler.NotFoundException("Meter assignment to location failed");
        }

    }

    @Transactional
    @Override
    public Map<String, Object> continueAssignMeter(AssignMeterToCustomer request, MultipartFile image) {
        int result;
        boolean state = false;
        try {
            // Gather client metadata
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel user = handleUserValidation();
            UUID nodeId = user.getNodeInfo().getNodeId();
            String nodeType = user.getNodeInfo().getType();

            Meter meterById = meterMapper.findById(request.getMeterId(), user.getOrgId(), nodeId);
            if(meterById == null) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getNotFoundDesc());
            }

            if(meterById.getMeterStage().contains("Pending") || meterById.getStatus().contains("Pending")) {
                throw new GlobalExceptionHandler.NotFoundException("Meter have a pending state that needs to be cleared");
            }

            if(nodeType.equalsIgnoreCase("Service center")){
                nodeId = nodeMapper.getParentNode(user.getOrgId(), nodeId);
            }

            if(!nodeId.equals(meterById.getNodeId())
                    || !nodeId.equals(meterById.getRoot())
                    || !nodeId.equals(meterById.getServiceCenter())
                    && (!nodeType.equalsIgnoreCase("Business hub")
                    && !nodeType.equalsIgnoreCase("Root")
                    && !nodeType.equalsIgnoreCase("Service center"))){
                throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
            }

            meterById.setStatus("Deactivated");
            meterById.setCreatedBy(user.getId());
            meterById.setMeterId(meterById.getId());
            meterById.setReason("Meter replacement");

            String changeDescription = buildChangeStatusDescription(meterById, state);
            meterById.setDescription("Meter Deactivated");

            // Deactivate old meter
            int u = meterMapper.updateMeter(meterById.getMeterStage(), meterById.getId(), meterById.getUpdatedAt(), meterById.getStatus());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException("Meter deactivated failed");
            Meter meter = meterMapper.getMeter(user.getOrgId(), meterById.getMeterId(), null, null, null, "", nodeId);
            user.setPassword("");
//            handleAddCache(newTariff);
            AuditLog auditLog = buildAuditLog(user, changeDescription, meterName, meter, metadata, "Meter deactivated by replacement");
            safeAuditService.saveAudit(auditLog);

            //Assign the new meter
            // Validate DSS
            SubStationTransformerFeederLine dss = meterMapper.verifyDss(request.getDssAssetId(), user.getOrgId());
            if (dss == null) {
                throw new GlobalExceptionHandler.NotFoundException("DSS " + status.getNotFoundDesc());
            }

            // Validate feeder line
            SubStationTransformerFeederLine feederLine = meterMapper.verifyFeeder(request.getFeederAssetId(), user.getOrgId());
            if (feederLine == null) {
                throw new GlobalExceptionHandler.NotFoundException("Feeder line " + status.getNotFoundDesc());
            }

            if(!dss.getParentId().equals(feederLine.getNodeId())){
                throw new GlobalExceptionHandler.NotFoundException("DSS ("+ request.getDssAssetId() +") " +
                        "provided does not belong to the feeder line ("+request.getFeederAssetId()+")");
            }

            Tariff tariff = tariffMapper.getApproveTariff(request.getTariffId());
            if(tariff == null){
                throw new GlobalExceptionHandler.NotFoundException("Tariff is either not found, not approved or deactivated" );
            }

            Customer customer = meterMapper.getByCustomer(request.getCustomerId(), meterById.getRegion(), user.getOrgId(), meterById.getNodeId());
            if(customer == null) throw new GlobalExceptionHandler.NotFoundException("Customer not found");

            request.setOrgId(user.getOrgId());
            request.setCreatedBy(user.getId());

            if(meter.getDebitCreditAdjustInfo() != null){
                request.setDebitCreditAdjust(meter.getDebitCreditAdjustInfo());
            }

            if(request.getMeterClass() == null) {

                // Validate main meter record
                Meter mainMeter = meterMapper.getMeter(user.getOrgId(), null, request.getMeterNumber(), null, null, request.getSimNumber(), nodeId);
                if (mainMeter == null) {
                    throw new GlobalExceptionHandler.NotFoundException("Meter " + status.getNotFoundDesc());
                }

                if (mainMeter.getMeterStage().contains("Pending") || mainMeter.getStatus().contains("Pending")) {
                    throw new GlobalExceptionHandler.NotFoundException("Meter has a pending record that needs to be cleared");
                }

                if (mainMeter.getStatus().contains("Deactivated")) {
                    throw new GlobalExceptionHandler.NotFoundException("Deactivated meter can not be assign");
                }

                // Validate node assignment
                if (mainMeter.getNodeId() == null) {
                    throw new GlobalExceptionHandler.NotFoundException(request.getMeterNumber() + " meter has not been allocated");
                }

                request.setOldKrn(mainMeter.getOldKrn());
                request.setNewKrn(mainMeter.getNewKrn());
                request.setOldSgc(mainMeter.getOldSgc());
                request.setNewSgc(mainMeter.getNewSgc());
                request.setType("NON-VIRTUAL");
                request.setOldTariffIndex(mainMeter.getOldTariffIndex());
                request.setNewTariffIndex(mainMeter.getNewTariffIndex());
                request.setMeterType(mainMeter.getMeterType());
                request.setMeterClass(mainMeter.getMeterClass());
                request.setMeterCategory(mainMeter.getMeterCategory());
                request.setSmartStatus(mainMeter.getSmartStatus());
                request.setMeterManufacturer(mainMeter.getMeterManufacturer());
                request.setMeterType(mainMeter.getMeterType());
                request.setMeterId(mainMeter.getId());
                request.setSimNumber(mainMeter.getSimNumber());

            } else {
                request.setType("VIRTUAL");
            }

            request.setNodeId(feederLine.getNodeId());
            request.setDss(dss.getNodeId());
            request.setOrgId(user.getOrgId());
            request.setCreatedBy(user.getId());

            resolveHierarchy(request, feederLine.getNodeId(), user.getOrgId(), meterById.getNodeId(), meterById.getRegion(), meterById.getRoot());

            handleMeterAssign(request);

            Meter m = meterMapper.getVersionMeter(user.getOrgId(), null, request.getMeterNumber(), null);
            String description = "Meter assigned to customer " + request.getCustomerId();

            AuditLog audit = buildAuditLog(user, description, meterName, m, metadata, "Meter replacement");
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(), "Meter assigned successfully", "");

        } catch (Exception exception) {
            log.error("Error occurred while changing user status [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Assigning meter with an existing cin failed");
            genericHandler.logAndSaveException(exception, "continue assign meter");
            throw exception;
        }

    }

    @Transactional
    @Override
    public Map<String, Object> detachMeter(UUID meterId, String reason) {
        try{
            // Gather client metadata
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel um = handleUserValidation();
            UUID nodeId = um.getNodeInfo().getNodeId();
            String nodeType = um.getNodeInfo().getType();

            HandlePermission.perm(nodeType);

            // verify if meter exist
            Meter meterById = meterMapper.findById(meterId, um.getOrgId(), nodeId);
            if(meterById == null) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getNotFoundDesc() +"or you do not have permission to detach");
            }

            boolean hasUnpaid = meterById.getDebitCreditAdjustInfo().stream()
                    .anyMatch(m ->
                            m.getStatus().equalsIgnoreCase("UNPAID") ||
                                    m.getStatus().equalsIgnoreCase("PARTIALLY_PAID")
                    );

            if (hasUnpaid) {
                throw new GlobalExceptionHandler.NotFoundException(
                        meterName + " (" + meterById.getMeterNumber() + ") have unpaid credit or debit adjustment"
                );
            }


            if (meterById.getMeterStage().contains("Pending") || meterById.getStatus().contains("Pending")) {
                throw new GlobalExceptionHandler.NotFoundException("Meter have a pending record that needs to be cleared");
            }

            if(meterById.getMeterStage().equalsIgnoreCase("Deactivated")
                    || meterById.getType().equalsIgnoreCase("virtual")
                    || meterById.getCustomerId() == null) {
                throw new GlobalExceptionHandler.NotFoundException("Meters detaching failed because meter is either unassigned, deactivated and virtual");
            }

//            // Validate feeder line
//            UUID parentNode = meterMapper.getFeederParentNode(meterById.getNodeId());
//            if (parentNode == null) {
//                throw new GlobalExceptionHandler.NotFoundException("Feeder line " + status.getNotFoundDesc());
//            }

            //set meter Id
            meterById.setMeterId(meterById.getId());
            meterById.setCreatedBy(um.getId());
            meterById.setDescription("Meter detached");
            meterById.setMeterStage("Pending-detached");
            meterMapper.updateMeterCategory(um.getOrgId(), meterId, "Pending-detached", meterById.getUpdatedAt());

            meterById.setSubstation(null);
            meterById.setDss(null);
            meterById.setFeeder(null);
//            meterById.setCustomerId(null);
            meterById.setAccountNumber(null);
            meterById.setTariff(null);
            meterById.setCin(null);
            meterById.setStatus("Active");
            meterById.setReason(reason);
            int m = meterMapper.insertMeterVersion(meterById);
            if(m == 0) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " detach failed");
            }

            meterById.getPaymentMode().setMeterStage("Pending-detached");
            meterById.getPaymentMode().setOrgId(um.getOrgId());
            meterById.getPaymentMode().setCreatedBy(um.getId());
            meterById.getPaymentMode().setDescription("Location detached");
            meterById.getMeterAssignLocation().setMeterStage("Pending-detached");
            meterById.getMeterAssignLocation().setDescription("Location detached");
            meterById.getMeterAssignLocation().setOrgId(um.getOrgId());
            meterById.getMeterAssignLocation().setCreatedBy(um.getId());

            int pm = meterMapper.assignPaymentModeVer(meterById.getPaymentMode());
            if(pm == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Payment mode detach failed");
            }

            int ml = meterMapper.assignVerMeterToLocation(meterById.getMeterAssignLocation());
            if(ml == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Meter location detach failed");
            }
            // get recent meter record
            Meter meter =  meterMapper.findById(meterId, um.getOrgId(), nodeId);

            AuditLog auditLog = buildAuditLog(um, "Meter detached", meterName, meter, metadata, reason);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(), "Meter detached successfully", "");
        } catch (Exception exception) {
            log.error("Error occurred while changing user status [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Migrating meter service failed");
            genericHandler.logAndSaveException(exception, "migrating meter");
            throw exception;
        }

    }

    @Transactional
    @Override
    public Map<String, Object> migrate(PaymentMode request) {
        String desc = "";
        try {
            // Gather client metadata
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel um = handleUserValidation();
            UUID nodeId = um.getNodeInfo().getNodeId();
            String nodeType = um.getNodeInfo().getType();
            // verify if meter exist
            Meter meterById = meterMapper.findById(request.getMeterId(), um.getOrgId(), nodeId);
            if(meterById == null) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getNotFoundDesc());
            }
            if (meterById.getMeterStage().contains("Pending") || meterById.getStatus().contains("Pending")) {
                throw new GlobalExceptionHandler.NotFoundException(
                        "Meter have a pending record that needs cleared"
                );
            }

            if(meterById.getMeterStage().equalsIgnoreCase("Deactivated")
                    || meterById.getType().equalsIgnoreCase("virtual")
                    || meterById.getCustomerId() == null) {
                throw new GlobalExceptionHandler.NotFoundException("Meters migration failed because meter is either unassigned, deactivated and virtual");
            }

            if(!nodeId.equals(meterById.getNodeId())
                    || !nodeId.equals(meterById.getServiceCenter())
                    && (!nodeType.equalsIgnoreCase("Business hub")
                    && !nodeType.equalsIgnoreCase("Service center"))){
                throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
            }

//            if(request.getMigrationFrom().equalsIgnoreCase("postpaid") && meterById.getMeterCategory().equalsIgnoreCase("prepaid")){
//                throw new GlobalExceptionHandler.NotFoundException("Meter is a prepaid meter");
//            }

            // prevent MD meter from migrating
            if(meterById.getMeterClass().equalsIgnoreCase("MD")){
                throw new GlobalExceptionHandler.NotFoundException("MD meter can not be migrated" );
            }
            String meterStage = "Pending-migrated";
            String description = "Meter Migrated";
            meterById.setMeterId(request.getMeterId());
            meterById.setCreatedBy(um.getId());
            meterById.setMeterStage(meterStage);
            meterById.setDescription(description);
            request.setOrgId(meterById.getOrgId());
            request.setCreatedBy(um.getId());
            request.setDescription(description);
            request.setMeterStage(meterStage);


            //migrate to prepaid
            if(request.getMigrationFrom().equalsIgnoreCase("postpaid") && meterById.getMeterCategory().equalsIgnoreCase("postpaid")){
                desc = "Meter migration from postpaid to prepaid";

                if(request.getDebitPaymentPlan().equalsIgnoreCase("one-off") &&
                        request.getDebitPaymentPlan().equalsIgnoreCase("percentage")){
                    request.setDebitPaymentPlan("");
                } else if(request.getCreditPaymentPlan().equalsIgnoreCase("one-off") &&
                        request.getCreditPaymentPlan().equalsIgnoreCase("percentage")){
                    request.setCreditPaymentPlan("");
                }
                else if(request.getDebitPaymentMode().equalsIgnoreCase("monthly") &&
                        request.getDebitPaymentPlan() == null || request.getDebitPaymentPlan().isBlank()) {
                    throw new GlobalExceptionHandler.NotFoundException("Debit Payment monthly plan is required");
                } else if(request.getCreditPaymentMode().equalsIgnoreCase("monthly") &&
                        request.getCreditPaymentPlan() == null || request.getCreditPaymentPlan().isBlank()) {
                    throw new GlobalExceptionHandler.NotFoundException("Credit Payment monthly plan is required");
                } else if(request.getDebitPaymentMode().equalsIgnoreCase("no-payment")) {
                    request.setDebitPaymentPlan("");
                } else if(request.getCreditPaymentMode().equalsIgnoreCase("no-payment")) {
                    request.setCreditPaymentPlan("");
                }
                else {
                    throw new GlobalExceptionHandler.NotFoundException("Payment mode field is required");
                }

                meterMapper.updateMeterCategory(um.getOrgId(), request.getMeterId(), meterStage, meterById.getUpdatedAt());

                meterById.setMeterCategory("Prepaid");

                int m = meterMapper.insertMeterVersion(meterById);
                if(m == 0) throw new GlobalExceptionHandler.NotFoundException(meterName+ " Migration " +status.getRegFailureDesc());
                request.setStatus(true);

                // insert payment method
                int migrate = meterMapper.assignPaymentModeWhenMigrationToPrepaid(request);
                if(migrate == 0) throw new GlobalExceptionHandler.NotFoundException(meterName+ " migration failed");

            } else if(request.getMigrationFrom().equalsIgnoreCase("prepaid") && meterById.getMeterCategory().equalsIgnoreCase("prepaid")){
                desc = "Meter migration from prepaid to postpaid";

                request.setDebitPaymentMode(meterById.getPaymentMode().getDebitPaymentMode());
                request.setDebitPaymentPlan(meterById.getPaymentMode().getDebitPaymentPlan());

                request.setCreditPaymentMode(meterById.getPaymentMode().getCreditPaymentMode());
                request.setCreditPaymentPlan(meterById.getPaymentMode().getCreditPaymentPlan());

                meterMapper.updateMeterCategory(um.getOrgId(), request.getMeterId(), meterStage, meterById.getUpdatedAt());

                meterById.setMeterCategory("Postpaid");

                int m = meterMapper.insertMeterVersion(meterById);
                if(m == 0) throw new GlobalExceptionHandler.NotFoundException(meterName+ " Migration " +status.getRegFailureDesc());

                request.setStatus(false);

                // insert payment method
                int migrate = meterMapper.assignPaymentModeWhenMigrationToPrepaid(request);
                if(migrate == 0) throw new GlobalExceptionHandler.NotFoundException(meterName+ " migration failed");
            } else {
                throw new GlobalExceptionHandler.NotFoundException("Migration not allowed because meter is already "+meterById.getMeterCategory());
            }

            // get recent meter record
            Meter meter = meterMapper.findById(request.getMeterId(), um.getOrgId(), nodeId);

//            handleAddCache(meter);
            AuditLog auditLog = buildAuditLog(um, desc, meterName, meter, metadata, "");
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(), "Meter migrated successfully", "");

        } catch (Exception exception) {
            log.error("Error occurred while changing user status [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logIncidentReport("Migrating meter service failed");
            genericHandler.logAndSaveException(exception, "migrating meter");
            throw exception;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Map<String, Object> approve(UUID meterVersionId, String approveStatus)
            throws MissingServletRequestParameterException {

        try {
            // --- Step 1: Validate request ---
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            UserModel user = handleUserValidation();
            UUID nodeId = user.getNodeInfo().getNodeId();
            String nodeType = user.getNodeInfo().getType();

            HandlePermission.perm(nodeType);

            Meter meter = meterMapper.findByIdApproveVersion(meterVersionId, user.getOrgId(), nodeId);

            if (meter == null) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getNotFoundDesc()+" or No permission");
            }

            if(meter.getMeterStage().equalsIgnoreCase("Pending-allocated")
                    && (!nodeType.equalsIgnoreCase("Region")
                    && !nodeType.equalsIgnoreCase("Root"))){
                throw new GlobalExceptionHandler.NotFoundException(
                        "You do not have permission to approve allocated meter");
            }

            prepareMeterForApproval(meter, user, meterVersionId);

            // --- Step 2: Handle approval / rejection ---
            if (isApprove(approveStatus)) {
                handleApproval(meter, user, approveStatus);
            } else if (isReject(approveStatus)) {
                handleRejection(meter, approveStatus, user);
            } else {
                throw new MissingServletRequestParameterException("approveStatus", "not found");
            }

            // --- Step 3: Audit log ---
            Meter updatedMeter = meterMapper.findById(meter.getId(), user.getOrgId(), nodeId);
            user.setPassword(null); // hide password in logs
            AuditLog auditLog = buildAuditLog(user, "Meter "+ approveStatus+"ed", meterName, updatedMeter, metadata, "");
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(
                    status.getSuccessCode(),
                     meterName + " ("+meter.getMeterNumber() +") " + approveStatus + "d successfully",
                    ""
            );

        } catch (Exception ex) {
            log.error("Error occurred while approving/rejecting meter: {}", ex.getMessage(), ex);
            genericHandler.logIncidentReport("approving meter service failed");
            genericHandler.logAndSaveException(ex, "approving meter");
            throw ex;
        }
    }


    private void prepareMeterForApproval(Meter meter, UserModel user, UUID meterVersionId) {
        meter.setOrgId(user.getOrgId());
        meter.setApproveBy(user.getId());

        if (meter.getMdMeterInfo() != null) {
            meter.getMdMeterInfo().setMeterId(meterVersionId);
            meter.getMdMeterInfo().setOrgId(user.getOrgId());
            meter.getMdMeterInfo().setApproveBy(user.getId());
        }

        if (meter.getSmartMeterInfo() != null) {
            meter.getSmartMeterInfo().setMeterId(meterVersionId);
            meter.getSmartMeterInfo().setOrgId(user.getOrgId());
            meter.getSmartMeterInfo().setApproveBy(user.getId());
        }
    }

    private void handleApproval(Meter meter, UserModel user, String approveStatus) {

        meter.setApproveBy(user.getId());

        String stage = meter.getMeterStage() != null ? meter.getMeterStage().trim() : "";
        String stat = meter.getStatus() != null ? meter.getStatus().trim() : "";

        // === Handle Pending-created cases ===
        if (stage.equalsIgnoreCase("Pending-created")) {

            if (meter.getMdMeterInfo() != null && meter.getSmartMeterInfo() != null) {
                System.out.println("Case: both mdMeterInfo and smartMeterInfo present");
                meter.setMeterStage("Created");
                meter.setStatus("Active");
                meter.getMdMeterInfo().setMeterStage("Created");
                meter.getSmartMeterInfo().setMeterStage("Created");

            } else if (meter.getMdMeterInfo() == null && meter.getSmartMeterInfo() != null) {
                System.out.println("Case: only smartMeterInfo present");
                meter.setMeterStage("Created");
                meter.setStatus("Active");
                meter.getSmartMeterInfo().setMeterStage("Created");

            } else if (meter.getMdMeterInfo() != null && meter.getSmartMeterInfo() == null) {
                System.out.println("Case: only mdMeterInfo present");
                meter.setMeterStage("Created");
                meter.setStatus("Active");
                meter.getMdMeterInfo().setMeterStage("Created");

            } else {
                System.out.println("Case: neither mdMeterInfo nor smartMeterInfo present");
                meter.setMeterStage("Created");
                meter.setStatus("Active");
            }

        // === Handle Pending-assigned ===
        } else if (stage.equalsIgnoreCase("Pending-assigned")) {
            meter.setMeterStage("Assigned");
            meter.setStatus("Active");

        // === Handle Pending-allocated ===
        } else if (stage.equalsIgnoreCase("Pending-allocated")) {
            meter.setMeterStage("Unassigned");
            meter.setStatus("Active");

        // === Handle Pending-detached ===
        } else if (stage.equalsIgnoreCase("Pending-detached")) {
            meter.setMeterStage("Unassigned");
            meter.setStatus("Deactivated");

        // === Handle Pending-migrated ===
        } else if (stage.equalsIgnoreCase("Pending-migrated") && meter.getSmartMeterInfo() != null) {
            meter.getSmartMeterInfo().setMeterStage("Active");
            meter.getPaymentMode().setMeterStage("Active");
            meter.setMeterStage("Assigned");
            meter.setStatus("Active");

        // === Handle Pending-edited ===
        } else if (stage.equalsIgnoreCase("Pending-edited")
                || stage.equalsIgnoreCase("Assign-edited")) {
            if (meter.getCustomerId() == null && meter.getNodeId() != null) {
                meter.setMeterStage("Unassigned");
                meter.setStatus("Active");
            } else if (meter.getCustomerId() == null && meter.getNodeId() == null) {
                meter.setMeterStage("Created");
                meter.setStatus("Active");
            } else if (meter.getCustomerId() != null && meter.getNodeId() != null) {
                meter.setMeterStage("Assigned");
                meter.setStatus("Active");
            }

            // === Handle Pending-deactivated ===
            } else if (stat.equalsIgnoreCase("Pending-deactivated")) {
                meter.setStatus("Deactivated");

            // === Handle Pending-activated ===
            } else if (stat.equalsIgnoreCase("Pending-activated")) {
                meter.setStatus("Active");

            // === Default fallback ===
            } else {
                meter.getPaymentMode().setMeterStage("Active");
                meter.setMeterStage("Assigned");
                meter.setStatus("Active");
            }

        int approved = meterMapper.approvedMeterVersion(meter.getMeterStage(), meter.getStatus(), meter.getApproveBy(), meter.getUpdatedAt(), meter.getMeterNumber());
        if (approved == 0) {
            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "d " + status.getUpdateFailureDesc());
        }

        String customerId = meter.getCustomerId();

        List<Meter> c = customerMapper.totalCustomer(customerId);

        meter.setCustomerId(customerId);

        if(!"Pending-detached".equalsIgnoreCase(stage)){

            if("Pending-assigned".equalsIgnoreCase(stage)) {

                if (meterMapper.approvePendingMeter(meter) == 0) {
                    throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "d " + status.getUpdateFailureDesc());
                }
                //Change customer status to Active
                int customerStatus = customerMapper.changeStatusCustomer(meter.getCustomerId(), "Active",user.getOrgId());
                if (customerStatus == 0) {
                    throw new GlobalExceptionHandler.NotFoundException("Customer status update failed");
                }
            } else {

                if ("Pending-edited".equalsIgnoreCase(stage)
                        || "Assign-edited".equalsIgnoreCase(stage)) {

                    if (meter.getCustomerId() != null) {

                        if (meterMapper.updateMeterAssignedMeter(meter) == 0) {
                            throw new GlobalExceptionHandler.NotFoundException(
                                    meterName + " " + approveStatus + "d " + status.getUpdateFailureDesc());
                        }

                    } else if (meter.getNodeId() == null) {

                        if (meterMapper.updateMeterCreatedMeter(meter) == 0) {
                            throw new GlobalExceptionHandler.NotFoundException(
                                    meterName + " " + approveStatus + "d " + status.getUpdateFailureDesc());
                        }

                    } else {

                        if (meterMapper.updateMeterAllocatedMeter(meter) == 0) {
                            throw new GlobalExceptionHandler.NotFoundException(
                                    meterName + " " + approveStatus + "d " + status.getUpdateFailureDesc());
                        }
                    }

                } else {

                    if (meterMapper.approveMeter(meter) == 0) {
                        throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "d " + status.getUpdateFailureDesc());
                    }
                }

//                //Change customer status to Active
//                if(c == 1) {
//                    int customerStatus = customerMapper.changeStatusCustomer(meter.getCustomerId(), "Inactive",user.getOrgId());
//                    if (customerStatus == 0) {
//                        throw new GlobalExceptionHandler.NotFoundException("Customer status update failed");
//                    }
//                }

            }

            //approve meter location
            if(meter.getMeterAssignLocation() != null ){
                meter.getMeterAssignLocation().setApproveBy(user.getId());
                approveMeterAssignLocation(meter);
            }

            if (meter.getMdMeterInfo() != null) {
                meter.getMdMeterInfo().setApproveBy(user.getId());
                approveMDMeterInfo(meter);
            }

            if (meter.getSmartMeterInfo() != null) {
                meter.getSmartMeterInfo().setApproveBy(user.getId());
                approveSmartMeterInfo(meter);
            }

            //approve payment mode for prepaid meter Information
            if(meter.getPaymentMode() != null){
                meter.getPaymentMode().setApproveBy(user.getId());
                approvePrepaidMeterInfo(meter);
            }
        }

        if("Pending-detached".equalsIgnoreCase(stage)){
            if (meterMapper.meterApproval(meter) == 0) {
                throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "d " + status.getUpdateFailureDesc());
            }

            if(meter.getMeterAssignLocation() != null){
                meter.getMeterAssignLocation().setApproveBy(user.getId());
                meter.getMeterAssignLocation().setOrgId(user.getOrgId());
                meter.getMeterAssignLocation().setMeterStage("Approved");

                if(meterMapper.removeAssignedLocation(meter.getMeterId()) == 0){
                    throw new GlobalExceptionHandler.NotFoundException("Unassigned location failed");
                }
                if(meterMapper.approveMeterAssignLocationVersion(meter.getMeterAssignLocation()) == 0){
                    throw new GlobalExceptionHandler.NotFoundException("Unassigned location failed");
                }
            }

            if(meter.getPaymentMode() != null){
                meter.getPaymentMode().setStatus(false);
                meter.getPaymentMode().setApproveBy(user.getId());
                meter.getPaymentMode().setOrgId(user.getOrgId());
                meter.getPaymentMode().setMeterStage("Approved");

                if(meterMapper.removePaymentMode(meter.getMeterId()) == 0){
                    throw new GlobalExceptionHandler.NotFoundException("Unassigned payment mode failed");
                }

                if(meterMapper.approvePrepaidMeterVersion(meter.getPaymentMode()) == 0){
                    throw new GlobalExceptionHandler.NotFoundException("Unassigned payment mode failed");
                }
            }

            System.out.println("customer size: "+c.size());

            if(c.size() == 1) {
                int customerStatus = customerMapper.changeStatusCustomer(meter.getCustomerId(), "Inactive",user.getOrgId());
                if (customerStatus == 0) {
                    throw new GlobalExceptionHandler.NotFoundException("Customer status update failed");
                }
            }
        }

        if(meter.getDebitCreditAdjustVersionInfo() != null){
            int res1 = meterMapper.updateDebitCreditAdj(
                    meter.getDebitCreditAdjustVersionInfo().getOldMeterId(),
                    meter.getDebitCreditAdjustVersionInfo().getNewMeterId(), user.getOrgId());

            int res2 = meterMapper.updateDebitCreditAdjVersion(
                    meter.getDebitCreditAdjustVersionInfo().getOldMeterId(),
                    meter.getDebitCreditAdjustVersionInfo().getNewMeterId(),
                    false, user.getOrgId());

            if (res1 == 0 || res2 == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Debit credit adjustment replacement failed");
            }
        }


    }

    private void approveMeterAssignLocation(Meter meter) {
        int updateMeterLocationApproval,meterLocationApproval;
        meterLocationApproval = meterMapper.approveMeterAssignLocationVersion(meter.getMeterAssignLocation());

        MeterAssignLocation check = meterMapper.getMeterAssignLocation(meter.getMeterId());
        if (check == null) {
            updateMeterLocationApproval = meterMapper.insertMeterLocation(meter.getMeterAssignLocation());
        } else {
            updateMeterLocationApproval = meterMapper.updateMeterLocation(meter.getMeterAssignLocation());
        }
        if (updateMeterLocationApproval == 0 || meterLocationApproval == 0) {
            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());
        }
    }

    private void approveMDMeterInfo(Meter meter) {
        int updateMDInfoApproval, mdInfoApproval;
        meter.getMdMeterInfo().setMeterStage(meter.getMeterStage());
        mdInfoApproval = meterMapper.approveMDMeterInfoVersion(meter.getMdMeterInfo());
        MDMeterInfo check = meterMapper.getMDMeterInfo(meter.getMeterId());
        if (check == null) {
            updateMDInfoApproval = meterMapper.insertMDMeterInfo(meter.getMdMeterInfo());
        } else if(meter.getMdMeterInfo() != null){
            updateMDInfoApproval = meterMapper.updateMDMeterInfo(meter.getMdMeterInfo());
        } else {
            return;
        }
        if (updateMDInfoApproval == 0 || mdInfoApproval == 0) {
            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());
        }
    }

    private void approveSmartMeterInfo(Meter meter) {
        int updateMDInfoApproval, mdInfoApproval;
        meter.getSmartMeterInfo().setMeterStage(meter.getMeterStage());
        mdInfoApproval = meterMapper.approveSmartMeterInfoVersion(meter.getSmartMeterInfo());
        SmartMeterInfo check = meterMapper.getSmartMeter(meter.getMeterId());
        if (check == null) {
            updateMDInfoApproval = meterMapper.insertSmartMeterInfo(meter.getSmartMeterInfo());
        } else if(meter.getSmartMeterInfo() != null){
            updateMDInfoApproval = meterMapper.updateSmartMeterInfo(meter.getSmartMeterInfo());
        }
        else {
           return;
        }
        if (updateMDInfoApproval == 0 || mdInfoApproval == 0) {
            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());
        }
    }

    private void approvePrepaidMeterInfo(Meter meter) {
        int updateMDInfoApproval, mdInfoApproval;
        mdInfoApproval = meterMapper.approvePrepaidMeterVersion(meter.getPaymentMode());
        if (mdInfoApproval == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());

        PaymentMode check = meterMapper.getPaymentMode(meter.getMeterId());
        if (check == null) {
            updateMDInfoApproval = meterMapper.insertPrepaidMeterVersion(meter.getPaymentMode());
            if (updateMDInfoApproval == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());

        } else if(meter.getPaymentMode() != null){
            updateMDInfoApproval = meterMapper.updatePrepaidMeterVersion(meter.getPaymentMode());
            if (updateMDInfoApproval == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());

        }
        else {
            return;
        }
//        if (updateMDInfoApproval == 0 || mdInfoApproval == 0) {
//            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + status.getUpdateFailureDesc());
//        }
    }

    private void handleRejection(Meter meter, String approveStatus, UserModel user) {
//        if(meter.getDebitCreditAdjustVersionInfo() != null) {
//            throw new GlobalExceptionHandler.NotFoundException(meter.getDebitCreditAdjustVersionInfo().getDescription());
//        }

        String st = meter.getMeterStage();
        String status = meter.getStatus();
        String s = status.equalsIgnoreCase("Pending-deactivated") ? "Active" : status.equalsIgnoreCase("Active") ? "Active" : "Deactivated";
        int reject;


        //Update meter meter-stage status in meters_version table to rejected
         reject = meterMapper.rejectedMeterVersion("Rejected", meter.getMeterNumber(), meter.getUpdatedAt(), user.getId(), s);
        if (reject == 0) {
            throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "ed failed");
        }

        //Update assigned location approve status to rejected in meter_assign_locations_version table
        if(meter.getMeterAssignLocation() != null) {
            int result = meterMapper.updateMeterAssignedLocation("Rejected", meter.getMeterAssignLocation().getMeterId(), user.getOrgId(), meter.getUpdatedAt(), user.getId());
            if(result == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " assigned location failed");
        }

        if(meter.getPaymentMode() != null){
            //Update smart meter Info, mater-stage to rejected in payment_mode_version table
            int result = meterMapper.removePaymentModeInfo("Rejected", meter.getPaymentMode().getMeterId(), user.getOrgId(), user.getId());
            if(result == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " assign payment mode failed");
        }

        if(meter.getMeterStage().trim().equalsIgnoreCase("Pending-created")){

            //Delete meter record in meters table
           int res = meterMapper.removeMeter(meter.getMeterNumber(), user.getOrgId());

            //Update MD meter Info, mater-stage to rejected in md_meters_info_version table
            if ("md".equalsIgnoreCase(meter.getMeterClass()) && !st.equalsIgnoreCase("Pending-allocated")) {
                res = meterMapper.updateMDMeterInfoVersion("Rejected", meter.getMdMeterInfo().getMeterId(), user.getOrgId(), user.getId());
            }

            //Update smart meter Info, mater-stage to rejected in smart_meter_info_version table
            if (Boolean.TRUE.equals(meter.getSmartStatus() && !st.equalsIgnoreCase("Pending-allocated"))) {
                res = meterMapper.updateSmartMeterInfoVersion("Rejected", meter.getSmartMeterInfo().getMeterId(), user.getOrgId(), user.getId());
            }

            if(res == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " failed to delete");

        }
        else if (meter.getMeterStage().trim().equalsIgnoreCase("Pending-assigned")
                && meter.getType().equalsIgnoreCase("virtual")) {
            int u = meterMapper.removeMeter(meter.getMeterNumber(), user.getOrgId());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " " + approveStatus + "ed failed");
        }
        else if((meter.getMeterStage().trim().equalsIgnoreCase("Pending-allocated"))
                && meter.getCustomerId() == null && meter.getNodeId() != null) {
            meter.setMeterStage("Created");
            meter.setStatus("Active");
            int u = meterMapper.updateMeter(meter.getMeterStage(), meter.getMeterId(), meter.getUpdatedAt(), meter.getStatus());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " deactivation failed");
        } else if((meter.getMeterStage().trim().equalsIgnoreCase("Pending-edited"))
                && meter.getCustomerId() == null && meter.getNodeId() != null) {
            meter.setMeterStage("Unassigned");
            meter.setStatus("Active");
            handleMeterInfoRejection(meter, user);
        } else if((meter.getMeterStage().trim().equalsIgnoreCase("Pending-edited"))
                && meter.getCustomerId() == null && meter.getNodeId() == null) {
            meter.setMeterStage("Created");
            meter.setStatus("Active");
            handleMeterInfoRejection(meter, user);
        } else if((meter.getMeterStage().trim().equalsIgnoreCase("Pending-assigned"))
                && meter.getCustomerId() != null && meter.getNodeId() != null && meter.getType().equalsIgnoreCase("non-virtual")) {
            meter.setMeterStage("Unassigned");
            meter.setStatus("Active");
//            handlePendingAssignedRejection(meter, user);
            int u = meterMapper.updateMeter(meter.getMeterStage(), meter.getMeterId(), meter.getUpdatedAt(), meter.getStatus());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " deactivation failed");
        } else if(meter.getMeterStage().trim().equalsIgnoreCase("Pending-edited")
                && meter.getCustomerId() != null && meter.getNodeId() != null) {
            meter.setMeterStage("Assigned");
            meter.setStatus("Active");
            handleMeterInfoRejection(meter, user);
        }  else if((meter.getMeterStage().trim().equalsIgnoreCase("Pending-detached")
                || meter.getMeterStage().trim().equalsIgnoreCase("Pending-migrated"))) {
            meter.setMeterStage("Assigned");
            meter.setStatus("Active");
            int u = meterMapper.updateMeter(meter.getMeterStage(), meter.getMeterId(), meter.getUpdatedAt(), meter.getStatus());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " migration failed");
        }
        else {
            meter.setStatus(s);
            int u = meterMapper.updateMeter(meter.getMeterStage(), meter.getMeterId(), meter.getUpdatedAt(), meter.getStatus());
            if(u == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " update failed");
        }

        if(meter.getDebitCreditAdjustVersionInfo() != null){
            int res = meterMapper.updateDebitCreditAdjVersion(
                    meter.getDebitCreditAdjustVersionInfo().getOldMeterId(),
                    meter.getDebitCreditAdjustVersionInfo().getNewMeterId(),
                    false, user.getOrgId());
            if (res == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Debit credit adjustment replacement rejection failed");
            }
        }

//        if(meter.getDebitCreditAdjustVersionInfo() != null){
//            int res = meterMapper.updateMeter(
//                    "Assigned",
//                    meter.getDebitCreditAdjustVersionInfo().getOldMeterId(),
//                    meter.getUpdatedAt(),
//                    "Active");
//            if (res == 0) {
//                throw new GlobalExceptionHandler.NotFoundException("Debit credit adjustment replacement rejection failed");
//            }
//        }
    }

    void handleMeterInfoRejection(Meter meter, UserModel user) {
        if(meter.getMdMeterInfo() != null){
            int v = meterMapper.updateMDMeter(meter.getMeterStage(), meter.getMeterId(), meter.getUpdatedAt(), meter.getStatus(), user.getId());
            if(v == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " editing failed");
        }
        if(meter.getSmartMeterInfo() != null){
            int v = meterMapper.updateSmartMeter(meter.getMeterStage(), meter.getMeterId(), meter.getUpdatedAt(), meter.getStatus(), user.getId());
            if(v == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " editing failed");
        }
        int m = meterMapper.updateMeter(meter.getMeterStage(), meter.getMeterId(), meter.getUpdatedAt(), meter.getStatus());
        if(m == 0) throw new GlobalExceptionHandler.NotFoundException(meterName + " editing failed");
    }

    private boolean isApprove(String status) {
        return status != null && status.toLowerCase().contains("approve");
    }

    private boolean isReject(String status) {
        return status != null && status.toLowerCase().contains("reject");
    }

    @Transactional
    @Override
    public Map<String, Object> allocateMeter(String meterNumber, String regionId) {
        try {
            // Gather client metadata
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

            UserModel um = handleUserValidation();

            UUID nodeId = um.getNodeInfo().getNodeId();
            String nodeName = um.getNodeInfo().getType();


            Meter verifyMeter = meterMapper.getMeterAlloc(um.getOrgId(), null, meterNumber, null, null, "", nodeId);
            if(verifyMeter == null){
                throw new GlobalExceptionHandler.NotFoundException("Meter " + status.getNotFoundDesc() +"or No Permission");
            }

            if (verifyMeter.getMeterStage().contains("Pending") || verifyMeter.getStatus().contains("Pending")) {
                throw new GlobalExceptionHandler.NotFoundException("Meter has a pending record that needs to be cleared");
            }

            if((!nodeName.equalsIgnoreCase("Region")
                    && !nodeId.equals(verifyMeter.getRegion()))
                    && (!nodeName.equalsIgnoreCase("Root")
                    && !nodeId.equals(verifyMeter.getRoot()))) {
                throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
            }
            // Fetch starting node using regionId
            NodeSummary node = nodeMapper.verifyNode(regionId, um.getOrgId());
            if (node == null) {
                throw new GlobalExceptionHandler.NotFoundException("Node " + status.getNotFoundDesc());
            }

//            resolveNodeHierarchyById(verifyMeter, node.getNodeId(), um.getOrgId());
//            UUID currentNodeId = node.getNodeId();
//
//            Set<UUID> visited = new HashSet<>();
//
//            while (currentNodeId != null) {
//
//                if (!visited.add(currentNodeId)) {
//                    throw new IllegalStateException("Circular hierarchy detected");
//                }
//
//                NodeSummary currentNode = nodeMapper.getNodeByNodeId(currentNodeId, um.getOrgId());
//
//                if (currentNode  == null) {
//                    break;
//                }
//
//                String type = currentNode.getType() == null ? "" : currentNode.getType().toLowerCase();
//
//                switch (type) {
//
//                    case "region":
//                        verifyMeter.setRegion(currentNode.getNodeId());
//                        break;
//
//                    case "service center":
//                        verifyMeter.setServiceCenter(currentNode.getNodeId());
//                        break;
//
//                    case "business hub":
//                        verifyMeter.setNodeId(currentNode.getNodeId());
//                        break;
//
//                    case "feeder line":
//                        verifyMeter.setFeeder(currentNode.getNodeId());
//                        break;
//
//                    case "dss":
//                        verifyMeter.setDss(currentNode.getNodeId());
//                        break;
//
//                    case "substation":
//                        verifyMeter.setSubstation(currentNode.getNodeId());
//                        break;
//
//                    case "root":
//                        verifyMeter.setRoot(currentNode.getNodeId());
//                        break;
//                }
//
//                currentNodeId = currentNode.getParentId();
//            }

            verifyMeter.setCreatedAt(LocalDateTime.now());
            verifyMeter.setUpdatedAt(LocalDateTime.now());

            String desc = meterNumber + " meter allocated to " + regionId;

            System.out.println("region: "+verifyMeter.getRegion());
            //Allocate meter
            int result;
            result = meterMapper.allocateMeterVersion(verifyMeter, node.getNodeId(), um.getId(), "Meter Allocated", node.getParentId());
            if(result == 0){
                throw new GlobalExceptionHandler.NotFoundException("Meter allocation failed");
            }

            result = meterMapper.updateMeter("Pending-allocated", verifyMeter.getId(), verifyMeter.getUpdatedAt(), verifyMeter.getStatus());
            if(result == 0){
                throw new GlobalExceptionHandler.NotFoundException("Meter allocation failed");
            }

            //fetch meter from the database
            Meter meter = meterMapper.getVersionMeter(um.getOrgId(), null, meterNumber, null);
//            String desc = capitalizeFirstLetter(meter.getMeterNumber() + " allocated " + node.getName());
            //save to audit (mongodb)
            AuditLog auditLog = buildAuditLog(um, desc, meterName, meter, metadata, "");
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(), meterName + " allocated successfully" , "");

        } catch (Exception exception) {
            log.error("Error filtering / fetching meters: {}", exception.getMessage(), exception);
            genericHandler.logIncidentReport("Allocating meter service failed");
            genericHandler.logAndSaveException(exception, "allocating meter");
            throw exception;
        }
    }

//    private void resolveNodeHierarchyById(Meter request, UUID startNodeId, UUID orgId) {
//
//        UUID currentNodeId = startNodeId;
//        Set<UUID> visited = new HashSet<>();
//
//        while (currentNodeId != null) {
//
//            if (!visited.add(currentNodeId)) {
//                throw new IllegalStateException("Circular hierarchy detected");
//            }
//
//            NodeSummary node = nodeMapper.getNodeByNodeId(currentNodeId, orgId);
//            if (node == null) break;
//
//            String type = node.getType() == null ? "" : node.getType().toLowerCase();
//
//            switch (type) {
//                case "business hub":
//                        request.setNodeId(node.getNodeId());
//                    break;
//                case "service center":
//                    request.setServiceCenter(node.getNodeId());
//                    break;
//                case "region":
//                    if(!request.getRegionInfo().getNodeId().equals(node.getNodeId())){
//                        throw new GlobalExceptionHandler
//                                .NotFoundException("Meter does not belong to this region");
//
//                    }
//                    request.setRegion(node.getNodeId());
//                    break;
//                case "substation":
//                    request.setSubstation(node.getNodeId());
//                    break;
//                case "root":
//                    request.setRoot(node.getNodeId());
//                    break;
//            }
//
//            currentNodeId = node.getParentId();
//        }
//    }


    @Async("bulkUploadExecutor")
    public CompletableFuture<Integer> insertSingleAsync(
            Meter meter, UserModel user, List<GenericResp> failedRecords) {
        try {
            insertSingleTransactional(meter, user);
            return CompletableFuture.completedFuture(1);
        } catch (Exception e) {
            String reason = extractErrorMessage(e);
            GenericResp resp = new GenericResp();
            resp.setId(meter.getMeterId().toString());
            resp.setMessage("Meter Allocate failed: "+reason);
            resp.setData(meter.getMeterNumber());

            failedRecords.add(resp);
//            failedRecords.add(meter.getMeterNumber() + " (" + reason + ")");
            log.warn("Async single insert failed for {}: {}", meter.getMeterNumber(), reason);
            return CompletableFuture.completedFuture(0);
        }
    }

    @Override
    public Map<String, Object> bulkUpload(MultipartFile file) throws IOException {
        try {
            UserModel user = handleUserValidation();

            // Determine file type
            String filename = Optional.ofNullable(file.getOriginalFilename())
                    .orElseThrow(() -> new IOException("File has no name"));

            String nodeName = user.getNodeInfo().getType();
            UUID nodeId = user.getNodeInfo().getNodeId();
            List<Meter> meters;
            if(nodeName.equalsIgnoreCase("Region")
                    || nodeName.equalsIgnoreCase("Root")) {
                if (filename.endsWith(".csv")) {
                    meters = processCsv(file.getInputStream(), user);
                } else if (filename.endsWith(".xlsx")) {
                    meters = processExcel(file.getInputStream(), user);
                } else {
                    throw new IOException("Unsupported file format. Only .csv or .xlsx allowed.");
                }
            } else {
                throw new IOException("You do not have permission");
            }
            return bulkInsertMeters(meters, user);

        } catch (Exception e) {
            log.error("Error in bulk upload: {}", e.getMessage(), e);
            genericHandler.logIncidentReport("Bulk upload service failed");
            genericHandler.logAndSaveException(e, "Bulk upload meter");
            throw new IOException("Bulk upload failed: " + e.getMessage());
        }
    }
//
//    private void resolveNodeHierarchy(Meter request, UUID startNodeId, UUID orgId) {
//
//        UUID currentNodeId = startNodeId;
//        Set<UUID> visited = new HashSet<>();
//
//        while (currentNodeId != null) {
//
//            if (!visited.add(currentNodeId)) {
//                throw new IllegalStateException("Circular hierarchy detected");
//            }
//
//            NodeSummary node = nodeMapper.getNodeByNodeId(currentNodeId, orgId);
//            if (node == null) break;
//
//            String type = node.getType() == null ? "" : node.getType().toLowerCase();
//
//            switch (type) {
////                case "business hub":
////                    System.out.println("bbbhhh:: "+node.getNodeId());
////                    if(bhubId.equals(node.getNodeId())){
////                        request.setNodeId(node.getNodeId());
////                    } else {
////                        throw new GlobalExceptionHandler
////                                .NotFoundException("Feeder does not belong to the bushiness hub meter is allocated");
////                    }
////
////                    break;
////                case "service center":
////                    request.setServiceCenter(node.getNodeId());
////                    break;
//                case "region":
//                    request.setRegion(node.getNodeId());
//                    break;
////                case "substation":
////                    request.setSubstation(node.getNodeId());
////                    break;
//                case "root":
//                    request.setRoot(node.getNodeId());
//                    break;
//            }
//
//            currentNodeId = node.getParentId();
//        }
//    }
//
//    private void prepareMeters(
//            List<Meter> batch,
//            UserModel user,
//            Map<String, UUID> manufacturerNameToId,
//            List<GenericResp> failedRecords
//    ) {
//        Iterator<Meter> iterator = batch.iterator();
//
//        while (iterator.hasNext()) {
//            Meter meter = iterator.next();
//
////            if(meter != null){
////                GenericResp resp = new GenericResp();
////                resp.setId(meter.getMeterNumber());
////                resp.setMessage("Meter already exist");
////                resp.setData(meter);
////
////                failedRecords.add(resp);
////                iterator.remove();
////                continue;
////            }
//
//            // --- Validate and set Manufacturer ID ---
//            String manuName = meter.getMeterManufacturerName();
//            if (manuName == null || manuName.trim().isBlank()) {
//                GenericResp resp = new GenericResp();
//                resp.setId(meter.getMeterNumber());
//                resp.setMessage("Missing manufacturer name");
//                resp.setData(meter.getMeterNumber());
//
//                failedRecords.add(resp);
//                iterator.remove();
//                continue;
//            }
//
//            UUID manuId = manufacturerNameToId.get(manuName.trim().toLowerCase());
//            if (manuId == null) {
//                GenericResp resp = new GenericResp();
//                resp.setId(meter.getMeterNumber());
//                resp.setMessage("Invalid manufacturer: "+manuName);
//                resp.setData(meter.getMeterNumber());
//
//                failedRecords.add(resp);
////                failedRecords.add(meter.getMeterNumber() + " (Invalid manufacturer: " + manuName + ")");
//                iterator.remove();
//                continue;
//            }
//
//            meter.setMeterManufacturer(manuId);
//
//            String validationError = validateRequiredFields(meter);
//
//            if (validationError != null) {
//                GenericResp resp = new GenericResp();
//                resp.setId(String.valueOf(meter.getMeterNumber()));
//                resp.setMessage(validationError);
//                resp.setData(meter.getMeterNumber());
//
//                failedRecords.add(resp);
//                iterator.remove();
//                continue;
//            }
//
//            // --- Default Meter Fields ---
//            meter.setOrgId(user.getOrgId());
//            meter.setCreatedBy(user.getId());
//            meter.setStatus("Active");
//            meter.setMeterStage("Pending-created");
//            meter.setType("NON-VIRTUAL");
//            meter.setDescription("Newly Added");
//        }
//    }
//public Map<String, Object> bulkInsertMeters(List<Meter> meters, UserModel user) {
//    Map<String, Object> result = new HashMap<>();
//    List<GenericResp> failedRecords = new ArrayList<>();
//
//    if (meters == null || meters.isEmpty()) {
//        throw new IllegalArgumentException("Meter list cannot be empty");
//    }
//
//    int totalRecords = meters.size();
//    int successCount = 0;
//
//    // ------------------------------------------
//    // Load Manufacturers
//    // ------------------------------------------
//    List<Manufacturer> manufacturers = meterMapper.getManufacturers(user.getOrgId());
//    Map<String, UUID> manufacturerNameToId = manufacturers.stream()
//            .collect(Collectors.toMap(
//                    m -> m.getName().trim().toLowerCase(),
//                    Manufacturer::getId
//            ));
//
//    if(manufacturerNameToId.isEmpty()) {
//        throw new GlobalExceptionHandler.PartialFailureException(
//                "Meters upload failed - manufacturer not found",
//                result
//        );
//    }
//
//    //------------------------------------------------
//    // Validate duplicates INSIDE FILE
//    //------------------------------------------------
//
//    Set<String> seenMeters = new HashSet<>();
//    Set<String> seenSims = new HashSet<>();
//
//    Iterator<Meter> fileIterator = meters.iterator();
//
//    while (fileIterator.hasNext()) {
//
//        Meter meter = fileIterator.next();
//
//        String meterNumber = Optional.ofNullable(meter.getMeterNumber()).orElse("").trim();
//        String simNumber = Optional.ofNullable(meter.getSimNumber()).orElse("").trim();
//
//        if (!seenMeters.add(meterNumber)) {
//            GenericResp resp = new GenericResp();
//            resp.setId(meterNumber);
//            resp.setMessage("Duplicate meter number in uploaded file");
//            resp.setData(simNumber);
//            failedRecords.add(resp);
//            fileIterator.remove();
//            continue;
//        }
//
//        if (!simNumber.isEmpty() && !seenSims.add(simNumber)) {
//            GenericResp resp = new GenericResp();
//            resp.setId(meterNumber);
//            resp.setMessage("Duplicate SIM number in uploaded file");
//            resp.setData(simNumber);
//            failedRecords.add(resp);
//            fileIterator.remove();
//            continue;
//        }
//    }
//
//    // ------------------------------------------
//    // Extract MeterNumbers + SimNumbers
//    // ------------------------------------------
//
//    Set<String> meterNumbers = meters.stream()
//            .map(Meter::getMeterNumber)
//            .filter(Objects::nonNull)
//            .map(String::trim)
//            .collect(Collectors.toSet());
//
//    Set<String> simNumbers = meters.stream()
//            .map(Meter::getSimNumber)
//            .filter(Objects::nonNull)
//            .map(String::trim)
//            .collect(Collectors.toSet());
//
//    // ---------------------------------------------------
//    // Fetch Existing Meter Numbers (ONE DB CALL)
//    // ---------------------------------------------------
//    Set<String> allMeterNumbers = meters.stream()
//            .map(Meter::getMeterNumber)
//            .filter(Objects::nonNull)
//            .map(String::trim)
//            .filter(s -> !s.isEmpty())
//            .collect(Collectors.toSet());
//
//
//    // ------------------------------------------
//    // Fetch Existing
//    // ------------------------------------------
//
//    List<Meter> existingMeters =
//            meterMapper.getMetersList(
//                    new ArrayList<>(meterNumbers),
////                        new ArrayList<>(simNumbers),
//                    user.getOrgId()
//            );
//
//    Set<String> existingMeterNumbers = existingMeters.stream()
//            .map(Meter::getMeterNumber)
//            .collect(Collectors.toSet());
//
//    Set<String> existingSimNumbers = existingMeters.stream()
//            .map(Meter::getSimNumber)
//            .collect(Collectors.toSet());
//
//
//    int batchSize = 500; // try 500–1000 for optimal JDBC performance
//
//    for (int i = 0; i < meters.size(); i += batchSize) {
//        int end = Math.min(i + batchSize, meters.size());
////            List<Meter> batch = meters.subList(i, end);
//        List<Meter> batch = new ArrayList<>(meters.subList(i, end));
//
//        // -----------------------------------------------
//        // Remove duplicates (already existing meters)
//        // -----------------------------------------------
//        Iterator<Meter> iterator = batch.iterator();
//
//        while (iterator.hasNext()) {
//
//            Meter meter = iterator.next();
//
//            String meterNumber = meter.getMeterNumber();
//            String simNumber = meter.getSimNumber();
//            String manufacturer = meter.getMeterManufacturerName();
//
//            if (meterNumber == null || meterNumber.trim().isEmpty()) {
//                GenericResp resp = new GenericResp();
//                resp.setId(null);
//                resp.setMessage("Missing meter number");
//                resp.setData(null);
//                failedRecords.add(resp);
//                iterator.remove();
//                continue;
//            }
//
//            meterNumber = meterNumber.trim();
//
//            if (existingMeterNumbers.contains(meterNumber)) {
//
//                GenericResp resp = new GenericResp();
//                resp.setId(meterNumber);
//                resp.setMessage("Meter already exists");
//                resp.setData(meterNumber);
//                failedRecords.add(resp);
//                iterator.remove();
//                continue;
//            }
//
//            if (simNumber != null && existingSimNumbers.contains(simNumber.trim())) {
//
//                GenericResp resp = new GenericResp();
//                resp.setId(meterNumber);
//                resp.setMessage("SIM number already exists");
//                resp.setData(meterNumber);
//                failedRecords.add(resp);
//                iterator.remove();
//                continue;
//            }
//
//            if (manufacturer == null ||
//                    !manufacturerNameToId.containsKey(manufacturer.trim().toLowerCase())) {
//
//                GenericResp resp = new GenericResp();
//                resp.setId(meterNumber);
//                resp.setMessage("Manufacturer does not exist: " + manufacturer);
//                resp.setData(manufacturer);
//                failedRecords.add(resp);
//                iterator.remove();
////                    continue;
//            }
//        }
//
//        if (batch.isEmpty()) {
//            continue;
//        }
//
//
//        try {
//            insertBatchTransactional(batch, user, manufacturerNameToId, failedRecords);
//            successCount += batch.size();
//        } catch (Exception e) {
//            log.warn("Batch {} failed — retrying sub batch upload", (i / batchSize) + 1);
//            // Attempt smaller sub-batches to isolate failure
//            successCount += insertSubBatchTransactional(batch, user, manufacturerNameToId, failedRecords);
//        }
//    }
//
//    result.put("totalRecords", totalRecords);
//    result.put("successCount", successCount);
//    result.put("failedCount", failedRecords.size());
//    result.put("failedRecords", failedRecords);
//
//    if (!failedRecords.isEmpty()) {
//        return ResponseMap.response(
//                "131",
//                failedRecords.size() + " of " + totalRecords + " Meters upload failed",
//                result
//        );
//    }
//
//    return ResponseMap.response(
//            status.getSuccessCode(),
//            successCount + " of " + totalRecords + " Meters uploaded successfully",
//            result
//    );
//}

    @Override
    public Map<String, Object> bulkAllocate(MultipartFile file) throws IOException {
        try {
            UserModel user = handleUserValidation();

            // Determine file type
            String filename = Optional.ofNullable(file.getOriginalFilename())
                    .orElseThrow(() -> new IOException("File has no name"));

            String nodeName = user.getNodeInfo().getType();
            List<MeterRequest> meters;
            if(nodeName.equalsIgnoreCase("Region")
                    || nodeName.equalsIgnoreCase("Root")){
                if (filename.endsWith(".csv")) {
                    meters = processAllocateCsv(file.getInputStream());
                } else if (filename.endsWith(".xlsx")) {
                    meters = processAllocateExcel(file.getInputStream());
                } else {
                    throw new IOException("Unsupported file format. Only .csv or .xlsx allowed.");
                }
            } else {
                throw new IOException("You do not have permission");
            }

            return bulkAllocateMeters(meters, user);

        } catch (Exception e) {
            log.error("Error in bulk allocate upload: {}", e.getMessage(), e);
            genericHandler.logIncidentReport("Bulk allocate service failed");
            genericHandler.logAndSaveException(e, "Bulk allocate meter");
            throw new IOException("Bulk allocate failed: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> bulkAllocateMeters(List<MeterRequest> allocations, UserModel user) {
        Map<String, Object> result = new HashMap<>();
        List<GenericResp> failedRecords = new ArrayList<>();
        int successCount = 0;
        Set<String> seenMeters = new HashSet<>();

        if (allocations == null || allocations.isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("No records found in uploaded file");
        }

        Iterator<MeterRequest> fileIterator = allocations.iterator();

        while (fileIterator.hasNext()) {

            MeterRequest meter = fileIterator.next();

            String meterNumber = Optional.ofNullable(meter.getMeterNumber()).orElse("").trim();

            if (!seenMeters.add(meterNumber)) {
                GenericResp resp = new GenericResp();
                resp.setId(meterNumber);
                resp.setMessage("Duplicate meter number in uploaded file");
                resp.setData(meterNumber);
                failedRecords.add(resp);
                fileIterator.remove();
                continue;
            }
        }

        final int BATCH_SIZE = 500;

        for (int i = 0; i < allocations.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, allocations.size());
//            List<MeterRequest> subBatch = allocations.subList(i, end);
            List<MeterRequest> subBatch = new ArrayList<>(allocations.subList(i, end));
            // Extract meter numbers and region IDs
            List<String> meterNumbers = subBatch.stream()
                    .map(MeterRequest::getMeterNumber)
                    .filter(num -> num != null && !num.trim().isEmpty())
                    .map(String::trim)
                    .toList();

            List<String> regionIds = subBatch.stream()
                    .map(MeterRequest::getRegionId)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            if (meterNumbers.isEmpty()  || regionIds.isEmpty()) {
                subBatch.forEach(req -> {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Missing meter number or region id");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
                });

                continue;
            }
            // Fetch meters
            List<Meter> meters = meterMapper.getMetersByMeterNumbers(meterNumbers, user.getOrgId(), user.getNodeInfo().getNodeId());
//            Map<String, Meter> meterMap = meters.stream()
//                    .collect(Collectors.toMap(Meter::getMeterNumber, m -> m));
            Map<String, Meter> meterMap = meters.stream()
                    .filter(m -> m.getMeterNumber() != null)
                    .collect(Collectors.toMap(
                            m -> m.getMeterNumber().trim(),
                            m -> m,
                            (a, b) -> a
                    ));

            // Fetch region → business-hub mappings
            List<RegionBhubServiceCenter> regionHubs = meterMapper.getRegionBhubMappings(regionIds, user.getOrgId());
            Map<String, RegionMapping> regionNodeIdMap = regionHubs.stream()
                    .collect(Collectors.toMap(
                            RegionBhubServiceCenter::getRegionId,
                            r -> new RegionMapping(
                                    r.getParentId(),
                                    r.getNodeId()
                            ),
                            (a, b) -> a // prevent duplicates crash
                    ));

            List<Meter> validAllocations = new ArrayList<>();

            for (MeterRequest req : subBatch) {
                String reqMeterNo = Optional.ofNullable(req.getMeterNumber()).orElse("").trim();

                Meter meter = meterMap.get(reqMeterNo);
                RegionMapping mapping = regionNodeIdMap.get(req.getRegionId());

                if (meter == null) {
                    System.out.println("meter2>>>>: ");
                    GenericResp resp = new GenericResp();
                    resp.setId(req.getMeterNumber());
                    resp.setMessage("Meter Not found");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
                    continue;
                }

                if (mapping == null || mapping.getParentId() == null || mapping.getNodeId() == null) {
                    GenericResp resp = new GenericResp();
                    resp.setId(meter.getMeterNumber());
                    resp.setMessage("Meter allocate failed: region not linked to business hub");
                    resp.setData(meter.getMeterNumber());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format("%s [Region: %s] (Region not found in business hub)", req.getMeterNumber(), req.getRegionId()));
                    continue;
                }

                meter.setNodeId(mapping.getNodeId());
                meter.setRegion(mapping.getParentId());
//                meter.getNodeInfo().setRegionId(req.getRegionId());
                meter.setOrgId(user.getOrgId());
                meter.setMeterStage("Pending-allocated");
                meter.setCreatedBy(user.getId());
                meter.setDescription("Meter Allocated");
                validAllocations.add(meter);
            }

            if (validAllocations.isEmpty()) continue;

            // Try allocating
            try {
                log.info("Processing batch {} - {} ({} records)", i, end - 1, subBatch.size());
                int allocated = allocateBatchTransactional(validAllocations, user);
                successCount += allocated;
            } catch (Exception e) {
                log.warn("Batch {} failed — retrying smaller sub-batches: {}", (i / BATCH_SIZE) + 1, e.getMessage());
//                successCount += allocateSubBatchTransactional(validAllocations, user, failedRecords);
            }
        }

        int total = successCount + failedRecords.size();

        result.put("totalRecords", total);
        result.put("successCount", successCount);
        result.put("failedCount", failedRecords.size());
        result.put("failedRecords", failedRecords);

        if (!failedRecords.isEmpty()) {
            return ResponseMap.response(
                    "131",
                    failedRecords.size() + " of " + total + " Meters allocate failed",
                    result
            );
        }

        return ResponseMap.response(
                status.getSuccessCode(),
                String.format("%d of %d meters allocated successfully", successCount, total),
                result
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int allocateBatchTransactional(List<Meter> batch, UserModel user) {
        if (batch.isEmpty()) return 0;

        try {
            // Update main meter table
            meterMapper.updateBatchMeterAllocation(batch);

//            System.out.println("node>>>: "+batch.get(0).getRegion());
            // Update version table (node_id + meter_stage)
            meterMapper.insertMeterVersions(batch);

            // Audit allocations
            auditBatch(batch, user, "Meter Allocated");

            log.info("Allocated {} meters successfully", batch.size());
            return batch.size();

        } catch (Exception e) {
            log.error("Transaction failed during allocation, rolling back batch of size {}: {}", batch.size(), e.getMessage());
            genericHandler.logIncidentReport("Bulk allocate batch service failed");
            genericHandler.logAndSaveException(e, "Bulk allocate batch meter");
            throw new RuntimeException("Batch allocation transaction failed. Rolled back.", e);
        }
    }

    public int allocateSubBatchTransactional(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        try {
            int successCount = 0;
            int subBatchSize = 100;

            for (int i = 0; i < batch.size(); i += subBatchSize) {
                int end = Math.min(i + subBatchSize, batch.size());
                List<Meter> subBatch = batch.subList(i, end);

                try {
                    successCount += allocateBatchTransactional(subBatch, user);
                } catch (Exception e) {
                    log.warn("Sub-batch allocation failed (size={}): {}", subBatch.size(), e.getMessage());

                    if (subBatch.size() > 50) {
                        successCount += allocateSinglesFallbackAsync(subBatch, user, failedRecords);
                    } else {
                        successCount += allocateSinglesFallback(subBatch, user, failedRecords);
                    }
                }
            }

            return successCount;
        } catch (Exception e) {
            genericHandler.logIncidentReport("Bulk allocate sub batch service failed");
            genericHandler.logAndSaveException(e, "Bulk allocate sub batch meter");
            throw new RuntimeException("Sub Batch allocation transaction failed. Rolled back.", e);
        }

    }

    public int allocateSinglesFallbackAsync(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        for (Meter meter : batch) {
            futures.add(allocateSingleAsync(meter, user, failedRecords));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream().mapToInt(CompletableFuture::join).sum();
    }

    public int allocateSinglesFallback(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        int successCount = 0;

        for (Meter meter : batch) {
            try {
                log.debug("Fallback single allocation for meter: {}", meter.getMeterNumber());
                allocateSingleTransactional(meter, user);
                successCount++;
            } catch (Exception e) {
                String reason = extractErrorMessage(e);
                GenericResp resp = new GenericResp();
                resp.setId(meter.getMeterId().toString());
                resp.setMessage("Meter single allocate failed: "+reason);
                resp.setData(meter.getMeterNumber());

                failedRecords.add(resp);
//                failedRecords.add(String.format(
//                        "%s [Region: %s] (Allocation failed: %s)",
//                        meter.getMeterNumber(),
////                        meter.getNodeInfo().getRegionId(),
//                        reason
//                ));
                log.warn("Meter {} failed individually: {}", meter.getMeterNumber(), reason);
            }
        }

        return successCount;
    }

    @Async
    public CompletableFuture<Integer> allocateSingleAsync(Meter meter, UserModel user, List<GenericResp> failedRecords) {
        try {
            allocateSingleTransactional(meter, user);
            return CompletableFuture.completedFuture(1);
        } catch (Exception e) {
            String reason = extractErrorMessage(e);
            GenericResp resp = new GenericResp();
            resp.setId(meter.getMeterId().toString());
            resp.setMessage("Meter single allocation failed: "+reason);
            resp.setData(meter.getMeterNumber());

            failedRecords.add(resp);
//            failedRecords.add(String.format(
//                    "%s [Region: %s] (Allocation failed: %s)",
//                    meter.getMeterNumber(),
////                    meter.getNodeInfo().getRegionId(),
//                    reason
//            ));
            log.warn("Async allocation failed for meter {}: {}", meter.getMeterNumber(), reason);
            return CompletableFuture.completedFuture(0);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void allocateSingleTransactional(Meter meter, UserModel user) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

        // --- Step 1: Prepare core meter entity ---
//        meter.setOrgId(user.getOrgId());
//        meter.setCreatedBy(user.getId());
//        meter.setStatus("Active");
//        meter.setMeterStage("Pending-allocated");
//        meter.setType("NON-VIRTUAL");
//        meter.setDescription("Meter Allocated");
//        String desc = meter.getMeterNumber() + " meter allocated to " + .;
//        String desc = meter.getMeterNumber() + " meter allocated to " + meter.getNodeInfo().getRegionId();

        // --- Step 2: Insert into main + version tables ---
        meterMapper.allocateMeterVersion(meter, meter.getNodeId(), meter.getId(), "Pending Allocated", meter.getRegion());
//        if(result == 0){
//            throw new GlobalExceptionHandler.NotFoundException("Meter allocation failed");
//        }

        meterMapper.updateMeter(meter.getDescription(), meter.getId(), meter.getUpdatedAt(), meter.getStatus());
//        if(result == 0){
//            throw new GlobalExceptionHandler.NotFoundException("Meter allocation failed");
//        }

        //fetch meter from the database
        Meter m = meterMapper.getVersionMeter(user.getOrgId(), null, meter.getMeterNumber(), null);
//            String desc = capitalizeFirstLetter(meter.getMeterNumber() + " allocated " + node.getName());
        //save to audit (mongodb)
        AuditLog auditLog = buildAuditLog(user, "Pending Allocated", meterName, m, metadata, "");
        safeAuditService.saveAudit(auditLog);

    }

    @Override
    public Map<String, Object> bulkApproval(List<MeterRequest> meters) {

        UserModel user = handleUserValidation();
        Map<String, Object> result = new HashMap<>();
        List<GenericResp> failedRecords = new ArrayList<>();
        int successCount = 0;
        int total;
        int updatedCount = 0;
        String nodeType = user.getNodeInfo().getType();

            if((!nodeType.equalsIgnoreCase("Root")
                    && !nodeType.equalsIgnoreCase("Region")
                    && !nodeType.equalsIgnoreCase("Business hub")
                    && !nodeType.equalsIgnoreCase("Service center"))){
                throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
            }

            if (meters == null || meters.isEmpty()) {
                throw new GlobalExceptionHandler.NotFoundException("No records found in file");
            }

            final int BATCH_SIZE = 500; // Tune as needed for performance

            for (int i = 0; i < meters.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, meters.size());
                List<MeterRequest> batch = meters.subList(i, end);

                // Collect all meter numbers in this subBatch
                List<String> meterNumbers = batch.stream()
                        .map(m -> m.getMeterNumber().trim())
                        .filter(num -> !num.isEmpty())
                        .toList();

                if (meterNumbers.isEmpty()) {
                    batch.forEach(req -> {
                        GenericResp resp = new GenericResp();
                        resp.setId("");
                        resp.setMessage("Missing meter number");
                        resp.setData(req.getMeterNumber());

                        failedRecords.add(resp);
                    });

                    continue;
                }

                // fetch found meters
                List<Meter> versionBatch = meterMapper.getMetersByVersionMeterNumbers(meterNumbers, user.getOrgId(), user.getNodeInfo().getNodeId());

                Set<String> foundNames = versionBatch.stream()
                        .map(Meter::getMeterNumber)
                        .map(String::trim)
                        .collect(Collectors.toSet());

                List<String> missingNames = meterNumbers.stream()
                        .filter(name -> !foundNames.contains(name.trim()))
                        .toList();

                // Record missing/invalid tariffs
                for (String name : missingNames) {
                    GenericResp resp = new GenericResp();
                    resp.setId(name);
                    resp.setMessage("Not found or not in pending state");
                    resp.setData(name);
                    failedRecords.add(resp);
                }

                try {
                    prepareUpdateMeters(versionBatch, user, failedRecords);

                    updatedCount = updateBatchTransactional(versionBatch, user, failedRecords);

                    successCount += updatedCount;

                } catch (Exception e) {
                    log.warn("Batch {} failed — retrying smaller sub-batches: {}", (i / BATCH_SIZE) + 1, e.getMessage());
                    int retrySuccess = updateSubBatchTransactional(versionBatch, user, failedRecords);
                    successCount += retrySuccess;
                }
            }

            total = meters.size();

            result.put("totalRecords", total);
            result.put("successCount", successCount);
            result.put("failedCount", failedRecords.size());
            result.put("failedRecords", failedRecords);

            if (!failedRecords.isEmpty()) {
                return ResponseMap.response(
                        "131",
                        failedRecords.size() + " of " + total + " meters approval failed",
                        result
                );
            }

        Map<String, Object> res;
        res = updatedCount > 0 ? ResponseMap.response(
                status.getSuccessCode(),
                successCount + " of " + total + " meters approved successfully",
                result
        ) : ResponseMap.response(
                "131",
                successCount + " of " + total + " meters approval failed",
                result
        );
        return res;
    }

    /** Validate and enrich meters before DB update. */
    private void prepareUpdateMeters(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        Iterator<Meter> iterator = batch.iterator();
        while (iterator.hasNext()) {
            Meter meter = iterator.next();
            if (meter.getMeterNumber() == null || meter.getMeterNumber().trim().isEmpty()) {
                GenericResp resp = new GenericResp();
                resp.setId(meter.getMeterId().toString());
                resp.setMessage("Missing meter number");
                resp.setData(meter.getMeterNumber());

                failedRecords.add(resp);
                iterator.remove();
                continue;
            }

            meter.setOrgId(user.getOrgId());
            meter.setApproveBy(user.getId());
            meter.setId(meter.getMeterId());

            if("Pending-created".equalsIgnoreCase(meter.getMeterStage())){
                meter.setStatus("Active");
            } else if ("Pending-allocated".equalsIgnoreCase(meter.getMeterStage())){
                meter.setStatus("Active");
            } else if ("Pending-assigned".equalsIgnoreCase(meter.getMeterStage())) {
                meter.setStatus("Active");
            } else if ("Pending-migrated".equalsIgnoreCase(meter.getMeterStage())) {
                meter.setStatus("Active");
            } else if ("Pending-detached".equalsIgnoreCase(meter.getMeterStage())) {
                meter.setStatus("Active");
            } else if ("Pending-edited".equalsIgnoreCase(meter.getMeterStage()) && meter.getCustomerId() != null) {
                meter.setStatus("Active");
            } else if ("Assign-edited".equalsIgnoreCase(meter.getMeterStage()) && meter.getCustomerId() != null) {
                meter.setStatus("Active");
            }
            else if ("Pending-edited".equalsIgnoreCase(meter.getMeterStage()) && meter.getNodeId() != null && meter.getCustomerId() == null) {
                meter.setStatus("Active");
            } else if ("Pending-edited".equalsIgnoreCase(meter.getMeterStage()) && meter.getNodeId() == null && meter.getCustomerId() == null) {
//                meter.setMeterStage("Created");
                meter.setStatus("Active");
            }
            else if ("Pending-activated".equalsIgnoreCase(meter.getStatus())) {
//                meter.setMeterStage("Created");
                meter.setStatus("Pending-activated");
            }else if ("Pending-deactivated".equalsIgnoreCase(meter.getStatus())) {
                meter.setStatus("Pending-deactivated");
            }
            else {
                GenericResp resp = new GenericResp();
                resp.setId(meter.getMeterNumber());
                resp.setMessage("Meter is not in a pending state");
                resp.setData(meter.getMeterNumber());

                failedRecords.add(resp);
            }

            if (meter.getMdMeterInfo() != null) {
                meter.getMdMeterInfo().setApproveBy(user.getId());
                meter.getMdMeterInfo().setMeterId(meter.getMeterId());
                meter.getMdMeterInfo().setOrgId(user.getOrgId());
            }

            if (meter.getSmartMeterInfo() != null) {
                meter.getSmartMeterInfo().setApproveBy(user.getId());
                meter.getSmartMeterInfo().setMeterId(meter.getMeterId());
                meter.getSmartMeterInfo().setOrgId(user.getOrgId());
            }
            if(meter.getMeterStage().equalsIgnoreCase("Pending-created")
                    || meter.getMeterStage().equalsIgnoreCase("Pending-allocated")
                    && (!user.getNodeInfo().getType().equalsIgnoreCase("Region")
                    && !user.getNodeInfo().getType().equalsIgnoreCase("Root"))){
                GenericResp resp = new GenericResp();
                resp.setId(meter.getMeterNumber());
                resp.setMessage("You do not have permission to approve meter");
                resp.setData(meter.getMeterNumber());

                failedRecords.add(resp);

            }
        }
    }

    /** Transactionally update main + version + children + audit */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int updateBatchTransactional(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        String desc = "";
        if (batch.isEmpty()) return 0;

        try {

            List<Meter> approvedCreatedMeters = batch.stream()
                    .filter(m -> "Pending-created".equalsIgnoreCase(m.getMeterStage()))
                    .peek(m -> m.setMeterStage("Created"))
                    .peek(m -> m.setStatus("Active"))
                    .toList();

            List<Meter> approvedAllocatedMeters = batch.stream()
                    .filter(m -> "Pending-allocated".equalsIgnoreCase(m.getMeterStage()))
                    .peek(m -> m.setMeterStage("Unassigned"))
                    .peek(m -> m.setStatus("Active"))
                    .toList();

            List<Meter> approvedAssignedMeters = batch.stream()
                    .filter(m -> "Pending-assigned".equalsIgnoreCase(m.getMeterStage()))
                    .peek(m -> m.setMeterStage("Assigned"))
                    .peek(m -> m.setStatus("Active"))
                    .toList();

            List<Meter> approvedMigratedMeters = batch.stream()
                    .filter(m -> "Pending-migrated".equalsIgnoreCase(m.getMeterStage()))
                    .peek(m -> m.setMeterStage("Assigned"))
                    .peek(m -> m.setStatus("Active"))
                    .toList();

            List<Meter> approvedDetachedMeters = batch.stream()
                    .filter(m -> "Pending-detached".equalsIgnoreCase(m.getMeterStage()))
                    .peek(m -> m.setMeterStage("Unassigned"))
                    .peek(m -> m.setStatus("Active"))
                    .toList();

            List<Meter> approvedDeactivatedMetersStatus = batch.stream()
                    .filter(m -> "Pending-deactivated".equalsIgnoreCase(m.getStatus()))
                    .peek(m -> m.setStatus("Active"))
                    .toList();

            List<Meter> approvedActiveMetersStatus = batch.stream()
                    .filter(m -> "Pending-activated".equalsIgnoreCase(m.getStatus()))
                    .peek(m -> m.setStatus("Active"))
                    .toList();

            // Handle "Pending-edited" dynamically
            List<Meter> approvedEditedMeters = batch.stream()
                    .filter(m -> "Pending-edited".equalsIgnoreCase(m.getMeterStage())
                            || "Assign-edited".equalsIgnoreCase(m.getMeterStage()))
                    .peek(m -> {
                        if (m.getCustomerId() != null) {
                            m.setMeterStage("Assigned");
                        } else if (m.getNodeId() != null && m.getCustomerId() == null) {
                            m.setMeterStage("Unassigned");
                        } else {
                            m.setMeterStage("Created");
                        }
                        m.setStatus("Active");
                    })
                    .toList();

            // Combine all for main update
            List<Meter> toUpdate = Stream.of(
                            approvedCreatedMeters,
                            approvedAllocatedMeters,
                            approvedAssignedMeters,
                            approvedMigratedMeters,
                            approvedEditedMeters,
                            approvedDeactivatedMetersStatus,
                            approvedActiveMetersStatus)
                    .flatMap(Collection::stream)
                    .toList();
            if (!toUpdate.isEmpty()) {
                System.out.println(">>>>>>>>>>>toUpdate:::: batch: " + batch.get(0).getSimNumber());
                meterMapper.updateBatchMeters(toUpdate);
                meterMapper.updateBatchVersionMeters(toUpdate);
            }

            // Combine all for main update
            List<Meter> detach = Stream.of(approvedDetachedMeters)
                    .flatMap(Collection::stream)
                    .toList();

            if (!detach.isEmpty()) {
                System.out.println(">>>>>>>>>>>detached1:::: ");
                meterMapper.updateDetachBatchMeters(detach, user.getOrgId());
                meterMapper.updateBatchVersionMeters(detach);
                meterMapper.removeBulkAssignedLocations(detach);
                meterMapper.removeBulkPaymentModes(detach);
                meterMapper.updateDetachLocation(detach);
                meterMapper.updateDetachPaymentModes(detach);
                System.out.println(">>>>>>>>>>>detached2:::: ");
            }

            Set<String> affectedCustomerIds = detach.stream()
                    .map(Meter::getCustomerId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            if (!affectedCustomerIds.isEmpty()) {

                List<String> customerIds = new ArrayList<>(affectedCustomerIds);

                List<Customer> counts =
                        meterMapper.countMetersByCustomerIds(customerIds);

                // Customers with meters
                Set<String> activeCustomers = counts.stream()
                        .filter(c -> c.getMeterCount() > 0)
                        .map(Customer::getCustomerId)
                        .collect(Collectors.toSet());

                // Customers with ZERO meters
                List<String> inactiveCustomers = customerIds.stream()
                        .filter(id -> !activeCustomers.contains(id))
                        .toList();

                // Batch updates
                if (!activeCustomers.isEmpty()) {
                    meterMapper.markCustomersActive(new ArrayList<>(activeCustomers));
                }

                if (!inactiveCustomers.isEmpty()) {
                    meterMapper.markCustomersInactive(inactiveCustomers);
                }
            }

            List<DebitCreditAdjustVersion> adjustmentList = batch.stream()
                    .filter(m -> m.getDebitCreditAdjustVersionInfo() != null)
                    .map(m -> {
                        var info = m.getDebitCreditAdjustVersionInfo();
                        info.setStatus(false);              // New status
                        return info;
                    }).toList();

            if (!adjustmentList.isEmpty()) {
                meterMapper.updateBatchDebitCreditAdj(adjustmentList);
                meterMapper.updateBatchDebitCreditAdjVersion(adjustmentList);
            }

            // --- Migration ---
            if (!approvedMigratedMeters.isEmpty()) {
                desc = "Meter migration approved";
                handleMigration(approvedMigratedMeters, user);
            }

            // --- Assigned ---
            if (!approvedAssignedMeters.isEmpty()) {
                desc = "Meter assigned approved";
                handleAssignment(approvedAssignedMeters, user);
            }

            // --- Edited (can behave similar to assigned) ---
            if (!approvedEditedMeters.isEmpty()) {
                desc = "Meter edit approved";
                handleEditedMeters(approvedEditedMeters, user);
            }

            // --- Created ---
            if (!approvedCreatedMeters.isEmpty()) {
                desc = "Meter created approved";
                updateChildMeterData(batch, user);
            }

            auditApproveBatch(batch, user, desc);

            log.info("Batch updated successfully: {}", batch.size());
            return batch.size();

        } catch (Exception e) {
            log.error("Transaction failed, rolling back batch of size {}: {}", batch.size(), e.getMessage());
//            failedRecords.add("Sub-batch failed: " + e.getMessage());
            genericHandler.logIncidentReport("Bulk approve sub batch service failed");
            genericHandler.logAndSaveException(e, "Bulk approve sub batch meter");
            throw new RuntimeException("Batch transaction failed. Rolled back.", e);
        }
    }

    private void handleEditedMeters(List<Meter> editedMeters, UserModel user) {

//        meterMapper.editAssignLocationFromVersion(editedMeters, user.getOrgId());
//        meterMapper.updateAssignLocationVersion(editedMeters);

        List<Meter> prepaidMeters = editedMeters.stream()
                .filter(m -> "Prepaid".equalsIgnoreCase(m.getMeterCategory()))
                .toList();

        if (prepaidMeters.isEmpty()) {
            return;
        }

        List<Meter> locationToUpdate = prepaidMeters.stream()
                .filter(m -> m.getMeterAssignLocation() != null)
                .toList();

        List<Meter> metersToUpdate = prepaidMeters.stream()
                .filter(m -> m.getPaymentMode() != null)
                .toList();

        List<Meter> updateMDMeterInfo = prepaidMeters.stream()
                .filter(m -> m.getMdMeterInfo() != null)
                .toList();

        List<Meter> updateSmartMeterInfo = prepaidMeters.stream()
                .filter(m -> m.getSmartMeterInfo() != null)
                .toList();

        if(!updateMDMeterInfo.isEmpty()){
            System.out.println("UPDATE missing MD meter info");
            meterMapper.updateMDMeterInfoFromVersion(prepaidMeters, user.getOrgId());
            meterMapper.updateBulkMDMeterInfoVersion(prepaidMeters);
        }

        if(!updateSmartMeterInfo.isEmpty()){
            System.out.println("UPDATE missing smart meter info");
            meterMapper.updateSmartMeterInfoFromVersion(prepaidMeters, user.getOrgId());
            meterMapper.updateBulkSmartMeterInfoVersion(prepaidMeters);
        }

        if(!locationToUpdate.isEmpty()){
            System.out.println("UPDATE missing location info");
            meterMapper.editAssignLocationFromVersion(editedMeters, user.getOrgId());
            meterMapper.updateAssignLocationVersion(editedMeters);
        }

        if (!metersToUpdate.isEmpty()) {
            System.out.println("UPDATE missing payment_mode");
            meterMapper.updatePaymentModeFromVersion(prepaidMeters, user.getOrgId());
            meterMapper.updatePaymentModeVersion(prepaidMeters);
        }

    }

    private void handleAssignment(List<Meter> assignedMeters, UserModel user) {
        // Copy approved locations from version → actual table
        meterMapper.copyAssignLocationFromVersion(assignedMeters, user.getOrgId());

        // Create a list of prepaid meters
        List<Meter> prepaidMeters = assignedMeters.stream()
                .filter(m -> "Prepaid".equalsIgnoreCase(m.getMeterCategory()))
                .toList();

        if (!prepaidMeters.isEmpty()) {
            // Copy approved payment modes from version → actual table (for prepaid)
            meterMapper.copyPaymentModeFromVersion(prepaidMeters, user.getOrgId());

            //   Update the version tables to mark as approved
            meterMapper.updatePaymentModeVersion(prepaidMeters);
        }

        // Clean up location version table
        meterMapper.updateAssignLocationVersion(assignedMeters);

        // Update customer record (status = active)
        customerMapper.changeStatusBulkCustomer(assignedMeters, user.getOrgId());
    }

    private void handleMigration(List<Meter> migratedMeters, UserModel user) {

            // Create a list of prepaid meters
            List<Meter> prepaidMeters = migratedMeters.stream()
                    .filter(m -> "Prepaid".equalsIgnoreCase(m.getMeterCategory()))
                    .toList();

            List<Meter> postpaidMeters = migratedMeters.stream()
                    .filter(m -> "Postpaid".equalsIgnoreCase(m.getMeterCategory()))
                    .toList();

            if (!prepaidMeters.isEmpty()) {
                // Copy approved payment modes from version → actual table (for prepaid)
                meterMapper.copyPaymentModeFromVersion(prepaidMeters, user.getOrgId());

                // Update the version tables to mark as approved
                meterMapper.updatePaymentModeVersion(prepaidMeters);
            }

            if (!postpaidMeters.isEmpty()) {

                // Copy approved payment modes from version → actual table (for prepaid)
                meterMapper.deletePaymentModeFromVersion(prepaidMeters, user.getOrgId());

                //   Update the version tables to mark as approved
                meterMapper.updatePaymentModeVersion(prepaidMeters);
            }
    }

    private List<Meter> getMetersByStage(List<Meter> batch, String stage, String newStage, String status) {
        System.out.println("meter_stage2: "+batch.get(0).getMeterStage());
        List<Meter> ms;
        ms = batch.stream()
                .filter(m -> stage.equalsIgnoreCase(m.getMeterStage()))
                .peek(m -> m.setMeterStage(newStage))
                .peek(m -> m.setStatus(status))
                .toList();

        return ms;
    }

    private List<Meter> getMetersByStatus(List<Meter> batch, String status, String newStatus) {
        System.out.println("getMetersByStatus: "+batch.get(0).getStatus());
        System.out.println("batch: "+batch.size());
        List<Meter> ms;
        ms = batch.stream()
                .filter(m -> status.equalsIgnoreCase(m.getStatus()))
                .peek(m -> m.setStatus(newStatus)).toList();
        System.out.println("ms "+ms.size());
        return ms;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleRejectionBatch(List<Meter> rejectList, UserModel user) {

        // Separate PENDING-CREATED and others
        List<Meter> pendingCreatedMeters = rejectList.stream()
                .filter(m -> "Pending-created".equalsIgnoreCase(m.getMeterStage()))
                .toList();

        List<Meter> otherRejections = rejectList.stream()
                .filter(m -> !"Pending-created".equalsIgnoreCase(m.getMeterStage()))
                .toList();

        // Extract IDs for each group
        List<UUID> pendingMeterIds = pendingCreatedMeters.stream()
                .map(Meter::getMeterId)
                .toList();

        List<UUID> otherMeterIds = otherRejections.stream()
                .map(Meter::getMeterId)
                .toList();

        // Handle Pending-created Meters (DELETE + REJECT)
        if (!pendingCreatedMeters.isEmpty()) {
            log.info("Deleting meters: {}", pendingCreatedMeters.stream()
                    .map(Meter::getMeterNumber)
                    .toList());

            // --- SmartMeterInfo (only if not null)
            List<UUID> smartMeterIds = pendingCreatedMeters.stream()
                    .filter(m -> m.getSmartMeterInfo() != null)
                    .map(Meter::getMeterId)
                    .toList();

            if (!smartMeterIds.isEmpty()) {
                meterMapper.rejectSmartMeterInfoVersion(smartMeterIds, user.getOrgId(), user.getId(), "Rejected");
            }

            // --- MDMeterInfo (only if not null)
            List<UUID> mdMeterIds = pendingCreatedMeters.stream()
                    .filter(m -> m.getMdMeterInfo() != null)
                    .map(Meter::getMeterId)
                    .toList();

            if (!mdMeterIds.isEmpty()) {
                meterMapper.rejectMDMeterInfoVersion(mdMeterIds, user.getOrgId(), user.getId(), "Rejected");
            }

            // --- Update version table
            meterMapper.rejectVersionMeters(pendingMeterIds, user.getOrgId(), user.getId(), "Rejected");

            // --- Finally, delete from main meter table
            meterMapper.deleteMetersByMeterIds(pendingMeterIds);
        }

        // Handle Other Rejections (Only Mark Rejected, No Delete)
        if (!otherRejections.isEmpty()) {
            log.info("Marking other meters as rejected: {}", otherRejections.stream()
                    .map(Meter::getMeterNumber)
                    .toList());

            // --- SmartMeterInfo (only if not null)
            List<UUID> smartMeterIds = otherRejections.stream()
                    .filter(m -> m.getSmartMeterInfo() != null)
                    .map(Meter::getMeterId)
                    .toList();

            if (!smartMeterIds.isEmpty()) {
                meterMapper.rejectSmartMeterInfoVersion(smartMeterIds, user.getOrgId(), user.getId(), "Rejected");
            }

            // --- MDMeterInfo (only if not null)
            List<UUID> mdMeterIds = otherRejections.stream()
                    .filter(m -> m.getMdMeterInfo() != null)
                    .map(Meter::getMeterId)
                    .toList();

            if (!mdMeterIds.isEmpty()) {
                meterMapper.rejectMDMeterInfoVersion(mdMeterIds, user.getOrgId(), user.getId(), "Rejected");
            }

            // --- Update version meter record
            meterMapper.rejectVersionMeters(otherMeterIds, user.getOrgId(), user.getId(), "Rejected");
        }

        // Audit
        auditRejectBatch(rejectList, user);
    }

    /** Retry mechanism for smaller sub-batches */
    public int updateSubBatchTransactional(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        int success = 0;
        int subSize = 100;

        for (int i = 0; i < batch.size(); i += subSize) {
            int end = Math.min(i + subSize, batch.size());
            List<Meter> subList = batch.subList(i, end);
            try {
                success += updateBatchTransactional(subList, user, failedRecords);
            } catch (Exception e) {
                log.error("Sub-batch {} failed: {}", (i / subSize) + 1, e.getMessage());
//                subList.forEach(m -> failedRecords.add(m.getMeterNumber() + " - " + e.getMessage()));
                if (batch.size() > 50) {
                    success += approveSinglesFallbackAsync(batch, user, failedRecords);
                } else {
                    success += approveSinglesFallback(batch, user, failedRecords);
                }
            }
        }
        return success;
    }

    public int approveSinglesFallbackAsync(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        for (Meter meter : batch) {
            futures.add(approveSingleAsync(meter, user, failedRecords));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream().mapToInt(CompletableFuture::join).sum();
    }

    public int approveSinglesFallback(List<Meter> meters, UserModel user, List<GenericResp> failedRecords) {
        int successCount = 0;

        for (Meter meter : meters) {
            try {
                log.debug("Fallback single allocation for meter: {}", meter.getMeterNumber());
                approveSingleTransactional(meter, user);
                successCount++;
            } catch (Exception e) {
                String reason = extractErrorMessage(e);
                GenericResp resp = new GenericResp();
                resp.setId(meter.getMeterId().toString());
                resp.setMessage("Meter Approve failed: "+reason);
                resp.setData(meter.getMeterNumber());

                failedRecords.add(resp);

                log.warn("Meter {} failed individually: {}",meter.getMeterNumber(), reason);
            }
        }

        return successCount;
    }

    @Async
    public CompletableFuture<Integer> approveSingleAsync(Meter meter, UserModel user, List<GenericResp> failedRecords) {
        try {
            approveSingleTransactional(meter, user);
            return CompletableFuture.completedFuture(1);
        } catch (Exception e) {
            String reason = extractErrorMessage(e);
            GenericResp resp = new GenericResp();
            resp.setId(meter.getMeterId().toString());
            resp.setMessage("Meter Approve failed: "+reason);
            resp.setData(meter.getMeterNumber());

            failedRecords.add(resp);
//            failedRecords.add(String.format(
//                    "%s [Region: %s] (Allocation failed: %s)",
//                    meter.getMeterNumber(),
////                    meter.getNodeInfo().getRegionId(),
//                    reason
//            ));
            log.warn("Async allocation failed for meter {}: {}",  meter.getMeterNumber(), reason);
            return CompletableFuture.completedFuture(0);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void approveSingleTransactional(Meter meter, UserModel user) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

        // --- Step 2: Insert into main + version tables ---
        meterMapper.approvedMeterVersion(meter.getMeterStage(), meter.getStatus(), meter.getApproveBy(), meter.getUpdatedAt(), meter.getMeterNumber());

        meterMapper.approveMeter(meter);

        //fetch meter from the database
        Meter m = meterMapper.findById(meter.getMeterId(), user.getOrgId(), user.getNodeInfo().getNodeId());
        //save to audit (mongodb)
        AuditLog auditLog = buildAuditLog(user, "Meter approved", meterName, m, metadata, "");
        safeAuditService.saveAudit(auditLog);

    }


    /** Update or insert approved child meter data */
    private void updateChildMeterData(List<Meter> batch, UserModel user) {
        List<MDMeterInfo> mdList = new ArrayList<>();
        List<SmartMeterInfo> smartList = new ArrayList<>();
        List<PaymentMode> prepaidList = new ArrayList<>();
        List<MDMeterInfo> newMDMeters = new ArrayList<>();
        List<SmartMeterInfo> newSmartMeters = new ArrayList<>();
        System.out.println(">>>>>>>>>>>> updateChildMeterData");
        for (Meter meter : batch) {
            if (meter.getMdMeterInfo() != null) {
                if ("Pending-created".equalsIgnoreCase(meter.getMdMeterInfo().getMeterStage())) {
                    meter.getMdMeterInfo().setMeterStage("Created");
                    newMDMeters.add(meter.getMdMeterInfo());
                } else {
                    mdList.add(meter.getMdMeterInfo());
                }
            }

            if (meter.getSmartMeterInfo() != null) {
                if ("Pending-created".equalsIgnoreCase(meter.getSmartMeterInfo().getMeterStage())) {
                    meter.getSmartMeterInfo().setMeterStage("Created");
                    newSmartMeters.add(meter.getSmartMeterInfo());
                } else {
                    smartList.add(meter.getSmartMeterInfo());
                }
            }
        }

        // Approve existing version data
        if (!mdList.isEmpty()) meterMapper.batchApproveMDMeterInfo(mdList);
        if (!smartList.isEmpty()) meterMapper.batchApproveSmartMeterInfo(smartList);

        // Insert new ones into main tables
        if (!newMDMeters.isEmpty()) {
            meterMapper.batchApproveMDMeterInfo(newMDMeters);
            meterMapper.insertBatchApproveMDMeterInfo(newMDMeters);
        }
        if (!newSmartMeters.isEmpty()) {
            meterMapper.batchApproveSmartMeterInfo(newSmartMeters);
            meterMapper.insertBatchApproveSmartMeterInfo(newSmartMeters);
        }
    }

    /**
     * Record audit logs for each approved meter.
     */
    private void auditApproveBatch(List<Meter> batch, UserModel user, String desc) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
        for (Meter m : batch) {
            AuditLog auditLog = buildAuditLog(user, desc, "Bulk Meter", m, metadata, "");
            safeAuditService.saveAudit(auditLog);
        }
    }

    private void auditRejectBatch(List<Meter> batch, UserModel user) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
        for (Meter m : batch) {
            AuditLog auditLog = buildAuditLog(user, "Meter reject", "Bulk Meter", m, metadata, "");
            safeAuditService.saveAudit(auditLog);
        }
    }


    // ---------------------------
    // Simple helper to extract human-friendly message from exceptions
    // ---------------------------
    private String extractErrorMessage1(Exception ex) {
        if (ex == null) return "Unknown error";
        String m = ex.getMessage();
        return m == null ? ex.getClass().getSimpleName() : m;
    }


    private List<MeterRequest> processAllocateExcel(InputStream inputStream) throws IOException {
        List<MeterRequest> meters = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip header row safely
            if (rows.hasNext()) {
                rows.next();
            }

            while (rows.hasNext()) {
                Row row = rows.next();
                MeterRequest meter = new MeterRequest();

                meter.setMeterNumber(getStringCellValue(row.getCell(0)));
                meter.setRegionId(getStringCellValue(row.getCell(1)));

                meters.add(meter);
            }
        }
        return meters;
    }

    private List<MeterRequest> processAllocateCsv(InputStream inputStream) throws IOException {
        List<MeterRequest> meters = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            for (CSVRecord record : csvParser) {
                MeterRequest meter = new MeterRequest();
                meter.setMeterNumber(record.get("meter number"));
                meter.setRegionId(record.get("business hub"));

                meters.add(meter);
            }
        }
        return meters;
    }

    public Map<String, Object> bulkInsertMeters(List<Meter> meters, UserModel user) {
        Map<String, Object> result = new HashMap<>();
        List<GenericResp> failedRecords = new ArrayList<>();

        if (meters == null || meters.isEmpty()) {
            throw new IllegalArgumentException("Meter list cannot be empty");
        }

        // --- Licence Meter Limit Check (import up to limit) ---
        int totalRecords = meters.size();
        Licence licence = LicenceFileUtil.readLicenceFile(dataDir, user.getOrgId());
        if (licence != null && licence.getMaxMeters() > 0) {
            int currentMeters = meterMapper.countMetersByOrgId(user.getOrgId());
            int remaining = licence.getMaxMeters() - currentMeters;
            if (remaining <= 0) {
                throw new GlobalExceptionHandler.NotFoundException(
                    "License meter limit reached (" + licence.getMaxMeters() + " max). No more meters can be added."
                );
            }
            if (meters.size() > remaining) {
                throw new GlobalExceptionHandler.NotFoundException(
                        "Upload exceeds your license limit. Remaining slots: " + remaining
                                + ". Attempted upload: " + meters.size()
                                + " meter(s). Please upload " + remaining + " meter(s) or fewer."
                );
            }
        }

        int successCount = 0;

        // ------------------------------------------
        // Load Manufacturers
        // ------------------------------------------
        List<Manufacturer> manufacturers = meterMapper.getManufacturers(user.getOrgId());
        Map<String, UUID> manufacturerNameToId = manufacturers.stream()
                .collect(Collectors.toMap(
                        m -> m.getName().trim().toLowerCase(),
                        Manufacturer::getId
                ));

        if(manufacturerNameToId.isEmpty()) {
            throw new GlobalExceptionHandler.PartialFailureException(
                    "Meters upload failed - manufacturer not found",
                    result
            );
        }

        //------------------------------------------------
        // Validate duplicates INSIDE FILE
        //------------------------------------------------

        Set<String> seenMeters = new HashSet<>();
        Set<String> seenSims = new HashSet<>();

        Iterator<Meter> fileIterator = meters.iterator();

        while (fileIterator.hasNext()) {

            Meter meter = fileIterator.next();

            String meterNumber = Optional.ofNullable(meter.getMeterNumber()).orElse("").trim();
            String simNumber = Optional.ofNullable(meter.getSimNumber()).orElse("").trim();

            if (!seenMeters.add(meterNumber)) {
                GenericResp resp = new GenericResp();
                resp.setId(meterNumber);
                resp.setMessage("Duplicate meter number in uploaded file");
                resp.setData(simNumber);
                failedRecords.add(resp);
                fileIterator.remove();
                continue;
            }

            if (!simNumber.isEmpty() && !seenSims.add(simNumber)) {
                GenericResp resp = new GenericResp();
                resp.setId(meterNumber);
                resp.setMessage("Duplicate SIM number in uploaded file");
                resp.setData(simNumber);
                failedRecords.add(resp);
                fileIterator.remove();
                continue;
            }
        }

        // ------------------------------------------
        // Extract MeterNumbers + SimNumbers
        // ------------------------------------------

        Set<String> meterNumbers = meters.stream()
                .map(Meter::getMeterNumber)
                .filter(Objects::nonNull)
                .map(String::trim)
                .collect(Collectors.toSet());

        Set<String> simNumbers = meters.stream()
                .map(Meter::getSimNumber)
                .filter(Objects::nonNull)
                .map(String::trim)
                .collect(Collectors.toSet());

        // ---------------------------------------------------
        // Fetch Existing Meter Numbers (ONE DB CALL)
        // ---------------------------------------------------
        Set<String> allMeterNumbers = meters.stream()
                .map(Meter::getMeterNumber)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());


        // ------------------------------------------
        // Fetch Existing
        // ------------------------------------------

        List<Meter> existingMeters =
                meterMapper.getMetersList(
                        new ArrayList<>(meterNumbers),
//                        new ArrayList<>(simNumbers),
                        user.getOrgId()
                );

        Set<String> existingMeterNumbers = existingMeters.stream()
                .map(Meter::getMeterNumber)
                .collect(Collectors.toSet());

        Set<String> existingSimNumbers = existingMeters.stream()
                .map(Meter::getSimNumber)
                .collect(Collectors.toSet());


        int batchSize = 500; // try 500–1000 for optimal JDBC performance

        for (int i = 0; i < meters.size(); i += batchSize) {
            int end = Math.min(i + batchSize, meters.size());
//            List<Meter> batch = meters.subList(i, end);
            List<Meter> batch = new ArrayList<>(meters.subList(i, end));

            // -----------------------------------------------
            // Remove duplicates (already existing meters)
            // -----------------------------------------------
            Iterator<Meter> iterator = batch.iterator();

            while (iterator.hasNext()) {

                Meter meter = iterator.next();

                String meterNumber = meter.getMeterNumber();
                String simNumber = meter.getSimNumber();
                String manufacturer = meter.getMeterManufacturerName();

                if (meterNumber == null || meterNumber.trim().isEmpty()) {
                    GenericResp resp = new GenericResp();
                    resp.setId(null);
                    resp.setMessage("Missing meter number");
                    resp.setData(null);
                    failedRecords.add(resp);
                    iterator.remove();
                    continue;
                }

                meterNumber = meterNumber.trim();

                if (existingMeterNumbers.contains(meterNumber)) {

                    GenericResp resp = new GenericResp();
                    resp.setId(meterNumber);
                    resp.setMessage("Meter already exists");
                    resp.setData(meterNumber);
                    failedRecords.add(resp);
                    iterator.remove();
                    continue;
                }

                if (simNumber != null && existingSimNumbers.contains(simNumber.trim())) {

                    GenericResp resp = new GenericResp();
                    resp.setId(meterNumber);
                    resp.setMessage("SIM number already exists");
                    resp.setData(meterNumber);
                    failedRecords.add(resp);
                    iterator.remove();
                    continue;
                }

                if (manufacturer == null ||
                        !manufacturerNameToId.containsKey(manufacturer.trim().toLowerCase())) {

                    GenericResp resp = new GenericResp();
                    resp.setId(meterNumber);
                    resp.setMessage("Manufacturer does not exist: " + manufacturer);
                    resp.setData(manufacturer);
                    failedRecords.add(resp);
                    iterator.remove();
//                    continue;
                }
            }

            if (batch.isEmpty()) {
                continue;
            }

            try {
                insertBatchTransactional(batch, user, manufacturerNameToId, failedRecords);
                successCount += batch.size();
            } catch (Exception e) {
                log.warn("Batch {} failed — retrying sub batch upload", (i / batchSize) + 1);
                // Attempt smaller sub-batches to isolate failure
                successCount += insertSubBatchTransactional(batch, user, manufacturerNameToId, failedRecords);
            }
        }

        result.put("totalRecords", totalRecords);
        result.put("successCount", successCount);
        result.put("failedCount", failedRecords.size());
        result.put("failedRecords", failedRecords);

        if (!failedRecords.isEmpty()) {
            return ResponseMap.response(
                    "131",
                    failedRecords.size() + " of " + totalRecords + " Meters upload failed",
                    result
            );
        }

        return ResponseMap.response(
                status.getSuccessCode(),
                successCount + " of " + totalRecords + " Meters uploaded successfully",
                result
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void insertBatchTransactional(List<Meter> batch, UserModel user,  Map<String, UUID> manufacturerNameToId, List<GenericResp> failedRecords) {

        prepareMeters(batch, user, manufacturerNameToId, failedRecords);

        // ABSOLUTE SAFETY CHECK
//        batch.removeIf(m -> m.getMeterManufacturer() == null);

        if (batch.isEmpty()) {
            return; // nothing valid to insert
        }

        // Step 1: Insert main meters
        meterMapper.insertMeters(batch);

        // Step 2: Map 'id' → 'meterId' before inserting version records
        for (Meter meter : batch) {
            meter.setMeterId(meter.getId()); // Copy generated ID
        }
        // Insert into meter_versions (replica)
        meterMapper.insertMeterVersions(batch);

        // Insert related info
        insertChildMeterData(batch, user);
        // Audit logs
        auditBatch(batch, user, "Meter created");
    }

    public int insertSubBatchTransactional(List<Meter> batch, UserModel user,  Map<String, UUID> manufacturerNameToId, List<GenericResp> failedRecords) {
        int successCount = 0;
        int subBatchSize = 100;


        for (int i = 0; i < batch.size(); i += subBatchSize) {
            int end = Math.min(i + subBatchSize, batch.size());
//            List<Meter> subBatch = batch.subList(i, end);
            List<Meter> subBatch = new ArrayList<>(batch.subList(i, end));

            try {
                insertBatchTransactional(subBatch, user, manufacturerNameToId, failedRecords);
                successCount += subBatch.size();
            } catch (Exception e) {
                log.warn("Sub-batch failed (size={}): {}", subBatch.size(), e.getMessage());

                if (subBatch.size() > 50) {
                    successCount += insertSinglesFallbackAsync(subBatch, user, failedRecords);
                } else {
                    successCount += insertSinglesFallback(subBatch, user, failedRecords);
                }

//                successCount += insertSinglesFallback(subBatch, user, failedRecords);
            }
        }
        return successCount;
    }

    public int insertSinglesFallbackAsync(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        for (Meter meter : batch) {
            futures.add(insertSingleAsync(meter, user, failedRecords));
        }

        // Wait for all tasks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Sum successful inserts
        return futures.stream().mapToInt(f -> f.join()).sum();
    }

    public int insertSinglesFallback(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        int successCount = 0;

        for (Meter meter : batch) {
            try {
                log.debug("Fallback single upload for meter: {}", meter.getMeterNumber());
                insertSingleTransactional(meter, user);
                successCount++;
            } catch (Exception e) {
                String reason = extractErrorMessage(e);
                GenericResp resp = new GenericResp();
                resp.setId(meter.getMeterNumber());
                resp.setMessage("Meter single save failed: "+reason);
                resp.setData(meter.getMeterNumber());

                failedRecords.add(resp);
//                failedRecords.add(meter.getMeterNumber() + " (" + reason + ")");
                log.warn("Meter {} failed individually: {}", meter.getMeterNumber(), reason);
            }
        }

        return successCount;
    }

    private void prepareMeters(
            List<Meter> batch,
            UserModel user,
            Map<String, UUID> manufacturerNameToId,
            List<GenericResp> failedRecords
    ) {
        Iterator<Meter> iterator = batch.iterator();
        UUID nodeId = user.getNodeInfo().getNodeId();
        while (iterator.hasNext()) {
            Meter meter = iterator.next();

//            if(meter != null){
//                GenericResp resp = new GenericResp();
//                resp.setId(meter.getMeterNumber());
//                resp.setMessage("Meter already exist");
//                resp.setData(meter);
//
//                failedRecords.add(resp);
//                iterator.remove();
//                continue;
//            }

            // --- Validate and set Manufacturer ID ---
            String manuName = meter.getMeterManufacturerName();
            if (manuName == null || manuName.trim().isBlank()) {
                GenericResp resp = new GenericResp();
                resp.setId(meter.getMeterNumber());
                resp.setMessage("Missing manufacturer name");
                resp.setData(meter.getMeterNumber());

                failedRecords.add(resp);
                iterator.remove();
                continue;
            }

            UUID manuId = manufacturerNameToId.get(manuName.trim().toLowerCase());
            if (manuId == null) {
                GenericResp resp = new GenericResp();
                resp.setId(meter.getMeterNumber());
                resp.setMessage("Invalid manufacturer: "+manuName);
                resp.setData(meter.getMeterNumber());

                failedRecords.add(resp);
//                failedRecords.add(meter.getMeterNumber() + " (Invalid manufacturer: " + manuName + ")");
                iterator.remove();
                continue;
            }

            meter.setMeterManufacturer(manuId);

            String validationError = validateRequiredFields(meter);

            if (validationError != null) {
                GenericResp resp = new GenericResp();
                resp.setId(String.valueOf(meter.getMeterNumber()));
                resp.setMessage(validationError);
                resp.setData(meter.getMeterNumber());

                failedRecords.add(resp);
                iterator.remove();
                continue;
            }

            if(nodeId != null){
                resolveNodeHierarchy(meter, nodeId, user.getOrgId());
            }

            if (meter.getMeterCategory().equalsIgnoreCase("prepaid")){
                meter.setMeterCategory("Prepaid");
            }

            if (meter.getMeterType().equalsIgnoreCase("Electricity")){
                meter.setMeterType("Electricity");
            }

            if (meter.getMeterClass().equalsIgnoreCase("single phase")
                    || meter.getMeterClass().equalsIgnoreCase("singlephase")
                    || meter.getMeterClass().equalsIgnoreCase("single-phase")){
                meter.setMeterClass("Single-Phase");
            }

            if (meter.getMeterClass().equalsIgnoreCase("three phase")
                    || meter.getMeterClass().equalsIgnoreCase("threephase")
                    || meter.getMeterClass().equalsIgnoreCase("three-phase")){
                meter.setMeterClass("Three-Phase");
            }

//            validateMeterRequest(meter, user);

            // --- Default Meter Fields ---
            meter.setOrgId(user.getOrgId());
            meter.setCreatedBy(user.getId());
            meter.setStatus("Active");
            meter.setMeterStage("Pending-created");
            meter.setType("NON-VIRTUAL");
            meter.setDescription("Newly Added");
        }
    }

    private String validateRequiredFields(Meter meter) {

        if (meter.getOldTariffIndex() == null) return "Old tariff index is required";
        if (meter.getNewTariffIndex() == null) return "New tariff index is required";
        if (meter.getMeterNumber() == null || meter.getMeterNumber().trim().isEmpty()) return "Meter number is required";
        if (meter.getMeterCategory() == null || meter.getMeterCategory().trim().isEmpty()) return "Meter category is required";
        if (meter.getMeterClass() == null || meter.getMeterClass().trim().isEmpty()) return "Meter class is required";
        if (meter.getMeterType() == null || meter.getMeterType().trim().isEmpty()) return "Meter type is required";
        if (meter.getOldKrn() == null || meter.getOldKrn().trim().isEmpty()) return "Old krn index is required";
        if (meter.getNewKrn() == null || meter.getNewKrn().trim().isEmpty()) return "New krn index is required";
        if (meter.getOldSgc() == null || meter.getOldSgc().trim().isEmpty()) return "Old sgc index is required";
        if (meter.getNewSgc() == null || meter.getNewSgc().trim().isEmpty()) return "New sgc index is required";

        if(meter.getSmartStatus()){
            if (meter.getSmartMeterInfo().getMeterModel() == null || meter.getSmartMeterInfo().getMeterModel().trim().isEmpty()) return "Meter model is required";
            if (meter.getSmartMeterInfo().getProtocol() == null || meter.getSmartMeterInfo().getProtocol().trim().isEmpty()) return "Protocol is required";
            if (meter.getSmartMeterInfo().getAuthentication() == null || meter.getSmartMeterInfo().getAuthentication().trim().isEmpty()) return "Authentication is required";
            if (meter.getSmartMeterInfo().getPassword() == null || meter.getSmartMeterInfo().getPassword().trim().isEmpty()) return "Password is required";
        }

        if(meter.getMeterClass().equalsIgnoreCase("MD")){
            if (meter.getMdMeterInfo().getCtRatioNum() == null || meter.getMdMeterInfo().getCtRatioNum().trim().isEmpty()) return "CT ration num is required";
            if (meter.getMdMeterInfo().getCtRatioDenom() == null || meter.getMdMeterInfo().getCtRatioDenom().trim().isEmpty()) return "CT ration denom is required";
            if (meter.getMdMeterInfo().getVoltRatioNum() == null || meter.getMdMeterInfo().getVoltRatioNum().trim().isEmpty()) return "Volt ration num is required";
            if (meter.getMdMeterInfo().getVoltRatioDenom() == null || meter.getMdMeterInfo().getVoltRatioDenom().trim().isEmpty()) return "Volt ratio denom is required";
            if (meter.getMdMeterInfo().getMeterRating() == null || meter.getMdMeterInfo().getMeterRating().trim().isEmpty()) return "Meter rating is required";
            if (meter.getMdMeterInfo().getInitialReading() == null || meter.getMdMeterInfo().getInitialReading().trim().isEmpty()) return "Initial reading is required";
            if (meter.getMdMeterInfo().getDial() == null || meter.getMdMeterInfo().getDial().trim().isEmpty()) return "Dial is required";
        }
        return null;
    }

    private void insertChildMeterData(List<Meter> batch, UserModel user) {
        List<SmartMeterInfo> smartInfos = new ArrayList<>();
        List<MDMeterInfo> mdInfos = new ArrayList<>();

        for (Meter m : batch) {
            if (m.getSmartMeterInfo() != null) {
                m.getSmartMeterInfo().setMeterId(m.getId());
                m.getSmartMeterInfo().setCreatedBy(user.getId());
                m.getSmartMeterInfo().setOrgId(user.getOrgId());
                m.getSmartMeterInfo().setMeterStage("Pending-created");
                m.getSmartMeterInfo().setDescription("Newly Added");

                smartInfos.add(m.getSmartMeterInfo());
            }
            if (m.getMdMeterInfo() != null) {
                double ctRatioNumerator = Double.parseDouble(m.getMdMeterInfo().getCtRatioNum());
                double ctRatioDenominator = Double.parseDouble(m.getMdMeterInfo().getCtRatioDenom());
                double vtRatioNumerator = Double.parseDouble(m.getMdMeterInfo().getVoltRatioNum());
                double vtRatioDenominator = Double.parseDouble(m.getMdMeterInfo().getVoltRatioDenom());
                double multiplier = (ctRatioNumerator / ctRatioDenominator) * (vtRatioNumerator / vtRatioDenominator);
                BigDecimal rounded = BigDecimal.valueOf(multiplier).setScale(2, RoundingMode.HALF_UP);

                m.getMdMeterInfo().setMeterId(m.getId());
                m.getMdMeterInfo().setCreatedBy(user.getId());
                m.getMdMeterInfo().setOrgId(user.getOrgId());
                m.getMdMeterInfo().setMeterStage("Pending-created");
                m.getMdMeterInfo().setDescription("Newly Added");
                m.getMdMeterInfo().setMultiplier(rounded.toString());

                mdInfos.add(m.getMdMeterInfo());
            }
        }

        if (!smartInfos.isEmpty()) meterMapper.insertBatchSmartMeterInfoVersion(smartInfos);
        if (!mdInfos.isEmpty()) meterMapper.insertBatchMDMeterInfoVersion(mdInfos);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void insertSingleTransactional(Meter meter, UserModel user) {
//        try {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

        // --- Step 1: Prepare core meter entity ---
        meter.setOrgId(user.getOrgId());
        meter.setCreatedBy(user.getId());
        meter.setStatus("Active");
        meter.setMeterStage("Pending-created");
        meter.setType("NON-VIRTUAL");
        meter.setDescription("Newly Added");

        // --- Step 2: Insert into main + version tables ---
        meterMapper.insertSingleBatchMeter(meter);

        // Link version table by setting meterId = generated meter.id
        meter.setMeterId(meter.getId());
        meterMapper.insertSingleBatchMeterVersion(meter);

        // --- Step 3: Child entities ---
        if ("md".equalsIgnoreCase(meter.getMeterClass())) {
            insertMDMeterInfo(meter, user);
        }
        if (Boolean.TRUE.equals(meter.getSmartStatus())) {
            insertSmartMeterInfo(meter, user);
        }

        // --- Step 4: Audit logging ---
        Meter newMeter = meterMapper.findByIdVersion(meter.getId(), user.getOrgId(), user.getNodeInfo().getNodeId());
        AuditLog auditLog = buildAuditLog(user, "Meter created", meterName, newMeter, metadata, "");
        safeAuditService.saveAudit(auditLog);

//        } catch (Exception e) {
//            log.error("Failed to insert meter {}: {}", meter.getMeterNumber(), e.getMessage(), e);
//            throw e; // rethrow so parent caller can track failure count
//        }
    }

    // Parse CSV file into a list of Meter objects
    public static List<Meter> processCsv(InputStream inputStream, UserModel user) throws IOException {
        List<Meter> meters = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            for (CSVRecord record : csvParser) {
                Meter meter = new Meter();
                meter.setMeterNumber(record.get("meterNumber".trim()));
                meter.setSimNumber(record.get("simNumber".trim().trim()));
                meter.setMeterCategory(record.get("meterCategory".trim()));
                meter.setMeterClass(record.get("meterClass".trim()));
                meter.setMeterManufacturerName(record.get("meterManufacturerName".trim()));
                meter.setMeterType(record.get("meterType".trim()));
                meter.setOldSgc(record.get("oldSgc".trim()));
                meter.setNewSgc(record.get("newSgc".trim()));
                meter.setOldKrn(record.get("oldKrn".trim()));
                meter.setNewKrn(record.get("newKrn".trim()));
                meter.setOldTariffIndex(Long.parseLong(record.get("oldTariffIndex".trim())));
                meter.setNewTariffIndex(Long.parseLong(record.get("newTariffIndex".trim())));

                boolean isSmart = Boolean.parseBoolean(record.get("smartStatus"));
                meter.setSmartStatus(isSmart);

                // Handle smart meter info
                if (isSmart) {
                    if (meter.getSmartMeterInfo() == null) {
                        meter.setSmartMeterInfo(new SmartMeterInfo());
                    }
                    meter.getSmartMeterInfo().setMeterModel(record.get("meterModel".trim()));
                    meter.getSmartMeterInfo().setProtocol(record.get("protocol".trim()));
                    meter.getSmartMeterInfo().setAuthentication(record.get("authentication".trim()));
                    meter.getSmartMeterInfo().setPassword(record.get("password".trim()));
                }

                // Handle MD meter info (only if class matches certain type)
                String meterClass = record.get("meterClass".trim());
                if ("MD".equalsIgnoreCase(meterClass)) { // or whatever condition applies
                    if (meter.getMdMeterInfo() == null) {
                        meter.setMdMeterInfo(new MDMeterInfo());
                    }
                    meter.getMdMeterInfo().setCtRatioNum(record.get("ctRatioNum".trim()));
                    meter.getMdMeterInfo().setCtRatioDenom(record.get("ctRatioDenom".trim()));
                    meter.getMdMeterInfo().setVoltRatioNum(record.get("voltRatioNum".trim()));
                    meter.getMdMeterInfo().setVoltRatioDenom(record.get("voltRatioDenom".trim()));
                    meter.getMdMeterInfo().setMultiplier(record.get("multiplier".trim()));
                    meter.getMdMeterInfo().setMeterRating(record.get("meterRating".trim()));
                    meter.getMdMeterInfo().setInitialReading(record.get("initialReading".trim()));
                    meter.getMdMeterInfo().setDial(record.get("dial".trim()));
                    meter.getMdMeterInfo().setLatitude(record.get("latitude".trim()));
                    meter.getMdMeterInfo().setLongitude(record.get("longitude".trim()));
                }

                meters.add(meter);
            }
        }
        return meters;
    }

    // Parse Excel (.xlsx) file into a list of Meter objects
    public static List<Meter> processExcel(InputStream inputStream, UserModel user) throws IOException {
        List<Meter> meters = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip header row safely
            if (rows.hasNext()) {
                rows.next();
            }

            while (rows.hasNext()) {
                Row row = rows.next();
                Meter meter = new Meter();

                meter.setMeterNumber(getStringCellValue(row.getCell(0)));
                meter.setSimNumber(getStringCellValue(row.getCell(1)));
                meter.setMeterCategory(getStringCellValue(row.getCell(2)));
                meter.setMeterClass(getStringCellValue(row.getCell(3)));
                meter.setMeterManufacturerName(getStringCellValue(row.getCell(4)));
                meter.setMeterType(getStringCellValue(row.getCell(5)));

                meter.setOldSgc(getStringCellValue(row.getCell(6)));
                meter.setNewSgc(getStringCellValue(row.getCell(7)));
                meter.setOldKrn(getStringCellValue(row.getCell(8)));
                meter.setNewKrn(getStringCellValue(row.getCell(9)));

                meter.setOldTariffIndex(parseLongSafe(getStringCellValue(row.getCell(10))));
                meter.setNewTariffIndex(parseLongSafe(getStringCellValue(row.getCell(11))));

                boolean isSmart = Boolean.parseBoolean(getStringCellValue(row.getCell(12)));
                meter.setSmartStatus(isSmart);

                // Smart meter info
                if (isSmart) {
                    if (meter.getSmartMeterInfo() == null) {
                        meter.setSmartMeterInfo(new SmartMeterInfo());
                    }
                    meter.getSmartMeterInfo().setMeterModel(getStringCellValue(row.getCell(13)));
                    meter.getSmartMeterInfo().setProtocol(getStringCellValue(row.getCell(14)));
                    meter.getSmartMeterInfo().setAuthentication(getStringCellValue(row.getCell(15)));
                    meter.getSmartMeterInfo().setPassword(getStringCellValue(row.getCell(16)));
                }

                // MD meter info
                String meterClass = meter.getMeterClass();
                if ("MD".equalsIgnoreCase(meterClass)) {
                    if (meter.getMdMeterInfo() == null) {
                        meter.setMdMeterInfo(new MDMeterInfo());
                    }
                    meter.getMdMeterInfo().setCtRatioNum(getStringCellValue(row.getCell(17)));
                    meter.getMdMeterInfo().setCtRatioDenom(getStringCellValue(row.getCell(18)));
                    meter.getMdMeterInfo().setVoltRatioNum(getStringCellValue(row.getCell(19)));
                    meter.getMdMeterInfo().setVoltRatioDenom(getStringCellValue(row.getCell(20)));
//                    meter.getMdMeterInfo().setMultiplier(getStringCellValue(row.getCell(21)));
                    meter.getMdMeterInfo().setMeterRating(getStringCellValue(row.getCell(21)));
                    meter.getMdMeterInfo().setInitialReading(getStringCellValue(row.getCell(22)));
                    meter.getMdMeterInfo().setDial(getStringCellValue(row.getCell(23)));
                    meter.getMdMeterInfo().setLatitude(getStringCellValue(row.getCell(24)));
                    meter.getMdMeterInfo().setLongitude(getStringCellValue(row.getCell(25)));
                }

                meters.add(meter);
            }
        }
        return meters;
    }


    private void auditBatch(List<Meter> batch, UserModel user, String desc) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
        for (Meter m : batch) {
            AuditLog auditLog = buildAuditLog(user, desc, "Meter", m, metadata, "");
            safeAuditService.saveAudit(auditLog);
        }
    }
    @Override
    public ByteArrayInputStream exportActualMeter() {

        UserModel user = handleUserValidation();

        List<Meter> allMeters = meterMapper.getAllMeters(user.getOrgId(), "NON-VIRTUAL");

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Meter Report");

            // Create header
            String[] headers = {
                    "S/N", "Meter Number", "SIM No", "Old SGC",
                    "New SGC", "Manufacturer", "Class", "Category", "Meter Stage", "Activation Status", "Feeder", "DSS", "Tariff"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // Data rows
            for (int i = 0; i < allMeters.size(); i++) {
                Meter meter = allMeters.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(meter.getMeterNumber());
                row.createCell(2).setCellValue(meter.getSimNumber());
                row.createCell(3).setCellValue(meter.getOldSgc());
                row.createCell(4).setCellValue(meter.getNewSgc());
                row.createCell(5).setCellValue(meter.getManufacturer().getName());
                row.createCell(6).setCellValue(meter.getMeterClass());
                row.createCell(7).setCellValue(meter.getMeterCategory());
                row.createCell(8).setCellValue(meter.getMeterStage());
                row.createCell(9).setCellValue(meter.getStatus());
                row.createCell(10).setCellValue(meter.getFeederInfo() == null ? "" : meter.getFeederInfo().getName());
                row.createCell(11).setCellValue(meter.getDssInfo() == null ? "" : meter.getDssInfo().getName());
                row.createCell(12).setCellValue(meter.getTariffInfo() == null ? "" : meter.getTariffInfo().getName());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Error exporting meter data", e);
        }
    }

    @Override
    public ByteArrayInputStream exportVirtualMeter() {

        UserModel user = handleUserValidation();

        List<Meter> allMeters = meterMapper.getAllMeters(user.getOrgId(), "VIRTUAL");

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Meter Report");

            // Create header
            String[] headers = {
                    "S/N", "Customer ID", "Meter Number", "Account Number",
                        "Feeder", "DSS", "CIN", "Tariff", "Status"
                };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // Data rows
            for (int i = 0; i < allMeters.size(); i++) {
                Meter meter = allMeters.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(meter.getCustomerId());
                row.createCell(2).setCellValue(meter.getMeterNumber());
                row.createCell(3).setCellValue(meter.getAccountNumber());
                row.createCell(4).setCellValue(meter.getFeederInfo().getName());
                row.createCell(5).setCellValue(meter.getDssInfo().getName());
                row.createCell(6).setCellValue(meter.getCin());
                row.createCell(7).setCellValue(meter.getTariffInfo().getName());
                row.createCell(8).setCellValue(meter.getStatus());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Error exporting meter data", e);
        }
    }
//    private void resolveHierarchy(AssignMeterToCustomer request, UUID startNodeId, UUID orgId, UUID bhubId) {
//
//        UUID currentNodeId = startNodeId;
//        Set<UUID> visited = new HashSet<>();
//
//        while (currentNodeId != null) {
//
//            if (!visited.add(currentNodeId)) {
//                throw new IllegalStateException("Circular hierarchy detected");
//            }
//
//            NodeSummary node = nodeMapper.getNodeByNodeId(currentNodeId, orgId);
//            if (node == null) break;
//
//            String type = node.getType() == null ? "" : node.getType().toLowerCase();
//
//            switch (type) {
//                case "business hub":
//                    System.out.println("bbbhhh:: "+node.getNodeId());
//                    if(bhubId.equals(node.getNodeId())){
//                        request.setNodeId(node.getNodeId());
//                    } else {
//                        throw new GlobalExceptionHandler
//                                .NotFoundException("Feeder does not belong to the bushiness hub meter is allocated");
//                    }
//
//                    break;
//                case "service center":
//                    request.setServiceCenter(node.getNodeId());
//                    break;
//                case "region":
//                    request.setRegion(node.getNodeId());
//                    break;
//                case "substation":
//                    request.setSubstation(node.getNodeId());
//                    break;
//                case "root":
//                    request.setRoot(node.getNodeId());
//                    break;
//            }
//
//            currentNodeId = node.getParentId();
//        }
//    }

    @Override
    public Map<String, Object> bulkAssign(MultipartFile file) throws IOException {
        try {
            UserModel user = handleUserValidation();
            Map<String, Object> result;
            if(!user.getNodeInfo().getType().equalsIgnoreCase("Root")
                    || !user.getNodeInfo().getType().equalsIgnoreCase("Region")
                    || !user.getNodeInfo().getType().equalsIgnoreCase("Business hub")
                    || !user.getNodeInfo().getType().equalsIgnoreCase("Service center")){
                // Determine file type
                String filename = Optional.ofNullable(file.getOriginalFilename())
                        .orElseThrow(() -> new IOException("File has no name"));

                List<AssignMeterToCustomer> meters;
                if (filename.endsWith(".csv")) {
                    meters = processAssignCsv(file.getInputStream());
                } else if (filename.endsWith(".xlsx")) {
                    meters = processAssignExcel(file.getInputStream());
                } else {
                    throw new IOException("Unsupported file format. Only .csv or .xlsx allowed.");
                }
                 result = bulkAssignMeters(meters, user);
            } else{
                throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
            }

            return result;

        } catch (Exception e) {
            log.error("Error in bulk assign upload: {}", e.getMessage(), e);
            genericHandler.logIncidentReport("Bulk assign service failed");
            genericHandler.logAndSaveException(e, "Bulk assign meter");
            throw new IOException("Bulk assigned failed: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> bulkVirtualAssign(MultipartFile file) throws IOException {
        try {
            UserModel user = handleUserValidation();

            // Determine file type
            String filename = Optional.ofNullable(file.getOriginalFilename())
                    .orElseThrow(() -> new IOException("File has no name"));
            if(!user.getNodeInfo().getType().equalsIgnoreCase("Business hub")
                    || !user.getNodeInfo().getType().equalsIgnoreCase("Service center")){
                throw new GlobalExceptionHandler.NotFoundException("You do not have permission");
            }
            List<AssignMeterToCustomer> meters;
            if (filename.endsWith(".csv")) {
                meters = processVirtualAssignCsv(file.getInputStream());
            } else if (filename.endsWith(".xlsx")) {
                meters = processVirtualAssignExcel(file.getInputStream());
            } else {
                throw new IOException("Unsupported file format. Only .csv or .xlsx allowed.");
            }
            Map<String, Object> result = bulkAssignVirtualMeters(meters, user);
            return result;

        } catch (Exception e) {
            log.error("Error in bulk assign upload: {}", e.getMessage(), e);
            genericHandler.logIncidentReport("Bulk virtual assign service failed");
            genericHandler.logAndSaveException(e, "Bulk virtual assign meter");
            throw new IOException("Bulk virtual assign failed: " + e.getMessage());
        }
    }

//    @Override
    public Map<String, Object> bulkAssignMeters(List<AssignMeterToCustomer> assign, UserModel user) {
        Map<String, Object> result = new HashMap<>();
        List<GenericResp> failedRecords = new ArrayList<>();
        int successCount = 0;

        if (assign == null || assign.isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("No records found in uploaded file");
        }

        // ---------------------------------------------------
        // Prevent duplicate meter numbers inside uploaded file
        // ---------------------------------------------------

        Set<String> seenMeters = new HashSet<>();

        Iterator<AssignMeterToCustomer> fileIterator = assign.iterator();

        while (fileIterator.hasNext()) {

            AssignMeterToCustomer req = fileIterator.next();

            String meterNumber = req.getMeterNumber();

            if (meterNumber == null || meterNumber.trim().isEmpty()) {

                GenericResp resp = new GenericResp();
                resp.setId("");
                resp.setMessage("Missing meter number");
                resp.setData(null);

                failedRecords.add(resp);
                fileIterator.remove();
                continue;
            }

            meterNumber = meterNumber.trim();

            if (!seenMeters.add(meterNumber)) {

                GenericResp resp = new GenericResp();
                resp.setId(meterNumber);
                resp.setMessage("Duplicate meter number in uploaded file");
                resp.setData(meterNumber);

                failedRecords.add(resp);
                fileIterator.remove();
            }
        }

        final int BATCH_SIZE = 500;

        for (int i = 0; i < assign.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, assign.size());
//            List<AssignMeterToCustomer> subBatch = assign.subList(i, end);
            List<AssignMeterToCustomer> subBatch = new ArrayList<>(assign.subList(i, end));
            // Extract required lists
            List<String> meterNumbers = subBatch.stream()
                    .map(AssignMeterToCustomer::getMeterNumber)
                    .filter(num -> num != null && !num.trim().isEmpty())
                    .map(String::trim)
                    .toList();

            List<String> tariffNames = subBatch.stream()
                    .map(AssignMeterToCustomer::getTariffName)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> customerIds = subBatch.stream()
                    .map(AssignMeterToCustomer::getCustomerId)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> dssIds = subBatch.stream()
                    .map(AssignMeterToCustomer::getDssAssetId)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> feederIds = subBatch.stream()
                    .map(AssignMeterToCustomer::getFeederAssetId)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> cins = subBatch.stream()
                    .map(AssignMeterToCustomer::getCin)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> state = subBatch.stream()
                    .map(AssignMeterToCustomer::getState)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> city = subBatch.stream()
                    .map(AssignMeterToCustomer::getCity)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> houseNo = subBatch.stream()
                    .map(AssignMeterToCustomer::getHouseNo)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();
            List<String> streetName = subBatch.stream()
                    .map(AssignMeterToCustomer::getStreetName)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

//            if (meterNumbers.isEmpty() || tariffNames.isEmpty() || customerIds.isEmpty() ||
//                    dssIds.isEmpty() || feederIds.isEmpty() || cins.isEmpty()) {
//                subBatch.forEach(req -> failedRecords.add(
//                        String.format("%s [TariffName: %s, customerId: %s, dssAssetId: %s, feederAssetId: %s, cin: %s] (Invalid or missing data)",
//                                req.getMeterNumber(), req.getTariffName(), req.getCustomerId(), req.getDssAssetId(), req.getFeederAssetId(), req.getCin())
//                ));
//                continue;
//            }

            if (meterNumbers.isEmpty() || tariffNames.isEmpty() || customerIds.isEmpty()
                    || dssIds.isEmpty() || feederIds.isEmpty() || cins.isEmpty()) {
                subBatch.forEach(req -> {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Missing meter number, tariff name, customer id, dss asset id, feeder asset id or cin");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
                });

                continue;
            }

            if (state.isEmpty() || city.isEmpty() || houseNo.isEmpty() || streetName.isEmpty()) {
                subBatch.forEach(req -> {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Missing state, city, customer id, houseNo, or streetName");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
                });

                continue;
            }

            // Fetch from DB
            List<Meter> meters = meterMapper.getUnassignMetersByMeterNumbers(meterNumbers, user.getOrgId());
            Map<String, Meter> meterMap = meters.stream()
                    .collect(Collectors.toMap(Meter::getMeterNumber, m -> m));

            List<Meter> cin = meterMapper.getMetersByCins(cins, user.getOrgId());
            Map<String, Meter> cinMap = cin.stream()
                    .filter(m -> m.getCin() != null)
                    .collect(Collectors.toMap(
                            Meter::getCin,
                            m -> m,
                            (existing, replacement) -> existing
                    ));
//            Map<String, Meter> cinMap = meters.stream()
//                    .collect(Collectors.toMap(Meter::getCin, m -> m));

            List<Tariff> tariff = meterMapper.getTariffByNames(tariffNames, user.getOrgId());
            Map<String, UUID> tariffMap = tariff.stream()
                    .collect(Collectors.toMap(Tariff::getName, Tariff::getId));

            List<Customer> cId = meterMapper.getByCustomerIds(customerIds, user.getOrgId());
            Map<String, String> customerIdMap = cId.stream()
                    .collect(Collectors.toMap(Customer::getCustomerId, Customer::getCustomerId));

            System.out.println("dsss:: "+dssIds);
            List<SubStationTransformerFeederLine> dssAssetId = meterMapper.getDss(dssIds, user.getOrgId());
            Map<String, SubStationTransformerFeederLine> dssMap =
                    dssAssetId.stream()
                            .collect(Collectors.toMap(
                                    SubStationTransformerFeederLine::getAssetId,
                                    d -> d,
                                    (existing, replacement) -> existing
                            ));
//
            System.out.println("feder:: "+feederIds);
            List<SubStationTransformerFeederLine> feederAssetId = meterMapper.getFeeder(feederIds, user.getOrgId());
            Map<String, SubStationTransformerFeederLine> feederMap =
                    feederAssetId.stream()
                            .collect(Collectors.toMap(
                                    SubStationTransformerFeederLine::getAssetId,
                                    f -> f,
                                    (existing, replacement) -> existing
                            ));


//            RegionBhubServiceCenter regionBhubServiceCenter = meterMapper.verifyBhub(meters.getNodeId(), user.getOrgId());

            List<Meter> validAssign = new ArrayList<>();

            List<MeterAssignLocation> validAssignLocation = new ArrayList<>();

            List<PaymentMode> validAssignPayment = new ArrayList<>();

            for (AssignMeterToCustomer req : subBatch) {
                Meter meter = meterMap.get(req.getMeterNumber());
                Meter c = cinMap.get(req.getCin());
                Meter businessHud = meterMap.get(req.getNodeId());
                UUID tariffId = tariffMap.get(req.getTariffName());
                String customerId = customerIdMap.get(req.getCustomerId());
                SubStationTransformerFeederLine dss = dssMap.get(req.getDssAssetId());
                SubStationTransformerFeederLine feeder = feederMap.get(req.getFeederAssetId());

                UUID dssId = dss != null ? dss.getNodeId() : null;
                UUID feederId = feeder != null ? feeder.getNodeId() : null;
//                UUID dssId = dssIdMap.get(req.getDssAssetId());
//                UUID feederId = feederIdMap.get(req.getFeederAssetId());

                if (meter == null) {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Meter not found, deactivated or in a pending state");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
                    continue;
                }

                if (c != null) {
                    GenericResp resp = new GenericResp();
                    resp.setId(meter.getMeterId().toString());
                    resp.setMessage("CIN already exist");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format("%s (CIN already exist)", req.getCin()));
                    continue; }

                if (tariffId == null) {
                    GenericResp resp = new GenericResp();
                    resp.setId(meter.getMeterNumber());
                    resp.setMessage("Tariff not found, deactivated or have a pending state");
                    resp.setData(req.getTariffName());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format("%s [Tariff: %s] (Tariff not found, deactivated or have a pending state)", req.getMeterNumber(), req.getTariffName()));
                    continue;
                }

                if (customerId == null) {
                    GenericResp resp = new GenericResp();
                    resp.setId(customerId);
                    resp.setMessage("Customer not found or blocked");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format("%s [Customer: %s] (Customer not found or blocked)", req.getMeterNumber(), req.getCustomerId()));
                    continue;
                }

                if (dssId == null) {
                    GenericResp resp = new GenericResp();
                    resp.setId(meter.getMeterNumber());
                    resp.setMessage("Dss not found");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format("%s [DssAssetId: %s] (Dss not found)", req.getMeterNumber(), req.getDssAssetId()));
                    continue;
                }

                if (feederId == null) {
                    GenericResp resp = new GenericResp();
                    resp.setId(meter.getMeterId().toString());
                    resp.setMessage("Feeder not found");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format("%s [FeederAssetId: %s] (Feeder not found)", req.getMeterNumber(), req.getFeederAssetId()));
                    continue;
                }

                // Validate DSS belongs to Feeder
                if (dss != null && feeder != null) {

                    if (!Objects.equals(dss.getParentId(), feeder.getNodeId())) {

                        GenericResp resp = new GenericResp();
                        resp.setId(req.getMeterNumber());
                        resp.setMessage("DSS does not belong to the chosen feeder");
                        resp.setData(req.getMeterNumber());

                        failedRecords.add(resp);
                        continue;
                    }
                }

                if (feeder != null && feeder.getParentId() == null) {

                    GenericResp resp = new GenericResp();
                    resp.setId(req.getMeterNumber());
                    resp.setMessage("Feeder does not belong to any Business Hub");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
                    continue;
                }

                assert feeder != null;
                resolveBulkNodeHierarchy(meter, feeder.getParentId(), user.getOrgId(), meter.getNodeId());


//                if (feeder != null) {
//
//                    if (!Objects.equals(meter.getNodeId(), feeder.getParentId())
//                            || !Objects.equals(meter.getSubstation(), feeder.getParentId())
//                            || !Objects.equals(meter.getServiceCenter(), feeder.getParentId())) {
//
//                        GenericResp resp = new GenericResp();
//                        resp.setId(req.getMeterNumber());
//                        resp.setMessage("Feeder does not belong to any business hub meter is allocated");
//                        resp.setData(req.getMeterNumber());
//
//                        failedRecords.add(resp);
//                        continue;
//                    }
//                }

                // Auto-generate unique account number
                String generatedAccountNumber = handleGetAccountNumber();

                meter.setOrgId(user.getOrgId());
                meter.setCin(req.getCin());
                meter.setAccountNumber(generatedAccountNumber);
//                meter.setNodeId(feederId);
                meter.setFeeder(feederId);
                meter.setDss(dssId);
                meter.setCustomerId(customerId);
                meter.setTariff(tariffId);
                meter.setOrgId(user.getOrgId());
                meter.setMeterStage("Pending-assigned");
                meter.setCreatedBy(user.getId());
                meter.setDescription("Meter Assigned");

//                System.out.println(">>>>meterId: "+meter.getId());

                // === New fields ===
                if (meter.getMeterAssignLocation() == null) {
                    meter.setMeterAssignLocation(new MeterAssignLocation());
                }
                if (meter.getPaymentMode() == null) {
                    meter.setPaymentMode(new PaymentMode());
                }

                MeterAssignLocation location = meter.getMeterAssignLocation();
                location.setOrgId(user.getOrgId());
                location.setCreatedBy(user.getId());
                location.setMeterStage("Pending-assigned");
                location.setDescription("Location assigned");
                location.setMeterId(meter.getId());
                location.setState(req.getState());
                location.setCity(req.getCity());
                location.setHouseNo(req.getHouseNo());
                location.setStreetName(req.getStreetName());


                // === Payment info only for PREPAID meters ===
                if ("PREPAID".equalsIgnoreCase(meter.getMeterCategory())) {
//                    PaymentMode payment = new PaymentMode();
                    PaymentMode payment = meter.getPaymentMode();
                    payment.setOrgId(user.getOrgId());
                    payment.setMeterId(meter.getId());
                    payment.setCreatedBy(user.getId());
                    payment.setDescription("Payment mode assigned");
                    payment.setMeterStage("Pending-assigned");
                    payment.setCreditPaymentMode(req.getCreditPaymentMode());
                    payment.setCreditPaymentPlan(req.getCreditPaymentPlan());
                    payment.setDebitPaymentMode(req.getDebitPaymentMode());
                    payment.setDebitPaymentPlan(req.getDebitPaymentPlan());
                    validAssignPayment.add(payment);
                }
                validAssign.add(meter);

                validAssignLocation.add(location);
            }

            if (validAssign.isEmpty()) continue;

            try {
                log.info("Processing batch {} - {} ({} records)", i, end - 1, subBatch.size());
                int assigned = assignBatchTransactional(validAssign, user, validAssignLocation,validAssignPayment);
                successCount += assigned;
            } catch (Exception e) {
                log.warn("Batch {} failed — retrying smaller sub-batches: {}", (i / BATCH_SIZE) + 1, e.getMessage());
//                successCount += assignSubBatchTransactional(validAssign, user, failedRecords, validAssignLocation, validAssignPayment);
            }
        }

        int total = successCount + failedRecords.size();

        result.put("totalRecords", total);
        result.put("successCount", successCount);
        result.put("failedCount", failedRecords.size());
        result.put("failedRecords", failedRecords);

        if (!failedRecords.isEmpty()) {

            return ResponseMap.response(
                    "131",
                    failedRecords.size() + " of " + total + " Meters assigned failed",
                    result
            );
        }

        return ResponseMap.response(
                status.getSuccessCode(),
                String.format("%d of %d meters assigned successfully", successCount, total),
                result
        );
    }

    private void resolveBulkNodeHierarchy(Meter request, UUID startNodeId, UUID orgId, UUID nodeId) {

        UUID currentNodeId = startNodeId;
        Set<UUID> visited = new HashSet<>();
        boolean check = false;

        while (currentNodeId != null) {

            if (!visited.add(currentNodeId)) {
                throw new IllegalStateException("Circular hierarchy detected");
            }

            NodeSummary node = nodeMapper.getNodeByNodeId(currentNodeId, orgId);
            if (node == null) break;

            String type = node.getType() == null ? "" : node.getType().toLowerCase();

            switch (type) {
                case "business hub":
                    if (nodeId.equals(node.getNodeId())) {
                        System.out.print(">>>>>>>>>>>>222222222");
                        request.setNodeId(node.getNodeId());
                    } else if (check) {
                        System.out.print(">>>>>66666666666622");
                        request.setNodeId(node.getNodeId());
                    } else {
                        System.out.print("0000000000000000000");
                        throw new GlobalExceptionHandler
                                .NotFoundException("Feeder does not belong to the bushiness hub meter is allocated");
                    }
                    break;
                case "service center":
                    if (nodeId.equals(node.getNodeId())) {
                        request.setNodeId(node.getNodeId());
                        check = true;
                    } else {
                        request.setServiceCenter(node.getNodeId());
                    }
                    break;
                case "region":
                    request.setRegion(node.getNodeId());
                    break;
                case "substation":
                    request.setSubstation(node.getNodeId());
                    break;
                case "feeder line":
                    request.setSubstation(node.getNodeId());
                    break;
                case "dss":
                    request.setSubstation(node.getNodeId());
                    break;
                case "root":
                    request.setRoot(node.getNodeId());
                    break;
            }
                currentNodeId = node.getParentId();
        }
    }


    public Map<String, Object> bulkAssignVirtualMeters(List<AssignMeterToCustomer> assign, UserModel user) {
        Map<String, Object> result = new HashMap<>();
        List<GenericResp> failedRecords = new ArrayList<>();
        int successCount = 0;

        if (assign == null || assign.isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("No records found in uploaded file");
        }

        final int BATCH_SIZE = 500;

        for (int i = 0; i < assign.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, assign.size());
            List<AssignMeterToCustomer> subBatch = assign.subList(i, end);

//            // Extract required lists
            List<String> tariffNames = subBatch.stream()
                    .map(AssignMeterToCustomer::getTariffName)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> meterClass = subBatch.stream()
                    .map(AssignMeterToCustomer::getMeterClass)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> customerIds = subBatch.stream()
                    .map(AssignMeterToCustomer::getCustomerId)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> dssIds = subBatch.stream()
                    .map(AssignMeterToCustomer::getDssAssetId)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> feederIds = subBatch.stream()
                    .map(AssignMeterToCustomer::getFeederAssetId)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> cins = subBatch.stream()
                    .map(AssignMeterToCustomer::getCin)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> state = subBatch.stream()
                    .map(AssignMeterToCustomer::getState)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> city = subBatch.stream()
                    .map(AssignMeterToCustomer::getCity)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            List<String> houseNo = subBatch.stream()
                    .map(AssignMeterToCustomer::getHouseNo)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();
            List<String> streetName = subBatch.stream()
                    .map(AssignMeterToCustomer::getStreetName)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            if (tariffNames.isEmpty() || customerIds.isEmpty()
                    || dssIds.isEmpty() || feederIds.isEmpty() || cins.isEmpty()) {
                subBatch.forEach(req -> {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Missing tariff name, customer id, dss asset id, feeder asset id or cin");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
                });

                continue;
            }

            if (state.isEmpty() || city.isEmpty() || houseNo.isEmpty() || streetName.isEmpty()) {
                subBatch.forEach(req -> {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Missing state, city, customer id, houseNo, or streetName");
                    resp.setData(req.getMeterNumber());

                    failedRecords.add(resp);
                });

                continue;
            }

//            if (tariffNames.isEmpty() || customerIds.isEmpty() || meterClass.isEmpty() ||
//                    dssIds.isEmpty() || feederIds.isEmpty() || cins.isEmpty()) {
//                subBatch.forEach(req -> failedRecords.add(
//                        String.format("[TariffName: %s, customerId: %s, dssAssetId: %s, feederAssetId: %s, cin: %s, meterClass: %s] (Invalid or missing data)",
//                                req.getTariffName(), req.getCustomerId(), req.getDssAssetId(), req.getFeederAssetId(), req.getCin(), req.getMeterClass())
//                ));
//                continue;
//            }
//
//
//            if (state.isEmpty() || city.isEmpty() || houseNo.isEmpty() || streetName.isEmpty()) {
//                subBatch.forEach(req -> failedRecords.add(
//                        String.format("[State: %s, city: %s, houseNo: %s, streetName: %s] (Invalid or missing data)",
//                                req.getState(), req.getCity(), req.getHouseNo(), req.getStreetName())
//                ));
//                continue;
//            }

            // Fetch from DB
            List<Meter> meters = meterMapper.getMetersByCins(cins, user.getOrgId());
            Map<String, Meter> meterMap = meters.stream()
                    .collect(Collectors.toMap(Meter::getCin, m -> m));

            // Fetch from DB
            List<Tariff> tariff = meterMapper.getTariffByNames(tariffNames, user.getOrgId());
            Map<String, UUID> tariffMap = tariff.stream()
                    .collect(Collectors.toMap(Tariff::getName, Tariff::getId));

            List<Customer> cId = meterMapper.getByCustomerIds(customerIds, user.getOrgId());
            Map<String, String> customerIdMap = cId.stream()
                    .collect(Collectors.toMap(Customer::getCustomerId, Customer::getCustomerId));

            List<SubStationTransformerFeederLine> dssAssetId = meterMapper.getDss(dssIds, user.getOrgId());
            Map<String, UUID> dssIdMap = dssAssetId.stream()
                    .collect(Collectors.toMap(SubStationTransformerFeederLine::getAssetId, SubStationTransformerFeederLine::getNodeId));

            List<SubStationTransformerFeederLine> feederAssetId = meterMapper.getFeeder(feederIds, user.getOrgId());
            Map<String, UUID> feederIdMap = feederAssetId.stream()
                    .collect(Collectors.toMap(SubStationTransformerFeederLine::getAssetId, SubStationTransformerFeederLine::getNodeId));

            List<Meter> validAssign = new ArrayList<>();

            List<MeterAssignLocation> validAssignLocation = new ArrayList<>();


            for (AssignMeterToCustomer req : subBatch) {
                Meter ci = meterMap.get(req.getCin());
                UUID tariffId = tariffMap.get(req.getTariffName());
                String customerId = customerIdMap.get(req.getCustomerId());
                UUID dssId = dssIdMap.get(req.getDssAssetId());
                UUID feederId = feederIdMap.get(req.getFeederAssetId());

                if (ci != null) {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("CIN already exist");
                    resp.setData(req.getCin());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format("%s (CIN already exist)", req.getCin()));
                    continue;
                }

                if (tariffId == null) {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Tariff not found, deactivated or have a pending state");
                    resp.setData(req.getTariffName());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format("%s [Tariff: %s] (Tariff not found, deactivated or have a pending state)", req.getCustomerId(), req.getTariffName()));
                    continue;
                }

                if (customerId == null) {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Customer not found or blocked");
                    resp.setData(req.getTariffName());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format("%s (Customer not found or blocked)", req.getCustomerId()));
                    continue;
                }

                if (dssId == null) {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Dss not found");
                    resp.setData(req.getDssAssetId());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format("%s [DssAssetId: %s] (Dss not found)", req.getCustomerId(), req.getDssAssetId()));
                    continue;
                }

                if (feederId == null) {
                    GenericResp resp = new GenericResp();
                    resp.setId("");
                    resp.setMessage("Feeder not found");
                    resp.setData(req.getFeederAssetId());

                    failedRecords.add(resp);
//                    failedRecords.add(String.format("%s [FeederAssetId: %s] (Feeder not found)", req.getCustomerId(), req.getFeederAssetId()));
                    continue;
                }



                // Auto-generate unique account number
                String generatedAccountNumber = handleGetAccountNumber();

                // Auto-generate unique meter number
                String generateMeterNumber = handleGetVirtualMeter();

                Meter meter = new Meter();

                meter.setMeterNumber(generateMeterNumber);
                meter.setOrgId(user.getOrgId());
                meter.setCin(req.getCin());
                meter.setAccountNumber(generatedAccountNumber);
                meter.setType("VIRTUAL");
                meter.setSimNumber("VIRTUAL");
                meter.setMeterClass(req.getMeterClass());
                meter.setMeterType("Electricity");
                meter.setOldSgc("0");
                meter.setNewSgc("0");
                meter.setOldKrn("0");
                meter.setNewKrn("0");
                meter.setOldTariffIndex(1L);
                meter.setNewTariffIndex(1L);
                meter.setNodeId(feederId);
                meter.setSmartStatus(false);
                meter.setDss(dssId);
                meter.setCustomerId(customerId);
                meter.setTariff(tariffId);
                meter.setStatus("Active");
                meter.setOrgId(user.getOrgId());
                meter.setMeterStage("Pending-assigned");
                meter.setCreatedBy(user.getId());
                meter.setDescription("Meter Assigned");
                meter.setFixedEnergy(req.getFixedEnergy());
                meter.setMeterCategory("Postpaid");

                // === New fields ===
                if (meter.getMeterAssignLocation() == null) {
                    meter.setMeterAssignLocation(new MeterAssignLocation());
                }

                MeterAssignLocation location = meter.getMeterAssignLocation();
                location.setOrgId(user.getOrgId());
                location.setCreatedBy(user.getId());
                location.setMeterStage("Pending-assigned");
                location.setDescription("Location assigned");
                location.setState(req.getState());
                location.setCity(req.getCity());
                location.setHouseNo(req.getHouseNo());
                location.setStreetName(req.getStreetName());

                validAssign.add(meter);

                validAssignLocation.add(location);
            }

            if (validAssign.isEmpty()) continue;

            try {
                log.info("Processing batch {} - {} ({} records)", i, end - 1, subBatch.size());
                int assigned = assignVirtualBatchTransactional(validAssign, user, validAssignLocation);
                successCount += assigned;
            } catch (Exception e) {
                log.warn("Batch {} failed — retrying smaller sub-batches: {}", (i / BATCH_SIZE) + 1, e.getMessage());
                successCount += assignVirtualSubBatchTransactional(validAssign, user, failedRecords, validAssignLocation);
            }
        }

        int total = successCount + failedRecords.size();

        result.put("totalRecords", total);
        result.put("successCount", successCount);
        result.put("failedCount", failedRecords.size());
        result.put("failedRecords", failedRecords);

        if (!failedRecords.isEmpty()) {
            throw new GlobalExceptionHandler.PartialFailureException(
                    failedRecords.size() + " of " + total + " Meters assigned failed",
                    result
            );
        }

        return ResponseMap.response(
                status.getSuccessCode(),
                String.format("%d of %d virtual meters assigned successfully", successCount, total),
                result
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int assignVirtualBatchTransactional(List<Meter> batch, UserModel user, List<MeterAssignLocation> locations) {
        if (batch.isEmpty()) return 0;

        try {

            // === Step 1: Update main meter table ===
            meterMapper.insertMeters(batch);

            // Step 2: Map 'id' → 'meterId' before inserting version records
            for (Meter meter : batch) {
                meter.setMeterId(meter.getId()); // Copy generated ID
                meter.getMeterAssignLocation().setMeterId(meter.getId());
            }

            // === Step 3: Insert meter version records ===
            meterMapper.insertMeterVersions(batch);

            // === Step 4: Bulk insert location assignments ===
            meterMapper.insertAssignLocation(locations);

            // Audit allocations
            auditBatch(batch, user, "Virtual Meter Assigned");

            log.info("Assign virtual {} meters successfully", batch.size());
            return batch.size();

        } catch (Exception e) {
            log.error("Transaction failed during assign, rolling back batch of size {}: {}", batch.size(), e.getMessage());
            genericHandler.logIncidentReport("Bulk virtual assign batch service failed");
            genericHandler.logAndSaveException(e, "Bulk virtual assign batch meter");
            throw new RuntimeException("Batch virtual assign transaction failed. Rolled back.", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int assignBatchTransactional(List<Meter> batch, UserModel user, List<MeterAssignLocation> locations, List<PaymentMode> paymentModes) {
        if (batch.isEmpty()) return 0;

        try {
            // === Step 1: Update main meter table ===
            meterMapper.updateBatchMeterAssign(batch);

            // === Step 2: Insert meter version records ===
            meterMapper.insertMeterVersions(batch);

            // === Step 3: Bulk insert location assignments ===
            meterMapper.insertAssignLocation(locations);

            // === Step 3: Bulk insert payment mode ===
            meterMapper.insertAssignPayment(paymentModes);

            // Audit allocations
            auditBatch(batch, user, "Virtual Meter Assigned");

            log.info("Assign {} meters successfully", batch.size());
            return batch.size();

        } catch (Exception e) {
            log.error("Transaction failed during assign, rolling back batch of size {}: {}", batch.size(), e.getMessage());
            genericHandler.logIncidentReport("Bulk assign batch service failed");
            genericHandler.logAndSaveException(e, "Bulk assign batch meter");
            throw new RuntimeException("Batch virtual assign transaction failed. Rolled back.", e);
        }
    }

    public int assignSubBatchTransactional(List<Meter> batch, UserModel user, List<GenericResp> failedRecords,  List<MeterAssignLocation> locations, List<PaymentMode> paymentModes) {
        try {
            int successCount = 0;
            int subBatchSize = 100;

            for (int i = 0; i < batch.size(); i += subBatchSize) {
                int end = Math.min(i + subBatchSize, batch.size());
//                List<Meter> subBatch = batch.subList(i, end);
                List<Meter> subBatch = new ArrayList<>(batch.subList(i, end));
                try {
                    successCount += assignBatchTransactional(subBatch, user,  locations, paymentModes);
                } catch (Exception e) {
                    log.warn("Sub-batch assign failed (size={}): {}", subBatch.size(), e.getMessage());

                    if (subBatch.size() > 50) {
                        successCount += assignSinglesFallbackAsync(subBatch, user, failedRecords);
                    } else {
                        successCount += assignSinglesFallback(subBatch, user, failedRecords);
                    }
                }
            }

            return successCount;
        } catch (Exception e) {
            genericHandler.logIncidentReport("Bulk assign sub batch service failed");
            genericHandler.logAndSaveException(e, "Bulk assign sub batch meter");
            throw new RuntimeException("Sub Batch assign transaction failed. Rolled back.", e);
        }

    }

    public int assignVirtualSubBatchTransactional(List<Meter> batch, UserModel user, List<GenericResp> failedRecords,  List<MeterAssignLocation> locations) {
        try {
            int successCount = 0;
            int subBatchSize = 100;

            for (int i = 0; i < batch.size(); i += subBatchSize) {
                int end = Math.min(i + subBatchSize, batch.size());
                List<Meter> subBatch = batch.subList(i, end);

                try {
                    successCount += assignVirtualBatchTransactional(subBatch, user,  locations);
                } catch (Exception e) {
                    log.warn("Sub-batch allocation failed (size={}): {}", subBatch.size(), e.getMessage());

                    if (subBatch.size() > 50) {
                        successCount += assignVirtualSinglesFallbackAsync(subBatch, user, failedRecords);
                    } else {
                        successCount += assignVirtualSinglesFallback(subBatch, user, failedRecords);
                    }
                }
            }

            return successCount;
        } catch (Exception e) {
            genericHandler.logIncidentReport("Bulk virtual assign sub batch service failed");
            genericHandler.logAndSaveException(e, "Bulk virtual assign sub batch meter");
            throw new RuntimeException("Sub Batch allocation transaction failed. Rolled back.", e);
        }

    }

    public int assignSinglesFallbackAsync(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        for (Meter meter : batch) {
            futures.add(assignSingleAsync(meter, user, failedRecords));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream().mapToInt(CompletableFuture::join).sum();
    }

    public int assignVirtualSinglesFallbackAsync(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        for (Meter meter : batch) {
            futures.add(assignVirtualSingleAsync(meter, user, failedRecords));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream().mapToInt(CompletableFuture::join).sum();
    }


//    public int assignSinglesFallback(
//            List<Meter> batch,
//            UserModel user,
//            List<GenericResp> failedRecords,
//            List<MeterAssignLocation> locations,
//            List<PaymentMode> paymentModes
//    ) {
//
//        int THREAD_POOL_SIZE = 10; // tune based on server capacity
//        int TIMEOUT_SECONDS = 5;
//
//        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
//
//        List<Future<Boolean>> futures = new ArrayList<>();
//
//        for (Meter meter : batch) {
//
//            Future<Boolean> future = executor.submit(() -> {
//                try {
//                    log.debug("Fallback assign for meter: {}", meter.getMeterNumber());
//
//                    // IMPORTANT: avoid Mongo/logging inside this method if possible
//                    assignSingleTransactional(meter, user, locations, paymentModes);
//
//                    return true;
//
//                } catch (Exception e) {
//
//                    String reason = extractErrorMessage(e);
//
//                    GenericResp resp = new GenericResp();
//                    resp.setId(
//                            meter.getMeterId() != null
//                                    ? meter.getMeterId().toString()
//                                    : meter.getMeterNumber()
//                    );
//                    resp.setMessage("Meter assign failed: " + reason);
//                    resp.setData(meter.getMeterNumber());
//
//                    // Thread-safe add
//                    synchronized (failedRecords) {
//                        failedRecords.add(resp);
//                    }
//
//                    log.warn("Meter {} failed individually: {}", meter.getMeterNumber(), reason);
//
//                    return false;
//                }
//            });
//
//            futures.add(future);
//        }
//
//        int successCount = 0;
//
//        for (Future<Boolean> future : futures) {
//            try {
//                Boolean result = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
//                if (Boolean.TRUE.equals(result)) {
//                    successCount++;
//                }
//            } catch (TimeoutException e) {
//                future.cancel(true);
//                log.error("Meter assignment timed out");
//            } catch (Exception e) {
//                log.error("Unexpected error during fallback execution: {}", e.getMessage());
//            }
//        }
//
//        executor.shutdown();
//
//        try {
//            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
//                executor.shutdownNow();
//            }
//        } catch (InterruptedException e) {
//            executor.shutdownNow();
//            Thread.currentThread().interrupt();
//        }
//
//        log.info("Fallback completed: {} success, {} failed",
//                successCount, failedRecords.size());
//
//        return successCount;
//    }



    public int assignSinglesFallback(
            List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        int successCount = 0;

        for (Meter meter : batch) {
            try {
                log.debug("Fallback single assign for meter: {}", meter.getMeterNumber());
                assignSingleTransactional(meter, user);
                successCount++;
            } catch (Exception e) {
                String reason = extractErrorMessage(e);
                GenericResp resp = new GenericResp();
                resp.setId(meter.getMeterNumber());
                resp.setMessage("Meter assign failed: "+reason);
                resp.setData(meter.getMeterNumber());

                failedRecords.add(resp);

                log.warn("Meter {} failed individually: {}", meter.getMeterNumber(), reason);
            }
        }

        return successCount;
    }

    public int assignVirtualSinglesFallback(List<Meter> batch, UserModel user, List<GenericResp> failedRecords) {
        int successCount = 0;

        for (Meter meter : batch) {
            try {
                log.debug("Fallback single assign for meter: {}", meter.getMeterNumber());
                assignVirtualSingleTransactional(meter, user);
                successCount++;
            } catch (Exception e) {
                String reason = extractErrorMessage(e);
                GenericResp resp = new GenericResp();
                resp.setId(meter.getMeterId().toString());
                resp.setMessage("Virtual meter assign failed: "+reason);
                resp.setData(meter.getMeterNumber());

                failedRecords.add(resp);
//                failedRecords.add(String.format(
//                        "%s Assigned failed: %s",
//                        meter.getCin(),
//                        reason
//                ));
                log.warn("Meter {} failed individually: {}", meter.getCin(), reason);
            }
        }

        return successCount;
    }

    @Async
    public CompletableFuture<Integer> assignSingleAsync(Meter meter, UserModel user, List<GenericResp> failedRecords) {
        try {
            assignSingleTransactional(meter, user);
            return CompletableFuture.completedFuture(1);
        } catch (Exception e) {
            String reason = extractErrorMessage(e);

            GenericResp resp = new GenericResp();
            resp.setId(meter.getMeterId().toString());
            resp.setMessage("Meter assign failed: "+reason);
            resp.setData(meter.getMeterNumber());

            failedRecords.add(resp);
//            failedRecords.add(String.format(
//                    "%s [Cin: %s] (Assign failed: %s)",
//                    meter.getMeterNumber(),
//                    meter.getCin(),
//                    reason
//            ));
            log.warn("Async assign failed for meter {}: {}", meter.getMeterNumber(), reason);
            return CompletableFuture.completedFuture(0);
        }
    }

    @Async
    public CompletableFuture<Integer> assignVirtualSingleAsync(Meter meter, UserModel user, List<GenericResp> failedRecords) {
        try {
            assignVirtualSingleTransactional(meter, user);
            return CompletableFuture.completedFuture(1);
        } catch (Exception e) {
            String reason = extractErrorMessage(e);
            GenericResp resp = new GenericResp();
            resp.setId(meter.getMeterId().toString());
            resp.setMessage("Virtual meter assign failed: "+reason);
            resp.setData(meter.getMeterNumber());

            failedRecords.add(resp);
//            failedRecords.add(String.format(
//                    "%s Cin assign failed (%s)",
//                    meter.getCin(),
//                    reason
//            ));
            log.warn("Async assign failed for meter {}: {}", meter.getMeterNumber(), reason);
            return CompletableFuture.completedFuture(0);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void assignSingleTransactional(Meter meter, UserModel user) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

        meterMapper.updateMeter(meter.getMeterStage(), meter.getId(), meter.getUpdatedAt(), meter.getStatus());
        meter.setMeterId(meter.getId());
        meterMapper.assignMeterVersion(meter, meter.getNodeId(), meter.getId(), "Pending Assigned");

        meterMapper.assignVerMeterToLocation(meter.getMeterAssignLocation());

        meterMapper.assignPaymentModeVer(meter.getPaymentMode());

        //fetch meter from the database
        Meter m = meterMapper.getVersionMeter(user.getOrgId(), null, meter.getMeterNumber(), null);

        //save to audit (mongodb)
        AuditLog auditLog = buildAuditLog(user, "Pending Assigned", meterName, m, metadata, "");
        safeAuditService.saveAudit(auditLog);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void assignVirtualSingleTransactional(Meter meter, UserModel user) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

        // --- Licence Meter Limit Check ---
        checkLicenceMeterLimit(user.getOrgId(), 1);

        meterMapper.insertMeter(meter);
        meter.setMeterId(meter.getId());
        meterMapper.assignMeterVersion(meter, meter.getNodeId(), meter.getId(), "Pending Assigned");

        meterMapper.assignVerMeterToLocation(meter.getMeterAssignLocation());

        //fetch meter from the database
        Meter m = meterMapper.getVersionMeter(user.getOrgId(), null, meter.getMeterNumber(), null);

        //save to audit (mongodb)
        AuditLog auditLog = buildAuditLog(user, "Pending Assigned", meterName, m, metadata, "");
        safeAuditService.saveAudit(auditLog);
    }

    @Transactional
    @Override
    public Map<String, Object> meterInfoLookUp(String meterNumber) {

        try {

            // 1. RETURN DB DATA IMMEDIATELY
            MeterView meter = meterMapper.getMeterLookUp(meterNumber);

            if (meter == null) {
                return ResponseMap.response(status.getNotFoundCode(),"Meter not found",null);
            }
            meter.setAddress("");
//            Object o = meter.getLastVendingDate() == null ? meter.setLastVendingDate("000") : meter.getLastVendingDate();

            return ResponseMap.response(status.getSuccessCode(), meterName + " " + status.getRegDesc(), meter);

        } catch (Exception exception) {

            log.error("Meter lookup failed: {}", exception.getMessage(), exception);

            genericHandler.logIncidentReport("Meter lookup service failed");
            genericHandler.logAndSaveException(exception, "meter lookup");

            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> readMeterLookUp(String meterNumber,String readClock,String readCredit,String readRelayStatus) {

        String token = auth.getAccessToken();

        List<Map<String, Object>> data = new ArrayList<>();

        try {

            List<ObisMapping> obisList = new ArrayList<>();

            addIfValid(obisList, getFirstObis(meterNumber, readClock));
            addIfValid(obisList, getFirstObis(meterNumber, readCredit));
            addIfValid(obisList, getFirstObis(meterNumber, readRelayStatus));

            for (ObisMapping obis : obisList) {

                try {
                    Map<String, Object> response =
                            dlmsWriteOpsClient.get()
                                    .uri(uriBuilder -> uriBuilder
                                            .path("/obis")
                                            .queryParam("serial", meterNumber)
                                            .queryParam("obis", obis.getObisCodeCombined())
                                            .build())
                                    .headers(h -> h.setBearerAuth(token))
                                    .retrieve()
                                    .onStatus(HttpStatusCode::isError,
                                            clientResponse -> clientResponse.bodyToMono(String.class)
                                                    .map(body -> new RuntimeException(
                                                            "read meter service error: " + body))
                                    )
                                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                                    .block();


                    if (response != null) {

                        response.put("operationAction", obis.getOperationCode());
                        response.put("status", "SUCCESS");

                        if ("read_relay_status".equalsIgnoreCase(obis.getOperationCode())) {

                            Object valueObj = response.get("value");

                            if (valueObj instanceof Boolean) {

                                boolean value = (Boolean) valueObj;

                                response.put("relayStatus", value ? "Relay Closed" : "Relay Open");
                            }
                        }

                        data.add(response);
                    }

                } catch (Exception ex) {

                    Map<String, Object> error = new HashMap<>();
                    error.put("operationAction", obis.getOperationCode());
                    error.put("obisCode", obis.getObisCodeCombined());
                    error.put("status", "FAILED");
                    error.put("error", ex.getMessage());

                    data.add(error);
                }
            }

            return ResponseMap.response(
                    status.getSuccessCode(),
                    status.getRegDesc(),
                    data
            );

        } catch (Exception exception) {

            log.error("Read Meter lookup failed: {}", exception.getMessage(), exception);

            genericHandler.logIncidentReport("Read Meter lookup service failed");
            genericHandler.logAndSaveException(exception, "Read meter lookup");

            throw exception;
        }
    }

    private ObisMapping getFirstObis(String meterNumber, String operationCode) {
        List<ObisMapping> list =
                hesMapper.getObisByOperation(meterNumber, operationCode);

        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    private void addIfValid(List<ObisMapping> list, ObisMapping obis) {

        if (obis == null) {
            return;
        }

        if (obis.getObisCodeCombined() == null ||
                obis.getObisCodeCombined().isBlank()) {

            log.warn("Skipping invalid OBIS mapping: {}", obis.getOperationCode());
            return;
        }

        list.add(obis);
    }

    private void checkLicenceMeterLimit(UUID orgId, int newMeterCount) {
        Licence licence = LicenceFileUtil.readLicenceFile(dataDir, orgId);
        if (licence == null) return;
        if (licence.getMaxMeters() <= 0) return;

        int currentMeters = meterMapper.countMetersByOrgId(orgId);
        if (currentMeters + newMeterCount > licence.getMaxMeters()) {
            int remaining = Math.max(0, licence.getMaxMeters() - currentMeters);
            throw new GlobalExceptionHandler.NotFoundException(
                    "License limit reached. " + remaining + " of " + licence.getMaxMeters() + " meters remaining"
            );
        }
    }

    private List<AssignMeterToCustomer> processAssignExcel(InputStream inputStream) throws IOException {
        List<AssignMeterToCustomer> meters = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip header row safely
            if (rows.hasNext()) {
                rows.next();
            }

            while (rows.hasNext()) {
                Row row = rows.next();
                AssignMeterToCustomer meter = new AssignMeterToCustomer();

                meter.setMeterNumber(getStringCellValue(row.getCell(0)));
                meter.setCustomerId(getStringCellValue(row.getCell(1)));
                meter.setTariffName(getStringCellValue(row.getCell(2)));
                meter.setDssAssetId(getStringCellValue(row.getCell(3)));
                meter.setFeederAssetId(getStringCellValue(row.getCell(4)));
                meter.setCin(getStringCellValue(row.getCell(5)));
                meter.setState(getStringCellValue(row.getCell(6)));
                meter.setCity(getStringCellValue(row.getCell(7)));
                meter.setHouseNo(getStringCellValue(row.getCell(8)));
                meter.setStreetName(getStringCellValue(row.getCell(9)));
                meter.setDebitPaymentMode(getStringCellValue(row.getCell(10)));
                meter.setDebitPaymentPlan(getStringCellValue(row.getCell(11)));
                meter.setCreditPaymentMode(getStringCellValue(row.getCell(12)));
                meter.setCreditPaymentPlan(getStringCellValue(row.getCell(13)));

                meters.add(meter);
            }
        }
        return meters;
    }

    private List<AssignMeterToCustomer> processAssignCsv(InputStream inputStream) throws IOException {
        List<AssignMeterToCustomer> meters = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            for (CSVRecord record : csvParser) {
                AssignMeterToCustomer meter = new AssignMeterToCustomer();
                meter.setMeterNumber(record.get("meter number"));
                meter.setCustomerId(record.get("customer id"));
                meter.setTariffName(record.get("tariff name"));
                meter.setDssAssetId(record.get("dss asset id"));

                meter.setFeederAssetId(record.get("feeder asset id"));
                meter.setCin(record.get("cin"));
                meter.setState(record.get("state"));

                meter.setCity(record.get("city"));
                meter.setHouseNo(record.get("house number"));
                meter.setStreetName(record.get("street name"));
                meter.setDebitPaymentMode(record.get("debit payment mode"));
                meter.setDebitPaymentPlan(record.get("debit payment plan"));
                meter.setCreditPaymentMode(record.get("credit payment mode"));
                meter.setCreditPaymentPlan(record.get("credit payment plan"));

                meters.add(meter);
            }
        }
        return meters;
    }

    private List<AssignMeterToCustomer> processVirtualAssignExcel(InputStream inputStream) throws IOException {
        List<AssignMeterToCustomer> meters = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip header row safely
            if (rows.hasNext()) {
                rows.next();
            }

            while (rows.hasNext()) {
                Row row = rows.next();
                AssignMeterToCustomer meter = new AssignMeterToCustomer();

                meter.setCustomerId(getStringCellValue(row.getCell(0)));
                meter.setTariffName(getStringCellValue(row.getCell(1)));
                meter.setDssAssetId(getStringCellValue(row.getCell(2)));
                meter.setFeederAssetId(getStringCellValue(row.getCell(3)));
                meter.setCin(getStringCellValue(row.getCell(4)));
                meter.setMeterClass(getStringCellValue(row.getCell(5)));
                meter.setState(getStringCellValue(row.getCell(6)));
                meter.setCity(getStringCellValue(row.getCell(7)));
                meter.setHouseNo(getStringCellValue(row.getCell(8)));
                meter.setStreetName(getStringCellValue(row.getCell(9)));
                meter.setFixedEnergy(getStringCellValue(row.getCell(10)));

                meters.add(meter);
            }
        }
        return meters;
    }

    private List<AssignMeterToCustomer> processVirtualAssignCsv(InputStream inputStream) throws IOException {
        List<AssignMeterToCustomer> meters = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            for (CSVRecord record : csvParser) {
                AssignMeterToCustomer meter = new AssignMeterToCustomer();
                meter.setCustomerId(record.get("customer id"));
                meter.setTariffName(record.get("tariff name"));
                meter.setDssAssetId(record.get("dss asset id"));

                meter.setFeederAssetId(record.get("feeder asset id"));
                meter.setCin(record.get("cin"));
                meter.setMeterClass(record.get("meter class"));
                meter.setState(record.get("state"));

                meter.setCity(record.get("city"));
                meter.setHouseNo(record.get("house number"));
                meter.setStreetName(record.get("street name"));
                meter.setFixedEnergy(record.get("fixed energy"));

                meters.add(meter);
            }
        }
        return meters;
    }

    // Helper method to avoid NumberFormatException
    private static Long parseLongSafe(String value) {
        try {
            return (value == null || value.isEmpty()) ? null : Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String getStringCellValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }


    private String extractErrorMessage(Exception e) {
        String message = e.getMessage();

        if (message == null) return "Unknown error";

        if (message.contains("duplicate key value")) {
            return "Duplicate record — Meter already exists.";
        }
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

    private AuditLog buildAuditLog(UserModel creator, String description, String type, Meter createdEntity, Map<String, String> metadata, String reason) {
        AuditLog log = new AuditLog();
        log.setCreator(creator);
        log.setDescription(description);
        log.setType(type);
        log.setCreatedMeter(createdEntity);
        log.setReason(reason);
        log.setIpAddress(metadata.get("ipAddress"));
        log.setUserAgent(metadata.get("userAgent"));
        log.setEndpoint(metadata.get("endpoint"));
        log.setHttpMethod(metadata.get("httpMethod"));
        return log;
    }

    private void handleAddCache(Meter meter) {
        meterCache.remove(meter.getId().toString()+"_"+meter.getOrgId());
        for (String key : auditCache.keySet()) {
            if (key.startsWith("grid_flex_audit_log_page_")) {
                auditCache.remove(key);
            }
        }
        for (String key : meterCache.keySet()) {
            if (key.startsWith("meters_"+meter.getOrgId())) {
                meterCache.remove(key);
            }
        }
        meterCache.put(meter.getId().toString()+"_"+meter.getOrgId(), meter);  // Cache updated or deleted entity
    }

//    private String handleGetAccountNumber(){
//        String accountNumber;
//        accountNumber = String.valueOf(Instant.now().getEpochSecond());
//        return accountNumber;
//    }

    private String handleGetAccountNumber() {
        String accountNumber;
        long millis = System.currentTimeMillis(); // e.g. 1730667129123
        String base = String.valueOf(millis);

        // Take last 7 digits of current milliseconds + 3 random digits = 10 total
        int random = ThreadLocalRandom.current().nextInt(100, 999);
        accountNumber = base.substring(base.length() - 7) + random;

        return accountNumber;
    }


    private String handleGetVirtualMeter(){
        String virtualMeterNo;
        long timePart = Instant.now().toEpochMilli(); // 13 digits
        int randomPart = new Random().nextInt(90) + 10; // 2-digit number
        virtualMeterNo = "V" + String.valueOf(timePart).substring(0, 11) + randomPart;
        return virtualMeterNo;
    }

    private String buildChangeDescription(Meter oldMeter, Meter newMeter) {
        StringBuilder changes = new StringBuilder("Edited meter ");

        if (!Objects.equals(oldMeter.getMeterNumber(), newMeter.getMeterNumber())) {
            changes.append(String.format("number: '%s' → '%s' ", oldMeter.getMeterNumber(), newMeter.getMeterNumber()));
        }

        if (!Objects.equals(oldMeter.getSimNumber(), newMeter.getSimNumber())) {
            changes.append(String.format("sim number: '%s' → '%s' ", oldMeter.getSimNumber(), newMeter.getSimNumber()));
        }

        if (!Objects.equals(oldMeter.getMeterCategory(), newMeter.getMeterCategory())) {
            changes.append(String.format("category: '%s' → '%s' ", oldMeter.getMeterCategory(), newMeter.getMeterCategory()));
        }

        if (!Objects.equals(oldMeter.getMeterClass(), newMeter.getMeterClass())) {
            changes.append(String.format("class: '%s' → '%s' ", oldMeter.getMeterClass(), newMeter.getMeterClass()));
        }

        if (!Objects.equals(oldMeter.getMeterType(), newMeter.getMeterType())) {
            changes.append(String.format("type: '%s' → '%s' ", oldMeter.getMeterType(), newMeter.getMeterType()));
        }

        if (!Objects.equals(oldMeter.getOldSgc(), newMeter.getOldSgc())) {
            changes.append(String.format("old sgc: '%s' → '%s' ", oldMeter.getOldSgc(), newMeter.getOldSgc()));
        }

        if (!Objects.equals(oldMeter.getNewSgc(), newMeter.getNewSgc())) {
            changes.append(String.format("new sgc: '%s' → '%s' ", oldMeter.getNewSgc(), newMeter.getNewSgc()));
        }

        if (!Objects.equals(oldMeter.getOldKrn(), newMeter.getOldKrn())) {
            changes.append(String.format("old krn: '%s' → '%s' ", oldMeter.getOldKrn(), newMeter.getOldKrn()));
        }

        if (!Objects.equals(oldMeter.getNewKrn(), newMeter.getNewKrn())) {
            changes.append(String.format("new krn: '%s' → '%s' ", oldMeter.getNewKrn(), newMeter.getNewKrn()));
        }

        if (!Objects.equals(oldMeter.getOldTariffIndex(), newMeter.getOldTariffIndex())) {
            changes.append(String.format("old tariff index: '%s' → '%s' ", oldMeter.getOldTariffIndex(), newMeter.getOldTariffIndex()));
        }

        if (!Objects.equals(oldMeter.getNewTariffIndex(), newMeter.getNewTariffIndex())) {
            changes.append(String.format("new tariff index: '%s' → '%s' ", oldMeter.getNewTariffIndex(), newMeter.getNewTariffIndex()));
        }
        return changes.toString();
    }

    private String buildMDMeterInfoChangeDescription(MDMeterInfo oldMeter, MDMeterInfo newMeter) {
        StringBuilder changes = new StringBuilder("Edited MD meter ");

        if (!Objects.equals(oldMeter.getCtRatioNum(), newMeter.getCtRatioNum())) {
            changes.append(String.format("ct ratio num: '%s' → '%s' ", oldMeter.getCtRatioNum(), newMeter.getCtRatioNum()));
        }

        if (!Objects.equals(oldMeter.getCtRatioDenom(), newMeter.getCtRatioDenom())) {
            changes.append(String.format("ct ratio denom: '%s' → '%s' ", oldMeter.getCtRatioDenom(), newMeter.getCtRatioDenom()));
        }

        if (!Objects.equals(oldMeter.getVoltRatioNum(), newMeter.getVoltRatioNum())) {
            changes.append(String.format("volt ratio num: '%s' → '%s' ", oldMeter.getVoltRatioNum(), newMeter.getVoltRatioNum()));
        }

        if (!Objects.equals(oldMeter.getVoltRatioDenom(), newMeter.getVoltRatioDenom())) {
            changes.append(String.format("volt ratio denom: '%s' → '%s' ", oldMeter.getVoltRatioDenom(), newMeter.getVoltRatioDenom()));
        }

        if (!Objects.equals(oldMeter.getMultiplier(), newMeter.getMultiplier())) {
            changes.append(String.format("multiplier: '%s' → '%s' ", oldMeter.getMultiplier(), newMeter.getMultiplier()));
        }

        if (!Objects.equals(oldMeter.getMeterRating(), newMeter.getMeterRating())) {
            changes.append(String.format("reading: '%s' → '%s' ", oldMeter.getMeterRating(), newMeter.getMeterRating()));
        }

        if (!Objects.equals(oldMeter.getInitialReading(), newMeter.getInitialReading())) {
            changes.append(String.format("initial reading: '%s' → '%s' ", oldMeter.getInitialReading(), newMeter.getInitialReading()));
        }

        if (!Objects.equals(oldMeter.getDial(), newMeter.getDial())) {
            changes.append(String.format("dial: '%s' → '%s' ", oldMeter.getDial(), newMeter.getDial()));
        }

        if (!Objects.equals(oldMeter.getLatitude(), newMeter.getLatitude())) {
            changes.append(String.format("latitude: '%s' → '%s' ", oldMeter.getLatitude(), newMeter.getLatitude()));
        }

        if (!Objects.equals(oldMeter.getLongitude(), newMeter.getLongitude())) {
            changes.append(String.format("longitude: '%s' → '%s' ", oldMeter.getLongitude(), newMeter.getLongitude()));
        }

        return changes.toString();
    }

    private String buildSmartMeterInfoChangeDescription(SmartMeterInfo smartMeter, SmartMeterInfo newMeter) {
        StringBuilder changes = new StringBuilder("Edited smart meter ");

        if (!Objects.equals(smartMeter.getMeterModel(), newMeter.getMeterModel())) {
            changes.append(String.format("model: '%s' → '%s' ", smartMeter.getMeterModel(), newMeter.getMeterModel()));
        }

        if (!Objects.equals(smartMeter.getAuthentication(), newMeter.getAuthentication())) {
            changes.append(String.format("authentication: '%s' → '%s' ", smartMeter.getAuthentication(), newMeter.getAuthentication()));
        }

        if (!Objects.equals(smartMeter.getPassword(), newMeter.getPassword())) {
            changes.append(String.format("password: '%s' → '%s' ", smartMeter.getPassword(), newMeter.getPassword()));
        }

        if (!Objects.equals(smartMeter.getProtocol(), newMeter.getProtocol())) {
            changes.append(String.format("protocol: '%s' → '%s' ", smartMeter.getProtocol(), newMeter.getProtocol()));
        }

        return changes.toString();
    }
}
