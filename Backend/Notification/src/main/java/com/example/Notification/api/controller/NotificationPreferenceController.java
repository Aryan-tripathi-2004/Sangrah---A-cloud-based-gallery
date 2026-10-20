package com.example.Notification.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import com.example.Notification.api.annotation.CurrentUserId;
import com.example.Notification.api.dto.request.NotificationPreferenceUpdateRequest;
import com.example.Notification.api.dto.response.NotificationPreferenceResponse;
import com.example.Notification.application.service.interfaces.INotificationService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications/preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final INotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get notification preferences")
    public ResponseEntity<NotificationPreferenceResponse> get(@CurrentUserId String userId) {
        return ResponseEntity.ok(notificationService.getPreferences(userId));
    }

    @PatchMapping
    @Operation(summary = "Update notification preferences")
    public ResponseEntity<NotificationPreferenceResponse> update(
            @CurrentUserId String userId,
            @RequestBody NotificationPreferenceUpdateRequest request) {
        return ResponseEntity.ok(notificationService.updatePreferences(userId, request.inAppEnabled(), request.emailEnabled()));
    }
}
