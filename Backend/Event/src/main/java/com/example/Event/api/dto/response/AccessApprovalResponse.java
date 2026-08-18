package com.example.Event.api.dto.response;

import com.example.Event.shared.enums.ApprovalDuration;
import com.example.Event.shared.enums.ApprovalStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccessApprovalResponse(
        ApprovalStatus status,
        String message,
        ApprovalDuration approvalDuration,
        String accessExpiresAt
) {
}
