package com.example.Auth.repository;

import com.example.Auth.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for UserProfile entity operations.
 * Provides CRUD operations and custom queries for user profile management.
 * Note: Update operations are handled in the DAO layer using EntityManager.
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

        /**
         * Find a user profile by user ID.
         *
         * @param userId the user ID
         * @return Optional containing the profile if found
         */
        @Query("SELECT up FROM UserProfile up WHERE up.user.id = :userId")
        Optional<UserProfile> findByUserId(@Param("userId") UUID userId);

        /**
         * Find a user profile by display name (case-insensitive).
         *
         * @param displayName the display name to search for
         * @return Optional containing the profile if found
         */
        Optional<UserProfile> findByDisplayNameIgnoreCase(String displayName);

        /**
         * Check if a display name already exists (case-insensitive).
         *
         * @param displayName the display name to check
         * @return true if the display name exists
         */
        boolean existsByDisplayNameIgnoreCase(String displayName);

        /**
         * Find all profiles with email verified.
         *
         * @return list of profiles with verified emails
         */
        List<UserProfile> findByEmailVerifiedTrue();

        /**
         * Find all profiles with email not verified.
         *
         * @return list of profiles with unverified emails
         */
        List<UserProfile> findByEmailVerifiedFalse();

        /**
         * Find all premium users.
         *
         * @return list of premium user profiles
         */
        List<UserProfile> findByPremiumTrue();

        /**
         * Find all deleted users.
         *
         * @return list of deleted user profiles
         */
        List<UserProfile> findByDeletedTrue();

        /**
         * Find all suspended users.
         *
         * @return list of suspended user profiles
         */
        List<UserProfile> findBySuspendedTrue();

        /**
         * Find all test accounts.
         *
         * @return list of test account profiles
         */
        List<UserProfile> findByTestAccountTrue();

        /**
         * Find public profiles.
         *
         * @return list of public profiles
         */
        List<UserProfile> findByProfilePublicTrue();

        /**
         * Find profiles with two-factor authentication enabled.
         *
         * @return list of profiles with 2FA enabled
         */
        List<UserProfile> findByTwoFactorEnabledTrue();

        // Note: Update operations (updateEmailVerified, updatePremiumStatus, etc.)
        // are handled in the DAO layer using EntityManager

        /**
         * Count premium users.
         *
         * @return count of premium users
         */
        long countByPremiumTrue();

        /**
         * Count users with verified emails.
         *
         * @return count of users with verified emails
         */
        long countByEmailVerifiedTrue();

        /**
         * Count suspended users.
         *
         * @return count of suspended users
         */
        long countBySuspendedTrue();

        /**
         * Count deleted users.
         *
         * @return count of deleted users
         */
        long countByDeletedTrue();
}
