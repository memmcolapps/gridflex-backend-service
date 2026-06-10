package org.memmcol.gridflexbackendservice.service.licence;

import org.memmcol.gridflexbackendservice.model.licence.Licence;
import org.memmcol.gridflexbackendservice.model.licence.LicenceValidationResult;


public class LicenceValidator {

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

//        if (licence.getMaxMeters() > 0
//                && currentMeters + 1 > licence.getMaxMeters()) {
//
//            return LicenceValidationResult.builder()
//                    .valid(false)
//                    .message("Adding this meter would exceed the licensed limit of "
//                            + licence.getMaxMeters())
//                    .licence(licence)
//                    .build();
//        }

        if (licence.getMaxMeters() > 0 && currentMeters >= licence.getMaxMeters()) {
            return LicenceValidationResult.builder()
                    .valid(false)
                    .message("Meter limit reached (" + licence.getMaxMeters() + " max)")
                    .licence(licence)
                    .build();
        }

        return LicenceValidationResult.builder()
                .valid(true)
                .message("Licence is valid")
                .licence(licence)
                .build();
    }

}
