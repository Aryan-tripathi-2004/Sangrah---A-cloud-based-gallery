package com.example.Event.api.dto.response;

import java.util.List;

public record EventParticipantsResponse(
        String eventId,
        List<ParticipantResponse> participants
) {
}
