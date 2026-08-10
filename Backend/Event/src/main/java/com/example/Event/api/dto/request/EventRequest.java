package com.example.Event.api.dto.request;

import com.example.Event.shared.enums.EventVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to create an event.
 */
@Schema(description = "Request to create or update an event")
public record EventRequest(
        @NotBlank(message = "Title is required")
        @Size(min = 1, max = 200, message = "Title must not exceed 200 characters")
        @Schema(description = "Event title", example = "Birthday Party")
        String title,

        @NotBlank(message = "Description is required")
        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        @Schema(description = "Event description")
        String description,

        @Schema(description = "Event date in ISO-8601 format", example = "2026-08-15T18:00:00")
        String eventDate,

        @Schema(description = "Event visibility", example = "PRIVATE")
        EventVisibility visibility,

        @Schema(description = "Cover image media ID")
        String coverImageId,

        @Schema(description = "Whether uploads require moderation", example = "false")
        Boolean moderationEnabled
) {
    public EventVisibility resolvedVisibility() {
        return visibility == null ? EventVisibility.PRIVATE : visibility;
    }

    public boolean moderationEnabledOrDefault() {
        return Boolean.TRUE.equals(moderationEnabled);
    }
}
