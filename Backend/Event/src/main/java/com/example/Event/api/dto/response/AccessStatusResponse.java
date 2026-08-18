package com.example.Event.api.dto.response;

import com.example.Event.shared.enums.AccessStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccessStatusResponse(
        AccessStatus status,
        boolean hasAccess,
        boolean showRequestButton,
        String createdAt,
        String expiresAt,
        String rejectionReason,
        String revokedAt
) {
    public static AccessStatusResponse simple(AccessStatus status, boolean hasAccess, boolean showRequestButton) {
        return new AccessStatusResponse(status, hasAccess, showRequestButton, null, null, null, null);
    }
}
