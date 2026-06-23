package org.memmcol.gridflexbackendservice.thirdPartyService.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.thirdPartyService.model.ApiClient;
import org.memmcol.gridflexbackendservice.thirdPartyService.model.ApiClientScope;
import org.memmcol.gridflexbackendservice.thirdPartyService.model.ClientLoginModel;
import org.memmcol.gridflexbackendservice.thirdPartyService.repository.ApiClientRepository;
import org.memmcol.gridflexbackendservice.thirdPartyService.repository.ApiClientScopeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ThirdPartyAuthServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(ThirdPartyAuthServiceImpl.class);

    private final HttpServletRequest httpServletRequest;
    private final ApiClientRepository apiClientRepository;
    private final ApiClientScopeRepository scopeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditRepository auditRepository;
    private final GenericHandler genericHandler;

    @Value("${odyssey.jwt-secret}")
    private String secret;

    @Transactional
    public String authenticate(ClientLoginModel request) {

        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

            System.out.println("metadata");
            ApiClient client = apiClientRepository.findByClientId(request.getClientId().toLowerCase())
                    .orElseThrow(() -> new RuntimeException("Client not found"));

            if (!client.getStatus()) {
                throw new RuntimeException("Client deactivated");
            }

            if (!passwordEncoder.matches(request.getClientSecret(), client.getClientSecretHash())) {
                throw new RuntimeException("Invalid secret");
            }

            List<String> scopes = scopeRepository.findByClientId(request.getClientId())
                    .stream()
                    .map(ApiClientScope::getScope)
                    .toList();

            AuditLog auditLog = buildAuditLog(request.getClientId(), "Authenticated", "Client", metadata);
            try {
                auditRepository.save(auditLog);
            } catch (Exception ex) {
                log.error("Failed to save audit log", ex);
            }

            return JWT.create()
                    .withSubject(request.getClientId())
                    .withClaim("userId", client.getId().toString())
                    .withClaim("status", client.getStatus())
                    .withClaim("orgId", client.getOrgId() != null ? client.getOrgId().toString() : null)
                    .withArrayClaim("scopes", scopes.toArray(new String[0]))
//                    .withIssuedAt(new Date())
//                    .withExpiresAt(new Date(System.currentTimeMillis() + 86_400_000L)) // 24 hours
                    .sign(Algorithm.HMAC256(secret));

        } catch (Exception exception) {
            genericHandler.logAndSaveException(exception, "third party authentication");
            throw exception;
        }
    }

    private AuditLog buildAuditLog(String creator, String description, String type, Map<String, String> metadata) {
        AuditLog log = new AuditLog();
        log.setUserClient(creator);
        log.setDescription(description);
        log.setType(type);
        log.setIpAddress(metadata.get("ipAddress"));
        log.setUserAgent(metadata.get("userAgent"));
        log.setEndpoint(metadata.get("endpoint"));
        log.setHttpMethod(metadata.get("httpMethod"));
        return log;
    }
}
