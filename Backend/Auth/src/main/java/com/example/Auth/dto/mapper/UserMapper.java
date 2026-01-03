package com.example.Auth.dto.mapper;

import com.example.Auth.dto.response.UserResponse;
import com.example.Auth.model.UserModel;
import com.example.Auth.model.UserProfileModel;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Mapper for User-related DTOs.
 */
@Component
public class UserMapper {

    /**
     * Convert UserModel to UserResponse.
     */
    public UserResponse toResponse(UserModel model) {
        if (model == null) {
            return null;
        }

        UserResponse response = new UserResponse();
        response.setId(model.getId());
        response.setUsername(model.getUsername());
        response.setEmail(model.getEmail());
        response.setEnabled(model.isEnabled());
        response.setLocked(model.isLocked());
        response.setCreatedAt(model.getCreatedAt());
        response.setUpdatedAt(model.getUpdatedAt());
        response.setLastLogin(model.getLastLoginAt());

        // Map roles to role names
        if (model.getRoles() != null) {
            response.setRoles(model.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(Collectors.toSet()));
        }

        return response;
    }

    /**
     * Convert UserModel and UserProfileModel to enriched UserResponse.
     */
    public UserResponse toResponse(UserModel userModel, UserProfileModel profileModel) {
        UserResponse response = toResponse(userModel);

        if (response != null && profileModel != null) {
            response.setFirstName(profileModel.getFirstName());
            response.setLastName(profileModel.getLastName());
            response.setDisplayName(profileModel.getDisplayName());
            response.setEmailVerified(profileModel.getEmailVerified());
            response.setPremium(profileModel.getPremium());
            response.setTwoFactorEnabled(profileModel.getTwoFactorEnabled());
        }

        return response;
    }
}
