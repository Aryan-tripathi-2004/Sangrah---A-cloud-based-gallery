package com.example.Event.api.dto.response;

import java.util.List;

public record CollaboratorsResponse(
        String eventId,
        List<CollaboratorResponse> collaborators
) {
}
