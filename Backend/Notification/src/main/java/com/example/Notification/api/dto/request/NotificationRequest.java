package com.example.Notification.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a notification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a notification")
public class NotificationRequest {

    @NotBlank(message = "User ID is required")
    @Schema(description = "Recipient user ID")
    private String userId;

    @NotBlank(message = "Title is required")
    @Schema(description = "Notification title", example = "File Uploaded")
    private String title;

    @NotBlank(message = "Message is required")
    @Schema(description = "Notification message")
    private String message;

    @NotBlank(message = "Notification type is required")
    @Schema(description = "Type of notification (INFO, WARNING, ERROR, SUCCESS)", example = "INFO")
    private String notificationType;

    @Schema(description = "Related resource ID")
    private String relatedResourceId;

    @Schema(description = "Action URL to trigger")
    private String actionUrl;
}
