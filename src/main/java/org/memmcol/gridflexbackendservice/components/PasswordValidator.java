package org.memmcol.gridflexbackendservice.components;

import org.memmcol.gridflexbackendservice.exception.GlobalExceptionHandler;
import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {

    public void validatePassword(String password) {

        if (password.length() < 8) {
            throw new GlobalExceptionHandler.NotFoundException(
                    "Password must be at least 8 characters.");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new GlobalExceptionHandler.NotFoundException(
                    "Password must contain at least one uppercase letter.");
        }

        if (!password.matches(".*[a-z].*")) {
            throw new GlobalExceptionHandler.NotFoundException(
                    "Password must contain at least one lowercase letter.");
        }

        if (!password.matches(".*\\d.*")) {
            throw new GlobalExceptionHandler.NotFoundException(
                    "Password must contain at least one number.");
        }

        if (!password.matches(".*[!@#$%^&*()_+=<>?/{}\\[\\]-].*")) {
            throw new GlobalExceptionHandler.NotFoundException(
                    "Password must contain at least one special character.");
        }
//
    }
}
