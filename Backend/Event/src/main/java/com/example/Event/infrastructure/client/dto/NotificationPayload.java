package com.example.Event.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationPayload(
        String eventId,
        String eventTitle,
        String addedByUserEmail,
        String requesterEmail,
        String mediaTitle,
        String rejectionReason,
        String message,
        String actionUrl
) {
}
