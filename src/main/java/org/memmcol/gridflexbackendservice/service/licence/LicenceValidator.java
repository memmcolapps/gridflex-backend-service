package org.memmcol.gridflexbackendservice.service.licence;

import org.memmcol.gridflexbackendservice.model.licence.Licence;
import org.memmcol.gridflexbackendservice.model.licence.LicenceValidationResult;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class LicenceValidator {

    private static final long EXPIRY_WARNING_DAYS = 7;

    private LicenceValidator() {
    }

    public static LicenceValidationResult validate(Licence licence) {
        return validateWithLimits(licence, 0);
    }

    public static LicenceValidationResult validateWithLimits(Licence licence, int currentMeters) {
        if (licence == null) {
            return LicenceValidationResult.builder()
                    .valid(false)
                    .message("Licence not found")
                    .build();
        }

        if (!licence.isActive()) {
            return LicenceValidationResult.builder()
                    .valid(false)
                    .message("Licence is inactive")
                    .licence(licence)
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(licence.getExpiryDate())) {
            return LicenceValidationResult.builder()
                    .valid(false)
                    .message("Licence has expired on " + licence.getExpiryDate())
                    .licence(licence)
                    .build();
        }

        if (licence.getMaxMeters() > 0 && currentMeters >= licence.getMaxMeters()) {
            return LicenceValidationResult.builder()
                    .valid(false)
                    .message("Meter limit reached (" + licence.getMaxMeters() + " max)")
                    .licence(licence)
                    .build();
        }

        long daysUntilExpiry = ChronoUnit.DAYS.between(now.toLocalDate(), licence.getExpiryDate().toLocalDate());
        String warningMessage = null;
        if (daysUntilExpiry <= EXPIRY_WARNING_DAYS) {
            warningMessage = "Licence expires in " + daysUntilExpiry + " day(s)";
        }

        return LicenceValidationResult.builder()
                .valid(true)
                .message("Licence is valid")
                .warningMessage(warningMessage)
                .licence(licence)
                .build();
    }

}
