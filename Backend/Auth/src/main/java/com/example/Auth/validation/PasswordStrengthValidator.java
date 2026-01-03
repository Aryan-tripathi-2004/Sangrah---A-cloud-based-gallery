package com.example.Auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

import static com.example.Auth.util.ValidationConstants.*;

/**
 * Validator implementation for password strength validation.
 * 
 * Validates that the password meets the following requirements:
 * - At least 8 characters long
 * - Contains at least one uppercase letter
 * - Contains at least one lowercase letter
 * - Contains at least one digit
 * - Contains at least one special character
 */
public class PasswordStrengthValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_LENGTH = 8;

    // Regex patterns for password requirements
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?].*");

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // Null or empty passwords should be handled by @NotBlank
        if (password == null || password.isEmpty()) {
            return true;
        }

        // Check minimum length
        if (password.length() < MIN_LENGTH) {
            setCustomMessage(context, VALIDATION_PASSWORD_MIN_LENGTH);
            return false;
        }

        // Check for uppercase letter
        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            setCustomMessage(context, VALIDATION_PASSWORD_UPPERCASE_REQUIRED);
            return false;
        }

        // Check for lowercase letter
        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            setCustomMessage(context, VALIDATION_PASSWORD_LOWERCASE_REQUIRED);
            return false;
        }

        // Check for digit
        if (!DIGIT_PATTERN.matcher(password).matches()) {
            setCustomMessage(context, VALIDATION_PASSWORD_DIGIT_REQUIRED);
            return false;
        }

        // Check for special character
        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            setCustomMessage(context, VALIDATION_PASSWORD_SPECIAL_CHAR_REQUIRED);
            return false;
        }

        return true;
    }

    /**
     * Sets a custom error message for validation failure.
     */
    private void setCustomMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
