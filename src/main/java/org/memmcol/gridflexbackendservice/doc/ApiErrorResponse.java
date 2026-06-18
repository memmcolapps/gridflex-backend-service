package org.memmcol.gridflexbackendservice.doc;

import io.swagger.v3.oas.annotations.media.Schema;

public class ApiErrorResponse {

    @Schema(example = "100")
    private String responsecode;

    @Schema(example = "An unexpected error occurred: Client not found")
    private String responsedesc;

    @Schema(example = "")
    private String responsedata;

    public String getResponsecode() {
        return responsecode;
    }

    public void setResponsecode(String responsecode) {
        this.responsecode = responsecode;
    }

    public String getResponsedata() {
        return responsedata;
    }

    public void setResponsedata(String responsedata) {
        this.responsedata = responsedata;
    }

    public String getResponsedesc() {
        return responsedesc;
    }

    public void setResponsedesc(String responsedesc) {
        this.responsedesc = responsedesc;
    }
}
