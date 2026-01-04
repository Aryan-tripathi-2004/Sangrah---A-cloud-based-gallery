package com.example.Auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing an audit log entry.
 * Tracks all security-relevant actions performed by users and the system.
 * Provides comprehensive audit trail for compliance and security monitoring.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user", columnList = "user_id"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_created", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    /**
     * User who performed the action (nullable for system actions).
     */
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    private UUID userId;

    /**
     * Action performed (e.g., "USER_REGISTERED", "USER_LOGIN", "PASSWORD_CHANGED").
     */
    @NotBlank(message = "Action is required")
    @Size(max = 100, message = "Action cannot exceed 100 characters")
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    /**
     * Type of entity affected (e.g., "User", "Session", "RefreshToken").
     */
    @Size(max = 50, message = "Entity type cannot exceed 50 characters")
    @Column(name = "entity_type", length = 50)
    private String entityType;

    /**
     * ID of the entity affected.
     */
    @Size(max = 100, message = "Entity ID cannot exceed 100 characters")
    @Column(name = "entity_id", length = 100)
    private String entityId;

    /**
     * Additional details about the action in JSON format.
     * Example: {"oldEmail": "old@example.com", "newEmail": "new@example.com"}
     * Stored as JSON in MySQL 5.7+ or TEXT in earlier versions
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "JSON")
    private Map<String, Object> details = new HashMap<>();

    /**
     * IP address from which the action was performed.
     */
    @Size(max = 45, message = "IP address cannot exceed 45 characters")
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent string from the client.
     */
    @Size(max = 500, message = "User agent cannot exceed 500 characters")
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Constructors

    public AuditLog() {
        this.id = UUID.randomUUID();
    }

    public AuditLog(UUID userId, String action) {
        this();
        this.userId = userId;
        this.action = action;
    }

    public AuditLog(UUID userId, String action, String entityType, String entityId) {
        this();
        this.userId = userId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    // Business Methods

    /**
     * Add a detail entry to the audit log.
     *
     * @param key   the detail key
     * @param value the detail value
     * @return this audit log for method chaining
     */
    public AuditLog addDetail(String key, Object value) {
        if (this.details == null) {
            this.details = new HashMap<>();
        }
        this.details.put(key, value);
        return this;
    }

    /**
     * Add multiple detail entries to the audit log.
     *
     * @param details map of detail entries to add
     * @return this audit log for method chaining
     */
    public AuditLog addDetails(Map<String, Object> details) {
        if (this.details == null) {
            this.details = new HashMap<>();
        }
        if (details != null) {
            this.details.putAll(details);
        }
        return this;
    }

    /**
     * Set the request context information (IP address and user agent).
     *
     * @param ipAddress IP address of the client
     * @param userAgent user agent string
     * @return this audit log for method chaining
     */
    public AuditLog withRequestContext(String ipAddress, String userAgent) {
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        return this;
    }

    /**
     * Create an audit log for a successful user login.
     *
     * @param userId    the user ID
     * @param ipAddress the IP address
     * @param userAgent the user agent
     * @return the audit log entry
     */
    public static AuditLog userLogin(UUID userId, String ipAddress, String userAgent) {
        AuditLog log = new AuditLog(userId, "USER_LOGIN", "User", userId.toString());
        log.withRequestContext(ipAddress, userAgent);
        return log;
    }

    /**
     * Create an audit log for a failed login attempt.
     *
     * @param email     the email used for login attempt
     * @param reason    the failure reason
     * @param ipAddress the IP address
     * @param userAgent the user agent
     * @return the audit log entry
     */
    public static AuditLog loginFailed(String email, String reason, String ipAddress, String userAgent) {
        AuditLog log = new AuditLog(null, "LOGIN_FAILED");
        log.addDetail("email", email);
        log.addDetail("reason", reason);
        log.withRequestContext(ipAddress, userAgent);
        return log;
    }

    /**
     * Create an audit log for user registration.
     *
     * @param userId    the user ID
     * @param email     the email address
     * @param ipAddress the IP address
     * @param userAgent the user agent
     * @return the audit log entry
     */
    public static AuditLog userRegistered(UUID userId, String email, String ipAddress, String userAgent) {
        AuditLog log = new AuditLog(userId, "USER_REGISTERED", "User", userId.toString());
        log.addDetail("email", email);
        log.withRequestContext(ipAddress, userAgent);
        return log;
    }

    /**
     * Create an audit log for password change.
     *
     * @param userId the user ID
     * @return the audit log entry
     */
    public static AuditLog passwordChanged(UUID userId) {
        return new AuditLog(userId, "PASSWORD_CHANGED", "User", userId.toString());
    }

    /**
     * Create an audit log for session revocation.
     *
     * @param userId    the user ID
     * @param sessionId the session ID
     * @param reason    the revocation reason
     * @return the audit log entry
     */
    public static AuditLog sessionRevoked(UUID userId, UUID sessionId, String reason) {
        AuditLog log = new AuditLog(userId, "SESSION_REVOKED", "Session", sessionId.toString());
        log.addDetail("reason", reason);
        return log;
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

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof AuditLog))
            return false;
        AuditLog auditLog = (AuditLog) o;
        return id != null && id.equals(auditLog.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", userId=" + userId +
                ", action='" + action + '\'' +
                ", entityType='" + entityType + '\'' +
                ", entityId='" + entityId + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
