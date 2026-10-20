package com.example.Event.infrastructure.mapper;

import com.example.Event.api.dto.response.EventParticipantsResponse;
import com.example.Event.api.dto.response.ParticipantResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EventParticipantMapper {

    default EventParticipantsResponse toParticipantsResponse(String eventId, List<String> participants) {
        return new EventParticipantsResponse(eventId, participants.stream()
                .map(ParticipantResponse::new)
                .toList());
    }
}
