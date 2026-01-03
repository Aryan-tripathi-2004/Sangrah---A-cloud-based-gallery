package com.example.Auth.repository;

import com.example.Auth.entity.OtpStore;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for OtpStore entity operations.
 * Provides CRUD operations and custom queries for OTP management.
 * Note: Update operations are handled in the DAO layer using MongoTemplate.
 */
@Repository
public interface OtpStoreRepository extends MongoRepository<OtpStore, UUID> {

        /**
         * Find the most recent valid OTP for an email and purpose.
         * Returns only non-expired, non-used OTPs that haven't exceeded max attempts.
         *
         * @param email   the email address
         * @param purpose the OTP purpose
         * @param now     current timestamp for expiry check
         * @return Optional containing the OTP if found
         */
        @Query(value = "{ 'email': ?0, 'purpose': ?1, 'used': false, 'expiresAt': { '$gt': ?2 }, 'attempts': { '$lt': 10 } }", sort = "{ 'createdAt': -1 }")
        Optional<OtpStore> findValidOtpByEmailAndPurpose(String email, OtpStore.Purpose purpose, Instant now);

        /**
         * Find all OTPs for a specific user.
         *
         * @param userId the user ID
         * @return list of OTPs
         */
        List<OtpStore> findByUserId(UUID userId);

        /**
         * Find all OTPs for a specific email address.
         *
         * @param email the email address
         * @return list of OTPs
         */
        List<OtpStore> findByEmail(String email);

        /**
         * Find OTPs by purpose.
         *
         * @param purpose the OTP purpose
         * @return list of OTPs
         */
        List<OtpStore> findByPurpose(OtpStore.Purpose purpose);

        /**
         * Find all expired OTPs.
         * Used for cleanup operations.
         *
         * @param now current timestamp
         * @return list of expired OTPs
         */
        @Query("{ 'expiresAt': { '$lte': ?0 } }")
        List<OtpStore> findExpiredOtps(Instant now);

        /**
         * Find all used OTPs.
         *
         * @return list of used OTPs
         */
        List<OtpStore> findByUsedTrue();

        /**
         * Find OTPs that have exceeded maximum attempts.
         *
         * @return list of OTPs with too many attempts
         */
        @Query("{ 'attempts': { '$gte': 10 } }")
        List<OtpStore> findOtpsWithMaxAttempts();

        /**
         * Find recent OTPs for an email (last 24 hours).
         * Used for rate limiting OTP generation.
         *
         * @param email   the email address
         * @param purpose the OTP purpose
         * @param since   timestamp for the lookback period
         * @return list of recent OTPs
         */
        @Query(value = "{ 'email': ?0, 'purpose': ?1, 'createdAt': { '$gte': ?2 } }", sort = "{ 'createdAt': -1 }")
        List<OtpStore> findRecentOtpsByEmailAndPurpose(String email, OtpStore.Purpose purpose, Instant since);

        /**
         * Count OTPs generated for an email in a time period.
         * Used for rate limiting.
         *
         * @param email   the email address
         * @param purpose the OTP purpose
         * @param since   timestamp for the lookback period
         * @return count of OTPs
         */
        @Query(value = "{ 'email': ?0, 'purpose': ?1, 'createdAt': { '$gte': ?2 } }", count = true)
        long countRecentOtpsByEmailAndPurpose(String email, OtpStore.Purpose purpose, Instant since);

        // Note: Update operations (markAsUsed, incrementAttempts) are handled in the
        // DAO layer using MongoTemplate

        /**
         * Delete expired OTPs.
         * Used for cleanup operations.
         *
         * @param expiryDate cutoff date for deletion
         */
        void deleteByExpiresAtBefore(Instant expiryDate);

        /**
         * Delete all OTPs for a specific email and purpose.
         * Used when invalidating old OTPs before generating a new one.
         *
         * @param email   the email address
         * @param purpose the OTP purpose
         */
        void deleteByEmailAndPurpose(String email, OtpStore.Purpose purpose);

        /**
         * Delete used OTPs older than a specific date.
         * Used for cleanup operations.
         *
         * @param date the cutoff date
         */
        @Query(value = "{ 'used': true, 'usedAt': { '$lt': ?0 } }", delete = true)
        void deleteUsedOtpsOlderThan(Instant date);

        /**
         * Count active (valid, non-expired, non-used) OTPs for a user.
         *
         * @param userId the user ID
         * @param now    current timestamp
         * @return count of active OTPs
         */
        @Query(value = "{ 'userId': ?0, 'used': false, 'expiresAt': { '$gt': ?1 }, 'attempts': { '$lt': 10 } }", count = true)
        long countActiveOtpsByUserId(UUID userId, Instant now);
}
