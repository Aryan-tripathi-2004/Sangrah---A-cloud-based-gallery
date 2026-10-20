package com.example.Notification.api.controller;

import com.example.Notification.api.dto.request.InternalNotificationRequest;
import com.example.Notification.api.dto.response.NotificationActionResponse;
import com.example.Notification.application.service.interfaces.INotificationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller dedicated strictly to internal system-to-system notification requests.
 *
 * <p>Isolated onto {@code /internal/api/v1/notifications} outside the API Gateway's
 * public client ingress perimeter to prevent Broken Function Level Authorization (BFLA).</p>
 */
@Slf4j
@RestController
@RequestMapping("/internal/api/v1/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final INotificationService notificationService;

    @PostMapping
    @Operation(summary = "Internal: Create notification from service")
    public ResponseEntity<NotificationActionResponse> createNotificationInternal(
            @Valid @RequestBody InternalNotificationRequest request) {
        log.info("Received internal notification creation request for user: {}", request.recipientUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.createNotification(request));
    }
}
