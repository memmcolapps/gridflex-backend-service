package org.memmcol.gridflexbackendservice.service.organization;

import org.memmcol.gridflexbackendservice.model.user.Organization;


import java.util.Map;
import java.util.UUID;

public interface OrganizationService {
    Map<String, Object> getOrganizationById(UUID id);
    Map<String, Object> updateOrganization(Organization organization);
}
