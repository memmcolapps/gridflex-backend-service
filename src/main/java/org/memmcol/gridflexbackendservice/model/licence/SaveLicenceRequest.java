package org.memmcol.gridflexbackendservice.model.licence;

import lombok.Data;

import java.util.UUID;

@Data
public class SaveLicenceRequest {
    private UUID orgId;
    private String encryptedLicence;
}
