package com.example.MediaService.infrastructure.persistence.document;

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
    private String userId;  // User who owns/uploaded this media

    @Indexed
    private String domain;  // "GALLERY", "EVENTS", "PROFILE_AVATAR", "MESSAGING"

    @Indexed
    private String entityRefId;  // Reference to event ID, profile ID, etc.

    private String fileName;
    private String originalFileName;
    private String mimeType;
    private Long sizeBytes;

    private String storageKey;  // Path/key in storage provider
    private String storageProvider;  // "LOCAL", "S3", "AZURE", etc.

    @Indexed
    private String checksumSha256;  // For deduplication

    private String contentType;

    private Map<String, Object> metadata;  // width, height, duration, etc.

    private Instant uploadedAt;

    @Indexed
    private Instant deletedAt;  // null = active, set value = soft deleted

    private Instant createdAt;
    private Instant updatedAt;

    // Helper method to check if media is active (not deleted)
    public boolean isActive() {
        return deletedAt == null;
    }
}
