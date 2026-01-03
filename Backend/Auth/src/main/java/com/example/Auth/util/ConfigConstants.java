package com.example.Auth.util;

/**
 * Configuration constants for OpenAPI documentation and application metadata.
 * These constants centralize all configuration strings used
 * in @OpenAPIDefinition,
 * @SecurityScheme, and other configuration annotations.
 */
public final class ConfigConstants {

    private ConfigConstants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    // ==================== API Info Constants ====================
    public static final String API_TITLE = "Sangrah Auth Service API";
    public static final String API_VERSION = "1.0.0";
    public static final String API_DESCRIPTION = """
            Authentication and Authorization Service for Sangrah Cloud Storage Platform.

            ## Features
            - User registration with email verification
            - JWT-based authentication (access + refresh tokens)
            - Session management with device tracking
            - Secure logout (single device and all devices)
            - Password strength validation
            - Rate limiting and security headers

            ## Security
            - BCrypt password hashing (12 rounds)
            - JWT tokens with HS512 signing
            - Access tokens expire in 15 minutes
            - Refresh tokens expire in 30 days (60 days with "remember me")
            - Token blacklisting for immediate revocation
            - Device fingerprinting for session security

            ## Authentication Flow
            1. Register user → Receive OTP via email
            2. Verify email with OTP
            3. Login with username/email + password → Receive access & refresh tokens
            4. Use access token (Bearer) for authenticated requests
            5. Refresh access token when expired using refresh token
            6. Logout to revoke tokens
            """;

    // ==================== Contact Info Constants ====================
    public static final String CONTACT_NAME = "Sangrah Support";
    public static final String CONTACT_EMAIL = "support@sangrah.com";
    public static final String CONTACT_URL = "https://sangrah.com/support";

    // ==================== License Constants ====================
    public static final String LICENSE_NAME = "Proprietary";
    public static final String LICENSE_URL = "https://sangrah.com/license";

    // ==================== Server Constants ====================
    public static final String SERVER_LOCAL_URL = "http://localhost:8080";
    public static final String SERVER_LOCAL_DESC = "Local development server";
    public static final String SERVER_PROD_URL = "https://api.sangrah.com";
    public static final String SERVER_PROD_DESC = "Production server";

    // ==================== Security Scheme Constants ====================
    public static final String SECURITY_SCHEME_NAME = "bearer-jwt";
    public static final String SECURITY_SCHEME_BEARER_FORMAT = "JWT";
    public static final String SECURITY_SCHEME_DESCRIPTION = """
            JWT authentication using Bearer token.

            **How to authenticate:**
            1. Login via POST /api/v1/auth/login
            2. Copy the `accessToken` from response
            3. Click 'Authorize' button above
            4. Enter: Bearer {accessToken}
            5. Click 'Authorize' to apply

            **Token Expiry:**
            - Access tokens expire in 15 minutes
            - Refresh via POST /api/v1/auth/refresh when expired

            **Example:**
            ```
            Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxMjM0NTY3OC0xMjM0...
            ```
            """;
}
