package org.memmcol.gridflexbackendservice.model.setup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class ChangeState {
    private String clientName;
    private Boolean status;
    private UUID orgId;
}
