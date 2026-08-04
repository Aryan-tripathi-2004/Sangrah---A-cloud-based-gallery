package com.example.Event.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthUserLookupResponse(
        String id,
        String userId,
        String email,
        String displayName,
        AuthUserDataResponse data
) {
    public String resolvedUserId() {
        if (userId != null && !userId.isBlank()) {
            return userId;
        }
        if (id != null && !id.isBlank()) {
            return id;
        }
        return data != null ? data.resolvedUserId() : null;
    }

    public String resolvedEmail() {
        if (email != null && !email.isBlank()) {
            return email;
        }
        return data != null ? data.email() : null;
    }

    public String resolvedDisplayName() {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        return data != null ? data.displayName() : null;
    }
}
