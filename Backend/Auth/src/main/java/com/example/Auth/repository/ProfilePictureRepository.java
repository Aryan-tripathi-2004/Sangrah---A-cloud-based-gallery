package com.example.Auth.repository;

import com.example.Auth.entity.ProfilePicture;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for ProfilePicture entity operations.
 * Provides CRUD operations and custom queries for profile picture management.
 * Note: Update operations are handled in the DAO layer using MongoTemplate.
 */
@Repository
public interface ProfilePictureRepository extends MongoRepository<ProfilePicture, UUID> {

        /**
         * Find all profile pictures for a specific user.
         *
         * @param userId the user ID
         * @return list of profile pictures ordered by creation date descending
         */
        @Query(value = "{ 'user.$id': ?0 }", sort = "{ 'createdAt': -1 }")
        List<ProfilePicture> findByUserId(UUID userId);

        /**
         * Find the current profile picture for a user.
         *
         * @param userId the user ID
         * @return Optional containing the current profile picture if found
         */
        @Query("{ 'user.$id': ?0, 'current': true }")
        Optional<ProfilePicture> findCurrentByUserId(UUID userId);

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
        @Query(value = "{ 'user.$id': ?0, 'current': true }", exists = true)
        boolean existsCurrentByUserId(UUID userId);

        // Note: Update operations (setCurrentPicture, unmarkAllAsCurrentForUser)
        // are handled in the DAO layer using MongoTemplate

        /**
         * Delete all profile pictures for a user.
         * Used when deleting a user account.
         *
         * @param userId the user ID
         */
        @Query(value = "{ 'user.$id': ?0 }", delete = true)
        void deleteByUserId(UUID userId);

        /**
         * Delete a profile picture by storage key.
         *
         * @param storageKey the storage key
         */
        void deleteByStorageKey(String storageKey);

        /**
         * Count profile pictures for a user.
         *
         * @param userId the user ID
         * @return count of profile pictures
         */
        @Query(value = "{ 'user.$id': ?0 }", count = true)
        long countByUserId(UUID userId);

        /**
         * Calculate total storage used by a user's profile pictures.
         * Note: Aggregation queries are better handled in the DAO layer.
         *
         * @param userId the user ID
         * @return total size in bytes
         */
        @Query(value = "{ 'user.$id': ?0 }")
        List<ProfilePicture> findAllByUserId(UUID userId);

        /**
         * Find profile pictures larger than a specific size.
         * Used for storage optimization.
         *
         * @param minSizeBytes minimum size in bytes
         * @return list of large profile pictures
         */
        @Query(value = "{ 'sizeBytes': { '$gte': ?0 } }", sort = "{ 'sizeBytes': -1 }")
        List<ProfilePicture> findLargePictures(long minSizeBytes);
}
