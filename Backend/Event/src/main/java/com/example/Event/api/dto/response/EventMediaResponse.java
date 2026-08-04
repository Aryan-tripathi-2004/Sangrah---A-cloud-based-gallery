package com.example.Event.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for event media response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Event media details")
public record EventMediaResponse(
        @Schema(description = "Media ID", example = "507f1f77bcf86cd799439011")
        String mediaId,
        String id,
        @Schema(description = "Event ID")
        String eventId,
        @Schema(description = "Uploader user ID")
        String uploaderId,
        @Schema(description = "Media MIME type")
        String mediaType,
        @Schema(description = "Original file name")
        String fileName,
        @Schema(description = "File size in bytes")
        Long fileSize,
        @Schema(description = "Cloud storage path")
        String storagePath,
        @Schema(description = "Media upload timestamp")
        String createdAt,
        String status
) {
}
