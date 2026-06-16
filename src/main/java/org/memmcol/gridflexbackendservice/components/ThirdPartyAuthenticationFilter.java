package org.memmcol.gridflexbackendservice.components;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.memmcol.gridflexbackendservice.thirdPartyService.model.ApiClient;
import org.memmcol.gridflexbackendservice.thirdPartyService.model.ThirdPartyPrincipal;
import org.memmcol.gridflexbackendservice.thirdPartyService.repository.ApiClientRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ThirdPartyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiClientRepository apiClientRepository;

    @Value("${odyssey.jwt-secret}")
    private String secret;

    @Value("${security.header.key}")
    private String adminHeaderKey;

    @Value("${security.setup.value}")
    private String setupHeaderValue;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getServletPath();

        // EXCLUDE TOKEN GENERATION ENDPOINT
        if (path.equals("/client/auth/token")) {

            String apiKey = request.getHeader(adminHeaderKey); // or x-api-key

            if (apiKey == null || !apiKey.equals(setupHeaderValue)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                Map<String, Object> error = new HashMap<>();
                error.put("responsecode", "401");
                error.put("responsedesc", "Missing or invalid API key header: " + adminHeaderKey);
                error.put("responsedata", "");

                new ObjectMapper().writeValue(response.getOutputStream(), error);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        // Only apply to Odyssey endpoints
        if (!(path.startsWith("/odyssey/") || (path.startsWith("/client/")))) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("Missing Authorization token");
            }

            String token = authHeader.substring(7);
            System.out.println("secrete: "+secret);
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT.require(algorithm).build();

            DecodedJWT jwt = verifier.verify(token);

            String clientId = jwt.getSubject();
            UUID userId = UUID.fromString(jwt.getClaim("userId").asString());

            UUID orgId = jwt.getClaim("orgId").isNull()
                    ? null
                    : UUID.fromString(jwt.getClaim("orgId").asString());

            List<String> scopes = jwt.getClaim("scopes").asList(String.class);

            // Optional DB validation (extra security layer)
            ApiClient client = apiClientRepository.findByClientId(clientId.toLowerCase())
                    .orElseThrow(() -> new RuntimeException("Invalid client"));

            if (!client.getStatus()) {
                throw new RuntimeException("Client disabled");
            }

            ThirdPartyPrincipal principal = new ThirdPartyPrincipal(
                    clientId,
                    userId,
                    orgId,
                    scopes
            );

            List<SimpleGrantedAuthority> authorities = scopes.stream()
                    .map(scope -> new SimpleGrantedAuthority(scope))
                    .toList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            authorities
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (BadCredentialsException ex) {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            Map<String, Object> error = new HashMap<>();
            error.put("responsecode", "401");
            error.put("responsedesc", ex.getMessage());

            new ObjectMapper().writeValue(response.getOutputStream(), error);

        } catch (Exception ex) {
//            log.error("Odyssey authentication failed", ex);

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            Map<String, Object> error = new HashMap<>();
            error.put("responsecode", "401");
            error.put("responsedesc", ex.getMessage());

            new ObjectMapper().writeValue(response.getOutputStream(), error);
        }
    }
}
