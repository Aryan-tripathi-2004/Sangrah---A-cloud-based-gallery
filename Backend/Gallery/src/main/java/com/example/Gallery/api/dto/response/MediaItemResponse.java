package com.example.Gallery.api.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.Map;

/**
 * Response for a single media item
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaItemResponse {
    private String id;
    private String originalFileName;
    private String mimeType;
    private long sizeBytes;
    private String type;  // IMAGE or VIDEO
    private String checksumSha256;
    private Map<String, Object> metadata;
    private Instant uploadedAt;
    private Instant deletedAt;  // null = not deleted
    private String visibilityStatus;  // PRIVATE, SHARED, etc.
}
