package org.memmcol.gridflexbackendservice.model.vend;

import java.util.List;

public class TokenErrorResponse {
    private List<ApiTokenError> errors;
    private Object meta;

    public List<ApiTokenError> getErrors() {
        return errors;
    }

    public void setErrors(List<ApiTokenError> errors) {
        this.errors = errors;
    }

    public Object getMeta() {
        return meta;
    }

    public void setMeta(Object meta) {
        this.meta = meta;
    }
}
