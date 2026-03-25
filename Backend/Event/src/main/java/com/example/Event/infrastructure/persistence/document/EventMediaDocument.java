package com.example.Event.infrastructure.persistence.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("event_media")
public class EventMediaDocument {
    @Id
    private String id;
    private String eventId;
    private String uploaderUserId;
    private String originalFileName;
    private String mimeType;
    private long sizeBytes;
    private String storageKey;
    private String storageProvider;
    private String checksumSha256;
    private String status; // PENDING|APPROVED|REJECTED
    private String reviewedByUserId;
    private Instant reviewedAt;
    private Instant uploadedAt;
    private Instant deletedAt;
}
