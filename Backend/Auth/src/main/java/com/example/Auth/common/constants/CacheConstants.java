package com.example.Auth.common.constants;

/**
 * Constants for Redis cache keys and TTL values.
 */
public final class CacheConstants {

    private CacheConstants() {
        throw new UnsupportedOperationException("Cannot instantiate constants class");
    }

    // Cache Key Prefixes
    public static final String PREFIX_USER_PROFILE = "users:profile:";
    public static final String PREFIX_USER_ROLES = "users:roles:";
    public static final String PREFIX_USER_PERMISSIONS = "users:permissions:";
    public static final String PREFIX_SESSION = "sessions:";
    public static final String PREFIX_TOKEN_BLACKLIST = "tokens:blacklist:";
    public static final String PREFIX_RATE_LIMIT = "ratelimit:";
    public static final String PREFIX_OTP_COUNTER = "otp:counter:";
    public static final String PREFIX_LOGIN_ATTEMPTS = "login:attempts:";

    // TTL Values (in seconds)
    public static final long TTL_USER_PROFILE = 600; // 10 minutes
    public static final long TTL_USER_ROLES = 1800; // 30 minutes
    public static final long TTL_USER_PERMISSIONS = 1800; // 30 minutes
    public static final long TTL_SESSION = 3600; // 1 hour
    public static final long TTL_TOKEN_BLACKLIST = 900; // 15 minutes
    public static final long TTL_RATE_LIMIT = 60; // 1 minute
    public static final long TTL_OTP_COUNTER = 3600; // 1 hour
    public static final long TTL_LOGIN_ATTEMPTS = 300; // 5 minutes

    // Cache Key Builders
    public static String userProfileKey(String userId) {
        return PREFIX_USER_PROFILE + userId;
    }

    public static String userRolesKey(String userId) {
        return PREFIX_USER_ROLES + userId;
    }

    public static String userPermissionsKey(String userId) {
        return PREFIX_USER_PERMISSIONS + userId;
    }

    public static String sessionKey(String sessionId) {
        return PREFIX_SESSION + sessionId;
    }

    public static String tokenBlacklistKey(String token) {
        return PREFIX_TOKEN_BLACKLIST + token;
    }

    public static String rateLimitKey(String identifier) {
        return PREFIX_RATE_LIMIT + identifier;
    }

    public static String otpCounterKey(String identifier) {
        return PREFIX_OTP_COUNTER + identifier;
    }

    public static String loginAttemptsKey(String identifier) {
        return PREFIX_LOGIN_ATTEMPTS + identifier;
    }
}
