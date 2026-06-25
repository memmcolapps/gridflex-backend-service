package org.memmcol.gridflexbackendservice.components;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.memmcol.gridflexbackendservice.model.licence.Licence;
import org.memmcol.gridflexbackendservice.model.licence.LicenceValidationResult;
import org.memmcol.gridflexbackendservice.service.licence.LicenceValidator;
import org.memmcol.gridflexbackendservice.util.LicenceFileUtil;
import org.memmcol.gridflexbackendservice.util.LicenceSecurityConstants;
import org.memmcol.gridflexbackendservice.util.LicenceSignerUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class LicenceInterceptor implements HandlerInterceptor {

    @Value("${gridflex.data.dir}")
    private String dataDir;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String path = request.getServletPath();

        // Allow licence-related endpoints (so users can validate/upload/save even with invalid licence)
        if (path.startsWith("/licence/service")) {
            return true;
        }

        // Allow auth endpoints
        if (path.startsWith("/auth/service")) {
            return true;
        }

        // Allow swagger/actuator
        if (path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars")
                || path.startsWith("/actuator")) {
            return true;
        }

        // Allow static resources
        if (path.startsWith("/uploads")) {
            return true;
        }

        // Extract token from Authorization header
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            // Let CustomAuthorizationFilter handle missing token
            return true;
        }

        String token = authorizationHeader.substring("Bearer ".length());

        try {
            // Decode JWT to get orgId
            Algorithm algorithm = Algorithm.HMAC256("secret".getBytes());
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT decodedJWT = verifier.verify(token);

            String orgIdString = decodedJWT.getClaim("orgId").asString();
            if (orgIdString == null) {
                blockAccess(response, "Organisation ID not found in token");
                return false;
            }

            UUID orgId = UUID.fromString(orgIdString);

            // Read licence file
            Licence licence = LicenceFileUtil.readLicenceFile(dataDir, orgId);

            if (licence == null) {
//                blockAccess(response, "Licence not found");
//                return false;
                return true;
            }

            // Verify HMAC signature to detect tampering
            if (licence.getHmacSignature() != null) {
                String savedSignature = licence.getHmacSignature();
                licence.setHmacSignature(null);
                ObjectMapper mapper = new ObjectMapper();
                String licenceJson = mapper.writeValueAsString(licence);
                licence.setHmacSignature(savedSignature);
                boolean signatureValid = LicenceSignerUtil.verify(licenceJson, savedSignature, LicenceSecurityConstants.getHmacKey());
                if (!signatureValid) {
                    blockAccess(response, "Licence has been tampered with");
                    return false;
                }
            }

            // Validate licence (pass 0 for meter count — meter limits enforced at service layer)
            LicenceValidationResult result = LicenceValidator.validateWithLimits(licence, 0);

            if (!result.isValid()) {
                blockAccess(response, result.getMessage());
                return false;
            }

            // Add expiry warning header if licence expires within 30 days
            if (result.getWarningMessage() != null) {
                response.setHeader("X-Licence-Warning", result.getWarningMessage());
            }

            return true;

        } catch (JWTVerificationException e) {
            // Let CustomAuthorizationFilter handle JWT errors
            return true;
        } catch (Exception e) {
            blockAccess(response, "Licence validation failed: " + e.getMessage());
            return false;
        }
    }

    private void blockAccess(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> error = new HashMap<>();
        error.put("responsecode", "403");
        error.put("responsedesc", "Access denied: " + message);
        error.put("responsedata", "");

        new ObjectMapper().writeValue(response.getOutputStream(), error);
    }
}
