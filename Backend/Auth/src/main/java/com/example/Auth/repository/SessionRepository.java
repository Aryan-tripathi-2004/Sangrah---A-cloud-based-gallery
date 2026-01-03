package com.example.Auth.repository;

import com.example.Auth.entity.Session;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Session entity operations.
 * Provides CRUD operations and custom queries for session management.
 * Note: Update operations are handled in the DAO layer using MongoTemplate.
 */
@Repository
public interface SessionRepository extends MongoRepository<Session, UUID> {

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
        @Query("{ 'user.$id': ?0 }")
        List<Session> findByUserId(UUID userId);

        /**
         * Find all active (non-revoked, non-expired) sessions for a user.
         *
         * @param userId the user ID
         * @param now    current timestamp for expiry check
         * @return list of active sessions
         */
        @Query("{ 'user.$id': ?0, 'revoked': false, 'expiresAt': { '$gt': ?1 } }")
        List<Session> findActiveSessionsByUserId(UUID userId, Instant now);

        /**
         * Find a session by user ID and device fingerprint.
         * Used to identify if a user is logging in from a known device.
         *
         * @param userId            the user ID
         * @param deviceFingerprint the device fingerprint
         * @param now               current timestamp
         * @return List of matching sessions
         */
        @Query(value = "{ 'user.$id': ?0, 'deviceFingerprint': ?1, 'revoked': false, 'expiresAt': { '$gt': ?2 } }", sort = "{ 'lastAccessedAt': -1 }")
        List<Session> findByUserIdAndDeviceFingerprint(UUID userId, String deviceFingerprint, Instant now);

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
        @Query("{ 'expiresAt': { '$lte': ?0 } }")
        List<Session> findExpiredSessions(Instant now);

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
        @Query("{ 'lastAccessedAt': { '$lt': ?0 }, 'revoked': false, 'expiresAt': { '$gt': ?1 } }")
        List<Session> findInactiveSessions(Instant cutoffDate, Instant now);

        /**
         * Count active sessions for a user.
         *
         * @param userId the user ID
         * @param now    current timestamp
         * @return count of active sessions
         */
        @Query(value = "{ 'user.$id': ?0, 'revoked': false, 'expiresAt': { '$gt': ?1 } }", count = true)
        long countActiveSessionsByUserId(UUID userId, Instant now);

        /**
         * Count total sessions by user.
         *
         * @param userId the user ID
         * @return total session count
         */
        @Query(value = "{ 'user.$id': ?0 }", count = true)
        long countByUserId(UUID userId);

        /**
         * Check if a session exists and is valid.
         *
         * @param sessionId the session ID
         * @param now       current timestamp
         * @return true if the session exists and is valid
         */
        @Query(value = "{ 'sessionId': ?0, 'revoked': false, 'expiresAt': { '$gt': ?1 } }", exists = true)
        boolean existsBySessionIdAndValid(UUID sessionId, Instant now);

        /**
         * Delete expired sessions.
         * Used for cleanup operations.
         *
         * @param expiryDate cutoff date for deletion
         */
        void deleteByExpiresAtBefore(Instant expiryDate);
}
