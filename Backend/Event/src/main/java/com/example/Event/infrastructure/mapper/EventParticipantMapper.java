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
    @Mapping(target = "page", constant = "0")
    @Mapping(target = "size", expression = "java(participants != null ? participants.size() : 0)")
    @Mapping(target = "totalElements", expression = "java(participants != null ? (long) participants.size() : 0L)")
    @Mapping(target = "totalPages", constant = "1")
    @Mapping(target = "hasMore", constant = "false")
    EventParticipantsResponse toParticipantsResponse(String eventId, List<String> participants);

    default EventParticipantsResponse toParticipantsResponse(
            String eventId, List<String> participants,
            int page, int size, long totalElements, int totalPages, boolean hasMore) {
        return new EventParticipantsResponse(eventId, toParticipantList(participants), page, size, totalElements, totalPages, hasMore);
    }
}
