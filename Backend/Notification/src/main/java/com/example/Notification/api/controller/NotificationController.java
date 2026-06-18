package com.example.Notification.api.controller;

import com.example.Notification.application.service.NotificationService;
import com.example.Notification.infrastructure.persistence.document.NotificationDocument;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
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

    private final NotificationService notificationService;

    /**
     * Get user's notifications (paginated, most recent first)
     * GET /api/v1/notifications?skip=0&limit=50
     */
    @GetMapping
    @Operation(summary = "Get user's notifications")
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            log.info("📋 [Notification Controller] Fetching notifications for user: {}, Skip: {}, Limit: {}", userId, skip, limit);

            if (userId == null || userId.isEmpty()) {
                log.warn("⚠️ [Notification Controller] Missing X-User-Id header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User ID not found in headers"));
            }

            // Validate pagination parameters
            if (skip < 0 || limit < 1 || limit > 100) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid pagination parameters. Limit must be 1-100"));
            }

            // Fetch notifications
            List<NotificationDocument> notifications = notificationService.getUserNotifications(userId, skip, limit);
            long unreadCount = notificationService.getUnreadCount(userId);
            long totalCount = notificationService.getTotalCount(userId);

            // Build response
            List<Map<String, Object>> notificationDtos = notifications.stream()
                    .map(n -> new HashMap<String, Object>() {{
                        put("id", n.getId());
                        put("type", n.getType());
                        put("payload", n.getPayload());
                        put("read", n.isRead());
                        put("createdAt", n.getCreatedAt());
                    }})
                    .collect(Collectors.toList());

            log.info("✅ [Notification Controller] Returning {} notifications for user: {}", notificationDtos.size(), userId);

            return ResponseEntity.ok(Map.of(
                    "notifications", notificationDtos,
                    "unreadCount", unreadCount,
                    "totalCount", totalCount,
                    "skip", skip,
                    "limit", limit
            ));

        } catch (Exception e) {
            log.error("❌ [Notification Controller] Error fetching notifications: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch notifications: " + e.getMessage()));
        }
    }

    /**
     * Mark a single notification as read
     * PATCH /api/v1/notifications/{notificationId}/read
     */
    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<?> markRead(
            @PathVariable String notificationId,
            HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            log.info("✏️ [Notification Controller] Marking notification as read - ID: {}, User: {}", notificationId, userId);

            if (userId == null || userId.isEmpty()) {
                log.warn("⚠️ [Notification Controller] Missing X-User-Id header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User ID not found in headers"));
            }

            NotificationDocument updated = notificationService.markAsRead(notificationId);

            // Verify ownership (notification belongs to this user)
            if (!updated.getRecipientUserId().equals(userId)) {
                log.warn("⚠️ [Notification Controller] User {} tried to read notification of user {}", userId, updated.getRecipientUserId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You don't have permission to read this notification"));
            }

            log.info("✅ [Notification Controller] Notification marked as read - ID: {}", notificationId);

            return ResponseEntity.ok(Map.of(
                    "notificationId", updated.getId(),
                    "read", updated.isRead(),
                    "message", "Notification marked as read"
            ));

        } catch (RuntimeException e) {
            log.warn("⚠️ [Notification Controller] Notification not found: {}", notificationId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Notification not found"));
        } catch (Exception e) {
            log.error("❌ [Notification Controller] Error marking notification as read: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to mark notification as read: " + e.getMessage()));
        }
    }

    /**
     * Mark all notifications as read for user
     * POST /api/v1/notifications/mark-all-read
     */
    @PostMapping("/mark-all-read")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<?> markAllRead(HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            log.info("✏️ [Notification Controller] Marking all notifications as read for user: {}", userId);

            if (userId == null || userId.isEmpty()) {
                log.warn("⚠️ [Notification Controller] Missing X-User-Id header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User ID not found in headers"));
            }

            long markedCount = notificationService.markAllAsRead(userId);
            log.info("✅ [Notification Controller] Marked {} notifications as read for user: {}", markedCount, userId);

            return ResponseEntity.ok(Map.of(
                    "markedCount", markedCount,
                    "message", "All notifications marked as read"
            ));

        } catch (Exception e) {
            log.error("❌ [Notification Controller] Error marking all as read: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to mark all notifications as read: " + e.getMessage()));
        }
    }

    /**
     * INTERNAL ENDPOINT - Create notification from other services
     * POST /api/v1/internal/notifications
     * Used by Event Service, Media Service, etc. for inter-service communication
     */
    @PostMapping("/internal/notifications")
    @Operation(summary = "Internal: Create notification from service")
    public ResponseEntity<?> createNotificationInternal(@RequestBody Map<String, Object> payload) {
        try {
            log.info("🔔 [Notification Controller Internal] Creating notification from service");

            String recipientUserId = (String) payload.get("recipientUserId");
            String type = (String) payload.get("type");
            @SuppressWarnings("unchecked")
            Map<String, Object> notificationPayload = (Map<String, Object>) payload.get("payload");

            if (recipientUserId == null || recipientUserId.isEmpty()) {
                log.warn("⚠️ [Notification Controller Internal] Missing recipientUserId");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "recipientUserId is required"));
            }

            if (type == null || type.isEmpty()) {
                log.warn("⚠️ [Notification Controller Internal] Missing notification type");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "type is required"));
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

        } catch (Exception e) {
            log.error("❌ [Notification Controller Internal] Error creating notification: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create notification: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     * GET /api/v1/notifications/health
     */
    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<?> health() {
        log.info("✅ [Notification Controller] Health check");
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Notification Service",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
