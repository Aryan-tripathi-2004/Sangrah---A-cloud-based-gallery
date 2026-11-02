package com.example.Event.api.dto.request;

import com.example.Event.shared.enums.ApprovalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body to transition event media moderation status")
public record MediaModerationRequest(
        
        @NotNull(message = "status is required")
        @Schema(description = "Target status", allowableValues = {"APPROVED", "REJECTED"})
        ApprovalStatus status,
        
        @Size(max = 1000, message = "Reason must not exceed 1000 characters")
        @Schema(description = "[REJECTED] Reason for rejecting media")
        String reason
) {
    public String rejectionDetails() {
        return reason == null || reason.isBlank() ? "Not specified" : reason.trim();
    }
}
