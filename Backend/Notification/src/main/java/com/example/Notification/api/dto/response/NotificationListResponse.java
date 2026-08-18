package com.example.Notification.api.dto.response;

import java.util.List;

public record NotificationListResponse(
        List<NotificationResponse> notifications,
        long unreadCount,
        long totalCount,
        int skip,
        int limit
) {}
