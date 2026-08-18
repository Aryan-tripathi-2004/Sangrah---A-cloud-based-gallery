package com.example.Event.api.dto.response;

import com.example.Event.shared.enums.EventVisibility;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventSummaryResponse(
        String eventId,
        String id,
        String ownerUserId,
        String title,
        String description,
        String coverImageId,
        String eventDate,
        EventVisibility visibility,
        Boolean moderationEnabled,
        List<CollaboratorResponse> collaborators,
        String createdAt
) {
}
