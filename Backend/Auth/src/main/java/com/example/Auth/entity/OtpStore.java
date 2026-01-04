package com.example.Auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a One-Time Password (OTP) stored for verification
 * purposes.
 * OTPs are used for email verification, password reset, and two-factor
 * authentication.
 * OTPs are hashed before storage for security.
 */
@Entity
@Table(name = "otp_store", indexes = {
        @Index(name = "idx_otp_user", columnList = "user_id"),
        @Index(name = "idx_otp_email", columnList = "email"),
        @Index(name = "idx_otp_expires", columnList = "expires_at")
})
@EntityListeners(AuditingEntityListener.class)
public class OtpStore {

    /**
     * Purpose of the OTP.
     */
    public enum Purpose {
        EMAIL_VERIFICATION,
        PASSWORD_RESET,
        TWO_FACTOR
    }

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    /**
     * User ID for which this OTP was generated (nullable for pre-registration
     * scenarios).
     */
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    private UUID userId;

    /**
     * Email address for which this OTP was generated.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * HMAC-SHA256 hash of the OTP.
     * Never store plain OTPs in the database.
     */
    @NotBlank(message = "OTP hash is required")
    @Size(min = 64, max = 64, message = "OTP hash must be 64 characters (HMAC-SHA256)")
    @Column(name = "otp_hash", nullable = false, length = 64)
    private String otpHash;

    @NotNull(message = "Purpose is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    private Purpose purpose;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @NotNull(message = "Expiry time is required")
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Flag indicating if this OTP has been used.
     * Once used, an OTP cannot be reused.
     */
    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Column(name = "used_at")
    private Instant usedAt;

    /**
     * Number of failed verification attempts for this OTP.
     * Limited to prevent brute force attacks.
     */
    @Min(value = 0, message = "Attempts cannot be negative")
    @Max(value = 10, message = "Maximum 10 attempts allowed")
    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    /**
     * IP address from which the OTP was requested.
     * Used for security monitoring and rate limiting.
     */
    @Size(max = 45, message = "IP address cannot exceed 45 characters")
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // Constructors

    public OtpStore() {
        this.id = UUID.randomUUID();
    }

    public OtpStore(UUID userId, String email, String otpHash, Purpose purpose, Instant expiresAt) {
        this();
        this.userId = userId;
        this.email = email;
        this.otpHash = otpHash;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
    }

    // Business Methods

    /**
     * Check if this OTP has expired.
     *
     * @return true if the OTP is past its expiration time
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if this OTP is valid for verification.
     * An OTP is valid if it's not expired, not used, and hasn't exceeded max
     * attempts.
     *
     * @return true if the OTP can be verified
     */
    public boolean isValid() {
        return !used && !isExpired() && attempts < 10;
    }

    /**
     * Increment the failed attempt counter.
     *
     * @return the new attempt count
     */
    public int incrementAttempts() {
        this.attempts++;
        return this.attempts;
    }

    /**
     * Check if the maximum number of attempts has been reached.
     *
     * @return true if no more attempts are allowed
     */
    public boolean hasExceededMaxAttempts() {
        return this.attempts >= 10;
    }

    /**
     * Mark this OTP as used.
     * Sets the used flag and records the usage timestamp.
     */
    public void markAsUsed() {
        this.used = true;
        this.usedAt = Instant.now();
    }

    /**
     * Get the remaining time until this OTP expires.
     *
     * @return seconds until expiration, or 0 if already expired
     */
    public long getSecondsUntilExpiry() {
        long seconds = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, seconds);
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }

    public Purpose getPurpose() {
        return purpose;
    }

    public void setPurpose(Purpose purpose) {
        this.purpose = purpose;
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

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof OtpStore))
            return false;
        OtpStore otpStore = (OtpStore) o;
        return id != null && id.equals(otpStore.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "OtpStore{" +
                "id=" + id +
                ", userId=" + userId +
                ", email='" + email + '\'' +
                ", purpose=" + purpose +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                ", used=" + used +
                ", attempts=" + attempts +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}
