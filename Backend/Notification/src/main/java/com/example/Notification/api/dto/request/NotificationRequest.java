package com.example.Notification.api.dto.request;

import com.example.Notification.shared.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * DTO for creating a notification.
 */
@Builder
@Schema(description = "Request to create a notification")
public record NotificationRequest(

    @NotBlank(message = "User ID is required")
    @Schema(description = "Recipient user ID")
    String userId,

    @NotBlank(message = "Title is required")
    @Schema(description = "Notification title", example = "File Uploaded")
    String title,

    @NotBlank(message = "Message is required")
    @Schema(description = "Notification message")
    String message,

    @NotNull(message = "Notification type is required")
    @Schema(description = "Type of notification (INFO, WARNING, ERROR, SUCCESS)", example = "INFO")
    NotificationType notificationType,

    @Schema(description = "Related resource ID")
    String relatedResourceId,

    @Schema(description = "Action URL to trigger")
    String actionUrl
) {}
