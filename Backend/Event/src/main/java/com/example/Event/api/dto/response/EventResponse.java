package com.example.Event.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO for event response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Event details")
public record EventResponse(
        @Schema(description = "Event ID", example = "507f1f77bcf86cd799439011")
        String eventId,
        String id,
        String ownerUserId,
        String title,
        String description,
        String coverImageId,
        String eventDate,
        String visibility,
        Boolean moderationEnabled,
        List<CollaboratorResponse> collaborators,
        String status,
        String createdAt,
        String accessStatus,
        Boolean requiresApproval,
        String message
) {
}
