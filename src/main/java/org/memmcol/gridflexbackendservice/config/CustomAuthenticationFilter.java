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
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@RequiredArgsConstructor
public class CustomAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
	 private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationFilter.class);
	 private AuthenticationManager authenticationManager;
//	@Autowired
	private AuthMapper authMapper;

	private AuditRepository auditRepository;

	private IMap<String, Boolean> auditCache;

//	private IMap<String, Boolean> authCache;

	// Define the required headers
	private static final String ADMIN_HEADER_KEY = "custom";
	private static final String ADMIN_HEADER_VALUE = "ab@#1cD3fG!mNXyZ$%Kl78&OH@beeb$"; // Change this to a secure value

	private static final String USER_HEADER_KEY = "custom";
	private static final String USER_HEADER_VALUE = "UvW$%12xYz!@#9LmNoP&*45QH@beeb&"; // Change this to a secure value

	public CustomAuthenticationFilter(
			AuthenticationManager authenticationManager,
			AuthMapper authMapper,
			AuditRepository auditRepository, HazelcastInstance hazelcastInstance) {
		this.authenticationManager = authenticationManager;
		this.authMapper = authMapper;
		this.auditRepository = auditRepository;
		this.auditCache = hazelcastInstance.getMap("audit-Cache");
//		this.authCache = hazelcastInstance.getMap("auth-Cache");
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException {
		String username = request.getParameter("username");
		String password = request.getParameter("password");

		// Fetch user details before authentication
		UserModel user = authMapper.findAuthByUserEmail(username.trim().toLowerCase());
//		List<Node> nodes = authMapper.getNodeWithChildren(user.getNodeId(), user.getOrgId());
////		if (nodes == null || nodes.isEmpty()) {
////			return ResponseMap.response(status.getSuccessCode(), "No nodes found", "");
////		}
//
//		Map<UUID, Node> nodeMap = new HashMap<>();
//		Node root = null;
//
//		for (Node node : nodes) {
//			node.setNodesTree(new ArrayList<>());
//			nodeMap.put(node.getId(), node);
//		}
//
//		for (Node node : nodes) {
//			if (node.getId().equals(user.getNodeId())) {
//				root = node; // this is the node we're querying for
//			}
//			if (node.getParentId() != null && nodeMap.containsKey(node.getParentId())) {
//				Node parent = nodeMap.get(node.getParentId());
//				parent.getNodesTree().add(node);
//			}
//		}
//		user.setNod(root);
//		if (user == null) {
//			throw new UsernameNotFoundException("User not found");
//		}

		String isSuperAdmin = user.getGroups().getModules().get(0).getName();
		String requiredHeaderKey = isSuperAdmin.equalsIgnoreCase("Full Access") ? ADMIN_HEADER_KEY : USER_HEADER_KEY;
		String requiredHeaderValue = isSuperAdmin.equalsIgnoreCase("Full Access") ? ADMIN_HEADER_VALUE : USER_HEADER_VALUE;
		// Determine if user is admin or regular user
//		boolean isAdmin = user.getUser().getStatus();
//		String requiredHeaderKey = isAdmin ? ADMIN_HEADER_KEY : USER_HEADER_KEY;
//		String requiredHeaderValue = isAdmin ? ADMIN_HEADER_VALUE : USER_HEADER_VALUE;

		// Validate the required header
		String headerValue = request.getHeader(requiredHeaderKey);
		if (headerValue == null || !headerValue.equals(requiredHeaderValue)) {
			throw new BadCredentialsException("Missing or invalid authentication header: " + requiredHeaderKey);
		}

		// Dynamically set service URL
		if (isSuperAdmin.equalsIgnoreCase("Full Access")) {
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
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal(); // use the provided authentication

//		User user = (User) authentication.getPrincipal();// Add a custom header with the JWT token
		AuditLog auditNotificationDTO = new AuditLog();
		Algorithm algorithm = Algorithm.HMAC256("secret".getBytes());
		String ipAddress = request.getRemoteAddr();
		String userAgent = request.getHeader("User-Agent");
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
		auditNotificationDTO.setCreator(user);
		user.setNodes(root);
		user.setPassword("");
		auditNotificationDTO.setDescription("Logged in");
		auditNotificationDTO.setUserAgent(userAgent);
		auditNotificationDTO.setIpAddress(ipAddress);
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
		token.put("user_info", user);
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


//		String access_token = JWT.create()
//				.withSubject(user.getUsername())
//				.withExpiresAt(new Date(System.currentTimeMillis() + 10 * 60 * 1000))
//				.withClaim("permissions", user.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
//				.sign(algorithm);


//Encrypt/Sign the token
//		String access_token = JWT.create()
//				.withSubject(user.getUsername())
//				.withExpiresAt(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)) // 30 days expiration
//				// .withExpiresAt(new Date(System.currentTimeMillis() + 30 * 60 * 1000))
//				.withIssuer(request.getRequestURL().toString())
//				.withClaim("roles", user.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
//				.sign(algorithm);


//		Operator operator = operatorMapper.findByAuthEmail(user.getUsername());
//		operator.setPasswordEncrypt("");