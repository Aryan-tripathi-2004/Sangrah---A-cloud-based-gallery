package com.example.Gallery.api.dto.response;

import com.example.Gallery.shared.enums.MediaType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Gallery media details")
public record GalleryMediaResponse(
    @Schema(description = "Media ID", example = "507f1f77bcf86cd799439011")
    String mediaId,

    @Schema(description = "Owner user ID")
    String ownerId,

    @Schema(description = "Original file name")
    String fileName,

    @Schema(description = "Media type (IMAGE, VIDEO)", example = "IMAGE")
    MediaType mediaType,

    @Schema(description = "File size in bytes")
    Long fileSize,

    @Schema(description = "File MIME type", example = "image/jpeg")
    String mimeType,

    @Schema(description = "Cloud storage path")
    String storagePath,

    @Schema(description = "Tags for the media")
    String tags,

    @Schema(description = "Media upload timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,

    @Schema(description = "Media update timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt
) {}
