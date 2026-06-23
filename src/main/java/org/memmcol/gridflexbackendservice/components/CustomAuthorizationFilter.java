package org.memmcol.gridflexbackendservice.components;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
//import org.memmcol.gridflexbackendservice.model.user.CustomUserPrincipal;
import org.memmcol.gridflexbackendservice.model.user.CustomUserPrincipal;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@Component
@Slf4j
public class CustomAuthorizationFilter extends OncePerRequestFilter {

    @Value("${security.header.key}")
    private String adminHeaderKey;

    @Value("${security.admin.value}")
    private String adminHeaderValue;

    @Value("${security.user.value}")
    private String userHeaderValue;

    @Value("${security.setup.value}")
    private String setupHeaderValue;

    private final IMap<String, Object> authCache;

	public CustomAuthorizationFilter(
            @Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
		this.authCache = hazelcastInstance.getMap("authCache");
	}

    // ============================
    // EXCLUDE SWAGGER + PUBLIC
    // ============================
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getServletPath();

        return path.startsWith("/swagger-ui")
                || path.startsWith("/gridflex/api-docs")
                || path.startsWith("/api")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars")
                || path.startsWith("/actuator/prometheus")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/actuator")
                || path.startsWith("/gridflex/api-docs/Client Authentication")
                || path.startsWith("/gridflex/api-docs/Odyssey")

                // existing public endpoints
                || path.startsWith("/odyssey/")
                || path.startsWith("/auth/service/admin/login")
                || path.startsWith("/auth/service/generate-otp")
                || path.startsWith("/auth/service/forget-password")
                || path.startsWith("/auth/service/test")
                || path.startsWith("/service/alerts")
                || path.startsWith("/service/reports/summary")
                || path.startsWith("/service/trigger")
                || path.startsWith("/band/service/clear-cache")
                || path.startsWith("/data-collection/schedules")
                || path.startsWith("/meter/service/meterInfo-lookup")
                || path.startsWith("/meter/service/readMeter-lookup")
                || path.startsWith("/admin/setup/client")
                || path.startsWith("/client/auth/token")
                || path.startsWith("/licence/service");
    }

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String path = request.getServletPath();

