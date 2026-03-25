package com.example.Event.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for uploading media to an event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to add media to an event")
public class EventMediaRequest {

    @NotBlank(message = "Event ID is required")
    @Schema(description = "Event ID", example = "507f1f77bcf86cd799439011")
    private String eventId;

    @NotBlank(message = "Media type is required")
    @Schema(description = "Media type (PHOTO, VIDEO, DOCUMENT)", example = "PHOTO")
    private String mediaType;

    @NotBlank(message = "File name is required")
    @Schema(description = "Original file name")
    private String fileName;

    @Schema(description = "File size in bytes")
    private Long fileSize;

    @Schema(description = "Cloud storage path")
    private String storagePath;
}
