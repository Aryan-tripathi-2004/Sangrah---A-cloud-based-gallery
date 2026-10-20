package com.example.Event.api.dto.response;

import com.example.Event.shared.enums.EventStatus;
import com.example.Event.shared.enums.EventVisibility;
import lombok.Builder;

import java.time.Instant;

@Builder
public record EventInternalDTO(
        String id,
        String ownerUserId,
        String title,
        String description,
        Instant eventDate,
        EventVisibility visibility,
        boolean moderationEnabled,
        EventStatus status,
        String coverImageId,
        Instant createdAt,
        Instant updatedAt
) {}
