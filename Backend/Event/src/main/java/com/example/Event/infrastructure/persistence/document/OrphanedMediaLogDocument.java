package com.example.Event.infrastructure.persistence.document;

import com.example.Event.shared.enums.OrphanedMediaStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

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

    private String eventId;
    private String oldMediaId;
    private String userId;
    private String errorMessage;
    private Instant createdAt;

    @Builder.Default
    private OrphanedMediaStatus status = OrphanedMediaStatus.PENDING;

    @Builder.Default
    private int retryCount = 0;

    private Instant lastAttemptAt;
    private Instant resolvedAt;
}
