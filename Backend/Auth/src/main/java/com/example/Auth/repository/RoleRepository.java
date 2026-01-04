package com.example.Auth.repository;

import com.example.Auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Role entity operations.
 * Provides CRUD operations and custom queries for role management.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Find a role by name.
     *
     * @param name the role name (e.g., "ROLE_USER", "ROLE_ADMIN")
     * @return Optional containing the role if found
     */
    Optional<Role> findByName(String name);

    /**
     * Check if a role exists by name.
     *
     * @param name the role name
     * @return true if the role exists
     */
    boolean existsByName(String name);

    /**
     * Delete a role by name.
     *
     * @param name the role name
     */
    void deleteByName(String name);
}
