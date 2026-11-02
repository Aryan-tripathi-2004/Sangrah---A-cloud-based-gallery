package com.example.Notification.api.dto.response;

import com.example.Notification.shared.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import com.example.Notification.api.dto.request.NotificationPayloadDTO;

/**
 * DTO for notification response.
 */
@Builder
@Schema(description = "Notification details")
public record NotificationResponse(

    @Schema(description = "Notification ID", example = "507f1f77bcf86cd799439011")
    String id,

    @Schema(description = "Recipient user ID")
    String recipientUserId,

    @Schema(description = "Type of notification", example = "INFO")
    NotificationType type,

    @Schema(description = "Notification payload")
    NotificationPayloadDTO payload,

    @Schema(description = "Is notification read")
    Boolean read,

    @Schema(description = "Notification creation timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,

    @Schema(description = "Notification read timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime readAt
) {}
