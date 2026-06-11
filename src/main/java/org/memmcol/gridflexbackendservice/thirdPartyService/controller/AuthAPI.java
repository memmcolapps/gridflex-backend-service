package org.memmcol.gridflexbackendservice.thirdPartyService.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.memmcol.gridflexbackendservice.exception.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.thirdPartyService.model.ClientLoginModel;
import org.memmcol.gridflexbackendservice.thirdPartyService.service.ThirdPartyAuthServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/client/auth")
@Tag(name = "Authentication", description = "Authentication Management APIs")
public class AuthAPI {

    @Autowired
    private ThirdPartyAuthServiceImpl authService;

    @Autowired
    private GlobalExceptionHandler exception;

    @PostMapping("/token")
    public ResponseEntity<?> token(@RequestBody ClientLoginModel request) {

        try {
            String token = authService.authenticate(
                    request
            );

            return ResponseEntity.ok(Map.of(
                    "access_token", token,
                    "token_type", "Bearer"
            ));

        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
