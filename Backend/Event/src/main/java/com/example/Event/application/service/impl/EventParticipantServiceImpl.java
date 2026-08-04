package com.example.Event.application.service.impl;

import com.example.Event.api.dto.response.EventParticipantsResponse;
import com.example.Event.application.service.interfaces.IEventParticipantService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventParticipantServiceImpl implements IEventParticipantService {
    @Override
    public EventParticipantsResponse listParticipants(String eventId) {
        return new EventParticipantsResponse(eventId, List.of());
    }

    @Override
    public void removeParticipant(String eventId, String participantUserId) {
    }
}
