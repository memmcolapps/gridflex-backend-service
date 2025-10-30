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

		// Define exempt paths where authorization is not required
		Set<String> exemptPaths = Set.of(
				"/service/alerts",
				"/auth/service/admin/login",
				"/actuator/prometheus",
				"/service/reports/summary",
				"/service/trigger/daily",
				"/service/trigger/monthly",
				"/band/service/clear-cache"
//				"/meter/service/virtual/export",
//				"/meter/service/export"
		);

		// If the path is exempt, skip the authorization filter
		if (exemptPaths.contains(path)) {
			System.out.println("Requested path: " + path);
			filterChain.doFilter(request, response);
			return;
		}

		// Enforce custom header validation for paths like logout and forget-password
		if (path.startsWith("/auth/service/generate-otp") || path.startsWith("/auth/service/forget-password")) {
			String apiKey = request.getHeader(ADMIN_HEADER_KEY);  // Get custom header
			System.out.println(apiKey);
			// Validate the custom API Key header
			if (apiKey == null || (!apiKey.equals(ADMIN_HEADER_VALUE) && !apiKey.equals(USER_HEADER_VALUE))) {
				// Instead of throwing an exception, handle the response
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // Set HTTP 401 status
				response.setContentType(MediaType.APPLICATION_JSON_VALUE);

				// Send a custom error message
				Map<String, String> errorMessage = new HashMap<>();
				errorMessage.put("responsecode", String.valueOf(HttpServletResponse.SC_UNAUTHORIZED));
				errorMessage.put("responsedesc", "Missing or invalid authentication header "+ADMIN_HEADER_KEY);
				errorMessage.put("responsedata", "");

				// Write the error message to the response output stream
				new ObjectMapper().writeValue(response.getOutputStream(), errorMessage);
				return;  // Exit the filter chain after sending the response
			}
			filterChain.doFilter(request, response);  // Proceed if valid API Key
		} else {

			System.out.println(">>>>heeeeeeeeeeeeee>>>:::");
			// Token-based authorization for other paths
			String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

			if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
				try {
					String token = authorizationHeader.substring("Bearer ".length());
					// Check if token is blacklisted
					if (Boolean.TRUE.equals(authCache.get(token))) {
						handleException(response, new Exception("Token is blacklisted"),
								"Token is blacklisted", HttpServletResponse.SC_UNAUTHORIZED);
						return;
					}
					Algorithm algorithm = Algorithm.HMAC256("secret".getBytes());
					JWTVerifier verifier = JWT.require(algorithm).build();
					DecodedJWT decodedJWT = verifier.verify(token);

					String username = decodedJWT.getSubject();

					// Decode roles correctly
					String[] roles = decodedJWT.getClaim("roles").asArray(String.class);
					Collection<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
							.map(SimpleGrantedAuthority::new)
							.collect(Collectors.toList());

					// Optionally: Parse permission_tree JSON (if needed)
					String permissionTreeJson = decodedJWT.getClaim("permission_tree").toString();

					CustomUserPrincipal principal = new CustomUserPrincipal(username, permissionTreeJson);
					UsernamePasswordAuthenticationToken authToken =
							new UsernamePasswordAuthenticationToken(principal, null, authorities);
					SecurityContextHolder.getContext().setAuthentication(authToken);

					// Continue filter chain
					filterChain.doFilter(request, response);

				} catch (JWTVerificationException exception) {
					handleException(response, exception, "Authorization Token Expired", HttpServletResponse.SC_FORBIDDEN);
				} catch (Exception exception) {
					handleException(response, exception, "Internal Server Error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
			} else {
				handleException(response, new Exception("Authorization Token Not Found"), "Authorization Token Not Found", HttpServletResponse.SC_UNAUTHORIZED);
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


//					String token = authorizationHeader.substring("Bearer ".length());
//					Algorithm algorithm = Algorithm.HMAC256("secret".getBytes());
//					JWTVerifier verifier = JWT.require(algorithm).build();
//					DecodedJWT decodedJWT = verifier.verify(token);
//
//					String username = decodedJWT.getSubject();
//					String[] roles = decodedJWT.getClaim("roles").asArray(String.class);
//					Collection<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
//							.map(SimpleGrantedAuthority::new)
//							.collect(Collectors.toList());
//
//					// Decode permission_tree
//					String permissionTreeJson = decodedJWT.getClaim("permission_tree").asString();
//					if (permissionTreeJson != null) {
//						// Convert the permission tree to a map or a list (e.g., TreeMap, HashMap, etc.)
//						Map<String, Object> permissionTree = new ObjectMapper().readValue(permissionTreeJson, Map.class);
//
//						// Check if the user has permission for the current path
//						if (!permissionEvaluator.hasPermissionForPath(permissionTree, path)) {
//							handleException(response, new Exception("Permission Denied"), "Permission Denied", HttpServletResponse.SC_FORBIDDEN);
//							return;
//						}
//					}
//
//					// Store `permissionTree` somewhere if needed (e.g., in SecurityContext or thread-local)
//					CustomUserPrincipal principal = new CustomUserPrincipal(username, permissionTreeJson);
//					UsernamePasswordAuthenticationToken authToken =
//							new UsernamePasswordAuthenticationToken(principal, null, authorities);
//					SecurityContextHolder.getContext().setAuthentication(authToken);
//
//					// Continue filter chain
//					filterChain.doFilter(request, response);


///
//					String token = authorizationHeader.substring("Bearer ".length());
//					Algorithm algorithm = Algorithm.HMAC256("secret".getBytes());
//					JWTVerifier verifier = JWT.require(algorithm).build();
//					DecodedJWT decodedJWT = verifier.verify(token);
//
//					String username = decodedJWT.getSubject();
//					String[] roles = decodedJWT.getClaim("permission_tree").asArray(String.class);
//					Collection<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
//							.map(SimpleGrantedAuthority::new)
//							.collect(Collectors.toList());
//
//					UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
//							username, null, authorities
//					);
//					SecurityContextHolder.getContext().setAuthentication(authenticationToken);
//
//					filterChain.doFilter(request, response);
