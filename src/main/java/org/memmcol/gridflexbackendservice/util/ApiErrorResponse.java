package org.memmcol.gridflexbackendservice.util;

import io.swagger.v3.oas.annotations.media.Schema;

public class ApiErrorResponse {

    @Schema(example = "100")
    private String responsecode;

    @Schema(example = "An unexpected error occurred: Client not found")
    private String responsedesc;

    @Schema(example = "")
    private String responsedata;

    // getters and setters
}
