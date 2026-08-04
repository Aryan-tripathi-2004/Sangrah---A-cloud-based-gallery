package com.example.Event.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CollaboratorResponse(
        String userId,
        Boolean canUploadMedia,
        Boolean canReviewMedia,
        Boolean canReviewAccessRequests,
        Boolean canDirectUpload,
        Boolean canDeleteMedia,
        Boolean canEditEventDetails,
        Instant addedAt,
        String addedByUserId,
        String email,
        String displayName
) {
}
