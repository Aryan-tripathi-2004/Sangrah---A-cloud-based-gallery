package com.example.Event.api.dto.response;

public record AccessRequestResponse(
        String requestId,
        String requesterUserId,
        String displayName,
        String message,
        String status,
        String requestedAt
) {
}
