package com.example.Auth.dto.mapper;

import com.example.Auth.dto.response.AuditLogResponse;
import com.example.Auth.model.AuditLogModel;
import org.springframework.stereotype.Component;

/**
 * Mapper for AuditLog-related DTOs.
 */
@Component
public class AuditLogMapper {

    /**
     * Convert AuditLogModel to AuditLogResponse.
     */
    public AuditLogResponse toResponse(AuditLogModel model) {
        if (model == null) {
            return null;
        }

        AuditLogResponse response = new AuditLogResponse();
        response.setId(model.getId());
        response.setUserId(model.getUserId());
        response.setAction(model.getAction());
        response.setEntityType(model.getEntityType());
        response.setEntityId(model.getEntityId());
        response.setIpAddress(model.getIpAddress());
        response.setUserAgent(model.getUserAgent());
        response.setDetails(model.getDetails());
        response.setCreatedAt(model.getCreatedAt());

        return response;
    }
}
