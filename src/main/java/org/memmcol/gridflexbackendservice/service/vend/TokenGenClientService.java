package org.memmcol.gridflexbackendservice.service.vend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.memmcol.gridflexbackendservice.components.GenericHandler;
import org.memmcol.gridflexbackendservice.model.audit.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.memmcol.gridflexbackendservice.model.vend.TokenErrorResponse;
import org.memmcol.gridflexbackendservice.model.vend.TokenGenRequest;
import org.memmcol.gridflexbackendservice.model.vend.TokenGenResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class TokenGenClientService {

    @Autowired
    private GenericHandler genericHandler;

    private final WebClient webClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TokenGenClientService(
            @Qualifier("tokenGenWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public TokenGenResponse generateToken(TokenGenRequest request, String url, String tokenType) {

        request.setMeterNo("62124022443");
        request.setSgc(600849);
        request.setTosgc(600849);
        // Print the payload
        try {
            String payload = objectMapper.writeValueAsString(request);
            System.out.println("Request payload: " + payload);
        } catch (Exception e) {
            System.out.println("Failed to serialize request payload: " + e.getMessage());
        }
        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(TokenErrorResponse.class)
                                .map(err -> new RuntimeException(
                                        err.getErrors().get(0).getTitle()
                                ))
                )
                // READ AS STRING FIRST
                .bodyToMono(String.class)
                .map(rawResponse -> {
                    // Print the raw response for debugging
                    System.out.println("Raw response: " + rawResponse);
                    return parseResponse(rawResponse, tokenType);
                })
                .block(); // OK because service is transactional
    }


    private TokenGenResponse parseResponse(String rawResponse, String tokenType) {

        String msg = switch (tokenType.toLowerCase()) {
            case "credit-token" -> "Generating credit token service failed";
            case "kct" -> "Generating kct token service failed";
            case "clear-tamper" -> "Generating clear tamper token service failed";
            case "clear-credit" -> "Generating clear credit token service failed";
            case "compensation" -> "Generating compensation token service failed";
            default -> "Generating token service failed";
        };

        try {
            // Properly unwrap JSON string if needed
            if (rawResponse.startsWith("\"") && rawResponse.endsWith("\"")) {
                rawResponse = objectMapper.readValue(rawResponse, String.class);
            }

            TokenGenResponse response =
                    objectMapper.readValue(rawResponse, TokenGenResponse.class);

            // Business-level error (API returns 200 but not success)
            if (!"SUCCESS".equalsIgnoreCase(response.getCode())) {
                String errorMsg =
                        (response.getTokens() != null && !response.getTokens().isEmpty())
                                ? response.getTokens().get(0)
                                : "Token generation failed";

                throw new RuntimeException(errorMsg);
            }

            // ✅ SUCCESS
            return response;

        } catch (Exception ex) {
            System.out.println("Invalid token generation response: " + ex.getMessage());
            genericHandler.logIncidentReport(msg);
            genericHandler.logAndSaveException(ex, msg);
            throw new RuntimeException("Invalid token generation response: " + rawResponse, ex);
        }
    }

//    private TokenGenResponse parseResponse(String rawResponse, String tokenType) {
//        String msg;
//        switch(tokenType.toLowerCase()) {
//            case "credit-token": msg = "Generating credit token service failed"; break;
//            case "kct": msg = "Generating kct token service failed"; break;
//            case "clear-tamper": msg = "Generating clear tamper token service failed"; break;
//            case "clear-credit": msg = "Generating clear credit token service failed"; break;
//            case "compensation": msg = "Generating compensation token service failed"; break;
//            default: msg = "Generating token service failed"; break;
//        }
//
//        try {
//            // If rawResponse is quoted JSON, unquote it
//            if (rawResponse.startsWith("\"") && rawResponse.endsWith("\"")) {
//                rawResponse = rawResponse.substring(1, rawResponse.length() - 1)
//                        .replace("\\\"", "\"");
//            }
//
//            // Check if response contains errors first
//            if (rawResponse.contains("\"errors\"")) {
//                TokenErrorResponse error = objectMapper.readValue(rawResponse, TokenErrorResponse.class);
//                String errorMsg = "Unknown error";
//                if (error.getErrors() != null && !error.getErrors().isEmpty()) {
//                    errorMsg = error.getErrors().get(0).getTitle();
//                }
//                throw new RuntimeException(errorMsg);
//            }
//
//            TokenGenResponse response =
//                    objectMapper.readValue(rawResponse, TokenGenResponse.class);
//
//            // LOGICAL error handling (important)
//            if (!"SUCCESS".equalsIgnoreCase(response.getCode())) {
//                String errorMsg = (response.getTokens() != null && !response.getTokens().isEmpty())
//                        ? response.getTokens().get(0)
//                        : "Unknown token generation error";
//
//                throw new RuntimeException(errorMsg);
//            }
//
//            // SUCCESS
//            return response;
//
////            // Parse into TokenGenResponse
////            return objectMapper.readValue(rawResponse, TokenGenResponse.class);
//
//        } catch (Exception ex) {
//            System.out.println("Invalid token generation response: " + ex.getMessage());
//            genericHandler.logIncidentReport(msg);
//            genericHandler.logAndSaveException(ex, msg);
//            throw new RuntimeException("Invalid token generation response: " + rawResponse, ex);
//        }
//    }

///-----------------------
//    private TokenGenResponse parseResponse(String rawResponse, String tokenType) {
//        System.out.println("::::::::::::::::::::pppppppppppp:::::::::::");
//        String msg = "";
//        if("credit-token".equalsIgnoreCase(tokenType)) {
//            msg = "Generating credit token service failed";
//        }
//        else if("kct".equalsIgnoreCase(tokenType)) {
//            msg = "Generating kct token service failed";
//        }
//        else if("clear-tamper".equalsIgnoreCase(tokenType)) {
//            msg = "Generating clear tamper token service failed";
//        }
//        else if("clear-credit".equalsIgnoreCase(tokenType)) {
//            msg = "Generating clear credit token service failed";
//        }
//        else if("compensation".equalsIgnoreCase(tokenType)) {
//            msg = "Generating compensation token service failed";
//        } else {
//            msg = "Generating token service failed";
//        }
//        System.out.println("::::::::::::::::::::pppppppppppp");
//        try {
//            return objectMapper.readValue(rawResponse, TokenGenResponse.class);
//
//        } catch (Exception ex) {
//            System.out.println("Invalid token generation response: "+ex.getMessage());
//            genericHandler.logIncidentReport(msg);
//            genericHandler.logAndSaveException(ex, msg);
//            throw new RuntimeException("Invalid token generation response: " + rawResponse, ex);
//        }
//    }
}
