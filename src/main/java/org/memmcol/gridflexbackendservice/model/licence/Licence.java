package org.memmcol.gridflexbackendservice.model.licence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Licence {
    private String licenceKey;
    private UUID organisationId;
    private String organisationName;
    private LocalDateTime issuedDate;
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;
    private String licenseType;
    private int maxMeters;
    private String hardwareFingerprint;
    private boolean active;
    private String hmacSignature;
}