//		// Define exempt paths where authorization is not required
//		Set<String> exemptPaths = Set.of(
//				"/service/alerts",
//				"/auth/service/admin/login",
//                "/auth/service/generate-otp",
//                "/auth/service/forget-password",
//				"/actuator/prometheus",
//				"/service/reports/summary",
//				"/service/trigger/daily",
//				"/service/trigger/monthly",
//				"/band/service/clear-cache",
//                "/auth/service/test",
//                "/data-collection/schedules",
//                "/meter/service/meterInfo-lookup",
//                "/meter/service/readMeter-lookup",
//                "/admin/setup/api-clients",
//                "/standard/auth/token",
//                "/api/licence/generate-fingerprint",
//                "/api/licence/get",
//                "/api/licence/deactivate",
//                "/api/licence/fingerprint",
//                "/api/licence/validate",
//                "/api/licence/upload",
//                "/swagger-ui",
//                "/swagger-ui.html",
//                "/v3/api-docs/**",
//                "/v3/api-docs",
//                "/swagger-resources/**",
//                "/webjars/**"
//		);
//
//        // Check if the path ends with any exempt pattern
//        boolean isExempt = exemptPaths.stream()
//                .anyMatch(path::endsWith);
//
//        // If the path is exempt, skip the authorization filter
//        if (isExempt) {
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        if (path.startsWith("/swagger-ui")
//                || path.startsWith("/v3/api-docs")
//                || path.startsWith("/swagger-resources")
//                || path.startsWith("/webjars")) {
//
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        if (path.startsWith("/odyssey/")) {
//            filterChain.doFilter(request, response);
//            return;
//        }

        // ADMIN / API KEY AUTH PATHS
        if (path.startsWith("/admin/setup/client")
                || path.startsWith("/client/auth/token")
                || path.startsWith("/auth/service/generate-otp")
                || path.startsWith("/auth/service/forget-password")) {

            String apiKey = request.getHeader(adminHeaderKey);

            if (apiKey == null || (
                    !apiKey.equals(adminHeaderValue)
                            && !apiKey.equals(userHeaderValue)
                            && !apiKey.equals(setupHeaderValue)
            )) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                Map<String, Object> error = new HashMap<>();
                error.put("responsecode", "401"); //"Missing or invalid authentication header: " + adminHeaderKey
                error.put("responsedesc", "Missing or invalid authentication header: " + adminHeaderKey);
                error.put("responsedata", "");

                new ObjectMapper().writeValue(response.getOutputStream(), error);
                return;
            }

            filterChain.doFilter(request, response);
            return;
        }
        else {
//            System.out.println("token required:: "+HttpHeaders.AUTHORIZATION);
            // Token-based authorization for other paths
            String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {

//                System.out.println("authorizationHeader1:: "+authorizationHeader);
                try {
                    String token = authorizationHeader.substring("Bearer ".length());

                    if (Boolean.TRUE.equals(authCache.get(token))) {
                        handleException(response, new Exception("Token is blacklisted"),
                                "Token is blacklisted", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }

                    Algorithm algorithm = Algorithm.HMAC256("secret".getBytes());
                    JWTVerifier verifier = JWT.require(algorithm).build();
                    DecodedJWT decodedJWT = verifier.verify(token);

                    String username = decodedJWT.getSubject();
                    String[] roles = decodedJWT.getClaim("roles").asArray(String.class);
                    Collection<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    String permissionTreeJson = decodedJWT.getClaim("permission_tree").toString();

                    CustomUserPrincipal principal =
                            new CustomUserPrincipal(username, permissionTreeJson);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    authorities
                            );

                    SecurityContextHolder.getContext().setAuthentication(auth);

                    filterChain.doFilter(request, response);

                } catch (JWTVerificationException exception) {
                    log.error("JWT Verification Exception caught", exception);
                    String errorMessage;
                    if (exception.getMessage().contains("expired")) {
                        errorMessage = "Authorization Token Expired";
                    } else if (exception.getMessage().contains("signature")) {
                        errorMessage = "Invalid Token Signature";
                    } else {
                        errorMessage = "Invalid Authorization Token";
                    }

                    handleException(response, exception, errorMessage, HttpServletResponse.SC_FORBIDDEN);
                } catch (Exception exception) {
                    log.error("Unexpected exception", exception);
                    handleException(response, exception, "Internal Server Error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            } else {
                handleException(response, new Exception("Authorization Token Not Found"),
                        "Authorization Token Not Found", HttpServletResponse.SC_UNAUTHORIZED);
            }
        }
	}

    // Helper method to handle exceptions and send a custom error message
	private void handleException(HttpServletResponse response, Exception exception, String description, int statusCode) throws IOException {
		Map<String, String> errorMessage = new HashMap<>();
		errorMessage.put("responsecode", String.valueOf(statusCode));
		errorMessage.put("responsedesc", description);
		errorMessage.put("responsedata", "");

		response.setStatus(statusCode);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		new ObjectMapper().writeValue(response.getOutputStream(), errorMessage);
	}
}


//		// If the path is exempt, skip the authorization filter
//		if (exemptPaths.contains(path)) {
//			System.out.println("Requested path: " + path);
//			filterChain.doFilter(request, response);
//			return;
//		}

//        // Enforce custom header validation for paths like logout and forget-password
//        if (path.contains("/auth/service/generate-otp")
//                || path.contains("/auth/service/forget-password")
//                || path.contains("/admin/setup/api-clients")) {
//            String apiKey = request.getHeader(ADMIN_HEADER_KEY);
////            System.out.println("apiKey:: "+apiKey);
//
//            if (apiKey == null || (!apiKey.equals(ADMIN_HEADER_VALUE)
//                    && !apiKey.equals(USER_HEADER_VALUE)
//                    && !apiKey.equals(SETUP_HEADER_VALUE))) {
//                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//                Map<String, String> errorMessage = new HashMap<>();
//                errorMessage.put("responsecode", String.valueOf(HttpServletResponse.SC_UNAUTHORIZED));
//                errorMessage.put("responsedesc", "Missing or invalid authentication header "+ADMIN_HEADER_KEY);
//                errorMessage.put("responsedata", "");
//                new ObjectMapper().writeValue(response.getOutputStream(), errorMessage);
//                return;
//
//            }
//            filterChain.doFilter(request, response);
//            return;
//        }