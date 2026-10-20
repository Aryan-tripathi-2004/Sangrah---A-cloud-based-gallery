package com.example.Event.infrastructure.mapper;

import com.example.Event.api.dto.response.EventInternalDTO;
import com.example.Event.infrastructure.persistence.document.EventDocument;
import org.springframework.stereotype.Component;

@Component
public class EventInternalMapper {

    public EventInternalDTO toDTO(EventDocument document) {
        if (document == null) {
            return null;
        }

        return EventInternalDTO.builder()
                .id(document.getId())
                .ownerUserId(document.getOwnerUserId())
                .title(document.getTitle())
                .description(document.getDescription())
                .eventDate(document.getEventDate())
                .visibility(document.getVisibility())
                .moderationEnabled(document.isModerationEnabled())
                .status(document.getStatus())
                .coverImageId(document.getCoverImageId())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
