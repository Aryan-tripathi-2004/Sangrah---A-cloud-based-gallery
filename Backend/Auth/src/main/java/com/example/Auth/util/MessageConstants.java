package com.example.Auth.util;

/**
 * Constants for controller response messages and log messages.
 * Contains all strings used in ApiResponse wrappers and logger statements.
 */
public final class MessageConstants {

    private MessageConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Registration Messages
    public static final String MSG_REGISTRATION_REQUEST_RECEIVED = "Registration request received";
    public static final String MSG_REGISTRATION_VALIDATION_FAILED = "Registration validation failed";
    public static final String MSG_USER_REGISTERED_SUCCESS = "User registered successfully";
    public static final String MSG_REGISTRATION_FAILED = "Registration failed";
    public static final String MSG_VALIDATION_FAILED_PREFIX = "Validation failed: ";

    // UserService Messages
    public static final String MSG_REGISTERING_NEW_USER = "Registering new user";
    public static final String MSG_USERNAME_ALREADY_EXISTS = "Username already exists";
    public static final String MSG_EMAIL_ALREADY_EXISTS = "Email already exists";
    public static final String MSG_USER_CREATED_SUCCESS = "User created successfully";
    public static final String MSG_USER_PROFILE_CREATED = "User profile created";
    public static final String MSG_FAILED_SEND_VERIFICATION_EMAIL = "Failed to send verification email";
    public static final String MSG_CREATING_DEFAULT_ROLE = "Creating default role";
    public static final String MSG_VERIFYING_EMAIL = "Verifying email";
    public static final String MSG_INVALID_OTP_EMAIL_VERIFICATION = "Invalid OTP for email verification";

    // Username Check Messages
    public static final String MSG_USERNAME_AVAILABILITY_CHECK = "Username availability check";
    public static final String MSG_USERNAME_AVAILABLE = "Username is available";
    public static final String MSG_USERNAME_TAKEN = "Username is already taken";

    // Email Check Messages
    public static final String MSG_EMAIL_AVAILABILITY_CHECK = "Email availability check";
    public static final String MSG_EMAIL_AVAILABLE = "Email is available";
    public static final String MSG_EMAIL_REGISTERED = "Email is already registered";

    // Email Verification Messages
    public static final String MSG_EMAIL_VERIFICATION_REQUEST = "Email verification request";
    public static final String MSG_EMAIL_VERIFIED_SUCCESS = "Email verified successfully";
    public static final String MSG_EMAIL_VERIFICATION_FAILED_INVALID_OTP = "Email verification failed - invalid OTP";
    public static final String MSG_INVALID_OTP = "Invalid or expired OTP code";
    public static final String MSG_EMAIL_VERIFICATION_ERROR = "Email verification error";
    public static final String MSG_VERIFICATION_FAILED_PREFIX = "Verification failed: ";

    // Login Messages
    public static final String MSG_LOGIN_REQUEST_RECEIVED = "Login request received";
    public static final String MSG_LOGIN_VALIDATION_FAILED = "Login validation failed";
    public static final String MSG_USER_LOGGED_IN_SUCCESS = "User logged in successfully";
    public static final String MSG_LOGIN_FAILED = "Login failed";
    public static final String MSG_LOGIN_ERROR = "Login error";
    public static final String MSG_LOGIN_SUCCESS = "Login successful";
    public static final String MSG_LOGIN_FAILED_PREFIX = "Login failed: ";

    // Token Refresh Messages
    public static final String MSG_TOKEN_REFRESH_REQUEST = "Token refresh request received";
    public static final String MSG_TOKEN_REFRESH_VALIDATION_FAILED = "Token refresh validation failed";
    public static final String MSG_TOKEN_REFRESHED_SUCCESS = "Token refreshed successfully";
    public static final String MSG_TOKEN_REFRESH_FAILED = "Token refresh failed";
    public static final String MSG_TOKEN_REFRESH_ERROR = "Token refresh error";
    public static final String MSG_TOKEN_REFRESH_FAILED_PREFIX = "Token refresh failed: ";

    // Logout Messages
    public static final String MSG_LOGOUT_REQUEST_RECEIVED = "Logout request received";
    public static final String MSG_LOGOUT_VALIDATION_FAILED = "Logout validation failed";
    public static final String MSG_USER_LOGGED_OUT_SUCCESS = "User logged out successfully";
    public static final String MSG_LOGOUT_ERROR = "Logout error";
    public static final String MSG_LOGGED_OUT_SUCCESS = "Logged out successfully";

