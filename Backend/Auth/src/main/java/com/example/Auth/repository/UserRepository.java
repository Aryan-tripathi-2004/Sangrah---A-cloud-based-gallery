package com.example.Auth.repository;

import com.example.Auth.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User entity operations.
 * Provides CRUD operations and custom queries for user management.
 */
@Repository
public interface UserRepository extends MongoRepository<User, UUID> {

    /**
     * Find a user by username (case-insensitive).
     *
     * @param username the username to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByUsernameIgnoreCase(String username);

    /**
     * Find a user by email address (case-insensitive).
     *
     * @param email the email to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Find a user by username or email (case-insensitive).
     * Useful for login where users can use either identifier.
     *
     * @param username the username to search for
     * @param email    the email to search for
     * @return Optional containing the user if found
     */
    @Query("{ '$or': [ { 'username': { '$regex': ?0, '$options': 'i' } }, { 'email': { '$regex': ?1, '$options': 'i' } } ] }")
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * Find a user by OAuth provider and provider ID.
     * Used for OAuth authentication flow.
     *
     * @param provider   the OAuth provider (GOOGLE, MICROSOFT, etc.)
     * @param providerId the unique ID from the provider
     * @return Optional containing the user if found
     */
    Optional<User> findByProviderAndProviderId(User.AuthProvider provider, String providerId);

    /**
     * Check if a username already exists (case-insensitive).
     *
     * @param username the username to check
     * @return true if the username exists
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Check if an email address already exists (case-insensitive).
     *
     * @param email the email to check
     * @return true if the email exists
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Find all enabled users.
     *
     * @return list of enabled users
     */
    List<User> findByEnabledTrue();

    /**
     * Find all locked users.
     *
     * @return list of locked users
     */
    List<User> findByLockedTrue();

    /**
     * Find users by OAuth provider.
     *
     * @param provider the OAuth provider
     * @return list of users registered via the provider
     */
    List<User> findByProvider(User.AuthProvider provider);

    /**
     * Find users who haven't logged in since a specific date.
     * Useful for identifying inactive accounts.
     *
     * @param date the cutoff date
     * @return list of inactive users
     */
    @Query("{ '$or': [ { 'lastLoginAt': null }, { 'lastLoginAt': { '$lt': ?0 } } ] }")
    List<User> findInactiveUsersSince(Instant date);

    /**
     * Count total number of users by provider.
     *
     * @param provider the OAuth provider
     * @return count of users
     */
    long countByProvider(User.AuthProvider provider);

    /**
     * Count enabled users.
     *
     * @return count of enabled users
     */
    long countByEnabledTrue();

    /**
     * Count locked users.
     *
     * @return count of locked users
     */
    long countByLockedTrue();
}
