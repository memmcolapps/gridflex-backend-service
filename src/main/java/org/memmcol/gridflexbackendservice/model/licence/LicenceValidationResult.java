package org.memmcol.gridflexbackendservice.model.licence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenceValidationResult {
    private boolean valid;
    private String message;
    private String warningMessage;
    private Licence licence;
}
