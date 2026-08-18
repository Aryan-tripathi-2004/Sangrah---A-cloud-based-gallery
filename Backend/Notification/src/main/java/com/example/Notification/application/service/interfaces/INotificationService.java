package com.example.Notification.application.service.interfaces;

import com.example.Notification.api.dto.request.InternalNotificationRequest;
import com.example.Notification.api.dto.response.BulkActionResponse;
import com.example.Notification.api.dto.response.HealthResponse;
import com.example.Notification.api.dto.response.NotificationActionResponse;
import com.example.Notification.api.dto.response.NotificationListResponse;
import com.example.Notification.api.dto.response.NotificationResponse;

import java.util.List;

public interface INotificationService {

    NotificationActionResponse createNotification(InternalNotificationRequest request);

    NotificationListResponse getUserNotifications(String userId, int skip, int limit);

    List<NotificationResponse> getUnreadNotifications(String userId);

    NotificationActionResponse markAsRead(String userId, String notificationId);

    BulkActionResponse markAllAsRead(String userId);

    void deleteNotification(String notificationId);

    long getTotalCount(String userId);

    long getUnreadCount(String userId);

    HealthResponse getHealth();

}
