package org.memmcol.gridflexbackendservice.util;

import java.io.Serializable;
import java.util.UUID;

public class GenericResp implements Serializable {
    String id;
    String message;
    Object data;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    //    public String getData() {
//        return data;
//    }
//
//    public void setData(String data) {
//        this.data = data;
//    }
}
