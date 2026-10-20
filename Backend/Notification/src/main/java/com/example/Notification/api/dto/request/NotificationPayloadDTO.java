package com.example.Notification.api.dto.request;

public record NotificationPayloadDTO(
        String eventId,
        String eventTitle,
        String addedByUserEmail,
        String requesterEmail,
        String mediaTitle,
        String rejectionReason,
        String message,
        String actionUrl
) {}