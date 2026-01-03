package com.example.Auth.util;

/**
 * Constants for OpenAPI/Swagger documentation.
 * Contains all strings used in @Tag, @Operation, @ApiResponse, @Parameter
 * annotations.
 */
public final class ApiDocConstants {

    private ApiDocConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Controller Tags
    public static final String TAG_AUTHENTICATION = "Authentication";
    public static final String TAG_AUTHENTICATION_DESC = "User authentication and registration API";

    // Register Endpoint
    public static final String OP_REGISTER_SUMMARY = "Register new user";
    public static final String OP_REGISTER_DESC = "Creates a new user account and sends email verification OTP";
    public static final String RESP_REGISTER_201_DESC = "User registered successfully";
    public static final String RESP_REGISTER_400_DESC = "Validation failed - invalid request data";
    public static final String RESP_REGISTER_500_DESC = "Registration failed - username or email already exists";

    // Check Username Endpoint
    public static final String OP_CHECK_USERNAME_SUMMARY = "Check username availability";
    public static final String OP_CHECK_USERNAME_DESC = "Checks if a username is available for registration";
    public static final String PARAM_USERNAME_DESC = "Username to check";
    public static final String PARAM_USERNAME_EXAMPLE = "john_doe";
    public static final String RESP_CHECK_USERNAME_200_DESC = "Username availability checked successfully";

    // Check Email Endpoint
    public static final String OP_CHECK_EMAIL_SUMMARY = "Check email availability";
    public static final String OP_CHECK_EMAIL_DESC = "Checks if an email address is available for registration";
    public static final String PARAM_EMAIL_DESC = "Email address to check";
    public static final String PARAM_EMAIL_EXAMPLE = "john@example.com";
    public static final String RESP_CHECK_EMAIL_200_DESC = "Email availability checked successfully";

    // Verify Email Endpoint
    public static final String OP_VERIFY_EMAIL_SUMMARY = "Verify email with OTP";
    public static final String OP_VERIFY_EMAIL_DESC = "Verifies user email address using the OTP code sent during registration";
    public static final String PARAM_EMAIL_VERIFY_DESC = "Email address to verify";
    public static final String PARAM_OTP_DESC = "6-digit OTP code";
    public static final String PARAM_OTP_EXAMPLE = "123456";
    public static final String RESP_VERIFY_EMAIL_200_DESC = "Email verified successfully";
    public static final String RESP_VERIFY_EMAIL_400_DESC = "Invalid or expired OTP code";
    public static final String RESP_VERIFY_EMAIL_500_DESC = "Verification failed due to server error";

    // Login Endpoint
    public static final String OP_LOGIN_SUMMARY = "User login";
    public static final String OP_LOGIN_DESC = "Authenticates user with username/email and password, returns JWT access and refresh tokens";
    public static final String RESP_LOGIN_200_DESC = "Login successful";
    public static final String RESP_LOGIN_400_DESC = "Validation failed - missing required fields";
    public static final String RESP_LOGIN_401_DESC = "Authentication failed - invalid credentials or account locked/disabled";
    public static final String RESP_LOGIN_500_DESC = "Login failed due to server error";

    // Refresh Token Endpoint
    public static final String OP_REFRESH_SUMMARY = "Refresh access token";
    public static final String OP_REFRESH_DESC = "Generates a new access token using a valid refresh token";
    public static final String RESP_REFRESH_200_DESC = "Token refreshed successfully";
    public static final String RESP_REFRESH_400_DESC = "Validation failed - missing refresh token";
    public static final String RESP_REFRESH_401_DESC = "Token refresh failed - invalid, expired, or revoked token";
    public static final String RESP_REFRESH_500_DESC = "Token refresh failed due to server error";

    // Logout Endpoint
    public static final String OP_LOGOUT_SUMMARY = "Logout user";
    public static final String OP_LOGOUT_DESC = "Logs out user by revoking the refresh token and invalidating the session. Operation is idempotent.";
    public static final String RESP_LOGOUT_200_DESC = "Logout successful (always returns success for idempotency)";
    public static final String RESP_LOGOUT_400_DESC = "Validation failed - missing refresh token";

    // Logout All Endpoint
    public static final String OP_LOGOUT_ALL_SUMMARY = "Logout from all devices";
    public static final String OP_LOGOUT_ALL_DESC = "Logs out user from all devices by revoking all refresh tokens and sessions. Requires JWT authentication.";
    public static final String OP_LOGOUT_ALL_SECURITY = "bearer-jwt";
    public static final String RESP_LOGOUT_ALL_200_DESC = "Logged out from all devices successfully";
    public static final String RESP_LOGOUT_ALL_401_DESC = "User not authenticated - JWT token required";
    public static final String RESP_LOGOUT_ALL_500_DESC = "Logout all failed due to server error";

    // HTTP Status Codes (as strings for @ApiResponse)
    public static final String HTTP_200 = "200";
    public static final String HTTP_201 = "201";
    public static final String HTTP_400 = "400";
    public static final String HTTP_401 = "401";
    public static final String HTTP_500 = "500";
}
