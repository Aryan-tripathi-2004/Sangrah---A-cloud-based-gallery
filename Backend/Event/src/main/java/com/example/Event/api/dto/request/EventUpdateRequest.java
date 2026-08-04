package com.example.Event.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Multipart event update request")
public record EventUpdateRequest(
        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
        String title,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        @NotBlank(message = "Event date is required")
        String eventDate,

        @NotBlank(message = "Visibility is required")
        @Pattern(regexp = "PUBLIC|PROTECTED|PRIVATE", message = "Visibility must be PUBLIC, PROTECTED, or PRIVATE")
        String visibility,

        Boolean moderationEnabled
) {
}
