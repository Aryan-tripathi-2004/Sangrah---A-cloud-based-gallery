package com.example.Auth.common.constants;

/**
 * Error codes and messages used across the Sangrah platform.
 */
public final class ErrorConstants {

    private ErrorConstants() {
        throw new UnsupportedOperationException("Cannot instantiate constants class");
    }

    // General Error Codes
    public static final String ERR_INTERNAL_SERVER = "ERR_INTERNAL_SERVER";
    public static final String ERR_BAD_REQUEST = "ERR_BAD_REQUEST";
    public static final String ERR_VALIDATION_FAILED = "ERR_VALIDATION_FAILED";
    public static final String ERR_RESOURCE_NOT_FOUND = "ERR_RESOURCE_NOT_FOUND";
    public static final String ERR_CONFLICT = "ERR_CONFLICT";
    public static final String ERR_EXTERNAL_SERVICE = "ERR_EXTERNAL_SERVICE";

    // Authentication Error Codes
    public static final String ERR_AUTH_INVALID_CREDENTIALS = "ERR_AUTH_INVALID_CREDENTIALS";
    public static final String ERR_AUTH_TOKEN_EXPIRED = "ERR_AUTH_TOKEN_EXPIRED";
    public static final String ERR_AUTH_TOKEN_INVALID = "ERR_AUTH_TOKEN_INVALID";
    public static final String ERR_AUTH_TOKEN_MISSING = "ERR_AUTH_TOKEN_MISSING";
    public static final String ERR_AUTH_UNAUTHORIZED = "ERR_AUTH_UNAUTHORIZED";
    public static final String ERR_AUTH_FORBIDDEN = "ERR_AUTH_FORBIDDEN";
    public static final String ERR_AUTH_ACCOUNT_LOCKED = "ERR_AUTH_ACCOUNT_LOCKED";
    public static final String ERR_AUTH_ACCOUNT_DISABLED = "ERR_AUTH_ACCOUNT_DISABLED";
    public static final String ERR_AUTH_EMAIL_NOT_VERIFIED = "ERR_AUTH_EMAIL_NOT_VERIFIED";
    public static final String ERR_AUTH_REFRESH_TOKEN_INVALID = "ERR_AUTH_REFRESH_TOKEN_INVALID";
    public static final String ERR_AUTH_REFRESH_TOKEN_REVOKED = "ERR_AUTH_REFRESH_TOKEN_REVOKED";
    public static final String ERR_AUTH_SESSION_EXPIRED = "ERR_AUTH_SESSION_EXPIRED";
    public static final String ERR_AUTH_SESSION_NOT_FOUND = "ERR_AUTH_SESSION_NOT_FOUND";

    // User Error Codes
    public static final String ERR_USER_ALREADY_EXISTS = "ERR_USER_ALREADY_EXISTS";
    public static final String ERR_USER_NOT_FOUND = "ERR_USER_NOT_FOUND";
    public static final String ERR_USER_EMAIL_EXISTS = "ERR_USER_EMAIL_EXISTS";
    public static final String ERR_USER_USERNAME_EXISTS = "ERR_USER_USERNAME_EXISTS";

    // OTP Error Codes
    public static final String ERR_OTP_INVALID = "ERR_OTP_INVALID";
    public static final String ERR_OTP_EXPIRED = "ERR_OTP_EXPIRED";
    public static final String ERR_OTP_MAX_ATTEMPTS = "ERR_OTP_MAX_ATTEMPTS";
    public static final String ERR_OTP_ALREADY_USED = "ERR_OTP_ALREADY_USED";

    // Rate Limiting Error Codes
    public static final String ERR_RATE_LIMIT_EXCEEDED = "ERR_RATE_LIMIT_EXCEEDED";
    public static final String ERR_TOO_MANY_REQUESTS = "ERR_TOO_MANY_REQUESTS";

    // Captcha Error Codes
    public static final String ERR_CAPTCHA_INVALID = "ERR_CAPTCHA_INVALID";
    public static final String ERR_CAPTCHA_REQUIRED = "ERR_CAPTCHA_REQUIRED";
    public static final String ERR_CAPTCHA_VERIFICATION_FAILED = "ERR_CAPTCHA_VERIFICATION_FAILED";

    // Error Messages
    public static final String MSG_INTERNAL_SERVER = "An internal server error occurred. Please try again later.";
    public static final String MSG_BAD_REQUEST = "The request is invalid.";
    public static final String MSG_VALIDATION_FAILED = "Validation failed for the provided data.";
    public static final String MSG_RESOURCE_NOT_FOUND = "The requested resource was not found.";
    public static final String MSG_CONFLICT = "A conflict occurred with the current state of the resource.";
    public static final String MSG_EXTERNAL_SERVICE = "An external service error occurred.";

    public static final String MSG_AUTH_INVALID_CREDENTIALS = "Invalid username or password.";
    public static final String MSG_AUTH_TOKEN_EXPIRED = "The authentication token has expired.";
    public static final String MSG_AUTH_TOKEN_INVALID = "The authentication token is invalid.";
    public static final String MSG_AUTH_TOKEN_MISSING = "Authentication token is required.";
    public static final String MSG_AUTH_UNAUTHORIZED = "You are not authorized to perform this action.";
    public static final String MSG_AUTH_FORBIDDEN = "Access to this resource is forbidden.";
    public static final String MSG_AUTH_ACCOUNT_LOCKED = "Your account has been locked. Please contact support.";
    public static final String MSG_AUTH_ACCOUNT_DISABLED = "Your account has been disabled.";
    public static final String MSG_AUTH_EMAIL_NOT_VERIFIED = "Please verify your email address before logging in.";

    public static final String MSG_USER_ALREADY_EXISTS = "A user with this information already exists.";
    public static final String MSG_USER_NOT_FOUND = "User not found.";
    public static final String MSG_USER_EMAIL_EXISTS = "This email is already registered.";
    public static final String MSG_USER_USERNAME_EXISTS = "This username is already taken.";

    public static final String MSG_OTP_INVALID = "The OTP code is invalid.";
    public static final String MSG_OTP_EXPIRED = "The OTP code has expired. Please request a new one.";
    public static final String MSG_OTP_MAX_ATTEMPTS = "Maximum OTP verification attempts exceeded.";
    public static final String MSG_OTP_ALREADY_USED = "This OTP has already been used.";

    public static final String MSG_RATE_LIMIT_EXCEEDED = "Rate limit exceeded. Please try again later.";
    public static final String MSG_TOO_MANY_REQUESTS = "Too many requests. Please slow down.";

    public static final String MSG_CAPTCHA_INVALID = "Captcha verification failed.";
    public static final String MSG_CAPTCHA_REQUIRED = "Captcha verification is required.";
    public static final String MSG_CAPTCHA_VERIFICATION_FAILED = "Failed to verify captcha. Please try again.";
}
