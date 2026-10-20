package com.example.Notification.infrastructure.mapper;

import com.example.Notification.api.dto.request.InternalNotificationRequest;
import com.example.Notification.api.dto.request.NotificationPayloadDTO;
import com.example.Notification.api.dto.request.NotificationRequest;
import com.example.Notification.api.dto.response.NotificationResponse;
import com.example.Notification.infrastructure.persistence.document.NotificationDocument;
import com.example.Notification.infrastructure.persistence.document.NotificationPayload;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * MapStruct mapper for Notification Entity ↔ DTO conversions.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = {Instant.class}
)
public interface NotificationMapper {

    /**
     * Convert NotificationDocument to NotificationResponse DTO
     */
    NotificationResponse toResponse(NotificationDocument document);

    /**
     * Convert NotificationRequest DTO to NotificationDocument
     */
    NotificationDocument toDocument(NotificationRequest request);

    /**
     * Convert InternalNotificationRequest DTO to NotificationDocument
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "read", constant = "false")
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    NotificationDocument toDocument(InternalNotificationRequest request);

    /**
     * Convert NotificationPayloadDTO to NotificationPayload entity
     */
    NotificationPayload toPayload(NotificationPayloadDTO dto);

    default LocalDateTime map(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
