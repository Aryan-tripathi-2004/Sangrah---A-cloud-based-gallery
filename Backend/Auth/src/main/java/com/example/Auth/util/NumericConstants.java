package com.example.Auth.util;

/**
 * Numeric constants used throughout the application.
 * Contains HTTP status codes, validation limits, timeouts, etc.
 */
public final class NumericConstants {

    private NumericConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // HTTP Status Codes
    public static final int HTTP_STATUS_OK = 200;
    public static final int HTTP_STATUS_CREATED = 201;
    public static final int HTTP_STATUS_BAD_REQUEST = 400;
    public static final int HTTP_STATUS_UNAUTHORIZED = 401;
    public static final int HTTP_STATUS_INTERNAL_SERVER_ERROR = 500;

    // Validation Limits
    public static final int USERNAME_MIN_LENGTH = 3;
    public static final int USERNAME_MAX_LENGTH = 50;
    public static final int EMAIL_MAX_LENGTH = 255;
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 128;
    public static final int FIRST_NAME_MAX_LENGTH = 100;
    public static final int LAST_NAME_MAX_LENGTH = 100;
    public static final int DISPLAY_NAME_MAX_LENGTH = 150;
    public static final int DEVICE_NAME_MAX_LENGTH = 200;
    public static final int USER_AGENT_MAX_LENGTH = 500;

    // OTP Constants
    public static final int OTP_LENGTH = 6;
    public static final int OTP_VALIDITY_MINUTES = 10;
    public static final int OTP_MAX_ATTEMPTS = 10;
    public static final int OTP_RATE_LIMIT_PER_HOUR = 5;

    // JWT Token Constants
    public static final int JWT_ACCESS_TOKEN_VALIDITY_SECONDS = 900; // 15 minutes
    public static final int JWT_REFRESH_TOKEN_VALIDITY_SECONDS = 2592000; // 30 days
    public static final int JWT_REFRESH_TOKEN_REMEMBER_ME_MULTIPLIER = 2; // 60 days with remember me

    // Session Constants
    public static final int SESSION_VALIDITY_DAYS = 30;

    // Password Validation Constants
    public static final int PASSWORD_MIN_UPPERCASE = 1;
    public static final int PASSWORD_MIN_LOWERCASE = 1;
    public static final int PASSWORD_MIN_DIGITS = 1;
    public static final int PASSWORD_MIN_SPECIAL_CHARS = 1;

    // BCrypt Constants
    public static final int BCRYPT_STRENGTH = 12;

    // Retry Constants
    public static final int EMAIL_RETRY_MAX_ATTEMPTS = 10;
    public static final int EMAIL_RETRY_DELAY_MS = 1000;
}
