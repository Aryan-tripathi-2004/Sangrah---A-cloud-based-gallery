package com.example.Gallery.infrastructure.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Audit document for tracking orphaned media assets whose physical deletion/compensation failed.
 * Enables automated reconciliation jobs or administrators to safely purge dangling resources.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("orphaned_media_logs")
public class OrphanedMediaLogDocument {
    @Id
    private String id;
    @Version
    private Long version;
    private String userId;
    private String mediaId;
    private String errorMessage;
    private Instant createdAt;
}
