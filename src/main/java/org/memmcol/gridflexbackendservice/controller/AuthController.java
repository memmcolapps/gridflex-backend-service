package org.memmcol.gridflexbackendservice.controller;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.memmcol.gridflexbackendservice.service.AuthService;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.util.GlobalExceptionHandler.SQLServerException;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.util.ResponseProperties;

import java.util.Map;

@RestController
@RequestMapping("/auth/service")
public class AuthController {

    @Autowired private AuthService service;

    @Autowired private ResponseProperties status;

    @Autowired private GlobalExceptionHandler exception;

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam String username, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7); // Remove "Bearer "

                String finalToken = token;
                Map<String, Object> result = service.logout(finalToken, 1800, username);
                // Return the map wrapped in ResponseEntity
                return ResponseEntity.ok(result);
            }
            Map<String, Object> errorResponse = ResponseMap.response(HttpStatus.UNAUTHORIZED.toString(), "Invalid Token", "");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);

        } catch (SQLServerException e) {
            return handleException(e);
        }
    }


    @PostMapping("/generate-otp")
    public ResponseEntity<?> generateOtp(@RequestParam String username) {
        try {
            Map<String, Object> result = service.generateOtp(username);
            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }

    }

    @PostMapping("/forget-password")
    public ResponseEntity<?> verifyOtp(@RequestParam String username, @RequestParam String password, @RequestParam String retype_password, @RequestParam String otp) {
        try {
        Map<String, Object> result = service.verifyOtp(username, otp, password, retype_password);
        return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    private ResponseEntity<Map<String, Object>> handleException(SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}