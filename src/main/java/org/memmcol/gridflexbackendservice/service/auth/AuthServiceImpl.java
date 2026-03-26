package org.memmcol.gridflexbackendservice.service.auth;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
//import org.memmcol.gridflexbackendservice.util.HandleCatchError;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.memmcol.gridflexbackendservice.components.HandleValidUser.handleUserValidation;

@Service
public class AuthServiceImpl implements AuthService {
	private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
	@Autowired
	private AuthMapper operatorMapper;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ResponseProperties status;

	@Autowired
	private AuditRepository auditRepository;

	@Autowired
	private GenericHandler genericHandler;

	@Autowired
	private ExceptionAuditRepository exceptionAuditRepository;

	@Autowired private RestTemplate restTemplate;

	@Autowired
	private HttpServletRequest httpServletRequest;

	private final Random random = new SecureRandom();

	String user = "Operator";

	private final IMap<String, Object> authCache;
	private final IMap<String, Object> auditCache;
	private final IMap<String, String> otpCache;
	private final IMap<String, Boolean> verifiedUsers;
    @Autowired
    private AuthMapper authMapper;

	public AuthServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
		this.authCache = hazelcastInstance.getMap("authCache");
		this.auditCache = hazelcastInstance.getMap("auditCache");
		this.otpCache = hazelcastInstance.getMap("otpCache");
		this.verifiedUsers = hazelcastInstance.getMap("verifiedUsers");

	}

	// TODO
	public Map<String, Object> logout() {
		try {
			Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

			// Extract raw token without decoding
			String authorizationHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
			if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
				throw new RuntimeException("Authorization header is missing or malformed");
			}
			String rawToken = authorizationHeader.substring("Bearer ".length());

			UserModel operator = handleUserValidation();
			operator.setPassword("");
			operatorMapper.updateLogoutState(operator.getEmail());

			// Blacklist the raw token
			blacklistToken(rawToken, 1800);
//			handleAddCache(user);
			AuditLog auditLog = buildAuditLog(operator, "Logged out", "auth", metadata);
			auditRepository.save(auditLog);

			return ResponseMap.response(status.getSuccessCode(), "Logged out successfully", "");

		} catch (Exception exception) {
			log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
			genericHandler.logIncidentReport("Logout user service failed");
			genericHandler.logAndSaveException(exception, "user logout");
			throw exception;
		}
	}


	@Transactional
	public Map<String, Object> handleForgetPassword(UserModel isOperator, String password) {
		try {
			Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

//			UserModel um = handleUserValidation();

//			UserModel isOperator = operatorMapper.findAuthByUserEmail(username);
//
//			if (isOperator == null) {
//				return ResponseMap.response(status.getExistCode(), user + " " + status.getExistDesc(), "");
//			}
//			if(!Objects.equals(um.getEmail(), isOperator.getEmail())){
//				return ResponseMap.response(status.getNotFoundCode(), "Do not have access to change an operator password", "");
//			}
			if (!verifiedUsers.containsKey(isOperator.getEmail())) {
				return ResponseMap.response(status.getNotFoundCode(), "OTP verification required before password change", "");
			}

			int passwordChangeResult = operatorMapper.resetPassword(isOperator.getEmail(), passwordEncoder.encode(password));
			if (passwordChangeResult == 0) {
				return ResponseMap.response(status.getBlockCode(), user + " " + status.getBlockFailureDesc(), "");
			}
			isOperator.setPassword("");
//			um.setPassword("");
			// Remove OTP verification from cache after successful password reset
			verifiedUsers.remove(isOperator.getEmail());
			AuditLog auditLog = buildAuditLog(isOperator, "Reset password", "auth", metadata);
			auditRepository.save(auditLog);
////			handleCacheUpdate(isOperator);
			return ResponseMap.response(status.getSuccessCode(), "Password " + status.getUpdateDesc(), "");

		} catch (Exception exception) {
			log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
			genericHandler.logIncidentReport("Forget password service failed");
			genericHandler.logAndSaveException(exception, "changing operator password");
			throw exception;
		}
	}

	@Override
	public Map<String, Object> generateOtp(String username) {
		return handleGenerateOtp(username);
	}

	@Transactional
	public Map<String, Object>  handleGenerateOtp(String username) {
		String otp = String.format("%04d", random.nextInt(10000));

		String emailServiceUrl = "http://localhost:8084/api/send";

		try {
			restTemplate.postForEntity(emailServiceUrl, Map.of(
					"toAddress", username,
					"subject", "OTP Code",
					"message", "Your OTP code is: " + otp
			), Void.class);
		} catch (RestClientException emailException) {
			log.error("Failed to send OTP email to {}: {}", username, emailException.getMessage().trim(), emailException);
			genericHandler.logIncidentReport("OTP mailer service failed");
			genericHandler.logAndSaveException(emailException, "OTP mailer failed");
			throw emailException;
		}

		otpCache.put(username, otp);
		return ResponseMap.response(status.getSuccessCode(), "OTP generated and sent successfully", "");
	}

	@Transactional(readOnly = true)
	@Override
	public Map<String, Object> profile(UUID userId) {
		try {

			UserModel um = handleUserValidation();

			handleGenerateOtp(um.getEmail());

            UserModel user = operatorMapper.findAuthByUserId(userId, um.getOrgId());
			user.setPassword("");

			return ResponseMap.response(status.getNotFoundCode(), "User " + status.getDesc(), user);
		} catch (Exception exception){
			log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
			genericHandler.logIncidentReport("Fetching user service failed");
			genericHandler.logAndSaveException(exception, "fetching user ");
			throw exception;
		}
	}

	public  Map<String, Object>  verifyOtp(String email, String otp, String password) {
		try {
			UserModel isOperator = operatorMapper.findAuthByUserEmail(email);

			if (isOperator == null) {
				return ResponseMap.response(status.getExistCode(), user + " " + status.getExistDesc(), "");
			}
			String storedOtp = otpCache.get(email);

			if (storedOtp != null && storedOtp.equals(otp)) {
				otpCache.remove(email);

				verifiedUsers.put(email, true, 2, TimeUnit.MINUTES);

				return handleForgetPassword(isOperator, password);

			}
			return ResponseMap.response(status.getNotFoundCode(), "OTP verification failed", "");
		} catch (Exception exception){
			log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
			genericHandler.logIncidentReport("Verify OTP service failed");
			genericHandler.logAndSaveException(exception, "verifying OTP");
			throw exception;
		}
	}

	private void blacklistToken(String token, int expirySeconds) {
		authCache.put(token, true, expirySeconds, TimeUnit.SECONDS);
	}

	private AuditLog buildAuditLog(UserModel creator, String description, String type, Map<String, String> metadata) {
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

	private void removeFromCache() {
//		authCache.remove("dashboard");
//		for (String key : authCache.keySet()) {
//			if (key.startsWith("operators_")) {
//				authCache.remove(key);
//			}
//		}
		for (String key : auditCache.keySet()) {
			if (key.startsWith("audit_log_page_")) {
				auditCache.remove(key);
			}
		}
//		for (String key : sbcCache.keySet()) {
//			if (key.startsWith("breakers_")) {
//				sbcCache.remove(key);
//			}
//		}
	}

}