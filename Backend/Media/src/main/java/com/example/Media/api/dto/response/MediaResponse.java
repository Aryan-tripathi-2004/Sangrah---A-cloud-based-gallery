package com.example.Media.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaResponse {
    private String id;
    private String userId;
    private String domain;
    private String entityRefId;
    private String fileName;
    private String originalFileName;
    private String mimeType;
    private Long sizeBytes;
    private String storageKey;
    private String checksumSha256;
    private Map<String, Object> metadata;
    private Instant uploadedAt;
    private Instant createdAt;
    private String status;  // "ACTIVE", "DELETED"
}
