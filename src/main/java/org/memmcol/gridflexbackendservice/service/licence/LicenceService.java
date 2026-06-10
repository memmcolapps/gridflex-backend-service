package org.memmcol.gridflexbackendservice.service.licence;

import java.util.Map;
import java.util.UUID;

public interface LicenceService {
    Map<String, Object> validateLicence(UUID organisationId);

    Map<String, Object> getLicence(UUID organisationId);

    Map<String, Object> deactivateLicence(UUID organisationId);

    Map<String, Object> generateFingerprint(UUID organisationId);

    Map<String, Object> getFingerprint(UUID organisationId);

    Map<String, Object> uploadLicence(UUID organisationId, String licenceContent);
}
