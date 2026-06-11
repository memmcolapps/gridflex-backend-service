package org.memmcol.gridflexbackendservice.service.licence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.memmcol.gridflexbackendservice.model.licence.HardwareFingerprint;
import org.memmcol.gridflexbackendservice.model.licence.Licence;
import org.memmcol.gridflexbackendservice.util.HardwareFingerprintUtil;
import org.memmcol.gridflexbackendservice.util.LicenceEncryptionUtil;
import org.memmcol.gridflexbackendservice.util.LicenceSecurityConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class HardwareBindingService {

    @Value("${gridflex.data.dir}")
    private String dataDir;

    private String getFingerprintDir() {
        return dataDir + "/fingerprints";
    }

//    @Value("${licence.encryption.key}")
//    private String encryptionKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public HardwareFingerprint generateFingerprint() {
        return HardwareFingerprintUtil.generateFingerprint();
    }

    public boolean verifyFingerprint(String storedHash) {
        return HardwareFingerprintUtil.verifyFingerprint(storedHash);
    }

    public Licence bindLicence(Licence licence) {
        HardwareFingerprint fingerprint = generateFingerprint();
        licence.setHardwareFingerprint(fingerprint.getHash());
        return licence;
    }

    public boolean isLicenceBoundToCurrentServer(Licence licence) {
        if (licence.getHardwareFingerprint() == null || licence.getHardwareFingerprint().isEmpty()) {
            return false;
        }
        return verifyFingerprint(licence.getHardwareFingerprint());
    }

    public String generateAndSaveFingerprintFile(UUID organisationId) {
        try {
            HardwareFingerprint fingerprint = generateFingerprint();

            String jsonContent = objectMapper.writeValueAsString(fingerprint);
            String encryptedContent = LicenceEncryptionUtil.encrypt(jsonContent, LicenceSecurityConstants.getEncryptionKey());

            Path fingerprintPath = Paths.get(getFingerprintDir(), organisationId + ".txt");
            Files.createDirectories(fingerprintPath.getParent());
            Files.write(fingerprintPath, encryptedContent.getBytes());

            return encryptedContent;
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate fingerprint file", e);
        }
    }

    public String readFingerprintFile(UUID organisationId) {
        try {
            Path fingerprintPath = Paths.get(getFingerprintDir(), organisationId + ".txt");
            if (!Files.exists(fingerprintPath)) {
                return null;
            }
            return new String(Files.readAllBytes(fingerprintPath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read fingerprint file", e);
        }
    }

    public HardwareFingerprint decryptFingerprintFile(String encryptedContent) {
        try {
            String decryptedContent = LicenceEncryptionUtil.decrypt(encryptedContent, LicenceSecurityConstants.getEncryptionKey());
            return objectMapper.readValue(decryptedContent, HardwareFingerprint.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt fingerprint file", e);
        }
    }
}
