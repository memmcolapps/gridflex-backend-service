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

	private static final String ADMIN_HEADER_KEY = "custom";  // Custom header key
	private static final String ADMIN_HEADER_VALUE = "ab@#1cD3fG!mNXyZ$%Kl78&OH@beeb$";
	private static final String USER_HEADER_VALUE = "UvW$%12xYz!@#9LmNoP&*45QH@beeb&";

	private final IMap<String, Object> authCache;

	public CustomAuthorizationFilter(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
		this.authCache = hazelcastInstance.getMap("authCache");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String path = request.getServletPath();

        System.out.println("Endpoint: " + path);
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
                "/data-collection/schedules"
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

//		// If the path is exempt, skip the authorization filter
//		if (exemptPaths.contains(path)) {
//			System.out.println("Requested path: " + path);
//			filterChain.doFilter(request, response);
//			return;
//		}

        // Enforce custom header validation for paths like logout and forget-password
        if (path.contains("/auth/service/generate-otp") || path.contains("/auth/service/forget-password")) {
            String apiKey = request.getHeader(ADMIN_HEADER_KEY);
//            System.out.println("apiKey:: "+apiKey);

            if (apiKey == null || (!apiKey.equals(ADMIN_HEADER_VALUE) && !apiKey.equals(USER_HEADER_VALUE))) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                Map<String, String> errorMessage = new HashMap<>();
                errorMessage.put("responsecode", String.valueOf(HttpServletResponse.SC_UNAUTHORIZED));
                errorMessage.put("responsedesc", "Missing or invalid authentication header "+ADMIN_HEADER_KEY);
                errorMessage.put("responsedata", "");

                new ObjectMapper().writeValue(response.getOutputStream(), errorMessage);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        } else {
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
