package org.memmcol.gridflexbackendservice.model.audit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogDto {
    private String id;
    private String type;
    private String username;
    private String email;
    private String groupPermission;
    private String activity;
    private String userAgent;
    private String ipAddress;
    private String reason;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timeStamp;

    public AuditLogDto(String id, String type, String username, String email, String groupPermission, String activity,
                       String userAgent, String ipAddress, LocalDateTime timeStamp, String reason) {
        this.id = id;
        this.type = type;
        this.username = username;
        this.email = email;
        this.groupPermission = groupPermission;
        this.activity = activity;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
        this.timeStamp = timeStamp;
        this.reason = reason;
    }

    public AuditLogDto() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGroupPermission() { return groupPermission; }
    public void setGroupPermission(String groupPermission) { this.groupPermission = groupPermission; }

    public String getActivity() { return activity; }
    public void setActivity(String activity) { this.activity = activity; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public LocalDateTime getTimeStamp() { return timeStamp; }
    public void setTimeStamp(LocalDateTime timeStamp) { this.timeStamp = timeStamp; }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}