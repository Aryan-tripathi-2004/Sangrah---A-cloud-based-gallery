package com.example.Auth.repository;

import com.example.Auth.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Session entity operations.
 * Provides CRUD operations and custom queries for session management.
 * Note: Update operations are handled in the DAO layer using EntityManager.
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

        /**
         * Find a session by session ID.
         *
         * @param sessionId the session ID
         * @return Optional containing the session if found
         */
        Optional<Session> findBySessionId(UUID sessionId);

        /**
         * Find all sessions for a specific user.
         *
         * @param userId the user ID
         * @return list of sessions
         */
        @Query("SELECT s FROM Session s WHERE s.user.id = :userId")
        List<Session> findByUserId(@Param("userId") UUID userId);

        /**
         * Find all active (non-revoked, non-expired) sessions for a user.
         *
         * @param userId the user ID
         * @param now    current timestamp for expiry check
         * @return list of active sessions
         */
        @Query("SELECT s FROM Session s WHERE s.user.id = :userId AND s.revoked = false AND s.expiresAt > :now")
        List<Session> findActiveSessionsByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

        /**
         * Find a session by user ID and device fingerprint.
         * Used to identify if a user is logging in from a known device.
         *
         * @param userId            the user ID
         * @param deviceFingerprint the device fingerprint
         * @param now               current timestamp
         * @return List of matching sessions
         */
        @Query("SELECT s FROM Session s WHERE s.user.id = :userId AND s.deviceFingerprint = :fingerprint AND s.revoked = false AND s.expiresAt > :now ORDER BY s.lastAccessedAt DESC")
        List<Session> findByUserIdAndDeviceFingerprint(@Param("userId") UUID userId,
                        @Param("fingerprint") String deviceFingerprint, @Param("now") Instant now);

        /**
         * Find sessions by IP address.
         * Used for security monitoring and anomaly detection.
         *
         * @param ipAddress the IP address
         * @return list of sessions
         */
        List<Session> findByIpAddress(String ipAddress);

        /**
         * Find all expired sessions.
         * Used for cleanup operations.
         *
         * @param now current timestamp
         * @return list of expired sessions
         */
        @Query("SELECT s FROM Session s WHERE s.expiresAt <= :now")
        List<Session> findExpiredSessions(@Param("now") Instant now);

        /**
         * Find all revoked sessions.
         *
         * @return list of revoked sessions
         */
        List<Session> findByRevokedTrue();

        /**
         * Find sessions that haven't been accessed recently.
         * Used to identify stale sessions.
         *
         * @param cutoffDate the cutoff date
         * @param now        current timestamp
         * @return list of inactive sessions
         */
        @Query("SELECT s FROM Session s WHERE s.lastAccessedAt < :cutoff AND s.revoked = false AND s.expiresAt > :now")
        List<Session> findInactiveSessions(@Param("cutoff") Instant cutoffDate, @Param("now") Instant now);

        /**
         * Count active sessions for a user.
         *
         * @param userId the user ID
         * @param now    current timestamp
         * @return count of active sessions
         */
        @Query("SELECT COUNT(s) FROM Session s WHERE s.user.id = :userId AND s.revoked = false AND s.expiresAt > :now")
        long countActiveSessionsByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

        /**
         * Count total sessions by user.
         *
         * @param userId the user ID
         * @return total session count
         */
        @Query("SELECT COUNT(s) FROM Session s WHERE s.user.id = :userId")
        long countByUserId(@Param("userId") UUID userId);

        /**
         * Check if a session exists and is valid.
         *
         * @param sessionId the session ID
         * @param now       current timestamp
         * @return true if the session exists and is valid
         */
        @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Session s WHERE s.sessionId = :sessionId AND s.revoked = false AND s.expiresAt > :now")
        boolean existsBySessionIdAndValid(@Param("sessionId") UUID sessionId, @Param("now") Instant now);

        /**
         * Delete expired sessions.
         * Used for cleanup operations.
         *
         * @param expiryDate cutoff date for deletion
         */
        @Modifying
        void deleteByExpiresAtBefore(Instant expiryDate);
}
