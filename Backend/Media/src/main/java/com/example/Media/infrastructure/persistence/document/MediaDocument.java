package com.example.Media.infrastructure.persistence.document;

import com.example.Media.shared.enums.MediaDomain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * MongoDB document representing a stored media asset.
 *
 * <p>Design notes:
 * <ul>
 *   <li>{@code @Data} is intentionally replaced by granular Lombok annotations to prevent
 *       accidental generation of a mutable {@code equals}/{@code hashCode} contract on a
 *       persistence entity whose identity is defined by its {@code @Id}.</li>
 *   <li>{@code @Version} enables MongoDB optimistic locking. Any concurrent write that
 *       presents a stale version will be rejected with an
 *       {@link org.springframework.dao.OptimisticLockingFailureException}, which is then
 *       handled centrally by {@code GlobalExceptionHandler}.</li>
 *   <li>{@code domain} is typed as {@link MediaDomain} to cure primitive obsession.
 *       Spring Data MongoDB serialises the enum to its {@link Enum#name()} string, so
 *       existing documents ("GALLERY", "EVENTS", etc.) are fully backward-compatible.</li>
 * </ul>
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "media")
public class MediaDocument {

    @Id
    private String id;

    @Indexed
    private String userId;

    /** Business domain that owns this asset. Stored as enum name string in MongoDB. */
    private MediaDomain domain;

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

    /**
     * Optimistic locking version field managed by Spring Data MongoDB.
     * Automatically incremented on every successful save.
     */
    @Version
    private Long version;

    // Helper method to check if media is active (not deleted)
    public boolean isActive() {
        return deletedAt == null;
    }
}
