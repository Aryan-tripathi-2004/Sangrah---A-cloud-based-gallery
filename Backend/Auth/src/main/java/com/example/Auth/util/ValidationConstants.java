package com.example.Auth.util;

/**
 * Validation message constants for custom validation annotations and
 * validators.
 * These constants centralize all validation error messages used in password
 * strength
 * validation and other custom validation logic.
 */
public final class ValidationConstants {

    private ValidationConstants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    // ==================== Password Validation Messages ====================

    /**
     * Default message for ValidPassword annotation when validation fails.
     */
    public static final String VALIDATION_PASSWORD_DEFAULT_MESSAGE = "Password must contain at least 8 characters, one uppercase letter, "
            +
            "one lowercase letter, one digit, and one special character";

    /**
     * Error message when password is shorter than minimum required length.
     */
    public static final String VALIDATION_PASSWORD_MIN_LENGTH = "Password must be at least 8 characters long";

    /**
     * Error message when password doesn't contain an uppercase letter.
     */
    public static final String VALIDATION_PASSWORD_UPPERCASE_REQUIRED = "Password must contain at least one uppercase letter";

    /**
     * Error message when password doesn't contain a lowercase letter.
     */
    public static final String VALIDATION_PASSWORD_LOWERCASE_REQUIRED = "Password must contain at least one lowercase letter";

    /**
     * Error message when password doesn't contain a digit.
     */
    public static final String VALIDATION_PASSWORD_DIGIT_REQUIRED = "Password must contain at least one digit";

    /**
     * Error message when password doesn't contain a special character.
     */
    public static final String VALIDATION_PASSWORD_SPECIAL_CHAR_REQUIRED = "Password must contain at least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)";
}
