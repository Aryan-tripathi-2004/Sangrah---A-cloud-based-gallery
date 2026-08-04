package com.example.Event.infrastructure.mapper;

import com.example.Event.api.dto.request.EventMediaRequest;
import com.example.Event.api.dto.response.EventMediaResponse;
import com.example.Event.infrastructure.persistence.document.EventMediaDocument;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.time.Instant;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EventMediaMapper {

    default EventMediaResponse toResponse(EventMediaDocument document) {
        if (document == null) {
            return null;
        }
        return new EventMediaResponse(
                document.getId(),
                document.getId(),
                document.getEventId(),
                document.getUploaderUserId(),
                document.getMimeType(),
                document.getOriginalFileName(),
                document.getSizeBytes(),
                document.getStorageKey(),
                toIso(document.getUploadedAt()),
                document.getStatus());
    }

    default EventMediaDocument toDocument(EventMediaRequest request) {
        if (request == null) {
            return null;
        }
        return EventMediaDocument.builder()
                .eventId(request.eventId())
                .originalFileName(request.fileName())
                .mimeType(request.mediaType())
                .sizeBytes(request.fileSize() == null ? 0L : request.fileSize())
                .storageKey(request.storagePath())
                .build();
    }

    default String toIso(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
