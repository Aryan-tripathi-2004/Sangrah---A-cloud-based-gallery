package com.example.Event.api.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Request to reject event media")
public record MediaRejectionRequest(
        @Size(max = 1000, message = "Reason must not exceed 1000 characters")
        String reason,
        @Size(max = 1000, message = "Comment must not exceed 1000 characters")
        String comment,
        @Size(max = 1000, message = "Message must not exceed 1000 characters")
        String message
) {
    public String resolvedReason() {
        if (reason != null && !reason.isBlank()) {
            return reason.trim();
        }
        if (comment != null && !comment.isBlank()) {
            return comment.trim();
        }
        if (message != null && !message.isBlank()) {
            return message.trim();
        }
        return "Not specified";
    }
}
