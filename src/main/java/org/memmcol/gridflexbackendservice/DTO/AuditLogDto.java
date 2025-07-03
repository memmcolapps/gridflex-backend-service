package org.memmcol.gridflexbackendservice.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogDto {
    private String username;
    private String email;
    private String groupPermission;
    private String activity;
    private String userAgent;
    private String ipAddress;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date timeStamp;

    public AuditLogDto(String username, String email, String groupPermission, String activity,
                       String userAgent, String ipAddress, Date timeStamp) {
        this.username = username;
        this.email = email;
        this.groupPermission = groupPermission;
        this.activity = activity;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
        this.timeStamp = timeStamp;
    }

    public AuditLogDto() {}

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

    public Date getTimeStamp() { return timeStamp; }
    public void setTimeStamp(Date timeStamp) { this.timeStamp = timeStamp; }
}