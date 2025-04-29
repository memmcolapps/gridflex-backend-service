package org.memmcol.gridflexbackendservice.config;


import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.model.Operator;
import org.memmcol.gridflexbackendservice.model.OperatorAudit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


@RequiredArgsConstructor
public class CustomAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
	 private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationFilter.class);
	 private AuthenticationManager authenticationManager;
//	@Autowired
	private AuthMapper operatorMapper;

	private AuditRepository auditRepository;

	private IMap<String, Boolean> auditCache;

	private IMap<String, Boolean> authCache;

//	private HazelcastInstance hazelcastInstance;
	// Define the required headers
	private static final String ADMIN_HEADER_KEY = "custom";
	private static final String ADMIN_HEADER_VALUE = "ab@#1cD3fG!mNXyZ$%Kl78&OH@beeb$"; // Change this to a secure value

	private static final String USER_HEADER_KEY = "custom";
	private static final String USER_HEADER_VALUE = "UvW$%12xYz!@#9LmNoP&*45QH@beeb&"; // Change this to a secure value

	public CustomAuthenticationFilter(
			AuthenticationManager authenticationManager,
			AuthMapper operatorMapper,
			AuditRepository auditRepository, HazelcastInstance hazelcastInstance) {
		this.authenticationManager = authenticationManager;
		this.operatorMapper = operatorMapper;
		this.auditRepository = auditRepository;
		this.auditCache = hazelcastInstance.getMap("audit-Cache");
		this.authCache = hazelcastInstance.getMap("auth-Cache");
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException {
		String username = request.getParameter("username");
		String password = request.getParameter("password");

		// Fetch user details before authentication
		Operator user = operatorMapper.findByAuthEmail(username);
		if (user == null) {
			throw new UsernameNotFoundException("User not found");
		}

		// Determine if user is admin or regular user
		boolean isAdmin = user.isPermission();
		String requiredHeaderKey = isAdmin ? ADMIN_HEADER_KEY : USER_HEADER_KEY;
		String requiredHeaderValue = isAdmin ? ADMIN_HEADER_VALUE : USER_HEADER_VALUE;

		// Validate the required header
		String headerValue = request.getHeader(requiredHeaderKey);
		if (headerValue == null || !headerValue.equals(requiredHeaderValue)) {
			throw new BadCredentialsException("Missing or invalid authentication header: " + requiredHeaderKey);
		}

		// Dynamically set service URL
		if (isAdmin) {
			setFilterProcessesUrl("/auth/service/admin/login");
		} else {
			setFilterProcessesUrl("/auth/service/login");
		}
		
		UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
		
		return authenticationManager.authenticate(authenticationToken);
	}

	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
			Authentication authentication) throws IOException, ServletException {
		User user = (User) authentication.getPrincipal();// Add a custom header with the JWT token
		OperatorAudit auditNotificationDTO = new OperatorAudit();
		Algorithm algorithm = Algorithm.HMAC256("secret".getBytes()); //Encrypt/Sign the token
		String access_token = JWT.create()
				.withSubject(user.getUsername())
				.withExpiresAt(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)) // 30 days expiration
				// .withExpiresAt(new Date(System.currentTimeMillis() + 30 * 60 * 1000))
				.withIssuer(request.getRequestURL().toString())
				.withClaim("roles", user.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
				.sign(algorithm);

		//response.setHeader("Authorization", "Bearer " + "123456"); // Add the JWT to the header

		Operator operator = operatorMapper.findByAuthEmail(user.getUsername());
		operator.setPasswordEncrypt("");
		auditNotificationDTO.setCreator(operator);
		auditNotificationDTO.setDescription(operator.getEmail()+" Logged in");
		auditNotificationDTO.setType("auth");
		for (String key : auditCache.keySet()) {
			if (key.startsWith("grid_flex_audit_log_page_")) {
				auditCache.remove(key);
			}
		}
//		authCache.remove("dashboard");
		auditRepository.save(auditNotificationDTO);
		Map<String, Object> resp = new HashMap<>();
		Map<String, Object> token = new HashMap<>();
		resp.put("responsecode", "000");
		resp.put("responsedesc", "Authentication Successful");
		token.put("user_info", operator);
		token.put("access_token", access_token);
		resp.put("responsedata", token);

		// Set content type to JSON
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		// Write the response as JSON
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.writeValue(response.getOutputStream(), resp);

	}

	@Override
	protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException failed) throws IOException, ServletException {

	    // Prepare the response message
	    Map<String, String> errorMessage = new HashMap<>();
	    errorMessage.put("responsecode", String.valueOf(HttpServletResponse.SC_UNAUTHORIZED));
	    errorMessage.put("responsedesc", failed.getMessage());
	    errorMessage.put("responsedata", "");

	    // Set the response status to indicate authentication failure
	    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

	    // Write the error message to the response body
	    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
	    new ObjectMapper().writeValue(response.getOutputStream(), errorMessage);
	}

}

