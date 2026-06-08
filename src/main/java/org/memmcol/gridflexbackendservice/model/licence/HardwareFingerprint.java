package org.memmcol.gridflexbackendservice.model.licence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HardwareFingerprint {
    private String biosSerial;
    private String motherboardSerial;
    private String diskSerial;
    private String osSerial;
    private String hash;
}
