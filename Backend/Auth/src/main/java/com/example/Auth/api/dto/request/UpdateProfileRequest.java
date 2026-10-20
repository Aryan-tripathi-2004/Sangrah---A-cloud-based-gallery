package com.example.Auth.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to update user profile")
public record UpdateProfileRequest(
        @Size(max = 50, message = "Display name must not exceed 50 characters")
        @Schema(description = "User display name", example = "John Doe")
        String displayName,

        @Size(max = 300, message = "Bio must not exceed 300 characters")
        @Schema(description = "User biography", example = "Photographer and digital nomad")
        String bio,
        
        @Schema(description = "User avatar URL")
        String avatarUrl,
        
        @Schema(description = "Storage region preference", example = "us-east-1")
        String storageRegion
) {
}
