package com.example.Notification.api.dto.response;

import java.util.List;

public record NotificationListResponse(
        List<NotificationResponse> notifications,
        long unreadCount,
        long totalCount,
        int skip,
        int limit,
        int page,
        int size,
        int totalPages,
        boolean hasMore
) {
    public NotificationListResponse(
            List<NotificationResponse> notifications,
            long unreadCount,
            long totalCount,
            int skip,
            int limit) {
        this(notifications, unreadCount, totalCount, skip, limit,
                limit > 0 ? skip / limit : 0, limit,
                limit > 0 ? (int) Math.ceil((double) totalCount / limit) : 1,
                skip + limit < totalCount);
    }
}
