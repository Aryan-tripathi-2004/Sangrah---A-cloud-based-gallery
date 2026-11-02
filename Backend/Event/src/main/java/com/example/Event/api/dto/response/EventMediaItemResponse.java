package com.example.Event.api.dto.response;

import com.example.Event.shared.enums.ApprovalStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventMediaItemResponse(
        String id,
        String mediaId,
        ApprovalStatus status,
        String uploaderId,
        String uploaderName,
        String uploadedAt,
        String fileName,
        String mimeType,
        String reason
) {
}
