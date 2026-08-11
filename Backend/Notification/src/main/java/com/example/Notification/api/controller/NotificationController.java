package com.example.Notification.api.controller;

import com.example.Notification.api.annotation.CurrentUserId;
import com.example.Notification.application.service.interfaces.INotificationService;
import com.example.Notification.infrastructure.persistence.document.NotificationDocument;
import com.example.Notification.shared.enums.NotificationType;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;

    /**
     * Get user's notifications (paginated, most recent first)
     * GET /api/v1/notifications?skip=0&limit=50
     */
    @GetMapping
    @Operation(summary = "Get user's notifications")
    public ResponseEntity<Map<String, Object>> list(
            @CurrentUserId String userId,
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "50") int limit) {

        log.info("📋 [Notification Controller] Fetching notifications for user: {}, Skip: {}, Limit: {}", userId, skip, limit);

        if (skip < 0 || limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Invalid pagination parameters. Limit must be 1-100");
        }

        List<NotificationDocument> notifications = notificationService.getUserNotifications(userId, skip, limit);
        long unreadCount = notificationService.getUnreadCount(userId);
        long totalCount = notificationService.getTotalCount(userId);

        List<Map<String, Object>> notificationDtos = notifications.stream()
                .map(n -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", n.getId());
                    map.put("type", n.getType());
                    map.put("payload", n.getPayload());
                    map.put("read", n.isRead());
                    map.put("createdAt", n.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());

        log.info("✅ [Notification Controller] Returning {} notifications for user: {}", notificationDtos.size(), userId);

        return ResponseEntity.ok(Map.of(
                "notifications", notificationDtos,
                "unreadCount", unreadCount,
                "totalCount", totalCount,
                "skip", skip,
                "limit", limit
        ));
    }

    /**
     * Mark a single notification as read
     * PATCH /api/v1/notifications/{notificationId}/read
     */
    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<Map<String, Object>> markRead(
            @CurrentUserId String userId,
            @PathVariable String notificationId) {

        log.info("✏️ [Notification Controller] Marking notification as read - ID: {}, User: {}", notificationId, userId);

        NotificationDocument updated = notificationService.markAsRead(notificationId);

        // Verify ownership (notification belongs to this user)
        if (!updated.getRecipientUserId().equals(userId)) {
            log.warn("⚠️ [Notification Controller] User {} tried to read notification of user {}", userId, updated.getRecipientUserId());
            throw new IllegalArgumentException("You don't have permission to read this notification");
        }

        log.info("✅ [Notification Controller] Notification marked as read - ID: {}", notificationId);

        return ResponseEntity.ok(Map.of(
                "notificationId", updated.getId(),
                "read", updated.isRead(),
                "message", "Notification marked as read"
        ));
    }

    /**
     * Mark all notifications as read for user
     * POST /api/v1/notifications/mark-all-read
     */
    @PostMapping("/mark-all-read")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Map<String, Object>> markAllRead(@CurrentUserId String userId) {
        log.info("✏️ [Notification Controller] Marking all notifications as read for user: {}", userId);

        long markedCount = notificationService.markAllAsRead(userId);
        log.info("✅ [Notification Controller] Marked {} notifications as read for user: {}", markedCount, userId);

        return ResponseEntity.ok(Map.of(
                "markedCount", markedCount,
                "message", "All notifications marked as read"
        ));
    }

    /**
     * INTERNAL ENDPOINT - Create notification from other services
     * POST /api/v1/internal/notifications
     * Used by Event Service, Media Service, etc. for inter-service communication
     */
    @PostMapping("/internal/notifications")
    @Operation(summary = "Internal: Create notification from service")
    public ResponseEntity<Map<String, Object>> createNotificationInternal(@RequestBody Map<String, Object> payload) {
        log.info("🔔 [Notification Controller Internal] Creating notification from service");

        String recipientUserId = (String) payload.get("recipientUserId");
        String typeStr = (String) payload.get("type");
        @SuppressWarnings("unchecked")
        Map<String, Object> notificationPayload = (Map<String, Object>) payload.get("payload");

        if (recipientUserId == null || recipientUserId.trim().isEmpty()) {
            throw new IllegalArgumentException("recipientUserId is required");
        }

        if (typeStr == null || typeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("type is required");
        }

        NotificationType type;
        try {
            type = NotificationType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid notification type: " + typeStr);
        }

        if (notificationPayload == null) {
            notificationPayload = new HashMap<>();
        }

        NotificationDocument created = notificationService.createNotification(recipientUserId, type, notificationPayload);
        log.info("✅ [Notification Controller Internal] Notification created - ID: {}, Type: {}, Recipient: {}",
                created.getId(), type, recipientUserId);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "notificationId", created.getId(),
                "type", created.getType(),
                "recipientUserId", created.getRecipientUserId(),
                "message", "Notification created successfully"
        ));
    }

    /**
     * Health check endpoint
     * GET /api/v1/notifications/health
     */
    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, Object>> health() {
        log.info("✅ [Notification Controller] Health check");
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Notification Service",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
