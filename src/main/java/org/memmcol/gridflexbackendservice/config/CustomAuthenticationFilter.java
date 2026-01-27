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
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
//import org.memmcol.gridflexbackendservice.model.UserDTO;
import org.memmcol.gridflexbackendservice.model.node.Node;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.service.CustomUserDetails;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

//import static org.memmcol.gridflexbackendservice.components.GenericHandler.getClientIp;


@RequiredArgsConstructor
public class CustomAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
	 private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationFilter.class);
	 private AuthenticationManager authenticationManager;
//	@Autowired
	private AuthMapper authMapper;

	private AuditRepository auditRepository;

	private IMap<String, Boolean> auditCache;

	private GenericHandler genericHandler;

	private ObjectMapper objectMapper;

    private ResponseProperties responseProperties;

//	private IMap<String, Boolean> authCache;

	// Define the required headers
	private static final String ADMIN_HEADER_KEY = "custom";
	private static final String ADMIN_HEADER_VALUE = "ab@#1cD3fG!mNXyZ$%Kl78&OH@beeb$"; // Change this to a secure value

	private static final String USER_HEADER_KEY = "custom";
	private static final String USER_HEADER_VALUE = "UvW$%12xYz!@#9LmNoP&*45QH@beeb&"; // Change this to a secure value

	public CustomAuthenticationFilter(
			AuthenticationManager authenticationManager,
			AuthMapper authMapper, AuditRepository auditRepository,
			HazelcastInstance hazelcastInstance, GenericHandler genericHandler,
			ObjectMapper objectMapper,
            ResponseProperties responseProperties) {
		this.authenticationManager = authenticationManager;
		this.authMapper = authMapper;
		this.auditRepository = auditRepository;
		this.auditCache = hazelcastInstance.getMap("auditCache");
		this.genericHandler = genericHandler;
		this.objectMapper = objectMapper;
        this.responseProperties = responseProperties;
//		this.authCache = hazelcastInstance.getMap("auth-Cache");
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException {
		String username = request.getParameter("username");
		String password = request.getParameter("password");

		// Fetch user details before authentication
//		UserModel user = authMapper.findAuthByUserEmail(username.trim().toLowerCase());

//		String isSuperAdmin = user.getGroups().getModules().get(0).getName();
//		String requiredHeaderKey = isSuperAdmin.equalsIgnoreCase("Full Access") ? ADMIN_HEADER_KEY : USER_HEADER_KEY;
//		String requiredHeaderValue = isSuperAdmin.equalsIgnoreCase("Full Access") ? ADMIN_HEADER_VALUE : USER_HEADER_VALUE;

		// Validate the required header
		String headerValue = request.getHeader(ADMIN_HEADER_KEY);
		if (headerValue == null || !headerValue.equals(ADMIN_HEADER_VALUE)) {
			throw new BadCredentialsException("Missing or invalid authentication header: " + ADMIN_HEADER_KEY);
		}

//		// Dynamically set service URL
//		if (isSuperAdmin.equalsIgnoreCase("Full Access")) {
//			setFilterProcessesUrl("/auth/service/admin/login");
//		} else {
//			setFilterProcessesUrl("/auth/service/login");
//		}
		
		UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
		
		return authenticationManager.authenticate(authenticationToken);
	}

	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
			Authentication authentication) throws IOException, ServletException {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal(); // use the provided authentication

		Algorithm algorithm = Algorithm.HMAC256("secret".getBytes());
		Map<String, String> metadata = genericHandler.extractRequestMetadata(request);
		ObjectMapper mapper = new ObjectMapper();
		List<Map<String, Object>> permissionTree = mapper.readValue(userDetails.getPermissionTreeJson(), List.class);

		String access_token = JWT.create()
				.withSubject(userDetails.getUsername())
				.withClaim("roles", userDetails.getAuthorities().stream()
						.map(GrantedAuthority::getAuthority)
						.collect(Collectors.toList()))
				.withClaim("permission_tree", permissionTree) // your structured JSON
				.withIssuedAt(new Date())
				.withExpiresAt(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)) // 24 hours
				.sign(algorithm);

		authMapper.updateLoginState(userDetails.getUsername(), LocalDateTime.now(ZoneId.of("Africa/Lagos")));

		UserModel user = authMapper.findAuthByUserEmail(userDetails.getUsername());
		List<Node> nodes = authMapper.getNodeWithChildren(user.getNodeId(), user.getOrgId());

		Map<UUID, Node> nodeMap = new HashMap<>();
		Node root = null;

		for (Node node : nodes) {
			node.setNodesTree(new ArrayList<>());
			nodeMap.put(node.getId(), node);
		}

		for (Node node : nodes) {
			if (node.getId().equals(user.getNodeId())) {
				root = node; // this is the node we're querying for
			}
			if (node.getParentId() != null && nodeMap.containsKey(node.getParentId())) {
				Node parent = nodeMap.get(node.getParentId());
				parent.getNodesTree().add(node);
			}
		}
		user.setNodes(root);
		AuditLog auditLog = buildAuditLog(user, "Logged in", "auth", null, metadata);
		auditRepository.save(auditLog);
		for (String key : auditCache.keySet()) {
			if (key.startsWith("grid_flex_audit_log_page_")) {
				auditCache.remove(key);
			}
		}
