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
 * Entity representing a refresh token for user authentication.
 * Refresh tokens are long-lived tokens used to obtain new access tokens.
 * Each token is associated with a specific session and device.
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token_user", columnList = "user_id"),
        @Index(name = "idx_refresh_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_refresh_token_session", columnList = "session_id"),
        @Index(name = "idx_refresh_token_expires", columnList = "expires_at")
})
@EntityListeners(AuditingEntityListener.class)
public class RefreshToken {

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    /**
     * SHA-256 hash of the refresh token.
     * Never store plain tokens in the database.
     */
    @NotBlank(message = "Token hash is required")
    @Size(min = 64, max = 64, message = "Token hash must be 64 characters (SHA-256)")
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /**
     * Session ID linking this token to a specific user session.
     * Used for session-based token revocation.
     */
    @NotNull(message = "Session ID is required")
    @Column(name = "session_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID sessionId;

    @CreatedDate
    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @NotNull(message = "Expiry time is required")
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Flag indicating if this token has been revoked.
     * Revoked tokens cannot be used to obtain new access tokens.
     */
    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * Timestamp of when this token was last used to refresh an access token.
     */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /**
     * Information about the device that requested this token.
     * Example: "Chrome 120.0.0 on Windows 10"
     */
    @Size(max = 255, message = "Device info cannot exceed 255 characters")
    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    /**
     * IP address from which the token was issued.
     */
    @Size(max = 45, message = "IP address cannot exceed 45 characters")
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent string from the client that requested the token.
     */
    @Size(max = 500, message = "User agent cannot exceed 500 characters")
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    // Constructors

    public RefreshToken() {
        this.id = UUID.randomUUID();
    }

    public RefreshToken(User user, String tokenHash, UUID sessionId, Instant expiresAt) {
        this();
        this.user = user;
        this.tokenHash = tokenHash;
        this.sessionId = sessionId;
        this.expiresAt = expiresAt;
    }

    // Business Methods

    /**
     * Check if this refresh token has expired.
     *
     * @return true if the token is past its expiration time
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if this refresh token is valid for use.
     * A token is valid if it's not revoked and not expired.
     *
     * @return true if the token can be used to obtain a new access token
     */
    public boolean isValid() {
        return !revoked && !isExpired();
    }

    /**
     * Revoke this refresh token.
     * Sets the revoked flag and records the revocation timestamp.
     */
    public void revoke() {
        this.revoked = true;
        this.revokedAt = Instant.now();
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
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

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RefreshToken))
            return false;
        RefreshToken that = (RefreshToken) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "RefreshToken{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", sessionId=" + sessionId +
                ", issuedAt=" + issuedAt +
                ", expiresAt=" + expiresAt +
                ", revoked=" + revoked +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}
