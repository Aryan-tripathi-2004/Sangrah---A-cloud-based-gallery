package com.example.Event.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request message for protected event access")
public record AccessMessageRequest(
        @Size(max = 1000, message = "Message must not exceed 1000 characters")
        @Schema(description = "Requester message")
        String message
) {
    public String normalizedMessage() {
        return message == null || message.isBlank() ? null : message.trim();
    }
}
