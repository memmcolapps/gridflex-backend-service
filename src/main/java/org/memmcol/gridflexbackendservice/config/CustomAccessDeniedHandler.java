package org.memmcol.gridflexbackendservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        Map<String, String> errorMessage = new HashMap<>();
        errorMessage.put("responsecode", String.valueOf(HttpServletResponse.SC_UNAUTHORIZED));
        errorMessage.put("responsedesc", "Access Denied: You do not have the required permissions");
        errorMessage.put("responsedata", "");

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        // Convert Map to JSON string
        String jsonResponse = objectMapper.writeValueAsString(errorMessage);
        response.getWriter().write(jsonResponse);
    }
}
