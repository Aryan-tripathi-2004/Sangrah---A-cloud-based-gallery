package com.example.Event.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MediaServiceResponse(
        String id,
        String mediaId,
        String userId,
        String domain,
        String entityRefId,
        String fileName,
        String originalFileName,
        String mimeType,
        Long sizeBytes,
        String storageKey,
        String checksumSha256,
        JsonNode metadata,
        Instant uploadedAt,
        Instant createdAt,
        String status,
        String title
) {
    public String resolvedMediaId() {
        if (id != null && !id.isBlank()) {
            return id;
        }
        return mediaId;
    }

    public String resolvedTitle() {
        if (originalFileName != null && !originalFileName.isBlank()) {
            return originalFileName;
        }
        if (title != null && !title.isBlank()) {
            return title;
        }
        if (fileName != null && !fileName.isBlank()) {
            return fileName;
        }
        return resolvedMediaId();
    }
}
