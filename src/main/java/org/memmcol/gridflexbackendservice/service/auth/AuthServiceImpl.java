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
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.util.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.memmcol.gridflexbackendservice.util.GenericHandler.getClientIp;
import static org.memmcol.gridflexbackendservice.util.handleValidUser.handleUserValidation;

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

	public Map<String, Object> logout() {
		ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
		AuditLog auditNotificationDTO = new AuditLog();
		try {
			String ipAddress = getClientIp(httpServletRequest);
			String userAgent = httpServletRequest.getHeader("User-Agent");

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

			auditNotificationDTO.setCreator(operator);
			auditNotificationDTO.setUserAgent(userAgent);
			auditNotificationDTO.setIpAddress(ipAddress);
			auditNotificationDTO.setDescription("Logged out");
			auditNotificationDTO.setType("auth");
			auditRepository.save(auditNotificationDTO);

			return ResponseMap.response(status.getSuccessCode(), "Logged out successfully", "");

		} catch (Exception exception) {
			log.error("Error occurred while logout: {}", exception.getMessage(), exception);
			errorLog.setDescription("Error occurred while logout");
			errorLog.setError_message(exception.getMessage());
			errorLog.setError(exception.toString());
			exceptionAuditRepository.save(errorLog);
			throw exception;
		}
	}


	@Transactional
	public Map<String, Object> handleForgetPassword(UserModel isOperator, String password) {
		AuditLog AuditLog = new AuditLog();
		String ipAddress = getClientIp(httpServletRequest);
		String userAgent = httpServletRequest.getHeader("User-Agent");
		try {

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
//			handleCacheUpdate(isOperator);
			AuditLog.setCreator(isOperator);
//			AuditLog.setCreatedUser(isOperator);
			AuditLog.setUserAgent(userAgent);
			AuditLog.setIpAddress(ipAddress);
			AuditLog.setDescription("Reset password");
			AuditLog.setType("auth");
			auditRepository.save(AuditLog);
			return ResponseMap.response(status.getSuccessCode(), "Password " + status.getUpdateDesc(), "");

		} catch (Exception exception) {
			ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
			log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
			exceptionErrorLogs.setDescription("Error occurred while changing operator password");
			exceptionErrorLogs.setError_message(exception.getMessage().trim());
			exceptionErrorLogs.setError(exception.toString().trim());
			exceptionAuditRepository.save(exceptionErrorLogs);
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

		String emailServiceUrl = "http://localhost:8081/smarte/email/api/send";

		try {
			restTemplate.postForEntity(emailServiceUrl, Map.of(
					"toAddress", username,
					"subject", "OTP Code",
					"message", "Your OTP code is: " + otp
			), Void.class);
		} catch (RestClientException emailException) {
			ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
			log.error("Failed to send OTP email to {}: {}", username, emailException.getMessage().trim(), emailException);
			exceptionErrorLogs.setDescription("Error occurred while generating OTP");
			exceptionErrorLogs.setError_message(emailException.getMessage().trim());
			exceptionErrorLogs.setError(emailException.toString().trim());
			exceptionAuditRepository.save(exceptionErrorLogs);
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
			ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
			log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
			exceptionErrorLogs.setDescription("Error occurred while verifying OTP");
			exceptionErrorLogs.setError_message(exception.getMessage().trim());
			exceptionErrorLogs.setError(exception.toString().trim());
			exceptionAuditRepository.save(exceptionErrorLogs);
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
			ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
			log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
			exceptionErrorLogs.setDescription("Error occurred while verifying OTP");
			exceptionErrorLogs.setError_message(exception.getMessage().trim());
			exceptionErrorLogs.setError(exception.toString().trim());
			exceptionAuditRepository.save(exceptionErrorLogs);
			throw exception;
		}
	}

	private void blacklistToken(String token, int expirySeconds) {
		authCache.put(token, true, expirySeconds, TimeUnit.SECONDS);
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