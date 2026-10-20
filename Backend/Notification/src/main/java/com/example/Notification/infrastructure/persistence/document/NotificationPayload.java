package com.example.Notification.infrastructure.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPayload {
    private String eventId;
    private String eventTitle;
    private String addedByUserEmail;
    private String requesterEmail;
    private String mediaTitle;
    private String rejectionReason;
    private String message;
    private String actionUrl;
}