package com.example.Auth.repository;

import com.example.Auth.entity.ProfilePicture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for ProfilePicture entity operations.
 * Provides CRUD operations and custom queries for profile picture management.
 * Note: Update operations are handled in the DAO layer using EntityManager.
 */
@Repository
public interface ProfilePictureRepository extends JpaRepository<ProfilePicture, UUID> {

        /**
         * Find all profile pictures for a specific user.
         *
         * @param userId the user ID
         * @return list of profile pictures ordered by creation date descending
         */
        @Query("SELECT pp FROM ProfilePicture pp WHERE pp.user.id = :userId ORDER BY pp.createdAt DESC")
        List<ProfilePicture> findByUserId(@Param("userId") UUID userId);

        /**
         * Find the current profile picture for a user.
         *
         * @param userId the user ID
         * @return Optional containing the current profile picture if found
         */
        @Query("SELECT pp FROM ProfilePicture pp WHERE pp.user.id = :userId AND pp.current = true")
        Optional<ProfilePicture> findCurrentByUserId(@Param("userId") UUID userId);

        /**
         * Find a profile picture by storage key.
         *
         * @param storageKey the storage key/path in cloud storage
         * @return Optional containing the profile picture if found
         */
        Optional<ProfilePicture> findByStorageKey(String storageKey);

        /**
         * Find all current profile pictures.
         * Used for administrative purposes or batch operations.
         *
         * @return list of current profile pictures
         */
        List<ProfilePicture> findByCurrentTrue();

        /**
         * Check if a user has a current profile picture.
         *
         * @param userId the user ID
         * @return true if the user has a current profile picture
         */
        @Query("SELECT CASE WHEN COUNT(pp) > 0 THEN true ELSE false END FROM ProfilePicture pp WHERE pp.user.id = :userId AND pp.current = true")
        boolean existsCurrentByUserId(@Param("userId") UUID userId);

        // Note: Update operations (setCurrentPicture, unmarkAllAsCurrentForUser)
        // are handled in the DAO layer using EntityManager

        /**
         * Delete all profile pictures for a user.
         * Used when deleting a user account.
         *
         * @param userId the user ID
         */
        @Modifying
        @Query("DELETE FROM ProfilePicture pp WHERE pp.user.id = :userId")
        void deleteByUserId(@Param("userId") UUID userId);

        /**
         * Delete a profile picture by storage key.
         *
         * @param storageKey the storage key
         */
        @Modifying
        void deleteByStorageKey(String storageKey);

        /**
         * Count profile pictures for a user.
         *
         * @param userId the user ID
         * @return count of profile pictures
         */
        @Query("SELECT COUNT(pp) FROM ProfilePicture pp WHERE pp.user.id = :userId")
        long countByUserId(@Param("userId") UUID userId);

        /**
         * Calculate total storage used by a user's profile pictures.
         * Note: Aggregation queries are better handled in the DAO layer.
         *
         * @param userId the user ID
         * @return total size in bytes
         */
        @Query("SELECT pp FROM ProfilePicture pp WHERE pp.user.id = :userId")
        List<ProfilePicture> findAllByUserId(@Param("userId") UUID userId);

        /**
         * Find profile pictures larger than a specific size.
         * Used for storage optimization.
         *
         * @param minSizeBytes minimum size in bytes
         * @return list of large profile pictures
         */
        @Query("SELECT pp FROM ProfilePicture pp WHERE pp.sizeBytes >= :minSize ORDER BY pp.sizeBytes DESC")
        List<ProfilePicture> findLargePictures(@Param("minSize") long minSizeBytes);
}
