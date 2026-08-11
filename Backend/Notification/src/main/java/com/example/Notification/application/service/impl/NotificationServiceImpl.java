package com.example.Notification.application.service.impl;

import com.example.Notification.api.dto.response.NotificationResponse;
import com.example.Notification.application.service.interfaces.INotificationService;
import com.example.Notification.infrastructure.mapper.NotificationMapper;
import com.example.Notification.infrastructure.persistence.document.NotificationDocument;
import com.example.Notification.infrastructure.persistence.repository.NotificationRepository;
import com.example.Notification.shared.enums.NotificationType;
import com.example.Notification.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public NotificationResponse createNotification(
            String recipientUserId,
            NotificationType type,
            Map<String, Object> payload) {

        log.info("🔔 [Notification Service] Creating notification - Type: {}, Recipient: {}", type, recipientUserId);

        NotificationDocument notification = NotificationDocument.builder()
                .recipientUserId(recipientUserId)
                .type(type)
                .payload(payload)
                .read(false)
                .createdAt(Instant.now())
                .build();

        NotificationDocument saved = notificationRepository.save(notification);
        log.info("✅ [Notification Service] Notification created successfully - ID: {}, Type: {}", saved.getId(), type);
        return notificationMapper.toResponse(saved);
    }

    @Override
    public List<NotificationResponse> getUserNotifications(String userId, int skip, int limit) {
        log.info("📋 [Notification Service] Fetching notifications for user: {} - Skip: {}, Limit: {}", userId, skip, limit);

        List<NotificationDocument> notifications = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId);

        int start = Math.min(skip, notifications.size());
        int end = Math.min(start + limit, notifications.size());
        List<NotificationDocument> paginated = notifications.subList(start, end);

        log.info("✅ [Notification Service] Retrieved {} notifications for user: {}", paginated.size(), userId);
        return paginated.stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationResponse> getUnreadNotifications(String userId) {
        log.info("📬 [Notification Service] Fetching unread notifications for user: {}", userId);
        List<NotificationDocument> unread = notificationRepository.findByRecipientUserIdAndReadFalse(userId);
        log.info("✅ [Notification Service] Found {} unread notifications for user: {}", unread.size(), userId);
        return unread.stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public NotificationResponse markAsRead(String userId, String notificationId) {
        log.info("✏️ [Notification Service] Marking notification as read - ID: {}", notificationId);

        NotificationDocument notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> {
                    log.warn("⚠️ [Notification Service] Notification not found - ID: {}", notificationId);
                    return new ResourceNotFoundException("Notification not found with ID: " + notificationId);
                });

        if (!notification.getRecipientUserId().equals(userId)) {
            log.warn("⚠️ [Notification Service] User {} tried to read notification of user {}", userId, notification.getRecipientUserId());
            throw new IllegalArgumentException("Permission denied");
        }

        notification.setRead(true);
        NotificationDocument updated = notificationRepository.save(notification);
        log.info("✅ [Notification Service] Notification marked as read - ID: {}", notificationId);
        return notificationMapper.toResponse(updated);
    }

    @Override
    public long markAllAsRead(String userId) {
        log.info("✏️ [Notification Service] Marking all notifications as read for user: {}", userId);

        List<NotificationDocument> unread = notificationRepository.findByRecipientUserIdAndReadFalse(userId);
        long count = unread.size();

        if (count > 0) {
            unread.forEach(n -> n.setRead(true));
            notificationRepository.saveAll(unread);
            log.info("✅ [Notification Service] Marked {} notifications as read for user: {}", count, userId);
        } else {
            log.info("ℹ️ [Notification Service] No unread notifications to mark for user: {}", userId);
        }

        return count;
    }

    @Override
    public void deleteNotification(String notificationId) {
        log.info("🗑️ [Notification Service] Deleting notification - ID: {}", notificationId);
        notificationRepository.deleteById(notificationId);
        log.info("✅ [Notification Service] Notification deleted successfully - ID: {}", notificationId);
    }

    @Override
    public long getTotalCount(String userId) {
        log.info("📊 [Notification Service] Getting total notification count for user: {}", userId);
        List<NotificationDocument> all = notificationRepository.findByRecipientUserId(userId);
        long count = all.size();
        log.info("✅ [Notification Service] Total notification count for user {}: {}", userId, count);
        return count;
    }

    @Override
    public long getUnreadCount(String userId) {
        log.info("🔔 [Notification Service] Getting unread notification count for user: {}", userId);
        List<NotificationDocument> unread = notificationRepository.findByRecipientUserIdAndReadFalse(userId);
        long count = unread.size();
        log.info("✅ [Notification Service] Unread notification count for user {}: {}", userId, count);
        return count;
    }
}
