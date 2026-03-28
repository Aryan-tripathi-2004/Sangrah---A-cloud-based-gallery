package com.example.Gallery.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for gallery media response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Gallery media details")
public class GalleryMediaResponse {

    @Schema(description = "Media ID", example = "507f1f77bcf86cd799439011")
    private String mediaId;

    @Schema(description = "Owner user ID")
    private String ownerId;

    @Schema(description = "Original file name")
    private String fileName;

    @Schema(description = "Media type (PHOTO, VIDEO, DOCUMENT)", example = "PHOTO")
    private String mediaType;

    @Schema(description = "File size in bytes")
    private Long fileSize;

    @Schema(description = "File MIME type", example = "image/jpeg")
    private String mimeType;

    @Schema(description = "Cloud storage path")
    private String storagePath;

    @Schema(description = "Tags for the media")
    private String tags;

    @Schema(description = "Media upload timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Media update timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
