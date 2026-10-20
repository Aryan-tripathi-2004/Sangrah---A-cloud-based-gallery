package com.example.Auth.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MediaUploadResponse(
        String id,
        String mediaId,
        String userId,
        String domain,
        String fileName,
        String originalFileName,
        String mimeType,
        Long sizeBytes
) {
    public String resolvedMediaId() {
        if (id != null && !id.isBlank()) {
            return id;
        }
        return mediaId;
    }
}
