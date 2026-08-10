package com.example.Event.api.dto.request;

import com.example.Event.shared.enums.ApprovalDuration;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to approve protected event access")
public record AccessApprovalRequest(
        @Schema(description = "Approval duration", example = "FOREVER")
        ApprovalDuration approvalDuration,

        @Schema(description = "Access expiry as an ISO-8601 instant when approvalDuration is UNTIL_DATE")
        String accessExpiresAt
) {
    public ApprovalDuration resolvedApprovalDuration() {
        return approvalDuration == null ? ApprovalDuration.FOREVER : approvalDuration;
    }
}
