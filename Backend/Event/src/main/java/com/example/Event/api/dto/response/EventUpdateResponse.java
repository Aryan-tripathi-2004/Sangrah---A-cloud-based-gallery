package com.example.Event.api.dto.response;

import java.util.List;

public record EventUpdateResponse(
        String status,
        String message,
        String eventId,
        String id,
        String title,
        String description,
        String eventDate,
        String visibility,
        boolean moderationEnabled,
        String coverImageId,
        List<CollaboratorResponse> collaborators
) {
}
