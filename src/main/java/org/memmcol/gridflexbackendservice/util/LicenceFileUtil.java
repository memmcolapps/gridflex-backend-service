package org.memmcol.gridflexbackendservice.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.memmcol.gridflexbackendservice.model.licence.Licence;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class LicenceFileUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private LicenceFileUtil() {
    }

    public static Licence readLicenceFile(String dataDir, UUID organisationId) {
        String licenseDir = dataDir + "/licenses";
        Path licensePath = Paths.get(licenseDir, organisationId + ".lic");
        File licenseFile = licensePath.toFile();

        if (!licenseFile.exists()) {
            return null;
        }

        try {
            String encryptedContent = new String(Files.readAllBytes(licensePath));
            String decryptedContent = LicenceEncryptionUtil.decrypt(encryptedContent, LicenceSecurityConstants.getEncryptionKey());
            return objectMapper.readValue(decryptedContent, Licence.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read license file", e);
        }
    }
}
