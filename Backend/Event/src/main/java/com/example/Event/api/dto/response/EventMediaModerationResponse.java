package com.example.Event.api.dto.response;

import com.example.Event.shared.enums.ApprovalStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventMediaModerationResponse(
        ApprovalStatus status,
        String reason,
        String message
) {
}
