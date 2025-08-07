package org.memmcol.gridflexbackendservice.service.auth;

import java.util.Map;
import java.util.UUID;

public interface AuthService {

	Map<String, Object> logout();

//	Map<String, Object> forgetPassword(String username, String password, String retype_password);

	Map<String, Object> generateOtp(String username);

	Map<String, Object> verifyOtp(String username, String otp, String password, String retypePassword);

    Map<String, Object> profile(UUID userId);
}
