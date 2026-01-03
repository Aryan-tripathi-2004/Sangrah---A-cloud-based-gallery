package com.example.Auth.repository;

import com.example.Auth.entity.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for RefreshToken entity operations.
 * Provides CRUD operations and custom queries for refresh token management.
 * Note: Update operations are handled in the DAO layer using MongoTemplate.
 */
@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, UUID> {

        /**
         * Find a refresh token by its hash.
         * Used during token refresh to validate the provided token.
         *
         * @param tokenHash the SHA-256 hash of the token
         * @return Optional containing the token if found
         */
        Optional<RefreshToken> findByTokenHash(String tokenHash);

        /**
         * Find all refresh tokens for a specific user.
         *
         * @param userId the user ID
         * @return list of refresh tokens
         */
        @Query("{ 'user.$id': ?0 }")
        List<RefreshToken> findByUserId(UUID userId);

        /**
         * Find all valid (non-revoked, non-expired) refresh tokens for a user.
         *
         * @param userId the user ID
         * @param now    current timestamp for expiry check
         * @return list of valid tokens
         */
        @Query("{ 'user.$id': ?0, 'revoked': false, 'expiresAt': { '$gt': ?1 } }")
        List<RefreshToken> findValidTokensByUserId(UUID userId, Instant now);

        /**
         * Find all refresh tokens for a specific session.
         * A session can have multiple refresh tokens (e.g., token rotation).
         *
         * @param sessionId the session ID
         * @return list of refresh tokens
         */
        List<RefreshToken> findBySessionId(UUID sessionId);

        /**
         * Find valid refresh tokens for a specific session.
         *
         * @param sessionId the session ID
         * @param now       current timestamp for expiry check
         * @return list of valid tokens
         */
        @Query("{ 'sessionId': ?0, 'revoked': false, 'expiresAt': { '$gt': ?1 } }")
        List<RefreshToken> findValidTokensBySessionId(UUID sessionId, Instant now);

        /**
         * Find all expired refresh tokens.
         * Used for cleanup operations.
         *
         * @param now current timestamp
         * @return list of expired tokens
         */
        @Query("{ 'expiresAt': { '$lte': ?0 } }")
        List<RefreshToken> findExpiredTokens(Instant now);

        /**
         * Find all revoked refresh tokens.
         *
         * @return list of revoked tokens
         */
        List<RefreshToken> findByRevokedTrue();

        // Note: Update operations (revoke, delete) are handled in the DAO layer using
        // MongoTemplate

        /**
         * Delete expired refresh tokens by date.
         * MongoDB will automatically delete documents matching the criteria.
         *
         * @param expiryDate cutoff date for deletion
         */
        void deleteByExpiresAtBefore(Instant expiryDate);

        /**
         * Count active (non-revoked, non-expired) tokens for a user.
         *
         * @param userId the user ID
         * @param now    current timestamp
         * @return count of active tokens
         */
        @Query(value = "{ 'user.$id': ?0, 'revoked': false, 'expiresAt': { '$gt': ?1 } }", count = true)
        long countActiveTokensByUserId(UUID userId, Instant now);

        /**
         * Count total tokens by user.
         *
         * @param userId the user ID
         * @return total token count
         */
        @Query(value = "{ 'user.$id': ?0 }", count = true)
        long countByUserId(UUID userId);
}
