package com.example.Notification.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record NotificationStatusUpdateRequest(
        @Schema(description = "List of notification IDs to update. If empty or null, all notifications are updated.")
        List<String> notificationIds,
        
        @Schema(description = "The target read status")
        boolean read
) {
}
