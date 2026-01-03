package com.example.Auth.dao;

import com.example.Auth.entity.Role;
import com.example.Auth.model.RoleModel;
import com.example.Auth.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * DAO for Role operations.
 * Handles data access logic and transformations between Role entity and
 * RoleModel.
 */
@Component
public class RoleDao extends BaseDao<Role, RoleModel, UUID> {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    protected Class<Role> getEntityClass() {
        return Role.class;
    }

    @Override
    protected RoleModel toModel(Role entity) {
        if (entity == null) {
            return null;
        }

        RoleModel model = new RoleModel();
        model.setId(entity.getId());
        model.setName(entity.getName());
        model.setDescription(entity.getDescription());
        model.setCreatedAt(entity.getCreatedAt());
        model.setUpdatedAt(entity.getUpdatedAt());

        return model;
    }

    @Override
    protected Role toEntity(RoleModel model) {
        if (model == null) {
            return null;
        }

        Role entity = new Role();
        entity.setId(model.getId());
        entity.setName(model.getName());
        entity.setDescription(model.getDescription());

        return entity;
    }

    // Business-specific query methods

    /**
     * Find role by name.
     *
     * @param name the role name
     * @return Optional containing RoleModel if found
     */
    public Optional<RoleModel> findByName(String name) {
        Optional<Role> entity = roleRepository.findByName(name);
        return entity.map(this::toModel);
    }

    /**
     * Check if role exists by name.
     *
     * @param name the role name
     * @return true if exists
     */
    public boolean existsByName(String name) {
        return roleRepository.existsByName(name);
    }
}
