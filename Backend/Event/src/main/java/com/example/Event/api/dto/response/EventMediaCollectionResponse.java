package com.example.Event.api.dto.response;

import java.util.List;

public record EventMediaCollectionResponse(
        String eventId,
        List<EventMediaItemResponse> media,
        String message
) {
}
