package com.example.Event.api.dto.response;

import java.util.List;

public record EventParticipantsResponse(
        String eventId,
        List<ParticipantResponse> participants,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasMore
) {
    public EventParticipantsResponse(String eventId, List<ParticipantResponse> participants) {
        this(eventId, participants, 0, participants != null ? participants.size() : 0, participants != null ? (long) participants.size() : 0L, 1, false);
    }
}
