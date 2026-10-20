package com.example.Notification.application.service.impl;

import com.example.Notification.api.dto.request.InternalNotificationRequest;
import com.example.Notification.api.dto.request.NotificationStatusUpdateRequest;
import com.example.Notification.api.dto.response.BulkActionResponse;
import com.example.Notification.api.dto.response.HealthResponse;
import com.example.Notification.api.dto.response.NotificationActionResponse;
import com.example.Notification.api.dto.response.NotificationListResponse;
import com.example.Notification.api.dto.response.NotificationPreferenceResponse;
import com.example.Notification.api.dto.response.NotificationResponse;
import com.example.Notification.application.service.interfaces.INotificationService;
import com.example.Notification.infrastructure.mapper.NotificationMapper;
import com.example.Notification.infrastructure.persistence.document.NotificationDocument;

import com.example.Notification.infrastructure.persistence.document.NotificationPreferenceDocument;
import com.example.Notification.infrastructure.persistence.repository.NotificationPreferenceRepository;
import com.example.Notification.infrastructure.persistence.repository.NotificationRepository;
import com.example.Notification.shared.exception.PermissionDeniedException;
import com.example.Notification.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationMapper notificationMapper;

    @Value("${notification.pagination.min-skip}")
    private int minSkip;

    @Value("${notification.pagination.min-limit}")
    private int minLimit;

    @Value("${notification.pagination.max-limit}")
    private int maxLimit;

    @Value("${notification.preference.default-in-app-enabled}")
    private boolean defaultInAppEnabled;

    @Value("${notification.preference.default-email-enabled}")
    private boolean defaultEmailEnabled;

    @Override
    public NotificationActionResponse createNotification(InternalNotificationRequest request) {
        log.info("ðŸ”” [Notification Service] Creating notification - Type: {}, Recipient: {}", request.type(), request.recipientUserId());

        NotificationDocument notification = notificationMapper.toDocument(request);








        NotificationDocument saved = notificationRepository.save(notification);
        log.info("âœ… [Notification Service] Notification created successfully - ID: {}, Type: {}", saved.getId(), request.type());
        
        return new NotificationActionResponse(
                saved.getId(),
                saved.isRead(),
                "Notification created successfully"
        );
    }

    @Override
    public NotificationListResponse getUserNotifications(String userId, int skip, int limit) {
        log.info("ðŸ“‹ [Notification Service] Fetching notifications for user: {} - Skip: {}, Limit: {}", userId, skip, limit);

        if (skip < minSkip || limit < minLimit || limit > maxLimit) {
            throw new IllegalArgumentException("Invalid pagination parameters. Limit must be " + minLimit + "-" + maxLimit);
        }

        int page = skip / limit;
        Pageable pageable = PageRequest.of(page, limit);

        Page<NotificationDocument> paginated = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable);

        List<NotificationResponse> dtos = paginated.getContent().stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());

        long unreadCount = getUnreadCount(userId);
        long totalCount = paginated.getTotalElements();

        log.info("âœ… [Notification Service] Retrieved {} notifications for user: {}", dtos.size(), userId);
        
        return new NotificationListResponse(
                dtos,
                unreadCount,
                totalCount,
                skip,
                limit
        );
    }

    @Override
    public List<NotificationResponse> getUnreadNotifications(String userId) {
        log.info("ðŸ“¬ [Notification Service] Fetching unread notifications for user: {}", userId);
        List<NotificationDocument> unread = notificationRepository.findByRecipientUserIdAndReadFalse(userId);
        log.info("âœ… [Notification Service] Found {} unread notifications for user: {}", unread.size(), userId);
        return unread.stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public NotificationActionResponse markAsRead(String userId, String notificationId) {
        log.info("âœï¸ [Notification Service] Marking notification as read - ID: {}", notificationId);

        NotificationDocument notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> {
                    log.warn("âš ï¸ [Notification Service] Notification not found - ID: {}", notificationId);
                    return new ResourceNotFoundException("Notification not found with ID: " + notificationId);
                });

        if (!notification.getRecipientUserId().equals(userId)) {
            log.warn("âš ï¸ [Notification Service] User {} tried to read notification of user {}", userId, notification.getRecipientUserId());
            throw new PermissionDeniedException("Permission denied");
        }

        notification.setRead(true);
        NotificationDocument updated = notificationRepository.save(notification);
        log.info("âœ… [Notification Service] Notification marked as read - ID: {}", notificationId);
        
        return new NotificationActionResponse(
                updated.getId(),
                updated.isRead(),
                "Notification marked as read"
        );
    }

    @Override
    public BulkActionResponse markAllAsRead(String userId) {
        log.info("âœï¸ [Notification Service] Marking all notifications as read for user: {}", userId);

        // Execute bulk update entirely within the database engine
        long count = notificationRepository.markAllAsReadByUserId(userId);

        log.info("âœ… [Notification Service] Marked {} notifications as read for user: {}", count, userId);
        return new BulkActionResponse(
                count,
                "All notifications marked as read"
        );
    }

    @Override
    public BulkActionResponse updateNotificationStatus(String userId, NotificationStatusUpdateRequest request) {
        log.info("âœï¸ [Notification Service] Bulk updating notification status for user: {}", userId);
        
        if (request.notificationIds() == null || request.notificationIds().isEmpty()) {
            if (request.read()) {
                return markAllAsRead(userId);
            } else {
                // If they pass an empty array and read=false, that would mean mark ALL as unread, 
                // but typically we only support bulk mark as read. For safety:
                log.info("â„¹ï¸ [Notification Service] Bulk mark as unread not supported via empty list");
                return new BulkActionResponse(0, "No notifications updated");
            }
        }

        List<NotificationDocument> notifications = notificationRepository.findAllById(request.notificationIds());

        List<NotificationDocument> validNotifications = notifications.stream()
                .filter(n -> n.getRecipientUserId().equals(userId))
                .peek(n -> n.setRead(request.read()))
                .collect(Collectors.toList());

        if (!validNotifications.isEmpty()) {
            notificationRepository.saveAll(validNotifications);
        }

        return new BulkActionResponse(validNotifications.size(), "Notifications updated");
    }

    @Override
    public void deleteNotification(String notificationId) {
        log.info("ðŸ—‘ï¸ [Notification Service] Deleting notification - ID: {}", notificationId);
        notificationRepository.deleteById(notificationId);
        log.info("âœ… [Notification Service] Notification deleted successfully - ID: {}", notificationId);
    }

    @Override
    public long getTotalCount(String userId) {
        log.info("ðŸ“Š [Notification Service] Getting total notification count for user: {}", userId);
        long count = notificationRepository.countByRecipientUserId(userId);
        log.info("âœ… [Notification Service] Total notification count for user {}: {}", userId, count);
        return count;
    }

    @Override
    public long getUnreadCount(String userId) {
        log.info("ðŸ”” [Notification Service] Getting unread notification count for user: {}", userId);
        long count = notificationRepository.countByRecipientUserIdAndReadFalse(userId);
        log.info("âœ… [Notification Service] Unread notification count for user {}: {}", userId, count);
        return count;
    }

    @Override
    public NotificationPreferenceResponse getPreferences(String userId) {
        return preferenceRepository.findByUserId(userId)
                .map(pref -> new NotificationPreferenceResponse(pref.isInAppEnabled(), pref.isEmailEnabled()))
                .orElseGet(() -> new NotificationPreferenceResponse(defaultInAppEnabled, defaultEmailEnabled));
    }

    @Override
    public NotificationPreferenceResponse updatePreferences(String userId, Boolean inAppEnabled, Boolean emailEnabled) {
        NotificationPreferenceDocument preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> NotificationPreferenceDocument.builder()
                        .userId(userId)
                        .inAppEnabled(defaultInAppEnabled)
                        .emailEnabled(defaultEmailEnabled)
                        .build());

        if (inAppEnabled != null) {
            preference.setInAppEnabled(inAppEnabled);
        }
        if (emailEnabled != null) {
            preference.setEmailEnabled(emailEnabled);
        }
        preference.setUpdatedAt(Instant.now());

        NotificationPreferenceDocument saved = preferenceRepository.save(preference);
        return new NotificationPreferenceResponse(saved.isInAppEnabled(), saved.isEmailEnabled());


    }






    @Override
    public HealthResponse getHealth() {
        log.info("âœ… [Notification Service] Health check requested");
        return new HealthResponse(
                "UP",
                "Notification Service",
                System.currentTimeMillis()
        );
    }
}
