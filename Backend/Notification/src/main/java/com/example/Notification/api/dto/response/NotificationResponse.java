package com.example.Notification.api.dto.response;

import com.example.Notification.shared.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * DTO for notification response.
 */
@Builder
@Schema(description = "Notification details")
public record NotificationResponse(

    @Schema(description = "Notification ID", example = "507f1f77bcf86cd799439011")
    String notificationId,

    @Schema(description = "Recipient user ID")
    String userId,

    @Schema(description = "Notification title", example = "File Uploaded")
    String title,

    @Schema(description = "Notification message")
    String message,

    @Schema(description = "Type of notification (INFO, WARNING, ERROR, SUCCESS)", example = "INFO")
    NotificationType notificationType,

    @Schema(description = "Is notification read")
    Boolean read,

    @Schema(description = "Related resource ID")
    String relatedResourceId,

    @Schema(description = "Action URL to trigger")
    String actionUrl,

    @Schema(description = "Notification creation timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,

    @Schema(description = "Notification read timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime readAt
) {}
