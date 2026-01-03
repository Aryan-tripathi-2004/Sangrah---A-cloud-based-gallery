package com.example.Auth.entity;

import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a One-Time Password (OTP) stored for verification
 * purposes.
 * OTPs are used for email verification, password reset, and two-factor
 * authentication.
 * OTPs are hashed before storage for security.
 */
@Document(collection = "otp_store")
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
    private UUID id;

    /**
     * User ID for which this OTP was generated (nullable for pre-registration
     * scenarios).
     */
    @Indexed
    private UUID userId;

    /**
     * Email address for which this OTP was generated.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    @Indexed
    private String email;

    /**
     * HMAC-SHA256 hash of the OTP.
     * Never store plain OTPs in the database.
     */
    @NotBlank(message = "OTP hash is required")
    @Size(min = 64, max = 64, message = "OTP hash must be 64 characters (HMAC-SHA256)")
    private String otpHash;

    @NotNull(message = "Purpose is required")
    private Purpose purpose;

    @CreatedDate
    private Instant createdAt;

    @NotNull(message = "Expiry time is required")
    @Indexed
    private Instant expiresAt;

    /**
     * Flag indicating if this OTP has been used.
     * Once used, an OTP cannot be reused.
     */
    private boolean used = false;

    private Instant usedAt;

    /**
     * Number of failed verification attempts for this OTP.
     * Limited to prevent brute force attacks.
     */
    @Min(value = 0, message = "Attempts cannot be negative")
    @Max(value = 10, message = "Maximum 10 attempts allowed")
    private int attempts = 0;

    /**
     * IP address from which the OTP was requested.
     * Used for security monitoring and rate limiting.
     */
    @Size(max = 45, message = "IP address cannot exceed 45 characters")
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
