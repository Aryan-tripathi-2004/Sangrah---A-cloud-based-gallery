package com.example.Auth.infrastructure.persistence.repository;

import com.example.Auth.infrastructure.persistence.document.UserDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User persistence in MongoDB.
 * Provides CRUD operations and custom queries for User entities.
 */
@Repository
public interface UserRepository extends MongoRepository<UserDocument, String> {
    /**
     * Find a user by email address
     */
    Optional<UserDocument> findByEmail(String email);

    /**
     * Check if user exists by email
     */
    boolean existsByEmail(String email);

    /**
     * Find a user by display name
     */
    Optional<UserDocument> findByDisplayName(String displayName);
}
