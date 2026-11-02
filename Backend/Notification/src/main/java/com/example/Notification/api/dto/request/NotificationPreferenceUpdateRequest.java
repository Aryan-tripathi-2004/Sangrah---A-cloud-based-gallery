package com.example.Notification.api.dto.request;

public record NotificationPreferenceUpdateRequest(
    Boolean inAppEnabled,
    Boolean emailEnabled
) {}
