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
import org.memmcol.gridflexbackendservice.util.LicenceSignerUtil;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Value("${licence.encryption.key}")
    private String encryptionKey;

    @Value("${licence.hmac.key}")
    private String hmacKey;

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
                        "Licence not found",
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
                    throw new RuntimeException("Failed to serialize licence for signature verification", e);
                }
                licence.setHmacSignature(savedSignature);
                boolean signatureValid = LicenceSignerUtil.verify(licenceJson, savedSignature, hmacKey);
                if (!signatureValid) {
                    return ResponseMap.response(
                            status.getFailCode(),
                            "Licence has been tampered with",
                            null
                    );
                }
            }

            int currentMeters = meterMapper.countMetersByOrgId(organisationId);
            LicenceValidationResult validationResult = LicenceValidator.validateWithLimits(licence, currentMeters);

            if (validationResult.isValid()) {
                boolean boundToServer = hardwareBindingService.isLicenceBoundToCurrentServer(licence);
                if (!boundToServer) {
                    validationResult = LicenceValidationResult.builder()
                            .valid(false)
                            .message("Licence is not bound to this server")
                            .licence(licence)
                            .build();
                }
            }

            AuditLog auditLog = buildAuditLog("licence validation", "licence", validationResult, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(
                    validationResult.isValid() ? status.getSuccessCode() : status.getFailCode(),
                    validationResult.getMessage(),
                    validationResult
            );

        } catch (Exception ex) {
            genericHandler.logIncidentReport("Licence validation failed");
            genericHandler.logAndSaveException(ex, "validating licence");
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
                        "Licence not found",
                        null
                );
            }

            AuditLog auditLog = buildAuditLog("licence retrieved", "licence", licence, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Licence retrieved successfully",
                    licence
            );

        } catch (Exception ex) {
            genericHandler.logIncidentReport("Licence retrieval failed");
            genericHandler.logAndSaveException(ex, "retrieving licence");
            throw ex;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> deactivateLicence(UUID organisationId) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

        try {
            Licence licence = readLicenceFile(organisationId);

            if (licence == null) {
                throw new GlobalExceptionHandler.NotFoundException("Licence not found");
            }

            licence.setActive(false);
            writeLicenceFile(licence);

            AuditLog auditLog = buildAuditLog("licence deactivated", "licence", licence, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Licence deactivated successfully",
                    licence
            );

        } catch (Exception ex) {
            genericHandler.logIncidentReport("Licence deactivation failed");
            genericHandler.logAndSaveException(ex, "deactivating licence");
            throw ex;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> generateFingerprint(UUID organisationId) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

        try {
            String encryptedFingerprint = hardwareBindingService.generateAndSaveFingerprintFile(organisationId);

            AuditLog auditLog = buildAuditLog("fingerprint generated", "licence", organisationId, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Fingerprint generated and saved successfully",
                    encryptedFingerprint
            );

        } catch (Exception ex) {
            genericHandler.logIncidentReport("Fingerprint generation failed");
            genericHandler.logAndSaveException(ex, "generating fingerprint");
            throw ex;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> getFingerprint(UUID organisationId) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

        try {
            String fingerprintContent = hardwareBindingService.readFingerprintFile(organisationId);

            if (fingerprintContent == null) {
                return ResponseMap.response(
                        status.getNotFoundCode(),
                        "Fingerprint not found. Generate fingerprint first.",
                        null
                );
            }

            AuditLog auditLog = buildAuditLog("fingerprint retrieved", "licence", organisationId, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Fingerprint retrieved successfully",
                    fingerprintContent
            );

        } catch (Exception ex) {
            genericHandler.logIncidentReport("Fingerprint retrieval failed");
            genericHandler.logAndSaveException(ex, "retrieving fingerprint");
            throw ex;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> uploadLicence(UUID organisationId, String licenceContent) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

        try {
            Path licencePath = Paths.get(getLicenceDir(), organisationId + ".lic");
            Files.createDirectories(licencePath.getParent());
            Files.write(licencePath, licenceContent.getBytes());

            AuditLog auditLog = buildAuditLog("licence uploaded", "licence", organisationId, metadata);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Licence uploaded successfully",
                    null
            );

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload licence file", e);
        }
    }

    private Licence readLicenceFile(UUID organisationId) {
        Path licencePath = Paths.get(getLicenceDir(), organisationId + ".lic");
        File licenceFile = licencePath.toFile();

        if (!licenceFile.exists()) {
            return null;
        }

        try {
            String encryptedContent = new String(Files.readAllBytes(licencePath));
            String decryptedContent = LicenceEncryptionUtil.decrypt(encryptedContent, encryptionKey);
            return objectMapper.readValue(decryptedContent, Licence.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read licence file", e);
        }
    }

    private void writeLicenceFile(Licence licence) {
        try {
            Path licencePath = Paths.get(getLicenceDir(), licence.getOrganisationId() + ".lic");
            Files.createDirectories(licencePath.getParent());

            String jsonContent = objectMapper.writeValueAsString(licence);
            String encryptedContent = LicenceEncryptionUtil.encrypt(jsonContent, encryptionKey);
            Files.write(licencePath, encryptedContent.getBytes());

        } catch (IOException e) {
            throw new RuntimeException("Failed to write licence file", e);
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
