package com.example.Event.infrastructure.mapper;

import com.example.Event.api.dto.response.EventParticipantsResponse;
import com.example.Event.api.dto.response.ParticipantResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface EventParticipantMapper {

    @Mapping(target = "userId", source = "userId")
    ParticipantResponse toParticipantResponse(String userId);

    List<ParticipantResponse> toParticipantList(List<String> userIds);

    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "participants", source = "participants")
    EventParticipantsResponse toParticipantsResponse(String eventId, List<String> participants);
}
