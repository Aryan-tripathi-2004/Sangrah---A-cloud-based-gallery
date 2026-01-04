package com.example.Auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a user session.
 * Sessions track active user logins across different devices and browsers.
 * Each session has a unique identifier and is linked to Redis for fast lookup.
 */
@Entity
@Table(name = "sessions", indexes = {
        @Index(name = "idx_session_user", columnList = "user_id"),
        @Index(name = "idx_session_expires", columnList = "expires_at"),
        @Index(name = "idx_session_fingerprint", columnList = "device_fingerprint")
})
@EntityListeners(AuditingEntityListener.class)
public class Session {

    @Id
    @Column(name = "session_id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @NotNull(message = "Expiry time is required")
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Last time this session was accessed.
     * Updated on each request to track session activity.
     */
    @NotNull(message = "Last accessed time is required")
    @Column(name = "last_accessed_at", nullable = false)
    private Instant lastAccessedAt;

    /**
     * IP address from which this session was created.
     */
    @Size(max = 45, message = "IP address cannot exceed 45 characters")
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent string from the client.
     * Used for device identification and security monitoring.
     */
    @Size(max = 500, message = "User agent cannot exceed 500 characters")
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * SHA-256 hash of device-specific attributes (user agent + IP).
     * Used to detect session hijacking attempts.
     */
    @NotBlank(message = "Device fingerprint is required")
    @Size(min = 64, max = 64, message = "Device fingerprint must be 64 characters (SHA-256)")
    @Column(name = "device_fingerprint", nullable = false, length = 64)
    private String deviceFingerprint;

    /**
     * Flag indicating if this session has been revoked.
     * Revoked sessions cannot be used for authentication.
     */
    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * Reason for session revocation (if applicable).
     * Examples: "USER_LOGOUT", "ADMIN_REVOKED", "SUSPICIOUS_ACTIVITY",
     * "PASSWORD_CHANGED"
     */
    @Size(max = 100, message = "Revocation reason cannot exceed 100 characters")
    @Column(name = "revocation_reason", length = 100)
    private String revocationReason;

    // Constructors

    public Session() {
        this.sessionId = UUID.randomUUID();
        this.lastAccessedAt = Instant.now();
    }

    public Session(User user, Instant expiresAt, String deviceFingerprint) {
        this();
        this.user = user;
        this.expiresAt = expiresAt;
        this.deviceFingerprint = deviceFingerprint;
    }

    // Business Methods

    /**
     * Check if this session has expired.
     *
     * @return true if the session is past its expiration time
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if this session is valid for use.
     * A session is valid if it's not revoked and not expired.
     *
     * @return true if the session can be used for authentication
     */
    public boolean isValid() {
        return !revoked && !isExpired();
    }

    /**
     * Revoke this session with a specific reason.
     *
     * @param reason the reason for revocation
     */
    public void revoke(String reason) {
        this.revoked = true;
        this.revokedAt = Instant.now();
        this.revocationReason = reason;
    }

    /**
     * Update the last accessed timestamp.
     * Called on each authenticated request to track session activity.
     */
    public void updateLastAccessed() {
        this.lastAccessedAt = Instant.now();
    }

    /**
     * Extend the session expiry time by the specified duration.
     *
     * @param extensionSeconds number of seconds to extend the session
     */
    public void extend(long extensionSeconds) {
        this.expiresAt = Instant.now().plusSeconds(extensionSeconds);
    }

    /**
     * Verify if the provided device fingerprint matches this session.
     * Used to detect session hijacking.
     *
     * @param fingerprint the device fingerprint to verify
     * @return true if the fingerprint matches
     */
    public boolean verifyDeviceFingerprint(String fingerprint) {
        return this.deviceFingerprint != null && this.deviceFingerprint.equals(fingerprint);
    }

    // Getters and Setters

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Session))
            return false;
        Session session = (Session) o;
        return sessionId != null && sessionId.equals(session.sessionId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Session{" +
                "sessionId=" + sessionId +
                ", userId=" + (user != null ? user.getId() : null) +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                ", lastAccessedAt=" + lastAccessedAt +
                ", revoked=" + revoked +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}
