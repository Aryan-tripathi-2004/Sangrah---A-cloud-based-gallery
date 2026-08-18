package com.example.Notification.api.controller;

import com.example.Notification.api.annotation.CurrentUserId;
import com.example.Notification.api.dto.request.InternalNotificationRequest;
import com.example.Notification.api.dto.response.BulkActionResponse;
import com.example.Notification.api.dto.response.HealthResponse;
import com.example.Notification.api.dto.response.NotificationActionResponse;
import com.example.Notification.api.dto.response.NotificationListResponse;
import com.example.Notification.application.service.interfaces.INotificationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, skip, limit));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<NotificationActionResponse> markRead(
            @CurrentUserId String userId,
            @PathVariable String notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(userId, notificationId));
    }

    @PostMapping("/mark-all-read")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<BulkActionResponse> markAllRead(@CurrentUserId String userId) {
        return ResponseEntity.ok(notificationService.markAllAsRead(userId));
    }

    @PostMapping("/internal/notifications")
    @Operation(summary = "Internal: Create notification from service")
    public ResponseEntity<NotificationActionResponse> createNotificationInternal(
            @Valid @RequestBody InternalNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.createNotification(request));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(notificationService.getHealth());
    }
}