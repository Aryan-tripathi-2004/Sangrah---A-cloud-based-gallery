package com.example.Event.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update event settings")
public record EventSettingsRequest(
        @NotNull(message = "moderationEnabled is required")
        Boolean moderationEnabled
) {
}
