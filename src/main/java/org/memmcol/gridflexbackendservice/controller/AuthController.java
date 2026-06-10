package org.memmcol.gridflexbackendservice.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.memmcol.gridflexbackendservice.service.auth.AuthService;
import org.memmcol.gridflexbackendservice.exception.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.exception.GlobalExceptionHandler.SQLServerException;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.config.ResponseProperties;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth/service")
@Tag(name = "Auth", description = "Auth Management APIs")
public class AuthController {

    @Autowired private AuthService service;

    @Autowired private ResponseProperties status;

    @Autowired private GlobalExceptionHandler exception;

//    @PostMapping("/logout")
//    public ResponseEntity<?> logout(@RequestParam String username, HttpServletRequest request) {
//        String token = request.getHeader("Authorization");
//        try {
//            if (token != null && token.startsWith("Bearer ")) {
//                token = token.substring(7); // Remove "Bearer "
//
//                String finalToken = token;
//                Map<String, Object> result = service.logout(finalToken, 1800, username);
//                // Return the map wrapped in ResponseEntity
//                return ResponseEntity.ok(result);
//            }
//            Map<String, Object> errorResponse = ResponseMap.response(HttpStatus.UNAUTHORIZED.toString(), "Invalid Token", "");
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
//
//        } catch (SQLServerException e) {
//            return handleException(e);
//        }
//    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        try {
            Map<String, Object> result = service.logout();
            return ResponseEntity.ok(result);

        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        } catch (Exception ex) {
            // Catch other errors (optional)
            Map<String, Object> errorResponse = ResponseMap.response(
                    HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                    "An error occurred during logout",
                    ex.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
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
    public ResponseEntity<?> verifyOtp(@RequestParam String username, @RequestParam String password, @RequestParam String otp) {
        try {
        Map<String, Object> result = service.verifyOtp(username, otp, password);
        return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile(@RequestParam UUID userId) {
        try {
            Map<String, Object> result = service.profile(userId);
            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        try {
            return ResponseEntity.ok("Hello World! Welcome to GridFlex");
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    private ResponseEntity<Map<String, Object>> handleException(SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}