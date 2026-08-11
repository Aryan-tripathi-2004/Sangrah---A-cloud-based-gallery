package com.example.Notification.application.service.interfaces;

import com.example.Notification.api.dto.response.NotificationResponse;
import com.example.Notification.shared.enums.NotificationType;

import java.util.List;
import java.util.Map;

public interface INotificationService {

    NotificationResponse createNotification(String recipientUserId, NotificationType type, Map<String, Object> payload);

    List<NotificationResponse> getUserNotifications(String userId, int skip, int limit);

    List<NotificationResponse> getUnreadNotifications(String userId);

    NotificationResponse markAsRead(String userId, String notificationId);

    long markAllAsRead(String userId);

    void deleteNotification(String notificationId);

    long getTotalCount(String userId);

    long getUnreadCount(String userId);
}
