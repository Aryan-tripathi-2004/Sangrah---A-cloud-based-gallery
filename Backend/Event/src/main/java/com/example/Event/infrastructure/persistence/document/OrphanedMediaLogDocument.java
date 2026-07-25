package com.example.Event.infrastructure.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("orphaned_media_logs")
public class OrphanedMediaLogDocument {
    @Id
    private String id;
    private String eventId;
    private String oldMediaId;
    private String errorMessage;
    private Instant createdAt;
}