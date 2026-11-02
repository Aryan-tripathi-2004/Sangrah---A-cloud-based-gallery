package com.example.Event.api.dto.request;

import com.example.Event.shared.enums.ApprovalDuration;
import com.example.Event.shared.enums.ApprovalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Unified request body to transition an access request status")
public record AccessStatusUpdateRequest(

        @NotNull(message = "status is required")
        @Schema(description = "Target status", allowableValues = {"APPROVED", "REJECTED", "REVOKED"})
        ApprovalStatus status,

        // ---- APPROVED fields ----
        @Schema(description = "[APPROVED] Approval duration", example = "FOREVER")
        String approvalDuration,

        @Schema(description = "[APPROVED] Access expiry (ISO-8601 instant), required when approvalDuration=UNTIL_DATE")
        String accessExpiresAt,

        // ---- REJECTED fields ----
        @Schema(description = "[REJECTED] Reason for rejection (max 1000 chars)", maxLength = 1000)
        String reason
) {
    public AccessApprovalRequest toApprovalRequest() {
        ApprovalDuration duration = approvalDuration != null
                ? ApprovalDuration.valueOf(approvalDuration)
                : ApprovalDuration.FOREVER;
        return new AccessApprovalRequest(duration, accessExpiresAt);
    }

    public AccessRejectionRequest toRejectionRequest() {
        return new AccessRejectionRequest(reason);
    }
}
