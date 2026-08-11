package com.example.Notification.application.service.interfaces;

import com.example.Notification.infrastructure.persistence.document.NotificationDocument;
import com.example.Notification.shared.enums.NotificationType;

import java.util.List;
import java.util.Map;

public interface INotificationService {

    NotificationDocument createNotification(String recipientUserId, NotificationType type, Map<String, Object> payload);

    List<NotificationDocument> getUserNotifications(String userId, int skip, int limit);

    List<NotificationDocument> getUnreadNotifications(String userId);

    NotificationDocument markAsRead(String notificationId);

    long markAllAsRead(String userId);

    void deleteNotification(String notificationId);

    long getTotalCount(String userId);

    long getUnreadCount(String userId);
}
