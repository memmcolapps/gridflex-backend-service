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
            @Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance
//            String adminHeaderKey, String adminHeaderValue,
//            String userHeaderValue, String setupHeaderValue
    ) {
		this.authCache = hazelcastInstance.getMap("authCache");
//        this.adminHeaderKey = adminHeaderKey;
//        this.adminHeaderValue = adminHeaderValue;
//        this.userHeaderValue = userHeaderValue;
//        this.setupHeaderValue = setupHeaderValue;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String path = request.getServletPath();

		// Define exempt paths where authorization is not required
		Set<String> exemptPaths = Set.of(
				"/service/alerts",
				"/auth/service/admin/login",
                "/auth/service/generate-otp",
                "/auth/service/forget-password",
				"/actuator/prometheus",
				"/service/reports/summary",
				"/service/trigger/daily",
				"/service/trigger/monthly",
				"/band/service/clear-cache",
                "/auth/service/test",
                "/data-collection/schedules",
                "/meter/service/meterInfo-lookup",
                "/meter/service/readMeter-lookup",
                "/admin/setup/api-clients",
                "/standard/auth/token",
                "/api/licence/generate-fingerprint",
                "/api/licence/get",
                "/api/licence/deactivate",
                "/api/licence/fingerprint",
                "/api/licence/validate",
                "/api/licence/upload"
//                "/standard/auth/token"
//                "/odyssey/standard/meter/readings",
//                "/odyssey/standard/electricity/payments"
//                "/uploads"
//				"/meter/service/virtual/export",
//				"/meter/service/export"
		);

        // Check if the path ends with any exempt pattern
        boolean isExempt = exemptPaths.stream()
                .anyMatch(path::endsWith);

        // If the path is exempt, skip the authorization filter
        if (isExempt) {
            filterChain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/odyssey/")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/admin/setup/api-clients")
                || path.startsWith("/standard/auth/token")
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
//                    CustomUserPrincipal principal = new CustomUserPrincipal(username, permissionTreeJson);
//                    UsernamePasswordAuthenticationToken authToken =
//                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
//                    SecurityContextHolder.getContext().setAuthentication(authToken);

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
		errorMessage.put("responsedata", exception.getMessage());

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