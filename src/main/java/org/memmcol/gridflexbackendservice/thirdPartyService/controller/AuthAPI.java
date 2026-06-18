package org.memmcol.gridflexbackendservice.thirdPartyService.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.memmcol.gridflexbackendservice.exception.GlobalExceptionHandler;
import org.memmcol.gridflexbackendservice.thirdPartyService.model.ClientLoginModel;
import org.memmcol.gridflexbackendservice.thirdPartyService.service.ThirdPartyAuthServiceImpl;
import org.memmcol.gridflexbackendservice.util.ApiErrorResponse;
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

    @SecurityRequirement(name = "apiKeyAuth")
    @Operation(
            summary = "Generate Access Token",
            description = "Authenticates client and returns JWT token."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Success",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "access_token": "eyJhbGciOiJIUzI1NiIs...",
                                  "token_type": "Bearer"
                                }
                                """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "responsecode": "401",
                                  "responsedesc": "Missing or invalid API key header: custom",
                                  "responsedata": ""
                                }
                                """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "responsecode": "102",
                                  "responsedesc": "We encountered a problem while processing your request, please try a gain later",
                                  "responsedata": ""
                                }
                                """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "responsecode": "100",
                                  "responsedesc": "An unexpected error occurred: Client not found",
                                  "responsedata": ""
                                }
                                """
                            )
                    )
            )
    })
    @PostMapping("/token")
    public ResponseEntity<?> token(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Client login credentials",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ClientLoginModel.class),
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "clientId": "client123",
                                      "clientSecret": "secret123"
                                    }
                                    """
                            )
                    )
            )
            @RequestBody ClientLoginModel request) {

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
