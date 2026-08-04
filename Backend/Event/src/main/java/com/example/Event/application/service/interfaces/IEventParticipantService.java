package com.example.Event.application.service.interfaces;

import com.example.Event.api.dto.response.EventParticipantsResponse;

public interface IEventParticipantService {
    EventParticipantsResponse listParticipants(String eventId);

    void removeParticipant(String eventId, String participantUserId);
}
