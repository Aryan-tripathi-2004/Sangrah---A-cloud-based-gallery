package com.example.Gallery.infrastructure.persistence.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

import com.example.Gallery.shared.enums.MediaType;
import com.example.Gallery.shared.enums.Visibility;
import org.springframework.data.annotation.Version;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("gallery_media")
public class GalleryMediaDocument {
    @Id
    private String id;

    @Version
    private Long version;

    @Indexed
    private String userId;

    private String ownerUserId;
    private String originalFileName;
    private String mimeType;
    private long sizeBytes;
    private String storageKey;
    private String storageProvider;

    @Indexed
    private String checksumSha256;

    private MediaType type;
    private Visibility visibility;

    // Metadata
    private Map<String, Object> metadata;  // Stores width, height, duration, EXIF, etc.

    @Indexed
    private Instant uploadedAt;

    @Indexed
    private Instant deletedAt;  // null = not deleted (soft delete)

    private Instant createdAt;
    private Instant updatedAt;

    // Helper method to check if file is deleted
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
