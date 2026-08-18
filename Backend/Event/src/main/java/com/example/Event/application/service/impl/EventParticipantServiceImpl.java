package com.example.Event.application.service.impl;

import com.example.Event.api.dto.response.EventParticipantsResponse;
import com.example.Event.application.service.interfaces.IEventParticipantService;
import com.example.Event.infrastructure.mapper.EventParticipantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventParticipantServiceImpl implements IEventParticipantService {
    private final EventParticipantMapper participantMapper;

    @Override
    public EventParticipantsResponse listParticipants(String eventId) {
        return participantMapper.toParticipantsResponse(eventId, List.of());
    }

    @Override
    public void removeParticipant(String eventId, String participantUserId) {
    }
}
