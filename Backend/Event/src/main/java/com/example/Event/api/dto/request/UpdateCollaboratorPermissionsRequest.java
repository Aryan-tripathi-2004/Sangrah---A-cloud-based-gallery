package com.example.Event.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update collaborator permissions")
public record UpdateCollaboratorPermissionsRequest(
        @NotNull(message = "Permissions are required")
        @Valid
        CollaboratorPermissionsRequest permissions
) {
}
