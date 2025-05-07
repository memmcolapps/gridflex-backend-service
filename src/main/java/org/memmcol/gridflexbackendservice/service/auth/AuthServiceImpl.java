package org.memmcol.gridflexbackendservice.service.auth;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.model.*;
import org.memmcol.gridflexbackendservice.repository.AuditRepository;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.util.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Transactional
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

	private final Random random = new SecureRandom();

	String user = "Operator";

	private final IMap<String, Object> authCache;
	private final IMap<String, Object> auditCache;
	private final IMap<String, String> otpCache;
	private final IMap<String, Boolean> verifiedUsers;

	public AuthServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
		this.authCache = hazelcastInstance.getMap("auth-Cache");
		this.auditCache = hazelcastInstance.getMap("audit-Cache");
		this.otpCache = hazelcastInstance.getMap("otp-Cache");
		this.verifiedUsers = hazelcastInstance.getMap("verified-Users");

	}

	@Override
	public Map<String, Object> logout(String token, int expirySeconds, String username) {
		AuditLog auditNotificationDTO = new AuditLog();
		try {
			UserDTO isOperatorExist = operatorMapper.findAuthByUserEmail(username);
			if (isOperatorExist == null) {
				return ResponseMap.response(status.getNotFoundCode(), user + status.getNotFoundDesc(), "");
			}
			isOperatorExist.getUser().setPassword("");
			operatorMapper.updateLogoutState(username);
			blacklistToken(token, expirySeconds);
			auditNotificationDTO.setCreator(isOperatorExist.getUser());
			auditNotificationDTO.setDescription(username + " Logged out");
			auditNotificationDTO.setType("auth");
			removeFromCache();
//			authCache.remove("dashboard");
			auditRepository.save(auditNotificationDTO);
			return ResponseMap.response(status.getSuccessCode(), "Logged out successfully", "");
		} catch (Exception exception) {
			ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
			log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
			exceptionErrorLogs.setDescription("Error occurred while logout");
			exceptionErrorLogs.setError_message(exception.getMessage().trim());
			exceptionErrorLogs.setError(exception.toString().trim());
			exceptionAuditRepository.save(exceptionErrorLogs);
			throw exception;
		}

	}


	public Map<String, Object> handleForgetPassword(String username, String password) {
		AuditLog AuditLog = new AuditLog();
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		try {
			String email = (authentication != null) ? authentication.getName() : "Unknown";
			UserDTO isUser = operatorMapper.findAuthByUserEmail(email);
			if (!isUser.getUser().getStatus()) {
				throw new LockedException("User is blocked");
			}

			UserDTO isOperator = operatorMapper.findAuthByUserEmail(username);
			if (isOperator == null) {
				return ResponseMap.response(status.getExistCode(), user + " " + status.getExistDesc(), "");
			}
			if(!Objects.equals(isUser.getUser().getEmail(), isOperator.getUser().getEmail())){
				return ResponseMap.response(status.getNotFoundCode(), "Do not have access to change an operator password", "");
			}
			if (!verifiedUsers.containsKey(username)) {
				return ResponseMap.response(status.getNotFoundCode(), "OTP verification required before password change", "");
			}

			int passwordChangeResult = operatorMapper.resetPassword(username, passwordEncoder.encode(password));
			if (passwordChangeResult == 0) {
				return ResponseMap.response(status.getBlockCode(), user + " " + status.getBlockFailureDesc(), "");
			}
			isOperator.getUser().setPassword("");
			isUser.getUser().setPassword("");
			// Remove OTP verification from cache after successful password reset
			verifiedUsers.remove(username);
//			handleCacheUpdate(isOperator);
			AuditLog.setCreator(isUser.getUser());
			AuditLog.setCreatedUser(isOperator.getUser());
			AuditLog.setDescription(isOperator.getUser().getEmail() + " Reset password");
			AuditLog.setType("operator");
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

	public  Map<String, Object>  verifyOtp(String email, String otp, String password, String retypePassword) {
		try {
			if(!password.equals(retypePassword)){
				return ResponseMap.response(status.getNotFoundCode(), "Passwords do not match", "");
			}
			String storedOtp = otpCache.get(email);

			if (storedOtp != null && storedOtp.equals(otp)) {
				otpCache.remove(email);

				verifiedUsers.put(email, true, 2, TimeUnit.MINUTES);

				return handleForgetPassword(email, password);

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