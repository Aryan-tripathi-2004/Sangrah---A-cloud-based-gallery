package com.example.Event.api.dto.response;

import com.example.Event.shared.enums.ApprovalStatus;

public record AccessRejectionResponse(
        ApprovalStatus status,
        String message,
        String reason
) {
}
