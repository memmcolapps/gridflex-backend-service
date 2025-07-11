package org.memmcol.gridflexbackendservice.service.organization;

import org.memmcol.gridflexbackendservice.model.user.Organization;


import java.util.Map;
import java.util.UUID;

public interface OrganizationService {
    Map<String, Object> addOrganization(Organization organization);
    Map<String, Object> creatRootNode(UUID organizationId, String name);
    Map<String, Object> createDefaultUser(UUID organizationId, UUID nodeId);
    Map<String, Object> createDefaultPermission(UUID organizationId);
    Map<String, Object> createDefaultGroup(UUID organizationId);
    Map<String, Object> createDefaultGroupPermission(UUID organizationId);
    Map<String, Object> getOrganization(int page,int size);
    Map<String, Object> getOrganizationById(UUID id);
    Map<String, Object> updateOrganization(Organization organization, UUID orgId);
}
