package com.example.Notification.infrastructure.persistence.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Version;
import com.example.Notification.shared.enums.NotificationType;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("notifications")
@CompoundIndex(name = "recipientUserId_read_idx", def = "{'recipientUserId': 1, 'read': 1}")
@CompoundIndex(name = "recipientUserId_createdAt_idx", def = "{'recipientUserId': 1, 'createdAt': -1}")
public class NotificationDocument {
    @Id
    private String id;

    @Indexed
    private String recipientUserId;

    @Version
    private Long version;

    private NotificationType type;

    private Map<String, Object> payload;

    private boolean read;

    @Indexed(name = "createdAt_idx")
    private Instant createdAt;
}
