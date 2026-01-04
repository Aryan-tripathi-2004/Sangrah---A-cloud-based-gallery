package com.example.Auth.dao;

import com.example.Auth.entity.User;
import com.example.Auth.model.UserModel;
import com.example.Auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    // Update operations using EntityManager

    /**
     * Update last login timestamp for a user.
     *
     * @param userId      the user ID
     * @param lastLoginAt the login timestamp
     * @return true if updated successfully
     */
    @Transactional
    public boolean updateLastLogin(UUID userId, Instant lastLoginAt) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLoginAt(lastLoginAt);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    /**
     * Lock a user account.
     *
     * @param userId the user ID
     * @return true if locked successfully
     */
    @Transactional
    public boolean lockUser(UUID userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLocked(true);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    /**
     * Unlock a user account.
     *
     * @param userId the user ID
     * @return true if unlocked successfully
     */
    @Transactional
    public boolean unlockUser(UUID userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLocked(false);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    /**
     * Enable a user account.
     *
     * @param userId the user ID
     * @return true if enabled successfully
     */
    @Transactional
    public boolean enableUser(UUID userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setEnabled(true);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    /**
     * Disable a user account.
     *
     * @param userId the user ID
     * @return true if disabled successfully
     */
    @Transactional
    public boolean disableUser(UUID userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setEnabled(false);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    /**
     * Update user password.
     *
     * @param userId       the user ID
     * @param passwordHash the new password hash
     * @return true if updated successfully
     */
    @Transactional
    public boolean updatePassword(UUID userId, String passwordHash) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPasswordHash(passwordHash);
            userRepository.save(user);
            return true;
        }
        return false;
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
