package com.example.Event.api.dto.response;

import com.example.Event.shared.enums.ApprovalStatus;

public record AccessRequestResponse(
        String requestId,
        String requesterUserId,
        String displayName,
        String message,
        ApprovalStatus status,
        String requestedAt
) {
}
