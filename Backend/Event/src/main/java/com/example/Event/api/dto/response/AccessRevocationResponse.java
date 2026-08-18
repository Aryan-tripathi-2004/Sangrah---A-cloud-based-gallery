package com.example.Event.api.dto.response;

import com.example.Event.shared.enums.ApprovalStatus;

public record AccessRevocationResponse(
        ApprovalStatus status,
        String message
) {
}
