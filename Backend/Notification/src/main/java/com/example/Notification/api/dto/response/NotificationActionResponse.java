package com.example.Notification.api.dto.response;

public record NotificationActionResponse(
        String notificationId,
        boolean read,
        String message
) {}
