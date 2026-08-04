package com.example.Event.api.dto.response;

public record AccessRejectionResponse(
        String status,
        String message,
        String reason
) {
}
