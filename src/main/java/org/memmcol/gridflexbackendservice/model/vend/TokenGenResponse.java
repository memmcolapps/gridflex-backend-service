package org.memmcol.gridflexbackendservice.model.vend;

import lombok.Data;

import java.util.List;

@Data
public class TokenGenResponse {
    private String code;
    private List<String> tokens;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<String> getTokens() {
        return tokens;
    }

    public void setTokens(List<String> tokens) {
        this.tokens = tokens;
    }
}

