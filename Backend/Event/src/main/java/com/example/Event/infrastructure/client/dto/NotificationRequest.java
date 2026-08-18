package com.example.Event.infrastructure.client.dto;

public record NotificationRequest(
        String recipientUserId,
        String type,
        NotificationPayload payload
) {
}
