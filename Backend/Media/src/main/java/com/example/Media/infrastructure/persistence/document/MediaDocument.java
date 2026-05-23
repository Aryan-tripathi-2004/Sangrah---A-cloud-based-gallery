package com.example.Media.infrastructure.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "media")
public class MediaDocument {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String domain;

    private String entityRefId;

    private String fileName;

    private String originalFileName;

    private String mimeType;

    private String contentType;

    private Long sizeBytes;

    private String storageKey;

    private String storageProvider;

    private String checksumSha256;

    private Map<String, Object> metadata;

    private Instant uploadedAt;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant deletedAt;

    // Helper method to check if media is active (not deleted)
    public boolean isActive() {
        return deletedAt == null;
    }
}
