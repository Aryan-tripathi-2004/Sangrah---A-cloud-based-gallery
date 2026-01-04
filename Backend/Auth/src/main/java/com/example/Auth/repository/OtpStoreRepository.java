package com.example.Auth.repository;

import com.example.Auth.entity.OtpStore;
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
 * Repository interface for OtpStore entity operations.
 * Provides CRUD operations and custom queries for OTP management.
 * Note: Update operations are handled in the DAO layer using EntityManager.
 */
@Repository
public interface OtpStoreRepository extends JpaRepository<OtpStore, UUID> {

        /**
         * Find the most recent valid OTP for an email and purpose.
         * Returns only non-expired, non-used OTPs that haven't exceeded max attempts.
         *
         * @param email   the email address
         * @param purpose the OTP purpose
         * @param now     current timestamp for expiry check
         * @return Optional containing the OTP if found
         */
        @Query("SELECT o FROM OtpStore o WHERE o.email = :email AND o.purpose = :purpose AND o.used = false AND o.expiresAt > :now AND o.attempts < 10 ORDER BY o.createdAt DESC")
        Optional<OtpStore> findValidOtpByEmailAndPurpose(@Param("email") String email,
                        @Param("purpose") OtpStore.Purpose purpose, @Param("now") Instant now);

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
        @Query("SELECT o FROM OtpStore o WHERE o.expiresAt <= :now")
        List<OtpStore> findExpiredOtps(@Param("now") Instant now);

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
        @Query("SELECT o FROM OtpStore o WHERE o.attempts >= 10")
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
        @Query("SELECT o FROM OtpStore o WHERE o.email = :email AND o.purpose = :purpose AND o.createdAt >= :since ORDER BY o.createdAt DESC")
        List<OtpStore> findRecentOtpsByEmailAndPurpose(@Param("email") String email,
                        @Param("purpose") OtpStore.Purpose purpose, @Param("since") Instant since);

        /**
         * Count OTPs generated for an email in a time period.
         * Used for rate limiting.
         *
         * @param email   the email address
         * @param purpose the OTP purpose
         * @param since   timestamp for the lookback period
         * @return count of OTPs
         */
        @Query("SELECT COUNT(o) FROM OtpStore o WHERE o.email = :email AND o.purpose = :purpose AND o.createdAt >= :since")
        long countRecentOtpsByEmailAndPurpose(@Param("email") String email, @Param("purpose") OtpStore.Purpose purpose,
                        @Param("since") Instant since);

        // Note: Update operations (markAsUsed, incrementAttempts) are handled in the
        // DAO layer using EntityManager

        /**
         * Delete expired OTPs.
         * Used for cleanup operations.
         *
         * @param expiryDate cutoff date for deletion
         */
        @Modifying
        void deleteByExpiresAtBefore(Instant expiryDate);

        /**
         * Delete all OTPs for a specific email and purpose.
         * Used when invalidating old OTPs before generating a new one.
         *
         * @param email   the email address
         * @param purpose the OTP purpose
         */
        @Modifying
        void deleteByEmailAndPurpose(String email, OtpStore.Purpose purpose);

        /**
         * Delete used OTPs older than a specific date.
         * Used for cleanup operations.
         *
         * @param date the cutoff date
         */
        @Modifying
        @Query("DELETE FROM OtpStore o WHERE o.used = true AND o.usedAt < :date")
        void deleteUsedOtpsOlderThan(@Param("date") Instant date);

        /**
         * Count active (valid, non-expired, non-used) OTPs for a user.
         *
         * @param userId the user ID
         * @param now    current timestamp
         * @return count of active OTPs
         */
        @Query("SELECT COUNT(o) FROM OtpStore o WHERE o.userId = :userId AND o.used = false AND o.expiresAt > :now AND o.attempts < 10")
        long countActiveOtpsByUserId(@Param("userId") UUID userId, @Param("now") Instant now);
}
