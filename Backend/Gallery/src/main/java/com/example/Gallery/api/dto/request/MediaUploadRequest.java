package com.example.Gallery.api.dto.request;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadRequest {
    private String originalFileName;
    private String mimeType;
    private long sizeBytes;
    private String checksumSha256;
    private Map<String, Object> metadata;  // Optional: width, height, duration, etc.
}
