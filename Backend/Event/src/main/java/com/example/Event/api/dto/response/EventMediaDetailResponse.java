package com.example.Event.api.dto.response;

import com.example.Event.infrastructure.client.dto.MediaServiceResponse;

public record EventMediaDetailResponse(
        String mediaId,
        String eventId,
        MediaServiceResponse details,
        String moderationStatus
) {
}
