package com.example.Event.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to approve protected event access")
public record AccessApprovalRequest(
        @Pattern(regexp = "FOREVER|UNTIL_DATE", message = "Approval duration must be FOREVER or UNTIL_DATE")
        @Schema(description = "Approval duration", example = "FOREVER")
        String approvalDuration,

        @Schema(description = "Access expiry as an ISO-8601 instant when approvalDuration is UNTIL_DATE")
        String accessExpiresAt
) {
    public String resolvedApprovalDuration() {
        return approvalDuration == null || approvalDuration.isBlank()
                ? "FOREVER"
                : approvalDuration.trim().toUpperCase();
    }
}
