package com.example.Event.api.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Request to add an event collaborator")
public record AddCollaboratorRequest(
        @NotBlank(message = "User email is required")
        @Email(message = "User email must be valid")
        @Schema(description = "Collaborator email")
        String userEmail,

        @Valid
        @Schema(description = "Collaborator permissions")
        CollaboratorPermissionsRequest permissions,

        @Schema(description = "Legacy direct permission flag")
        Boolean canUploadMedia,
        @Schema(description = "Legacy direct permission flag")
        Boolean canReviewMedia,
        @Schema(description = "Legacy direct permission flag")
        Boolean canReviewAccessRequests,
        @Schema(description = "Legacy direct permission flag")
        Boolean canDirectUpload,
        @Schema(description = "Legacy direct permission flag")
        Boolean canDeleteMedia,
        @Schema(description = "Legacy direct permission flag")
        Boolean canEditEventDetails
) {
    public CollaboratorPermissionsRequest resolvedPermissions() {
        if (permissions != null) {
            return permissions;
        }
        return new CollaboratorPermissionsRequest(
                canUploadMedia,
                canReviewMedia,
                canReviewAccessRequests,
                canDirectUpload,
                canDeleteMedia,
                canEditEventDetails);
    }
}
