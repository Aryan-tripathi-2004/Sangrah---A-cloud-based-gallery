package com.example.Auth.shared.enums;

/**
 * Represents the reason a token was blacklisted or revoked.
 */
public enum RevocationReason {
    LOGOUT,
    TOKEN_REFRESH,
    SECURITY_BREACH
}
