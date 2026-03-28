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
@Document("events")
public class EventDocument {
    @Id
    private String id;
    private String ownerUserId;
    private String title;
    private String description;
    private Instant eventDate;
    private String visibility;
    private boolean moderationEnabled;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
