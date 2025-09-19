package org.memmcol.gridflexbackendservice.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.memmcol.gridflexbackendservice.service.band.BandServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class GenericHandler {
//    private static final Logger log = LoggerFactory.getLogger(GenericHandler.class);

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;

    public static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    public void logAndSaveException(Exception exception, String actionDescription) {
//        log.error("Error occurred while {} [ACTION]: {}", actionDescription, exception.getMessage(), exception);

        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        exceptionErrorLogs.setDescription("Error occurred while trying to " + actionDescription);
        exceptionErrorLogs.setError_message(exception.getMessage().trim());
        exceptionErrorLogs.setError(exception.toString().trim());

        exceptionAuditRepository.save(exceptionErrorLogs);
    }

    public Map<String, String> extractRequestMetadata(HttpServletRequest request) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("ipAddress", getClientIp(request));
        metadata.put("userAgent", request.getHeader("User-Agent"));
        metadata.put("endpoint", request.getRequestURI());
        metadata.put("httpMethod", request.getMethod());
        return metadata;
    }

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

}
