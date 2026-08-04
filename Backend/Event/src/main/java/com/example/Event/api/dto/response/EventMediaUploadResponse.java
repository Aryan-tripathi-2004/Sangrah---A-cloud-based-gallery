package com.example.Event.api.dto.response;

public record EventMediaUploadResponse(
        String mediaId,
        String moderationStatus,
        String message
) {
}
