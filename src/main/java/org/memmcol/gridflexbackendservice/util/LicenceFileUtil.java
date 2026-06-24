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
        String licenceDir = dataDir + "/licenses";
        Path licencePath = Paths.get(licenceDir, organisationId + ".lic");
        File licenceFile = licencePath.toFile();

        if (!licenceFile.exists()) {
            return null;
        }

        try {
            String encryptedContent = new String(Files.readAllBytes(licencePath));
            String decryptedContent = LicenceEncryptionUtil.decrypt(encryptedContent, LicenceSecurityConstants.getEncryptionKey());
            return objectMapper.readValue(decryptedContent, Licence.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read licence file", e);
        }
    }
}
