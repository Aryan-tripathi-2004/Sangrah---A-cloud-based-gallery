package com.example.Notification.infrastructure.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("notification_preferences")
public class NotificationPreferenceDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private boolean inAppEnabled;

    private boolean emailEnabled;

    @LastModifiedDate
    private Instant updatedAt;
}
