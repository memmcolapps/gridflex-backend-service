package org.memmcol.gridflexbackendservice.service.auth;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.model.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.Operator;
import org.memmcol.gridflexbackendservice.model.OperatorAudit;
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
		OperatorAudit auditNotificationDTO = new OperatorAudit();
		try {
			Operator isOperatorExist = operatorMapper.findByAuthEmail(username);
			if (isOperatorExist == null) {
				return ResponseMap.response(status.getNotFoundCode(), user + status.getNotFoundDesc(), "");
			}
			isOperatorExist.setPasswordEncrypt("");
			operatorMapper.updateLogoutState(username);
			blacklistToken(token, expirySeconds);
			auditNotificationDTO.setCreator(isOperatorExist);
			auditNotificationDTO.setDescription(username + " Logged out");
			auditNotificationDTO.setType("auth");
			for (String key : auditCache.keySet()) {
				if (key.startsWith("grid_flex_audit_log_page_")) {
					auditCache.remove(key);
				}
			}
//			authCache.remove("dashboard");
			auditRepository.save(auditNotificationDTO);
			return ResponseMap.response(status.getSuccessCode(), "Logged out successfully", "");
		} catch (Exception exception) {
			ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
			log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
			exceptionErrorLogs.setDescription("Error occurred while logout");
			exceptionErrorLogs.setError_message(exception.getMessage());
			exceptionErrorLogs.setError(exception);
			exceptionAuditRepository.save(exceptionErrorLogs);
			throw exception;
		}

	}


	public Map<String, Object> handleForgetPassword(String username, String password) {

		OperatorAudit operatorAudit = new OperatorAudit();
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		try {
			String email = (authentication != null) ? authentication.getName() : "Unknown";
			Operator isUser = operatorMapper.GetOperator(email);
			if (!isUser.isUstate()) {
				throw new LockedException("User is blocked");
			}

//			if(!Objects.equals(password, retype_password)){
//				return ResponseMap.response(status.getNotFoundCode(), "Passwords do not match", "");
//			}

			Operator isOperator = operatorMapper.GetOperator(username);
			if (isOperator == null) {
				return ResponseMap.response(status.getExistCode(), user + " " + status.getExistDesc(), "");
			}
			if(!Objects.equals(isUser.getEmail(), isOperator.getEmail())){
				return ResponseMap.response(status.getNotFoundCode(), "Do not have access to change an operator password", "");
			}
			if (!verifiedUsers.containsKey(username)) {
				return ResponseMap.response(status.getNotFoundCode(), "OTP verification required before password change", "");
			}

			int passwordChangeResult = operatorMapper.resetPassword(username, passwordEncoder.encode(password));
			if (passwordChangeResult == 0) {
				return ResponseMap.response(status.getBlockCode(), user + " " + status.getBlockFailureDesc(), "");
			}
			isOperator.setPasswordEncrypt("");
			isUser.setPasswordEncrypt("");
			// Remove OTP verification from cache after successful password reset
			verifiedUsers.remove(username);
//			handleCacheUpdate(isOperator);
			operatorAudit.setCreator(isUser);
			operatorAudit.setCreatedOperator(isOperator);
			operatorAudit.setDescription(isOperator.getEmail() + " Reset password");
			operatorAudit.setType("operator");
			auditRepository.save(operatorAudit);
			return ResponseMap.response(status.getSuccessCode(), "Password " + status.getUpdateDesc(), "");

		} catch (Exception exception) {
			ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
			log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
			exceptionErrorLogs.setDescription("Error occurred while changing operator password");
			exceptionErrorLogs.setError_message(exception.getMessage());
			exceptionErrorLogs.setError(exception);
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
			log.error("Failed to send OTP email to {}: {}", username, emailException.getMessage(), emailException);
			exceptionErrorLogs.setDescription("Error occurred while generating OTP");
			exceptionErrorLogs.setError_message(emailException.getMessage());
			exceptionErrorLogs.setError(emailException);
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
			log.error("Error occurred while [ACTION]: {}", exception.getMessage(), exception);
			exceptionErrorLogs.setDescription("Error occurred while verifying OTP");
			exceptionErrorLogs.setError_message(exception.getMessage());
			exceptionErrorLogs.setError(exception);
			exceptionAuditRepository.save(exceptionErrorLogs);
			throw exception;
		}
	}

	private void blacklistToken(String token, int expirySeconds) {
		authCache.put(token, true, expirySeconds, TimeUnit.SECONDS);
	}

}