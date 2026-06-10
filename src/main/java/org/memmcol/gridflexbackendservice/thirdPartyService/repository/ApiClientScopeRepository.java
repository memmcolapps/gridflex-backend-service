package org.memmcol.gridflexbackendservice.thirdPartyService.repository;

import org.memmcol.gridflexbackendservice.thirdPartyService.model.ApiClientScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApiClientScopeRepository extends JpaRepository<ApiClientScope, UUID> {
    List<ApiClientScope> findByClientId(String clientId);
}
