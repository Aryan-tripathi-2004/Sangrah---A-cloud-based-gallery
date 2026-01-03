package com.example.Auth.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Business model for AuditLog.
 * Used in the service layer for audit logging operations.
 * Decoupled from database entity annotations.
 */
public class AuditLogModel {
    private UUID id;
    private UUID userId;
    private String action;
    private String entityType;
    private String entityId;
    private String ipAddress;
    private String userAgent;
    private Map<String, Object> details;
    private Instant createdAt;

    // Constructors

    public AuditLogModel() {
        this.details = new HashMap<>();
    }

    public AuditLogModel(String action, String entityType, String entityId) {
        this.id = UUID.randomUUID();
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = new HashMap<>();
        this.createdAt = Instant.now();
    }

    // Business Logic Methods

    /**
     * Check if this is a system action (no user).
     *
     * @return true if system action
     */
    public boolean isSystemAction() {
        return userId == null;
    }

    /**
     * Check if this is a failed login attempt.
     *
     * @return true if failed login
     */
    public boolean isFailedLogin() {
        return "LOGIN_FAILED".equals(action);
    }

    /**
     * Add detail to the details map.
     *
     * @param key   the detail key
     * @param value the detail value
     */
    public void addDetail(String key, Object value) {
        if (this.details == null) {
            this.details = new HashMap<>();
        }
        this.details.put(key, value);
    }

    /**
     * Get detail by key.
     *
     * @param key the detail key
     * @return the detail value, or null if not found
     */
    public Object getDetail(String key) {
        return details != null ? details.get(key) : null;
    }

    /**
     * Check if a detail exists.
     *
     * @param key the detail key
     * @return true if exists
     */
    public boolean hasDetail(String key) {
        return details != null && details.containsKey(key);
    }

    /**
     * Get log age in hours.
     *
     * @return hours since creation
     */
    public long getAgeInHours() {
        if (createdAt == null) {
            return 0;
        }
        return createdAt.until(Instant.now(), java.time.temporal.ChronoUnit.HOURS);
    }

    /**
     * Check if this log is older than specified days.
     *
     * @param days number of days
     * @return true if older
     */
    public boolean isOlderThanDays(long days) {
        if (createdAt == null) {
            return false;
        }
        Instant threshold = Instant.now().minus(days, java.time.temporal.ChronoUnit.DAYS);
        return createdAt.isBefore(threshold);
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
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

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
