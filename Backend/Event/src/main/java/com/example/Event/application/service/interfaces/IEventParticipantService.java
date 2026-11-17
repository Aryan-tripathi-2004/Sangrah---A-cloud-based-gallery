package com.example.Event.application.service.interfaces;

import com.example.Event.api.dto.response.EventParticipantsResponse;
import org.springframework.data.domain.Pageable;

public interface IEventParticipantService {
    EventParticipantsResponse listParticipants(String eventId, String requesterUserId);

    EventParticipantsResponse listParticipants(String eventId, String requesterUserId, Pageable pageable);

    void removeParticipant(String eventId, String participantUserId, String requesterUserId);
}
