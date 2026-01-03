package com.example.Auth.dto.mapper;

import com.example.Auth.dto.response.SessionResponse;
import com.example.Auth.model.SessionModel;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Mapper for Session-related DTOs.
 */
@Component
public class SessionMapper {

    /**
     * Convert SessionModel to SessionResponse.
     */
    public SessionResponse toResponse(SessionModel model) {
        if (model == null) {
            return null;
        }

        SessionResponse response = new SessionResponse();
        response.setSessionId(model.getSessionId());
        response.setUserId(model.getUserId());
        response.setCreatedAt(model.getCreatedAt());
        response.setExpiresAt(model.getExpiresAt());
        response.setLastAccessedAt(model.getLastAccessedAt());
        response.setIpAddress(model.getIpAddress());
        response.setUserAgent(model.getUserAgent());
        response.setDeviceFingerprint(model.getDeviceFingerprint());
        response.setRevoked(model.isRevoked());
        response.setRevokedAt(model.getRevokedAt());
        response.setRevocationReason(model.getRevocationReason());

        // Set computed fields
        response.setActive(model.isActive());
        response.setExpired(model.isExpired());

        // Calculate remaining seconds
        if (model.getExpiresAt() != null && !model.isExpired()) {
            long remaining = Duration.between(Instant.now(), model.getExpiresAt()).getSeconds();
            response.setRemainingSeconds(Math.max(0, remaining));
        } else {
            response.setRemainingSeconds(0);
        }

        return response;
    }
}
