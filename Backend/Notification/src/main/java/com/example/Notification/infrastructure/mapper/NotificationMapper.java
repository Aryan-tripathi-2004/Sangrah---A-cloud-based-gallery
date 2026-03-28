package com.example.Notification.infrastructure.mapper;

import com.example.Notification.api.dto.request.NotificationRequest;
import com.example.Notification.api.dto.response.NotificationResponse;
import com.example.Notification.infrastructure.persistence.document.NotificationDocument;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for Notification Entity ↔ DTO conversions.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NotificationMapper {

    /**
     * Convert NotificationDocument to NotificationResponse DTO
     */
    NotificationResponse toResponse(NotificationDocument document);

    /**
     * Convert NotificationRequest DTO to NotificationDocument
     */
    NotificationDocument toDocument(NotificationRequest request);
}