    // Logout All Messages
    public static final String MSG_LOGOUT_ALL_REQUEST_RECEIVED = "Logout all request received";
    public static final String MSG_LOGOUT_ALL_FAILED_NOT_AUTHENTICATED = "Logout all failed - user not authenticated";
    public static final String MSG_USER_LOGGED_OUT_ALL_SUCCESS = "User logged out from all sessions";
    public static final String MSG_LOGOUT_ALL_ERROR = "Logout all error";
    public static final String MSG_LOGGED_OUT_ALL_SUCCESS = "Logged out from all devices successfully";
    public static final String MSG_USER_NOT_AUTHENTICATED = "User not authenticated";
    public static final String MSG_LOGOUT_ALL_FAILED_PREFIX = "Logout all failed: ";

    // Common Log Field Names
    public static final String LOG_FIELD_USERNAME = "username";
    public static final String LOG_FIELD_EMAIL = "email";
    public static final String LOG_FIELD_ERRORS = "errors";
    public static final String LOG_FIELD_USER_ID = "userId";
    public static final String LOG_FIELD_USERNAME_OR_EMAIL = "usernameOrEmail";
    public static final String LOG_FIELD_ERROR = "error";
    public static final String LOG_FIELD_AVAILABLE = "available";
    public static final String LOG_FIELD_SESSION_ID = "sessionId";

    // Common Response Field Names
    public static final String FIELD_AVAILABLE = "available";
    public static final String FIELD_VERIFIED = "verified";
    public static final String FIELD_SUCCESS = "success";

    // AuthenticationService Messages
    public static final String MSG_LOGIN_ATTEMPT = "Login attempt";
    public static final String MSG_LOGIN_FAILED_USER_NOT_FOUND = "Login failed - user not found";
    public static final String MSG_LOGIN_FAILED_ACCOUNT_LOCKED = "Login failed - account locked";
    public static final String MSG_LOGIN_FAILED_ACCOUNT_DISABLED = "Login failed - account disabled";
    public static final String MSG_LOGIN_FAILED_INVALID_PASSWORD = "Login failed - invalid password";
    public static final String MSG_TOKEN_REFRESH_FAILED_INVALID = "Token refresh failed - invalid token";
    public static final String MSG_TOKEN_REFRESH_FAILED_NOT_FOUND = "Token refresh failed - token not found or revoked";
    public static final String MSG_TOKEN_REFRESH_FAILED_REVOKED = "Token refresh failed - token revoked";
    public static final String MSG_TOKEN_REFRESH_FAILED_EXPIRED = "Token refresh failed - token expired";
    public static final String MSG_TOKEN_REFRESH_FAILED_ACCOUNT_INACTIVE = "Token refresh failed - account inactive";
    public static final String MSG_TOKEN_REFRESHED_SUCCESS_LOG = "Token refreshed successfully";
    public static final String MSG_LOGOUT_REQUEST = "Logout request";
    public static final String MSG_LOGOUT_SUCCESS = "Logout successful";
    public static final String MSG_LOGOUT_ERROR_LOG = "Logout error";
    public static final String MSG_LOGOUT_ALL_SESSIONS = "Logout all sessions";
    public static final String MSG_ALL_SESSIONS_LOGGED_OUT = "All sessions logged out";
    public static final String MSG_FAILED_LOGIN_ATTEMPT_RECORDED = "Failed login attempt recorded";

    // OTP Service Messages
    public static final String MSG_GENERATING_OTP = "Generating OTP";
    public static final String MSG_OTP_GENERATED_SUCCESS = "OTP generated successfully";
    public static final String MSG_VERIFYING_OTP = "Verifying OTP";
    public static final String MSG_MAX_OTP_ATTEMPTS_EXCEEDED = "Max OTP attempts exceeded";
    public static final String MSG_INVALID_OTP_PROVIDED = "Invalid OTP code provided";
    public static final String MSG_OTP_VERIFIED_SUCCESS = "OTP verified successfully";
    public static final String MSG_OTP_RATE_LIMIT_EXCEEDED = "OTP rate limit exceeded";
    public static final String MSG_FAILED_GENERATE_OTP_HASH = "Failed to generate OTP hash";
    public static final String MSG_CLEANING_UP_EXPIRED_OTPS = "Cleaning up expired OTPs";
    public static final String MSG_EXPIRED_OTPS_CLEANUP_COMPLETED = "Expired OTPs cleanup completed";

    // Email Service Messages
    public static final String MSG_SENDING_VERIFICATION_EMAIL = "Sending verification email";
    public static final String MSG_VERIFICATION_EMAIL_SENT_SUCCESS = "Verification email sent successfully";
    public static final String MSG_FAILED_SEND_VERIFICATION_EMAIL_LOG = "Failed to send verification email";
    public static final String MSG_EMAIL_SENT = "Email sent";
    public static final String MSG_FAILED_SEND_EMAIL = "Failed to send email";
    public static final String MSG_SENDING_PASSWORD_RESET_EMAIL = "Sending password reset email";
    public static final String MSG_PASSWORD_RESET_EMAIL_SENT_SUCCESS = "Password reset email sent successfully";
    public static final String MSG_FAILED_SEND_PASSWORD_RESET_EMAIL = "Failed to send password reset email";
}
