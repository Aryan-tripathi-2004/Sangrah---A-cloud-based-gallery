package com.example.Auth.repository;

import com.example.Auth.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for AuditLog entity operations.
 * Provides CRUD operations and custom queries for audit log management.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

        /**
         * Find all audit logs for a specific user.
         *
         * @param userId   the user ID
         * @param pageable pagination information
         * @return page of audit logs
         */
        Page<AuditLog> findByUserId(UUID userId, Pageable pageable);

        /**
         * Find audit logs by action type.
         *
         * @param action   the action type
         * @param pageable pagination information
         * @return page of audit logs
         */
        Page<AuditLog> findByAction(String action, Pageable pageable);

        /**
         * Find audit logs by entity type.
         *
         * @param entityType the entity type
         * @param pageable   pagination information
         * @return page of audit logs
         */
        Page<AuditLog> findByEntityType(String entityType, Pageable pageable);

        /**
         * Find audit logs for a specific entity.
         *
         * @param entityType the entity type
         * @param entityId   the entity ID
         * @param pageable   pagination information
         * @return page of audit logs
         */
        Page<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

        /**
         * Find audit logs by IP address.
         * Used for security monitoring.
         *
         * @param ipAddress the IP address
         * @param pageable  pagination information
         * @return page of audit logs
         */
        Page<AuditLog> findByIpAddress(String ipAddress, Pageable pageable);

        /**
         * Find audit logs within a date range.
         *
         * @param startDate start of the range
         * @param endDate   end of the range
         * @param pageable  pagination information
         * @return page of audit logs
         */
        @Query("SELECT a FROM AuditLog a WHERE a.createdAt >= :start AND a.createdAt <= :end ORDER BY a.createdAt DESC")
        Page<AuditLog> findByDateRange(@Param("start") Instant startDate, @Param("end") Instant endDate,
                        Pageable pageable);

        /**
         * Find audit logs for a user within a date range.
         *
         * @param userId    the user ID
         * @param startDate start of the range
         * @param endDate   end of the range
         * @param pageable  pagination information
         * @return page of audit logs
         */
        @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.createdAt >= :start AND a.createdAt <= :end ORDER BY a.createdAt DESC")
        Page<AuditLog> findByUserIdAndDateRange(@Param("userId") UUID userId, @Param("start") Instant startDate,
                        @Param("end") Instant endDate, Pageable pageable);

        /**
         * Find audit logs by action and user.
         *
         * @param userId   the user ID
         * @param action   the action type
         * @param pageable pagination information
         * @return page of audit logs
         */
        Page<AuditLog> findByUserIdAndAction(UUID userId, String action, Pageable pageable);

        /**
         * Find recent audit logs for a user (last N entries).
         *
         * @param userId   the user ID
         * @param pageable pagination information
         * @return page of audit logs ordered by creation date descending
         */
        @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId ORDER BY a.createdAt DESC")
        Page<AuditLog> findRecentByUserId(@Param("userId") UUID userId, Pageable pageable);

        /**
         * Find all system actions (where userId is null).
         *
         * @param pageable pagination information
         * @return page of system audit logs
         */
        @Query("SELECT a FROM AuditLog a WHERE a.userId IS NULL ORDER BY a.createdAt DESC")
        Page<AuditLog> findSystemActions(Pageable pageable);

        /**
         * Find failed login attempts within a time period.
         * Used for security monitoring and brute force detection.
         *
         * @param since    timestamp for the lookback period
         * @param pageable pagination information
         * @return page of failed login audit logs
         */
        @Query("SELECT a FROM AuditLog a WHERE a.action = 'LOGIN_FAILED' AND a.createdAt >= :since ORDER BY a.createdAt DESC")
        Page<AuditLog> findFailedLoginsSince(@Param("since") Instant since, Pageable pageable);

        /**
         * Count failed login attempts for a specific IP in a time period.
         *
         * @param ipAddress the IP address
         * @param since     timestamp for the lookback period
         * @return count of failed attempts
         */
        @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action = 'LOGIN_FAILED' AND a.ipAddress = :ip AND a.createdAt >= :since")
        long countFailedLoginsByIpSince(@Param("ip") String ipAddress, @Param("since") Instant since);

        /**
         * Count failed login attempts for a specific email in a time period.
         * Note: This requires the email to be stored in the details field.
         *
         * @param email the email address
         * @param since timestamp for the lookback period
         * @return count of failed attempts
         */
        @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action = 'LOGIN_FAILED' AND JSON_EXTRACT(a.details, '$.email') = :email AND a.createdAt >= :since")
        long countFailedLoginsByEmailSince(@Param("email") String email, @Param("since") Instant since);

        /**
         * Find audit logs by action types (multiple actions).
         *
         * @param actions  list of action types
         * @param pageable pagination information
         * @return page of audit logs
         */
        @Query("SELECT a FROM AuditLog a WHERE a.action IN :actions ORDER BY a.createdAt DESC")
        Page<AuditLog> findByActions(@Param("actions") List<String> actions, Pageable pageable);

        /**
         * Count audit logs by action type.
         *
         * @param action the action type
         * @return count of audit logs
         */
        long countByAction(String action);

        /**
         * Count audit logs for a user.
         *
         * @param userId the user ID
         * @return count of audit logs
         */
        long countByUserId(UUID userId);

        /**
         * Delete old audit logs.
         * Used for data retention compliance.
         *
         * @param date cutoff date for deletion
         */
        @Modifying
        void deleteByCreatedAtBefore(Instant date);
}
