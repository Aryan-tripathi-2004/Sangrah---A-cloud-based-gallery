package com.example.Event.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmailNotificationRequest(
        String userEmail,
        String type,
        String subject,
        String body
) {
}
