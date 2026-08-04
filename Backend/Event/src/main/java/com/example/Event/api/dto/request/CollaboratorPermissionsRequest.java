package com.example.Event.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Collaborator permission flags")
public record CollaboratorPermissionsRequest(
        Boolean canUploadMedia,
        Boolean canReviewMedia,
        Boolean canReviewAccessRequests,
        Boolean canDirectUpload,
        Boolean canDeleteMedia,
        Boolean canEditEventDetails
) {
    public boolean uploadMediaOrFalse() {
        return Boolean.TRUE.equals(canUploadMedia);
    }

    public boolean reviewMediaOrFalse() {
        return Boolean.TRUE.equals(canReviewMedia);
    }

    public boolean reviewAccessRequestsOrFalse() {
        return Boolean.TRUE.equals(canReviewAccessRequests);
    }

    public boolean directUploadOrFalse() {
        return Boolean.TRUE.equals(canDirectUpload);
    }

    public boolean deleteMediaOrFalse() {
        return Boolean.TRUE.equals(canDeleteMedia);
    }

    public boolean editEventDetailsOrFalse() {
        return Boolean.TRUE.equals(canEditEventDetails);
    }
}
