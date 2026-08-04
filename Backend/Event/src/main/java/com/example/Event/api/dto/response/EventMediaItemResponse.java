package com.example.Event.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventMediaItemResponse(
        String id,
        String mediaId,
        String status,
        String uploaderUserId,
        String uploaderName,
        String uploadedAt,
        String originalFileName,
        String mimeType
) {
}
