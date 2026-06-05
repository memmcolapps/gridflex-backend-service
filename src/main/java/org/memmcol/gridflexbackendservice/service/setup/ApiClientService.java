package org.memmcol.gridflexbackendservice.service.setup;

import lombok.RequiredArgsConstructor;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.memmcol.gridflexbackendservice.model.setup.ApiClientResponse;
import org.memmcol.gridflexbackendservice.model.setup.CreateApiClientRequest;
import org.memmcol.gridflexbackendservice.thirdPartyService.model.ApiClient;
import org.memmcol.gridflexbackendservice.thirdPartyService.model.ApiClientScope;
import org.memmcol.gridflexbackendservice.thirdPartyService.repository.ApiClientRepository;
import org.memmcol.gridflexbackendservice.thirdPartyService.repository.ApiClientScopeRepository;
import org.memmcol.gridflexbackendservice.exception.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiClientService {

    private final ApiClientRepository apiClientRepository;
    private final ApiClientScopeRepository scopeRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResponseProperties status;

    @Transactional
    public Map<String, Object> createClient(CreateApiClientRequest request) {

        Optional<ApiClient> existing = apiClientRepository
                .findByClientNameAndOrgId(request.getClientName(), request.getOrgId());

        if (existing.isPresent()) {
            ApiClient client = existing.get();

            throw new GlobalExceptionHandler.NotFoundException("Client already exist - clientId: " + client.getClientId());

            // You already created it before → do NOT regenerate secret
//            return ApiClientResponse.builder()
//                    .clientId(client.getClientId())
//                    .message("Client already exist")
//                    .build();
        }

        UUID clientUuid = UUID.randomUUID();

        String clientId = request.getClientName()+"_" + UUID.randomUUID().toString().substring(0, 8);

        String rawSecret = UUID.randomUUID().toString();

        String hashedSecret = passwordEncoder.encode(rawSecret);

        ApiClient client = new ApiClient();
        client.setId(clientUuid);
        client.setClientId(clientId);
        client.setClientName(request.getClientName());
        client.setClientSecretHash(hashedSecret);
        client.setOrgId(request.getOrgId());
        client.setStatus(true);
        client.setCreatedAt(LocalDateTime.now());
        client.setUpdatedAt(LocalDateTime.now());

        apiClientRepository.save(client);

        // Save scopes
        if (request.getScopes() != null) {
            for (String scope : request.getScopes()) {
                ApiClientScope s = new ApiClientScope();
                s.setId(UUID.randomUUID());
                s.setClientId(clientId);
                s.setScope(scope);
                scopeRepository.save(s);
            }
        }
        ApiClientResponse resp = new ApiClientResponse(clientId, rawSecret, null);

        return ResponseMap.response(status.getSuccessCode(), "Created successfully", resp);
    }
}
