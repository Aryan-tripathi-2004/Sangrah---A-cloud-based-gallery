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
import org.springframework.transaction.annotation.Transactional;

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

    @Value("${notification.pagination.default-skip}")
    private int defaultSkip;

    @Value("${notification.pagination.default-limit}")
    private int defaultLimit;

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
    @Transactional
    public NotificationActionResponse createNotification(InternalNotificationRequest request) {
        log.info("[Notification Service] Creating notification - Type: {}, Recipient: {}", request.type(), request.recipientUserId());

        NotificationDocument notification = notificationMapper.toDocument(request);
        NotificationDocument saved = notificationRepository.save(notification);
        log.info("[Notification Service] Notification created successfully - ID: {}, Type: {}", saved.getId(), request.type());

        return new NotificationActionResponse(
                saved.getId(),
                saved.isRead(),
                "Notification created successfully"
        );
    }

    @Override
    public NotificationListResponse getUserNotifications(String userId, int skip, int limit) {
        int validLimit = Math.min(maxLimit, Math.max(minLimit, limit));
        int validSkip = Math.max(minSkip, skip);
        int page = validSkip / validLimit;
        return getUserNotificationsPaged(userId, page, validLimit);
    }

    @Override
    public NotificationListResponse getUserNotifications(String userId, Integer page, Integer size, Integer skip, Integer limit) {
        int calculatedPage = 0;
        int calculatedSize = defaultLimit;

        if (page != null || size != null) {
            calculatedPage = page != null ? Math.max(0, page) : 0;
            calculatedSize = size != null ? Math.min(maxLimit, Math.max(minLimit, size)) : defaultLimit;
        } else if (skip != null || limit != null) {
            int lim = limit != null ? Math.min(maxLimit, Math.max(minLimit, limit)) : defaultLimit;
            int sk = skip != null ? Math.max(minSkip, skip) : defaultSkip;
            calculatedPage = sk / lim;
            calculatedSize = lim;
        }
        return getUserNotificationsPaged(userId, calculatedPage, calculatedSize);
    }

    @Override
    public NotificationListResponse getUserNotificationsPaged(String userId, int page, int size) {
        int validPage = Math.max(0, page);
        int validSize = Math.min(maxLimit, Math.max(minLimit, size));
        log.info("[Notification Service] Fetching paged notifications for user: {} - Page: {}, Size: {}", userId, validPage, validSize);

        Pageable pageable = PageRequest.of(validPage, validSize);
        Page<NotificationDocument> paginated = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable);

        List<NotificationResponse> dtos = paginated.getContent().stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());

        long unreadCount = getUnreadCount(userId);
        long totalCount = paginated.getTotalElements();

        log.info("[Notification Service] Retrieved {} notifications for user: {}", dtos.size(), userId);

        return new NotificationListResponse(
                dtos,
                unreadCount,
                totalCount,
                validPage * validSize,
                validSize,
                validPage,
                validSize,
                paginated.getTotalPages(),
                paginated.hasNext()
        );
    }

    @Override
    public List<NotificationResponse> getUnreadNotifications(String userId) {
        log.info("[Notification Service] Fetching unread notifications for user: {}", userId);
        List<NotificationDocument> unread = notificationRepository.findByRecipientUserIdAndReadFalse(userId);
        log.info("[Notification Service] Found {} unread notifications for user: {}", unread.size(), userId);
        return unread.stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NotificationActionResponse markAsRead(String userId, String notificationId) {
        log.info("[Notification Service] Marking notification as read - ID: {}", notificationId);

        NotificationDocument notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> {
                    log.warn("[Notification Service] Notification not found - ID: {}", notificationId);
                    return new ResourceNotFoundException("Notification not found with ID: " + notificationId);
                });

        if (!notification.getRecipientUserId().equals(userId)) {
            log.warn("[Notification Service] User {} tried to read notification of user {}", userId, notification.getRecipientUserId());
            throw new PermissionDeniedException("Permission denied");
        }

        notification.setRead(true);
        NotificationDocument updated = notificationRepository.save(notification);
        log.info("[Notification Service] Notification marked as read - ID: {}", notificationId);

        return new NotificationActionResponse(
                updated.getId(),
                updated.isRead(),
                "Notification marked as read"
        );
    }

    @Override
    public BulkActionResponse markAllAsRead(String userId) {
        log.info("[Notification Service] Marking all notifications as read for user: {}", userId);

        long count = notificationRepository.markAllAsReadByUserId(userId);

        log.info("[Notification Service] Marked {} notifications as read for user: {}", count, userId);
        return new BulkActionResponse(
                count,
                "All notifications marked as read"
        );
    }

    @Override
    @Transactional
    public BulkActionResponse updateNotificationStatus(String userId, NotificationStatusUpdateRequest request) {
        log.info("[Notification Service] Bulk updating notification status for user: {}", userId);

        if (request.notificationIds() == null || request.notificationIds().isEmpty()) {
            if (request.read()) {
                return markAllAsRead(userId);
            } else {
                log.info("[Notification Service] Bulk mark as unread not supported via empty list");
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
    @Transactional
    public void deleteNotification(String userId, String notificationId) {
        log.info("[Notification Service] Deleting notification - ID: {} for user: {}", notificationId, userId);

        if (userId == null || userId.isBlank()) {
            throw new PermissionDeniedException("Authentication context is required to delete a notification");
        }

        NotificationDocument notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        if (!userId.equals(notification.getRecipientUserId())) {
            throw new PermissionDeniedException("Permission denied: You do not own this notification");
        }

        notificationRepository.delete(notification);
        log.info("[Notification Service] Notification deleted successfully - ID: {}", notificationId);
    }

    @Override
    public long getTotalCount(String userId) {
        log.info("[Notification Service] Getting total notification count for user: {}", userId);
        long count = notificationRepository.countByRecipientUserId(userId);
        log.info("[Notification Service] Total notification count for user {}: {}", userId, count);
        return count;
    }

    @Override
    public long getUnreadCount(String userId) {
        log.info("[Notification Service] Getting unread notification count for user: {}", userId);
        long count = notificationRepository.countByRecipientUserIdAndReadFalse(userId);
        log.info("[Notification Service] Unread notification count for user {}: {}", userId, count);
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
        log.info("[Notification Service] Health check requested");
        return new HealthResponse(
                "UP",
                "Notification Service",
                System.currentTimeMillis()
        );
    }
}
