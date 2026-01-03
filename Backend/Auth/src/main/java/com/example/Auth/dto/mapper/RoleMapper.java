package com.example.Auth.dto.mapper;

import com.example.Auth.dto.response.RoleResponse;
import com.example.Auth.model.RoleModel;
import org.springframework.stereotype.Component;

/**
 * Mapper for Role-related DTOs.
 */
@Component
public class RoleMapper {

    /**
     * Convert RoleModel to RoleResponse.
     */
    public RoleResponse toResponse(RoleModel model) {
        if (model == null) {
            return null;
        }

        RoleResponse response = new RoleResponse();
        response.setId(model.getId());
        response.setName(model.getName());
        response.setDescription(model.getDescription());

        return response;
    }
}
