package com.example.Event.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for event media response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Event media details")
public class EventMediaResponse {

    @Schema(description = "Media ID", example = "507f1f77bcf86cd799439011")
    private String mediaId;

    @Schema(description = "Event ID")
    private String eventId;

    @Schema(description = "Uploader user ID")
    private String uploaderId;

    @Schema(description = "Media type (PHOTO, VIDEO, DOCUMENT)", example = "PHOTO")
    private String mediaType;

    @Schema(description = "Original file name")
    private String fileName;

    @Schema(description = "File size in bytes")
    private Long fileSize;

    @Schema(description = "Cloud storage path")
    private String storagePath;

    @Schema(description = "Media upload timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
