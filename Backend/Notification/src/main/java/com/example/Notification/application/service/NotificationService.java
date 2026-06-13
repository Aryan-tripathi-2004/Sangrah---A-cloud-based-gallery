package com.example.Notification.application.service;

import com.example.Notification.infrastructure.persistence.document.NotificationDocument;
import com.example.Notification.infrastructure.persistence.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Create a new notification
     */
    public NotificationDocument createNotification(
            String recipientUserId,
            String type,
            Map<String, Object> payload) {

        log.info("🔔 [Notification Service] Creating notification - Type: {}, Recipient: {}", type, recipientUserId);

        try {
            NotificationDocument notification = NotificationDocument.builder()
                    .recipientUserId(recipientUserId)
                    .type(type)
                    .payload(payload)
                    .read(false)
                    .createdAt(Instant.now())
                    .build();

            NotificationDocument saved = notificationRepository.save(notification);
            log.info("✅ [Notification Service] Notification created successfully - ID: {}, Type: {}", saved.getId(), type);
            return saved;

        } catch (Exception e) {
            log.error("❌ [Notification Service] Failed to create notification - Type: {}, Recipient: {} - Error: {}",
                    type, recipientUserId, e.getMessage(), e);
            throw new RuntimeException("Failed to create notification: " + e.getMessage());
        }
    }

    /**
     * Get all notifications for a user (paginated, sorted by creation date descending)
     */
    public List<NotificationDocument> getUserNotifications(String userId, int skip, int limit) {
        log.info("📋 [Notification Service] Fetching notifications for user: {} - Skip: {}, Limit: {}", userId, skip, limit);

        try {
            Pageable pageable = PageRequest.of(skip / limit, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
            List<NotificationDocument> notifications = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId);

            // Apply pagination manually since findByRecipientUserIdOrderByCreatedAtDesc doesn't support Pageable
            int start = skip;
            int end = Math.min(start + limit, notifications.size());
            List<NotificationDocument> paginated = notifications.subList(start, end);

            log.info("✅ [Notification Service] Retrieved {} notifications for user: {}", paginated.size(), userId);
            return paginated;

        } catch (Exception e) {
            log.error("❌ [Notification Service] Failed to fetch notifications for user: {} - Error: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch notifications: " + e.getMessage());
        }
    }

    /**
     * Get unread notifications for a user
     */
    public List<NotificationDocument> getUnreadNotifications(String userId) {
        log.info("📬 [Notification Service] Fetching unread notifications for user: {}", userId);

        try {
            List<NotificationDocument> unread = notificationRepository.findByRecipientUserIdAndReadFalse(userId);
            log.info("✅ [Notification Service] Found {} unread notifications for user: {}", unread.size(), userId);
            return unread;

        } catch (Exception e) {
            log.error("❌ [Notification Service] Failed to fetch unread notifications for user: {} - Error: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch unread notifications: " + e.getMessage());
        }
    }

    /**
     * Mark a single notification as read
     */
    public NotificationDocument markAsRead(String notificationId) {
        log.info("✏️ [Notification Service] Marking notification as read - ID: {}", notificationId);

        try {
            Optional<NotificationDocument> optional = notificationRepository.findById(notificationId);
            if (optional.isEmpty()) {
                log.warn("⚠️ [Notification Service] Notification not found - ID: {}", notificationId);
                throw new RuntimeException("Notification not found with ID: " + notificationId);
            }

            NotificationDocument notification = optional.get();
            notification.setRead(true);
            NotificationDocument updated = notificationRepository.save(notification);
            log.info("✅ [Notification Service] Notification marked as read - ID: {}", notificationId);
            return updated;

        } catch (Exception e) {
            log.error("❌ [Notification Service] Failed to mark notification as read - ID: {} - Error: {}", notificationId, e.getMessage(), e);
            throw new RuntimeException("Failed to mark notification as read: " + e.getMessage());
        }
    }

    /**
     * Mark all notifications as read for a user
     */
    public long markAllAsRead(String userId) {
        log.info("✏️ [Notification Service] Marking all notifications as read for user: {}", userId);

        try {
            List<NotificationDocument> unread = getUnreadNotifications(userId);
            long count = unread.size();

            if (count > 0) {
                unread.forEach(n -> n.setRead(true));
                notificationRepository.saveAll(unread);
                log.info("✅ [Notification Service] Marked {} notifications as read for user: {}", count, userId);
            } else {
                log.info("ℹ️ [Notification Service] No unread notifications to mark for user: {}", userId);
            }

            return count;

        } catch (Exception e) {
            log.error("❌ [Notification Service] Failed to mark all notifications as read for user: {} - Error: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to mark all notifications as read: " + e.getMessage());
        }
    }

    /**
     * Delete a notification
     */
    public void deleteNotification(String notificationId) {
        log.info("🗑️ [Notification Service] Deleting notification - ID: {}", notificationId);

        try {
            notificationRepository.deleteById(notificationId);
            log.info("✅ [Notification Service] Notification deleted successfully - ID: {}", notificationId);

        } catch (Exception e) {
            log.error("❌ [Notification Service] Failed to delete notification - ID: {} - Error: {}", notificationId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete notification: " + e.getMessage());
        }
    }

    /**
     * Get total count of notifications for a user
     */
    public long getTotalCount(String userId) {
        log.info("📊 [Notification Service] Getting total notification count for user: {}", userId);

        try {
            List<NotificationDocument> all = notificationRepository.findByRecipientUserId(userId);
            long count = all.size();
            log.info("✅ [Notification Service] Total notification count for user {}: {}", userId, count);
            return count;

        } catch (Exception e) {
            log.error("❌ [Notification Service] Failed to get notification count for user: {} - Error: {}", userId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Get unread count for a user
     */
    public long getUnreadCount(String userId) {
        log.info("🔔 [Notification Service] Getting unread notification count for user: {}", userId);

        try {
            List<NotificationDocument> unread = getUnreadNotifications(userId);
            long count = unread.size();
            log.info("✅ [Notification Service] Unread notification count for user {}: {}", userId, count);
            return count;

        } catch (Exception e) {
            log.error("❌ [Notification Service] Failed to get unread count for user: {} - Error: {}", userId, e.getMessage(), e);
            return 0;
        }
    }
}
