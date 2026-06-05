package org.memmcol.gridflexbackendservice.model.setup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class ApiClientResponse {
    private String clientId;
    private String clientSecret;
    private String message;
}
