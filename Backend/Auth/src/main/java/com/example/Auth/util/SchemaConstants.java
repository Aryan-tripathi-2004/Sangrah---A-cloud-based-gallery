package com.example.Auth.util;

/**
 * Constants for DTO Schema annotations.
 * Contains all strings used in @Schema annotations for request/response DTOs.
 */
public final class SchemaConstants {

    private SchemaConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // RegisterRequest Schema
    public static final String SCHEMA_REGISTER_REQUEST_DESC = "User registration request";
    public static final String SCHEMA_USERNAME_DESC = "Username (3-50 chars, alphanumeric with _ and -)";
    public static final String SCHEMA_USERNAME_EXAMPLE = "john_doe";
    public static final String SCHEMA_EMAIL_DESC = "Email address";
    public static final String SCHEMA_EMAIL_EXAMPLE = "john@example.com";
    public static final String SCHEMA_PASSWORD_DESC = "Password (8-128 chars, must contain uppercase, lowercase, digit, and special character)";
    public static final String SCHEMA_PASSWORD_EXAMPLE = "SecurePass123!";
    public static final String SCHEMA_FIRST_NAME_DESC = "First name";
    public static final String SCHEMA_FIRST_NAME_EXAMPLE = "John";
    public static final String SCHEMA_LAST_NAME_DESC = "Last name";
    public static final String SCHEMA_LAST_NAME_EXAMPLE = "Doe";
    public static final String SCHEMA_DISPLAY_NAME_DESC = "Display name (auto-generated if not provided)";
    public static final String SCHEMA_DISPLAY_NAME_EXAMPLE = "John Doe";
    public static final String SCHEMA_MARKETING_OPT_IN_DESC = "Marketing email opt-in";
    public static final String SCHEMA_MARKETING_OPT_IN_EXAMPLE = "false";
    public static final String SCHEMA_MARKETING_OPT_IN_DEFAULT = "false";

    // LoginRequest Schema
    public static final String SCHEMA_LOGIN_REQUEST_DESC = "User login request";
    public static final String SCHEMA_USERNAME_OR_EMAIL_DESC = "Username or email address";
    public static final String SCHEMA_USERNAME_OR_EMAIL_EXAMPLE = "john_doe";
    public static final String SCHEMA_USER_PASSWORD_DESC = "User password";
    public static final String SCHEMA_DEVICE_NAME_DESC = "Device name for session tracking";
    public static final String SCHEMA_DEVICE_NAME_EXAMPLE = "iPhone 14 Pro";
    public static final String SCHEMA_USER_AGENT_DESC = "User agent string for device fingerprinting";
    public static final String SCHEMA_USER_AGENT_EXAMPLE = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X)";
    public static final String SCHEMA_REMEMBER_ME_DESC = "Remember me - extends refresh token expiry to 60 days";
    public static final String SCHEMA_REMEMBER_ME_EXAMPLE = "false";
    public static final String SCHEMA_REMEMBER_ME_DEFAULT = "false";

    // RefreshTokenRequest Schema
    public static final String SCHEMA_REFRESH_TOKEN_REQUEST_DESC = "Token refresh request";
    public static final String SCHEMA_REFRESH_TOKEN_DESC = "Refresh token received during login";
    public static final String SCHEMA_REFRESH_TOKEN_EXAMPLE = "eyJhbGciOiJIUzUxMiJ9...";
}
