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
@Document("event_access_requests")
public class EventAccessRequestDocument {
    @Id
    private String id;
    private String eventId;
    private String requesterUserId;
    private String status; // PENDING|APPROVED|REJECTED|CANCELLED
    private Instant requestedAt;
    private Instant decisionAt;
    private String decidedByUserId;
}
