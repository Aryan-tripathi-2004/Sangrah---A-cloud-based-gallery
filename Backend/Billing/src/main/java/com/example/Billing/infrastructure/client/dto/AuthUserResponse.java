package com.example.Billing.infrastructure.client.dto;

public record AuthUserResponse(
    AuthUserDataResponse data,
    String status,
    String message
) {
    public record AuthUserDataResponse(
        String id,
        String email,
        String displayName,
        String createdAt
    ) {}
}
