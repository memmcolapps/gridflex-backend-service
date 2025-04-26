package org.memmcol.gridflexbackendservice.service;

import java.util.Map;

public interface AuthService {

	Map<String, Object> logout(String token, int expirySeconds, String username);

//	Map<String, Object> forgetPassword(String username, String password, String retype_password);

	Map<String, Object> generateOtp(String username);

	Map<String, Object> verifyOtp(String username, String otp, String password, String retypePassword);
}
