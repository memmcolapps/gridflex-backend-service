package org.memmcol.gridflexbackendservice.exception;

import java.util.Map;

public class OdysseyServiceException extends RuntimeException {

    private final Map<String, Object> response;

    public OdysseyServiceException(String message, Map<String, Object> response) {
        super(message);
        this.response = response;
    }

    public Map<String, Object> getResponse() {
        return response;
    }
}
