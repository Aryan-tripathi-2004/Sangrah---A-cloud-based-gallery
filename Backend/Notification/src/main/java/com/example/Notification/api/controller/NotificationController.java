package com.example.Notification.api.controller;

import com.example.Notification.api.annotation.CurrentUserId;
import com.example.Notification.api.dto.request.NotificationStatusUpdateRequest;
import com.example.Notification.api.dto.response.BulkActionResponse;
import com.example.Notification.api.dto.response.HealthResponse;
import com.example.Notification.api.dto.response.NotificationListResponse;
import com.example.Notification.api.dto.response.UnreadCountResponse;
import com.example.Notification.application.service.interfaces.INotificationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get user's notifications")
    public ResponseEntity<NotificationListResponse> list(
            @CurrentUserId String userId,
            @RequestParam(defaultValue = "${notification.pagination.default-skip:0}") int skip,
            @RequestParam(defaultValue = "${notification.pagination.default-limit:50}") int limit) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, skip, limit));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get user's unread notification count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(@CurrentUserId String userId) {
        return ResponseEntity.ok(new UnreadCountResponse(notificationService.getUnreadCount(userId)));
    }

    @PatchMapping("/status")
    @Operation(summary = "Update notification read status", description = "Provide specific IDs to update, or leave empty to update all")
    public ResponseEntity<BulkActionResponse> updateNotificationStatus(
            @CurrentUserId String userId,
            @Valid @RequestBody NotificationStatusUpdateRequest request) {
        return ResponseEntity.ok(notificationService.updateNotificationStatus(userId, request));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(notificationService.getHealth());
    }
}