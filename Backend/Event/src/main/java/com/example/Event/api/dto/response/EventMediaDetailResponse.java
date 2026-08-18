package com.example.Event.api.dto.response;

import com.example.Event.infrastructure.client.dto.MediaServiceResponse;
import com.example.Event.shared.enums.ApprovalStatus;

public record EventMediaDetailResponse(
        String mediaId,
        String eventId,
        MediaServiceResponse details,
        ApprovalStatus moderationStatus
) {
}
