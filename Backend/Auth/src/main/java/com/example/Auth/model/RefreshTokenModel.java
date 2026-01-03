package com.example.Auth.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Business model for RefreshToken.
 * Used in the service layer for token refresh operations.
 * Decoupled from database entity annotations.
 */
public class RefreshTokenModel {
    private UUID id;
    private String tokenHash;
    private UUID userId; // Reference to user
    private UUID sessionId; // Reference to session
    private Instant expiresAt;
    private boolean revoked;
    private Instant revokedAt;
    private Instant createdAt;
    private Instant lastUsedAt;

    // Constructors

    public RefreshTokenModel() {
    }

    public RefreshTokenModel(String tokenHash, UUID userId, UUID sessionId, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.sessionId = sessionId;
        this.expiresAt = expiresAt;
        this.revoked = false;
        this.createdAt = Instant.now();
    }

    // Business Logic Methods

    /**
     * Check if the token is expired.
     *
     * @return true if expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if the token is valid (not revoked and not expired).
     *
     * @return true if valid
     */
    public boolean isValid() {
        return !revoked && !isExpired();
    }

    /**
     * Revoke the token.
     */
    public void revoke() {
        this.revoked = true;
        this.revokedAt = Instant.now();
    }

    /**
     * Update last used timestamp.
     */
    public void markAsUsed() {
        this.lastUsedAt = Instant.now();
    }

    /**
     * Check if token expires soon (within threshold).
     *
     * @param thresholdMinutes minutes before expiry
     * @return true if expiring soon
     */
    public boolean expiresSoon(long thresholdMinutes) {
        if (expiresAt == null) {
            return false;
        }
        Instant threshold = Instant.now().plusSeconds(thresholdMinutes * 60);
        return threshold.isAfter(expiresAt);
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
