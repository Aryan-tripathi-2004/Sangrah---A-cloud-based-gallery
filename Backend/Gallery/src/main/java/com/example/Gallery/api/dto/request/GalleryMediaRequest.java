package com.example.Gallery.api.dto.request;

import com.example.Gallery.shared.enums.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to upload media to gallery")
public record GalleryMediaRequest(
    @NotBlank(message = "File name is required")
    @Schema(description = "Original file name")
    String fileName,

    @NotNull(message = "Media type is required")
    @Schema(description = "Media type (IMAGE, VIDEO)", example = "IMAGE")
    MediaType mediaType,

    @Schema(description = "File size in bytes")
    Long fileSize,

    @Schema(description = "File MIME type", example = "image/jpeg")
    String mimeType,

    @Schema(description = "Tags for the media")
    String tags
) {}
