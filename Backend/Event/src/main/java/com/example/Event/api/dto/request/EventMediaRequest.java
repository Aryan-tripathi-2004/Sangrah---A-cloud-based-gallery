package com.example.Event.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * DTO for uploading media to an event.
 */
@Schema(description = "Request to add media to an event")
public record EventMediaRequest(
        @NotBlank(message = "Event ID is required")
        @Schema(description = "Event ID", example = "507f1f77bcf86cd799439011")
        String eventId,

        @NotBlank(message = "Media type is required")
        @Schema(description = "Media type (PHOTO, VIDEO, DOCUMENT)", example = "PHOTO")
        String mediaType,

        @NotBlank(message = "File name is required")
        @Size(max = 255, message = "File name must not exceed 255 characters")
        @Schema(description = "Original file name")
        String fileName,

        @PositiveOrZero(message = "File size must be zero or greater")
        @Schema(description = "File size in bytes")
        Long fileSize,

        @Size(max = 1024, message = "Storage path must not exceed 1024 characters")
        @Schema(description = "Cloud storage path")
        String storagePath
) {
}
