package com.example.Notification.api.dto.response;

public record HealthResponse(
        String status,
        String service,
        long timestamp
) {}
