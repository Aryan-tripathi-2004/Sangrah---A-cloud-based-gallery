package com.example.Event.api.dto.response;

public record EventCreateResponse(
        String eventId,
        String id,
        String title,
        String message
) {
}
