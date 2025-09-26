package org.memmcol.gridflexbackendservice.components;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.memmcol.gridflexbackendservice.mapper.UserMapper;
import org.memmcol.gridflexbackendservice.model.audit.ExceptionErrorLogs;
import org.memmcol.gridflexbackendservice.model.audit.IncidentReport;
import org.memmcol.gridflexbackendservice.repository.ExceptionAuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class GenericHandler {

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;

    @Autowired
    private UserMapper userMapper;

    public static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAndSaveException(Exception exception, String actionDescription) {

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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logIncidentReport(String msg) {
        IncidentReport incidentReport = new IncidentReport();
        incidentReport.setMessage(msg);
        incidentReport.setType("auto");
        incidentReport.setStatus(false);
        userMapper.insertIncidentReport(incidentReport);
    }
}
