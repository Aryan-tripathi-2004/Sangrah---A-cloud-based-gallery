package com.example.Event.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccessApprovalResponse(
        String status,
        String message,
        String approvalDuration,
        String accessExpiresAt
) {
}
