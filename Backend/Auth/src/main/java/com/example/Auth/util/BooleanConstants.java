package com.example.Auth.util;

/**
 * Boolean constants used throughout the application.
 */
public final class BooleanConstants {

    private BooleanConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Default Boolean Values
    public static final Boolean DEFAULT_MARKETING_OPT_IN = false;
    public static final Boolean DEFAULT_REMEMBER_ME = false;
    public static final Boolean DEFAULT_EMAIL_VERIFIED = false;
    public static final Boolean DEFAULT_ACCOUNT_ENABLED = false;
    public static final Boolean DEFAULT_ACCOUNT_LOCKED = false;
    public static final Boolean DEFAULT_SESSION_REVOKED = false;
    public static final Boolean DEFAULT_TOKEN_REVOKED = false;

    // Boolean True/False for responses
    public static final Boolean TRUE = true;
    public static final Boolean FALSE = false;
}
