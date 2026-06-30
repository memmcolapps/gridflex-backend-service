package org.memmcol.gridflexbackendservice.service.licence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.memmcol.gridflexbackendservice.exception.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.mapper.MeterMapper;
import org.memmcol.gridflexbackendservice.model.licence.Licence;
import org.memmcol.gridflexbackendservice.model.licence.LicenceValidationResult;
import org.memmcol.gridflexbackendservice.service.audit.SafeAuditService;
import org.memmcol.gridflexbackendservice.util.LicenceEncryptionUtil;
import org.memmcol.gridflexbackendservice.util.LicenceFileUtil;
import org.memmcol.gridflexbackendservice.util.LicenceSecurityConstants;
import org.memmcol.gridflexbackendservice.util.LicenceSignerUtil;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class LicenceServiceImpl implements LicenceService {

//    private static final String LICENCE_DIR = "./licenses";

    @Value("${gridflex.data.dir}")
    private String dataDir;

    private String getLicenceDir() {
        return dataDir + "/licenses";
    }

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private GenericHandler genericHandler;

    @Autowired
    private ResponseProperties status;

    @Autowired
    private SafeAuditService safeAuditService;

    @Autowired
    private HardwareBindingService hardwareBindingService;

    @Autowired
    private MeterMapper meterMapper;

//    @Value("${licence.encryption.key}")
//    private String encryptionKey;

//    @Value("${licence.hmac.key}")
//    private String hmacKey;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Transactional
    @Override
    public Map<String, Object> validateLicence(UUID organisationId) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

        try {
            Licence licence = readLicenceFile(organisationId);
            if (licence == null) {
                return ResponseMap.response(
                        status.getNotFoundCode(),
                        "License not found",
                        null
                );
            }

            if (licence.getHmacSignature() != null) {
                String savedSignature = licence.getHmacSignature();
                licence.setHmacSignature(null);
                String licenceJson;
                try {
                    licenceJson = objectMapper.writeValueAsString(licence);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to serialize license for signature verification", e);
                }
                licence.setHmacSignature(savedSignature);
                boolean signatureValid = LicenceSignerUtil.verify(licenceJson, savedSignature, LicenceSecurityConstants.getHmacKey());
                if (!signatureValid) {
                    return ResponseMap.response(
                            status.getFailCode(),
                            "License has been tampered with",
                            null
                    );
                }
            }

            int currentMeters = meterMapper.countMetersByOrgId(organisationId);
            LicenceValidationResult validationResult = LicenceValidator.validateWithLimits(licence, currentMeters);

            if (!validationResult.isValid()
                    && validationResult.getMessage() != null
                    && validationResult.getMessage().startsWith("License has expired")) {
                licence.setActive(false);
                writeLicenceFile(licence);
            }

            if (validationResult.isValid()) {
                boolean boundToServer = hardwareBindingService.isLicenceBoundToCurrentServer(licence);
                if (!boundToServer) {
                    validationResult = LicenceValidationResult.builder()
                            .valid(false)
                            .message("License is not bound to this server")
                            .licence(licence)
                            .build();
                }
            }

            AuditLog auditLog = buildAuditLog("license validation", "license", validationResult, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(
                    validationResult.isValid() ? status.getSuccessCode() : status.getFailCode(),
                    validationResult.getMessage(),
                    validationResult
            );

        } catch (Exception ex) {
            genericHandler.logIncidentReport("License validation failed");
            genericHandler.logAndSaveException(ex, "validating license");
            throw ex;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> getLicence(UUID organisationId) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

        try {
            Licence licence = readLicenceFile(organisationId);

            if (licence == null) {
                return ResponseMap.response(
                        status.getNotFoundCode(),
                        "License not found",
                        null
                );
            }

            AuditLog auditLog = buildAuditLog("license retrieved", "license", licence, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "License retrieved successfully",
                    licence
            );

        } catch (Exception ex) {
            genericHandler.logIncidentReport("License retrieval failed");
            genericHandler.logAndSaveException(ex, "retrieving license");
            throw ex;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> generateFingerprint(UUID organisationId) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

        try {
            Map<String, String> fingerprintData = hardwareBindingService.generateAndSaveFingerprintFile(organisationId);

            AuditLog auditLog = buildAuditLog("fingerprint generated", "license", organisationId, metadata);
            safeAuditService.saveAudit(auditLog);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("machineId", fingerprintData.get("hash"));
            responseData.put("encryptedFingerprint", fingerprintData.get("encryptedContent"));

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Fingerprint generated and saved successfully",
                    responseData
            );

        } catch (Exception ex) {
            genericHandler.logIncidentReport("Fingerprint generation failed");
            genericHandler.logAndSaveException(ex, "generating fingerprint");
            throw ex;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> saveLicense(UUID orgId, String encryptedLicence) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

        try {
            if (orgId == null) {
                throw new GlobalExceptionHandler.NotFoundException("Organisation ID is required");
            }

            if (encryptedLicence == null || encryptedLicence.isBlank()) {
                throw new GlobalExceptionHandler.NotFoundException("Encrypted license content is required");
            }

            try {
                Path licencePath = Paths.get(getLicenceDir(), orgId + ".lic");
                Files.createDirectories(licencePath.getParent());
                Files.write(licencePath, encryptedLicence.getBytes());
            } catch (IOException e) {
                throw new RuntimeException("Failed to write license file", e);
            }

            AuditLog auditLog = buildAuditLog("license saved", "license", orgId, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "License saved successfully",
                    null
            );

        } catch (GlobalExceptionHandler.NotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            genericHandler.logIncidentReport("License save failed");
            genericHandler.logAndSaveException(ex, "saving license");
            throw ex;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> getFingerprint(UUID organisationId) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

        try {
//            String fingerprintContent
            Map<String, String> fingerprintContent = hardwareBindingService.readFingerprintFile(organisationId);

            if (fingerprintContent == null) {
                return ResponseMap.response(
                        status.getNotFoundCode(),
                        "Fingerprint not found. Generate fingerprint first.",
                        null
                );
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("machineId", fingerprintContent.get("hash"));
            responseData.put("encryptedFingerprint", fingerprintContent.get("encryptedContent"));

            AuditLog auditLog = buildAuditLog("fingerprint retrieved", "license", organisationId, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Fingerprint retrieved successfully",
                    responseData
            );

        } catch (Exception ex) {
            genericHandler.logIncidentReport("Fingerprint retrieval failed");
            genericHandler.logAndSaveException(ex, "retrieving fingerprint");
            throw ex;
        }
    }

    private Licence readLicenceFile(UUID organisationId) {
        return LicenceFileUtil.readLicenceFile(dataDir, organisationId);
    }

    private void writeLicenceFile(Licence licence) {
        try {
            Path licencePath = Paths.get(getLicenceDir(), licence.getOrganisationId() + ".lic");
            Files.createDirectories(licencePath.getParent());

            String jsonContent = objectMapper.writeValueAsString(licence);
            String encryptedContent = LicenceEncryptionUtil.encrypt(jsonContent, LicenceSecurityConstants.getEncryptionKey());
            Files.write(licencePath, encryptedContent.getBytes());

        } catch (IOException e) {
            throw new RuntimeException("Failed to write license file", e);
        }
    }

    private AuditLog buildAuditLog(String description, String type, Object data, Map<String, String> metadata) {
        AuditLog auditLog = new AuditLog();
        auditLog.setDescription(description);
        auditLog.setType(type);
        auditLog.setIpAddress(metadata.get("ipAddress"));
        auditLog.setUserAgent(metadata.get("userAgent"));
        auditLog.setEndpoint(metadata.get("endpoint"));
        auditLog.setHttpMethod(metadata.get("httpMethod"));
        return auditLog;
    }
}
