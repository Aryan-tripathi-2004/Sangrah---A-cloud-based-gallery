package com.example.Auth.common.constants;

/**
 * Constants related to JWT token operations and claims.
 */
public final class JwtConstants {

    private JwtConstants() {
        throw new UnsupportedOperationException("Cannot instantiate constants class");
    }

    // JWT Claims
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_SESSION_ID = "sessionId";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String CLAIM_DEVICE_ID = "deviceId";

    // Token Types
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    // Token Settings Keys
    public static final String SETTING_SECRET_KEY = "jwt.secret";
    public static final String SETTING_ACCESS_TOKEN_EXPIRY = "jwt.access-token.expiry-minutes";
    public static final String SETTING_REFRESH_TOKEN_EXPIRY = "jwt.refresh-token.expiry-days";
    public static final String SETTING_ALGORITHM = "jwt.algorithm";

    // Default Values
    public static final int DEFAULT_ACCESS_TOKEN_EXPIRY_MINUTES = 15;
    public static final int DEFAULT_REFRESH_TOKEN_EXPIRY_DAYS = 30;
    public static final String DEFAULT_ALGORITHM = "HS512";
}
