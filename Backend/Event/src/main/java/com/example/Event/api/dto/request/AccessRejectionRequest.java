package com.example.Event.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to reject protected event access")
public record AccessRejectionRequest(
        @Size(max = 1000, message = "Reason must not exceed 1000 characters")
        @Schema(description = "Reason for rejecting access")
        String reason
) {
    public String resolvedReason() {
        return reason == null || reason.isBlank() ? "Request denied" : reason.trim();
    }
}
