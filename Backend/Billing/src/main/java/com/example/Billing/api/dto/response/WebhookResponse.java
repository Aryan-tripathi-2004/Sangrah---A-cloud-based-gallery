package com.example.Billing.api.dto.response;

import lombok.Builder;

@Builder
public record WebhookResponse(
    String status,
    String eventId,
    String eventType,
    Long processingTime_ms,
    String timestamp,
    String message
) {


    public static WebhookResponse success(
        String eventId,
        String eventType,
        long processingTime,
        String timestamp
    ) {
        return new WebhookResponse(
            "SUCCESS",
            eventId,
            eventType,
            processingTime,
            timestamp,
            "Webhook processed successfully"
        );
    }

    public static WebhookResponse error(String message, String detail, String timestamp) {
        String combinedMessage = detail == null || detail.isBlank()
            ? message
            : message + ": " + detail;
        return new WebhookResponse("ERROR", null, null, null, timestamp, combinedMessage);
    }
}
