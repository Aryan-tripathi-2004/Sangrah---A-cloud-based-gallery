package com.example.Auth.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Business model for Session.
 * Used in the service layer for session management operations.
 * Decoupled from database entity annotations.
 */
public class SessionModel {
    private UUID sessionId;
    private UUID userId; // Reference to user (avoid loading full user object)
    private Instant createdAt;
    private Instant expiresAt;
    private Instant lastAccessedAt;
    private String ipAddress;
    private String userAgent;
    private String deviceFingerprint;
    private boolean revoked;
    private Instant revokedAt;
    private String revocationReason;

    // Constructors

    public SessionModel() {
    }

    public SessionModel(UUID sessionId, UUID userId) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.createdAt = Instant.now();
        this.revoked = false;
    }

    // Business Logic Methods

    /**
     * Check if the session is expired.
     *
     * @return true if expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if the session is active (not revoked and not expired).
     *
     * @return true if active
     */
    public boolean isActive() {
        return !revoked && !isExpired();
    }

    /**
     * Check if the session needs renewal (will expire soon).
     *
     * @param thresholdMinutes minutes before expiry to consider renewal
     * @return true if renewal is needed
     */
    public boolean needsRenewal(long thresholdMinutes) {
        if (expiresAt == null) {
            return false;
        }
        Instant threshold = Instant.now().plusSeconds(thresholdMinutes * 60);
        return threshold.isAfter(expiresAt);
    }

    /**
     * Revoke the session with a reason.
     *
     * @param reason the revocation reason
     */
    public void revoke(String reason) {
        this.revoked = true;
        this.revokedAt = Instant.now();
        this.revocationReason = reason;
    }

    /**
     * Update last accessed timestamp.
     */
    public void updateLastAccessed() {
        this.lastAccessedAt = Instant.now();
    }

    /**
     * Extend the session expiry.
     *
     * @param additionalMinutes minutes to add to expiry
     */
    public void extendExpiry(long additionalMinutes) {
        if (expiresAt != null) {
            this.expiresAt = expiresAt.plusSeconds(additionalMinutes * 60);
        }
    }

    // Getters and Setters

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(Instant lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
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

    public String getRevocationReason() {
        return revocationReason;
    }

    public void setRevocationReason(String revocationReason) {
        this.revocationReason = revocationReason;
    }
}
