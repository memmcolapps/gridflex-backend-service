package org.memmcol.gridflexbackendservice.service.licence;

import org.memmcol.gridflexbackendservice.model.licence.Licence;

import java.util.Map;
import java.util.UUID;

public interface LicenceService {
    Map<String, Object> validateLicence(UUID organisationId);

    Map<String, Object> getLicence(UUID organisationId);

    Map<String, Object> generateFingerprint(UUID organisationId);

    Map<String, Object> saveLicense(UUID orgId, String encryptedLicence);

    Map<String, Object> getFingerprint(UUID organisationId);

}
