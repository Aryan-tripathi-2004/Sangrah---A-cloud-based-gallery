package com.example.Event.api.dto.response;

import com.example.Event.shared.enums.ApprovalStatus;

public record EventMediaUploadResponse(
        String mediaId,
        ApprovalStatus moderationStatus,
        String message
) {
}
