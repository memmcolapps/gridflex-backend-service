package org.memmcol.gridflexbackendservice.util;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class StatusConstants {

    // Response Codes
    @Value("${success.code}")
    private String successCode;

    @Value("${fail.code}")
    private String failCode;

    @Value("${token.code}")
    private String tokenCode;

    @Value("${reg.code}")
    private String regCode;

    @Value("${payload.code}")
    private String payloadCode;

    @Value("${block.code}")
    private String blockCode;

    @Value("${update.code}")
    private String updateCode;

    @Value("${del.code}")
    private String delCode;

    @Value("${get.code}")
    private String getCode;

    @Value("${exist.code}")
    private String existCode;

    @Value("${notfound.code}")
    private String notFoundCode;

    @Value("${token.notfound.code}")
    private String tokenNotFoundCode;

    @Value("${role.code}")
    private String roleCode;

    @Value("${role.del.code}")
    private String roleDelCode;

    @Value("${role.update.code}")
    private String roleUpdateCode;

    // Response Descriptions
    @Value("${payload.desc}")
    private String payloadDesc;

    @Value("${success.desc}")
    private String successDesc;

    @Value("${fail.desc}")
    private String failDesc;

    @Value("${token.desc}")
    private String tokenDesc;

    @Value("${token.notfound.desc}")
    private String tokenNotFoundDesc;

    @Value("${reg.desc}")
    private String regDesc;

    @Value("${notfound.desc}")
    private String notFoundDesc;

    @Value("${update.desc}")
    private String updateDesc;

    @Value("${del.desc}")
    private String delDesc;

    @Value("${get.desc}")
    private String getDesc;

    @Value("${exist.desc}")
    private String existDesc;

    @Value("${block.fail.desc}")
    private String blockFailDesc;

    @Value("${reg.fail.desc}")
    private String regFailDesc;

    @Value("${update.fail.desc}")
    private String updateFailDesc;

    @Value("${del.fail.desc}")
    private String delFailDesc;

    @Value("${get.fail.desc}")
    private String getFailDesc;
}
