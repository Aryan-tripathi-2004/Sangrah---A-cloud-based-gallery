package com.example.Notification.api.dto.response;

public record NotificationPreferenceResponse(
    boolean inAppEnabled,
    boolean emailEnabled
) {}
