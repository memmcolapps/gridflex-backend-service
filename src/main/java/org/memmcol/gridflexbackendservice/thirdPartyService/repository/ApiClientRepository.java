package org.memmcol.gridflexbackendservice.thirdPartyService.repository;

import org.memmcol.gridflexbackendservice.thirdPartyService.model.ApiClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApiClientRepository extends JpaRepository<ApiClient, UUID> {
    Optional<ApiClient> findByClientId(String clientId);

    Optional<ApiClient> findByClientNameAndOrgId(String clientName, UUID orgId);
}
