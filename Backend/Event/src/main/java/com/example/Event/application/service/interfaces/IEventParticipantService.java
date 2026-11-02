package com.example.Event.application.service.interfaces;

import com.example.Event.api.dto.response.EventParticipantsResponse;

public interface IEventParticipantService {
    EventParticipantsResponse listParticipants(String eventId, String requesterUserId);

    void removeParticipant(String eventId, String participantUserId, String requesterUserId);
}
