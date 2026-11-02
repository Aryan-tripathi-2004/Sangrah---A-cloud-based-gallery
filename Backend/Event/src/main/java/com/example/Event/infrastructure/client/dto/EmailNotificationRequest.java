package com.example.Event.infrastructure.client.dto;

import com.example.Event.shared.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmailNotificationRequest(
        String userEmail,
        NotificationType emailType,
        String subject,
        String body
) {
}
