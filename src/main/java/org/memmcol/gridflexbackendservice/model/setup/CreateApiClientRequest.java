package org.memmcol.gridflexbackendservice.model.setup;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateApiClientRequest {
    private String clientName;
    private UUID orgId;
    private List<String> scopes;
}
