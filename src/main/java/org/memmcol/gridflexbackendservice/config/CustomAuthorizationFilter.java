package org.memmcol.gridflexbackendservice.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.memmcol.gridflexbackendservice.model.ExceptionErrorLogs;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;


@Component
@Slf4j
public class CustomAuthorizationFilter extends OncePerRequestFilter {
//	final ExceptionAuditRepository exceptionAuditRepository;
//
//	public CustomAuthorizationFilter(ExceptionAuditRepository exceptionAuditRepository) {
//		this.exceptionAuditRepository = exceptionAuditRepository;
//	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (request.getServletPath().equals("/service/login")
				|| request.getServletPath().equals("/service/admin/login")
				|| request.getServletPath().equals("/service/reset-password")
				|| request.getServletPath().equals("/service/generate-otp")
				|| request.getServletPath().equals("/service/verify-otp")
		) {
			filterChain.doFilter(request, response);
		} else {
			String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
			if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
				try {
					String token = authorizationHeader.substring("Bearer ".length());

					Algorithm algorithm = Algorithm.HMAC256("secret".getBytes());
					JWTVerifier verifier = JWT.require(algorithm).build();
					DecodedJWT decodedJWT = verifier.verify(token);
					String username = decodedJWT.getSubject();
					String[] roles = decodedJWT.getClaim("roles").asArray(String.class);
					Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
					Stream<String> rolesStream = Arrays.stream(roles);
					rolesStream.forEach((role) -> {
						authorities.add(new SimpleGrantedAuthority(role));
					});
					UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
							username, null, authorities);
					SecurityContextHolder.getContext().setAuthentication(authenticationToken);
					filterChain.doFilter(request, response);
				} catch (Exception exception) {
					ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
					response.setHeader("error", exception.getMessage());
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					Map<String, String> errorMessage = new HashMap<>();
					errorMessage.put("responsecode", "121");
					errorMessage.put("responsedesc", "Authorization Token Expired");
					errorMessage.put("responsedata", exception.getMessage());
					exceptionErrorLogs.setDescription("Error occurred while authorizing user");
					exceptionErrorLogs.setError_message(exception.getMessage());
					exceptionErrorLogs.setError(exception);
//					exceptionAuditRepository.save(exceptionErrorLogs);
					response.setContentType(MediaType.APPLICATION_JSON_VALUE);
					new ObjectMapper().writeValue(response.getOutputStream(), errorMessage);
				}
			} else {
				Map<String, String> errorMessage = new HashMap<>();
				errorMessage.put("responsecode", "061");
				errorMessage.put("responsedesc", "Authorization Token Not Found");
				errorMessage.put("responsedata", "");

				response.setContentType(MediaType.APPLICATION_JSON_VALUE);
				new ObjectMapper().writeValue(response.getOutputStream(), errorMessage);
//				filterChain.doFilter(request, response);
			}
		}

	}

}
