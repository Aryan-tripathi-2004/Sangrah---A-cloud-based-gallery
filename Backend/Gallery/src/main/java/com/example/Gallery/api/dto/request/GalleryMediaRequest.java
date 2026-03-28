package com.example.Gallery.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for uploading media to gallery.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to upload media to gallery")
public class GalleryMediaRequest {

    @NotBlank(message = "File name is required")
    @Schema(description = "Original file name")
    private String fileName;

    @NotBlank(message = "Media type is required")
    @Schema(description = "Media type (PHOTO, VIDEO, DOCUMENT)", example = "PHOTO")
    private String mediaType;

    @Schema(description = "File size in bytes")
    private Long fileSize;

    @Schema(description = "File MIME type", example = "image/jpeg")
    private String mimeType;

    @Schema(description = "Tags for the media")
    private String tags;
}
