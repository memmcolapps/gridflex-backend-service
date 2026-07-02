package org.memmcol.gridflexbackendservice.service.licence;

import org.memmcol.gridflexbackendservice.model.licence.Licence;
import org.memmcol.gridflexbackendservice.model.licence.LicenceValidationResult;
import org.memmcol.gridflexbackendservice.util.TrustedTimeProvider;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class LicenceValidator {

    private static final long EXPIRY_WARNING_DAYS = 30;

    private LicenceValidator() {
    }

    public static LicenceValidationResult validate(Licence licence) {
        return validateWithLimits(licence, 0);
    }

    private static final int METER_COUNTDOWN_WARNING_THRESHOLD = 30;

    public static LicenceValidationResult validateWithLimits(Licence licence, int currentMeters) {
        if (licence == null) {
            return LicenceValidationResult.builder()
                    .valid(false)
                    .message("License not found")
                    .build();
        }

        if (!licence.isActive()) {
            return LicenceValidationResult.builder()
                    .valid(false)
                    .message("License is inactive")
                    .licence(licence)
                    .build();
        }

        LocalDateTime now = TrustedTimeProvider.getCurrentTime();

        if (now == null) {
            return LicenceValidationResult.builder()
                    .valid(false)
                    .message("Unable to verify license time")
                    .licence(licence)
                    .build();
        }

        if (licence.getExpiryDate() != null && now.isAfter(licence.getExpiryDate())) {
            return LicenceValidationResult.builder()
                    .valid(false)
                    .message("License has expired on " + licence.getExpiryDate())
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

        String warningMessage = null;

        if (licence.getExpiryDate() != null) {
            long daysUntilExpiry = ChronoUnit.DAYS.between(now.toLocalDate(), licence.getExpiryDate().toLocalDate());
            if (daysUntilExpiry <= EXPIRY_WARNING_DAYS) {
                warningMessage = "License expires in " + daysUntilExpiry + " day(s)";
            }
        } else if (licence.getMaxMeters() > 0) {
            int remaining = licence.getMaxMeters() - currentMeters;
            if (remaining <= METER_COUNTDOWN_WARNING_THRESHOLD) {
                warningMessage = "License meter limit countdown: " + remaining + " meter(s) remaining out of " + licence.getMaxMeters();
            }
        }

        return LicenceValidationResult.builder()
                .valid(true)
                .message("License is valid")
                .warningMessage(warningMessage)
                .licence(licence)
                .build();
    }

}
