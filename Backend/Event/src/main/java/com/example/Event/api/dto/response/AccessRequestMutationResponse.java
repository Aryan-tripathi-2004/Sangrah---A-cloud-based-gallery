package com.example.Event.api.dto.response;

public record AccessRequestMutationResponse(
        String requestId,
        String status,
        String message
) {
}
