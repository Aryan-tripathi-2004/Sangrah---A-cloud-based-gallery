package com.example.Notification.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for notification response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification details")
public class NotificationResponse {

    @Schema(description = "Notification ID", example = "507f1f77bcf86cd799439011")
    private String notificationId;

    @Schema(description = "Recipient user ID")
    private String userId;

    @Schema(description = "Notification title", example = "File Uploaded")
    private String title;

    @Schema(description = "Notification message")
    private String message;

    @Schema(description = "Type of notification (INFO, WARNING, ERROR, SUCCESS)", example = "INFO")
    private String notificationType;

    @Schema(description = "Is notification read")
    private Boolean read;

    @Schema(description = "Related resource ID")
    private String relatedResourceId;

    @Schema(description = "Action URL to trigger")
    private String actionUrl;

    @Schema(description = "Notification creation timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Notification read timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime readAt;
}
