package org.memmcol.gridflexbackendservice.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResponseProperties {

    @Value("${success.code}")
    private String successCode;

    @Value("${token.code}")
    private String tokenCode;
    
    @Value("${token.notfound.code}")
    private String tokenNotfoundCode;
    
    @Value("${reg.code}")
    private String regCode;

    @Value("${update.code}")
    private String updateCode;

    @Value("${del.code}")
    private String deleteCode;

    @Value("${get.code}")
    private String getCode;

    @Value("${exist.code}")
    private String existCode;

    @Value("${notfound.code}")
    private String notFoundCode;
    
    @Value("${role.code}")
    private String roleCode;

    @Value("${role.del.code}")
    private String roleDelCode;
    
    @Value("${role.update.code}")
    private String roleUpdateCode;
    
    @Value("${payload.code}")
    private String payloadCode;
    
    @Value("${block.code}")
    private String blockCode;

    @Value("${fail.code}")
    private String failCode;
    
    // Descriptions
    @Value("${token.desc}")
    private String tokenDesc;

    @Value("${reg.desc}")
    private String regDescription;

    @Value("${update.desc}")
    private String updateDescription;

    @Value("${del.desc}")
    private String deleteDescription;

    @Value("${get.desc}")
    private String getDescription;
    
   
    @Value("${exist.desc}")
    private String existDescription;

    @Value("${notfound.desc}")
    private String notFoundDescription;
    
    @Value("${token.notfound.desc}")
    private String tokenNotFoundDescription;

    // Failure Descriptions
    @Value("${reg.fail.desc}")
    private String regFailureDescription;

    @Value("${update.fail.desc}")
    private String updateFailureDescription;

    @Value("${del.fail.desc}")
    private String deleteFailureDescription;
    
    @Value("${get.fail.desc}")
    private String getFailureDescription;
    
    @Value("${payload.desc}")
    private String getPayloadDescription;

    @Value("${block.fail.desc}")
    private String blockDescription;

    @Value("${fail.desc}")
    private String failDescription;

    // Getters
    public String getSuccessCode() {
        return successCode;
    }
    
    public String getTokenCode() {
        return tokenCode;
    }
    
    public String getTokenNotfoundCode() {
        return tokenNotfoundCode;
    }

    public String getRegCode() {
        return regCode;
    }

    public String getUpdateCode() {
        return updateCode;
    }

    public String getDeleteCode() {
        return deleteCode;
    }

    public String getCode() {
        return getCode;
    }

    public String getExistCode() {
        return existCode;
    }
    
    public String getRoleCode() {
        return roleCode;
    }
    
    public String getRoleDelCode() {
        return roleDelCode;
    }
    
    public String getRoleUpdateCode() {
        return roleUpdateCode;
    }

    public String getNotFoundCode() {
        return notFoundCode;
    }

    public String getPayloadCode() {
        return payloadCode;
    }
    
    public String getBlockCode() {
        return blockCode;
    }
    
    public String getPayloadDesc() {
        return getPayloadDescription;
    }
    
    public String getTokenDesc() {
        return tokenDesc;
    }
    
    public String getTokenNotFoundDesc() {
        return tokenNotFoundDescription;
    }
    public String getRegDesc() {
        return regDescription;
    }

    public String getUpdateDesc() {
        return updateDescription;
    }

    public String getDeleteDesc() {
        return deleteDescription;
    }

    public String getDesc() {
        return getDescription;
    }

    public String getExistDesc() {
        return existDescription;
    }

    public String getNotFoundDesc() {
        return notFoundDescription;
    }

    public String getRegFailureDesc() {
        return regFailureDescription;
    }

    public String getUpdateFailureDesc() {
        return updateFailureDescription;
    }

    public String getDeleteFailureDesc() {
        return deleteFailureDescription;
    }

    public String getFailureDesc() {
        return getFailureDescription;
    }
    
    public String getBlockFailureDesc() {
        return blockDescription;
    }

    public String getFailDesc() {
        return failDescription;
    }

    public String getFailCode() {
        return failCode;
    }
}
