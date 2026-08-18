package com.example.Event.api.dto.response;

import com.example.Event.shared.enums.ApprovalStatus;

public record AccessRequestMutationResponse(
        String requestId,
        ApprovalStatus status,
        String message
) {
}
