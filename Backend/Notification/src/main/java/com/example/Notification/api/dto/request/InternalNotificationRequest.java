package com.example.Notification.api.dto.request;

import com.example.Notification.shared.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record InternalNotificationRequest(
        @NotBlank String recipientUserId,
        @NotNull NotificationType type,
        Map<String, Object> payload
) {}
