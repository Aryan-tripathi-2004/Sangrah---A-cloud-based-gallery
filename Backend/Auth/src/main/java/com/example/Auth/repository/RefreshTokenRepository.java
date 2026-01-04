package com.example.Auth.repository;

import com.example.Auth.entity.RefreshToken;
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
 * Repository interface for RefreshToken entity operations.
 * Provides CRUD operations and custom queries for refresh token management.
 * Note: Update operations are handled in the DAO layer using EntityManager.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

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
        @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId")
        List<RefreshToken> findByUserId(@Param("userId") UUID userId);

        /**
         * Find all valid (non-revoked, non-expired) refresh tokens for a user.
         *
         * @param userId the user ID
         * @param now    current timestamp for expiry check
         * @return list of valid tokens
         */
        @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId AND rt.revoked = false AND rt.expiresAt > :now")
        List<RefreshToken> findValidTokensByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

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
        @Query("SELECT rt FROM RefreshToken rt WHERE rt.sessionId = :sessionId AND rt.revoked = false AND rt.expiresAt > :now")
        List<RefreshToken> findValidTokensBySessionId(@Param("sessionId") UUID sessionId, @Param("now") Instant now);

        /**
         * Find all expired refresh tokens.
         * Used for cleanup operations.
         *
         * @param now current timestamp
         * @return list of expired tokens
         */
        @Query("SELECT rt FROM RefreshToken rt WHERE rt.expiresAt <= :now")
        List<RefreshToken> findExpiredTokens(@Param("now") Instant now);

        /**
         * Find all revoked refresh tokens.
         *
         * @return list of revoked tokens
         */
        List<RefreshToken> findByRevokedTrue();

        // Note: Update operations (revoke, delete) are handled in the DAO layer using
        // EntityManager

        /**
         * Delete expired refresh tokens by date.
         *
         * @param expiryDate cutoff date for deletion
         */
        @Modifying
        void deleteByExpiresAtBefore(Instant expiryDate);

        /**
         * Count active (non-revoked, non-expired) tokens for a user.
         *
         * @param userId the user ID
         * @param now    current timestamp
         * @return count of active tokens
         */
        @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user.id = :userId AND rt.revoked = false AND rt.expiresAt > :now")
        long countActiveTokensByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

        /**
         * Count total tokens by user.
         *
         * @param userId the user ID
         * @return total token count
         */
        @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user.id = :userId")
        long countByUserId(@Param("userId") UUID userId);
}
