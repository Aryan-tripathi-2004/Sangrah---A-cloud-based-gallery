package com.example.Event.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthUserDataResponse(
        String id,
        String userId,
        String email,
        String displayName
) {
    public String resolvedUserId() {
        if (id != null && !id.isBlank()) {
            return id;
        }
        return userId;
    }
}
