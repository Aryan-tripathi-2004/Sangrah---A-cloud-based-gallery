package com.example.Auth.dao;

import com.example.Auth.entity.User;
import com.example.Auth.model.UserModel;
import com.example.Auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DAO for User operations.
 * Handles data access logic and transformations between User entity and
 * UserModel.
 * Services should call this DAO instead of directly using repositories.
 */
@Component
public class UserDao extends BaseDao<User, UserModel, UUID> {

    @Autowired
    private UserRepository userRepository;

    @Override
    protected Class<User> getEntityClass() {
        return User.class;
    }

    @Override
    protected UserModel toModel(User entity) {
        if (entity == null) {
            return null;
        }

        UserModel model = new UserModel();
        model.setId(entity.getId());
        model.setUsername(entity.getUsername());
        model.setEmail(entity.getEmail());
        model.setPasswordHash(entity.getPasswordHash());
        model.setEnabled(entity.getEnabled());
        model.setLocked(entity.getLocked());
        model.setLastLoginAt(entity.getLastLoginAt());
        model.setProvider(entity.getProvider() != null ? entity.getProvider().name() : null);
        model.setProviderId(entity.getProviderId());
        model.setCreatedAt(entity.getCreatedAt());
        model.setUpdatedAt(entity.getUpdatedAt());

        // Note: Roles and Profile are loaded separately to avoid circular dependencies
        // and optimize performance (lazy loading)

        return model;
    }

    @Override
    protected User toEntity(UserModel model) {
        if (model == null) {
            return null;
        }

        User entity = new User();
        entity.setId(model.getId());
        entity.setUsername(model.getUsername());
        entity.setEmail(model.getEmail());
        entity.setPasswordHash(model.getPasswordHash());
        entity.setEnabled(model.isEnabled());
        entity.setLocked(model.isLocked());
        entity.setLastLoginAt(model.getLastLoginAt());

        if (model.getProvider() != null) {
            entity.setProvider(User.AuthProvider.valueOf(model.getProvider()));
        }
        entity.setProviderId(model.getProviderId());

        return entity;
    }

    // Business-specific query methods

    /**
     * Find user by username (case-insensitive).
     *
     * @param username the username
     * @return Optional containing UserModel if found
     */
    public Optional<UserModel> findByUsername(String username) {
        Optional<User> entity = userRepository.findByUsernameIgnoreCase(username);
        return entity.map(this::toModel);
    }

    /**
     * Find user by email (case-insensitive).
     *
     * @param email the email
     * @return Optional containing UserModel if found
     */
    public Optional<UserModel> findByEmail(String email) {
        Optional<User> entity = userRepository.findByEmailIgnoreCase(email);
        return entity.map(this::toModel);
    }

    /**
     * Find user by username or email.
     *
     * @param usernameOrEmail the username or email
     * @return Optional containing UserModel if found
     */
    public Optional<UserModel> findByUsernameOrEmail(String usernameOrEmail) {
        Optional<User> entity = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
        return entity.map(this::toModel);
    }

    /**
     * Check if username exists.
     *
     * @param username the username
     * @return true if exists
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsernameIgnoreCase(username);
    }

    /**
     * Check if email exists.
     *
     * @param email the email
     * @return true if exists
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }

    /**
     * Find all enabled users.
     *
     * @return list of enabled users
     */
    public List<UserModel> findEnabledUsers() {
        List<User> entities = userRepository.findByEnabledTrue();
        return entities.stream().map(this::toModel).toList();
    }

    /**
     * Find inactive users since a specific date.
     *
     * @param date the cutoff date
     * @return list of inactive users
     */
    public List<UserModel> findInactiveUsersSince(Instant date) {
        List<User> entities = userRepository.findInactiveUsersSince(date);
        return entities.stream().map(this::toModel).toList();
    }

    // Update operations using MongoTemplate

    /**
     * Update last login timestamp for a user.
     *
     * @param userId      the user ID
     * @param lastLoginAt the login timestamp
     * @return true if updated successfully
     */
    public boolean updateLastLogin(UUID userId, Instant lastLoginAt) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update().set("lastLoginAt", lastLoginAt);
        return updateOne(query, update);
    }

    /**
     * Lock a user account.
     *
     * @param userId the user ID
     * @return true if locked successfully
     */
    public boolean lockUser(UUID userId) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update().set("locked", true);
        return updateOne(query, update);
    }

    /**
     * Unlock a user account.
     *
     * @param userId the user ID
     * @return true if unlocked successfully
     */
    public boolean unlockUser(UUID userId) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update().set("locked", false);
        return updateOne(query, update);
    }

    /**
     * Enable a user account.
     *
     * @param userId the user ID
     * @return true if enabled successfully
     */
    public boolean enableUser(UUID userId) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update().set("enabled", true);
        return updateOne(query, update);
    }

    /**
     * Disable a user account.
     *
     * @param userId the user ID
     * @return true if disabled successfully
     */
    public boolean disableUser(UUID userId) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update().set("enabled", false);
        return updateOne(query, update);
    }

    /**
     * Update user password.
     *
     * @param userId       the user ID
     * @param passwordHash the new password hash
     * @return true if updated successfully
     */
    public boolean updatePassword(UUID userId, String passwordHash) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update().set("passwordHash", passwordHash);
        return updateOne(query, update);
    }

    // Counting methods

    /**
     * Count all enabled users.
     *
     * @return count of enabled users
     */
    public long countEnabledUsers() {
        return userRepository.countByEnabledTrue();
    }

    /**
     * Count all locked users.
     *
     * @return count of locked users
     */
    public long countLockedUsers() {
        return userRepository.countByLockedTrue();
    }
}
