package com.example.Notification.infrastructure.persistence.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("notifications")
public class NotificationDocument {
    @Id
    private String id;
    private String recipientUserId;
    private String type;
    private Map<String, Object> payload;
    private boolean read;
    private Instant createdAt;
}
