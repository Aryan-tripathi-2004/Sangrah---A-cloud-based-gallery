package com.example.Auth.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Business model for OtpStore.
 * Used in the service layer for OTP verification operations.
 * Decoupled from database entity annotations.
 */
public class OtpStoreModel {
    private UUID id;
    private String otpHash;
    private String email;
    private UUID userId;
    private Purpose purpose;
    private Instant expiresAt;
    private boolean used;
    private Instant usedAt;
    private int attempts;
    private Instant createdAt;

    /**
     * OTP purpose enum.
     */
    public enum Purpose {
        EMAIL_VERIFICATION,
        PASSWORD_RESET,
        TWO_FACTOR_AUTH,
        ACCOUNT_RECOVERY,
        PHONE_VERIFICATION
    }

    // Constructors

    public OtpStoreModel() {
    }

    public OtpStoreModel(String otpHash, String email, Purpose purpose, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.otpHash = otpHash;
        this.email = email;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
        this.used = false;
        this.attempts = 0;
        this.createdAt = Instant.now();
    }

    // Business Logic Methods

    /**
     * Check if the OTP is expired.
     *
     * @return true if expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if the OTP is valid (not used, not expired, attempts below max).
     *
     * @return true if valid
     */
    public boolean isValid() {
        return !used && !isExpired() && attempts < 10;
    }

    /**
     * Mark OTP as used.
     */
    public void markAsUsed() {
        this.used = true;
        this.usedAt = Instant.now();
    }

    /**
     * Increment the attempt counter.
     *
     * @return true if attempts are still below max
     */
    public boolean incrementAttempt() {
        this.attempts++;
        return this.attempts < 10;
    }

    /**
     * Check if maximum attempts have been reached.
     *
     * @return true if max attempts reached
     */
    public boolean hasReachedMaxAttempts() {
        return attempts >= 10;
    }

    /**
     * Get remaining attempts.
     *
     * @return number of remaining attempts
     */
    public int getRemainingAttempts() {
        return Math.max(0, 10 - attempts);
    }

    /**
     * Get time remaining until expiry in seconds.
     *
     * @return seconds until expiry, or 0 if expired
     */
    public long getSecondsUntilExpiry() {
        if (expiresAt == null || isExpired()) {
            return 0;
        }
        return Instant.now().until(expiresAt, java.time.temporal.ChronoUnit.SECONDS);
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Purpose getPurpose() {
        return purpose;
    }

    public void setPurpose(Purpose purpose) {
        this.purpose = purpose;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
