package org.memmcol.gridflexbackendservice.thirdPartyService.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.RequiredArgsConstructor;
import org.memmcol.gridflexbackendservice.thirdPartyService.model.ApiClient;
import org.memmcol.gridflexbackendservice.thirdPartyService.model.ApiClientScope;
import org.memmcol.gridflexbackendservice.thirdPartyService.repository.ApiClientRepository;
import org.memmcol.gridflexbackendservice.thirdPartyService.repository.ApiClientScopeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ThirdPartyAuthServiceImpl {

    private final ApiClientRepository apiClientRepository;
    private final ApiClientScopeRepository scopeRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${odyssey.jwt-secret}")
    private String secret;

    @Transactional
    public String authenticate(String clientId, String clientSecret) {

        System.out.println("secrete1: "+secret);

        ApiClient client = apiClientRepository.findByClientId(clientId)
                .orElseThrow(() -> new RuntimeException("Invalid client"));

        if (!client.getStatus()) {
            throw new RuntimeException("Client disabled");
        }

        if (!passwordEncoder.matches(clientSecret, client.getClientSecretHash())) {
            throw new RuntimeException("Invalid secret");
        }

        List<String> scopes = scopeRepository.findByClientId(clientId)
                .stream()
                .map(ApiClientScope::getScope)
                .toList();

        return JWT.create()
                .withSubject(clientId)
                .withClaim("userId", client.getId().toString())
                .withClaim("orgId", client.getOrgId() != null ? client.getOrgId().toString() : null)
                .withArrayClaim("scopes", scopes.toArray(new String[0]))
                .sign(Algorithm.HMAC256(secret));
    }
}