//		authCache.remove("dashboard");
//		auditRepository.save(auditNotificationDTO);
		Map<String, Object> resp = new HashMap<>();
		Map<String, Object> token = new HashMap<>();
		resp.put("responsecode", "000");
		resp.put("responsedesc", "Authentication Successful");
		token.put("user_info", user);
		token.put("access_token", access_token);
		resp.put("responsedata", token);

		// Set content type to JSON
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		// Write the response as JSON
//		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.writeValue(response.getOutputStream(), resp);

	}

	private AuditLog buildAuditLog(UserModel creator, String description, String type, Object createdEntity, Map<String, String> metadata) {
		AuditLog log = new AuditLog();
		log.setCreator(creator);
		log.setDescription(description);
		log.setType(type);
		log.setIpAddress(metadata.get("ipAddress"));
		log.setUserAgent(metadata.get("userAgent"));
		log.setEndpoint(metadata.get("endpoint"));
		log.setHttpMethod(metadata.get("httpMethod"));
		return log;
	}

    @Override
    protected void unsuccessfulAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException failed
    ) throws IOException {

        try {
            genericHandler.logIncidentReport("Login service failed");
        } catch (Exception ex) {
            log.error("Incident logging failed", ex);
            // DO NOT rethrow
        }

        Map<String, Object> errorMessage = new HashMap<>();
        errorMessage.put("responsecode", responseProperties.getFailCode());
        errorMessage.put("responsedesc", resolveAuthMessage(failed));
        errorMessage.put("responsedata", null);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getOutputStream(), errorMessage);
    }


    //	@Override
	protected void unsuccessfulAuthenticationBackUp(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException failed) throws IOException, ServletException {

		genericHandler.logIncidentReport("Login service failed");

	    // Prepare the response message
	    Map<String, String> errorMessage = new HashMap<>();
	    errorMessage.put("responsecode", String.valueOf(HttpServletResponse.SC_UNAUTHORIZED));
	    errorMessage.put("responsedesc", failed.getMessage());
	    errorMessage.put("responsedata", "");

	    // Set the response status to indicate authentication failure
	    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

		System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>");

	    // Write the error message to the response body
	    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
	    new ObjectMapper().writeValue(response.getOutputStream(), errorMessage);
	}

    private String resolveAuthMessage(AuthenticationException ex) {

        if (ex instanceof BadCredentialsException) {
            return "Invalid username or password";
        }

        if (ex instanceof org.springframework.security.core.userdetails.UsernameNotFoundException) {
            return "Invalid username or password";
        }

        return "Authentication failed";
    }


}
