package com.example.Notification.api.controller;

import com.example.Notification.api.annotation.CurrentUserId;
import com.example.Notification.api.dto.request.InternalNotificationRequest;
import com.example.Notification.api.dto.response.BulkActionResponse;
import com.example.Notification.api.dto.response.NotificationActionResponse;
import com.example.Notification.api.dto.response.NotificationListResponse;
import com.example.Notification.api.dto.response.NotificationResponse;
import com.example.Notification.application.service.interfaces.INotificationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public ResponseEntity<NotificationListResponse> list(
            @CurrentUserId String userId,
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "50") int limit) {

        log.info("📋 [Notification Controller] Fetching notifications for user: {}, Skip: {}, Limit: {}", userId, skip, limit);

        if (skip < 0 || limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Invalid pagination parameters. Limit must be 1-100");
        }

        List<NotificationResponse> notifications = notificationService.getUserNotifications(userId, skip, limit);
        long unreadCount = notificationService.getUnreadCount(userId);
        long totalCount = notificationService.getTotalCount(userId);

        log.info("✅ [Notification Controller] Returning {} notifications for user: {}", notifications.size(), userId);

        NotificationListResponse response = new NotificationListResponse(
                notifications,
                unreadCount,
                totalCount,
                skip,
                limit
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Mark a single notification as read
     * PATCH /api/v1/notifications/{notificationId}/read
     */
    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<NotificationActionResponse> markRead(
            @CurrentUserId String userId,
            @PathVariable String notificationId) {

        log.info("✏️ [Notification Controller] Marking notification as read - ID: {}, User: {}", notificationId, userId);

        NotificationResponse updated = notificationService.markAsRead(userId, notificationId);

        log.info("✅ [Notification Controller] Notification marked as read - ID: {}", notificationId);

        NotificationActionResponse response = new NotificationActionResponse(
                updated.notificationId(),
                updated.read(),
                "Notification marked as read"
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Mark all notifications as read for user
     * POST /api/v1/notifications/mark-all-read
     */
    @PostMapping("/mark-all-read")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<BulkActionResponse> markAllRead(@CurrentUserId String userId) {
        log.info("✏️ [Notification Controller] Marking all notifications as read for user: {}", userId);

        long markedCount = notificationService.markAllAsRead(userId);
        log.info("✅ [Notification Controller] Marked {} notifications as read for user: {}", markedCount, userId);

        BulkActionResponse response = new BulkActionResponse(
                markedCount,
                "All notifications marked as read"
        );

        return ResponseEntity.ok(response);
    }

    /**
     * INTERNAL ENDPOINT - Create notification from other services
     * POST /api/v1/internal/notifications
     * Used by Event Service, Media Service, etc. for inter-service communication
     */
    @PostMapping("/internal/notifications")
    @Operation(summary = "Internal: Create notification from service")
    public ResponseEntity<NotificationActionResponse> createNotificationInternal(
            @Valid @RequestBody InternalNotificationRequest request) {
        
        log.info("🔔 [Notification Controller Internal] Creating notification from service");

        NotificationResponse created = notificationService.createNotification(
                request.recipientUserId(), 
                request.type(), 
                request.payload()
        );
        
        log.info("✅ [Notification Controller Internal] Notification created - ID: {}, Type: {}, Recipient: {}",
                created.notificationId(), created.notificationType(), created.userId());

        NotificationActionResponse response = new NotificationActionResponse(
                created.notificationId(),
                created.read(),
                "Notification created successfully"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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