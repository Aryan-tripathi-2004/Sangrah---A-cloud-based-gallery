package com.example.Event.infrastructure.mapper;

import com.example.Event.api.dto.request.EventMediaRequest;
import com.example.Event.api.dto.response.EventMediaCollectionResponse;
import com.example.Event.api.dto.response.EventMediaDetailResponse;
import com.example.Event.api.dto.response.EventMediaFileResponse;
import com.example.Event.api.dto.response.EventMediaItemResponse;
import com.example.Event.api.dto.response.EventMediaModerationResponse;
import com.example.Event.api.dto.response.EventMediaResponse;
import com.example.Event.api.dto.response.EventMediaUploadResponse;
import com.example.Event.api.dto.response.MessageResponse;
import com.example.Event.infrastructure.client.dto.MediaServiceResponse;
import com.example.Event.infrastructure.persistence.document.EventMediaApprovalDocument;
import com.example.Event.infrastructure.persistence.document.EventMediaDocument;
import com.example.Event.shared.enums.ApprovalStatus;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public abstract class EventMediaMapper {

    public EventMediaResponse toResponse(EventMediaDocument document) {
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

    public EventMediaDocument toDocument(EventMediaRequest request) {
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

    public EventMediaFileResponse toFileResponse(byte[] fileBytes, MediaType contentType) {
        return new EventMediaFileResponse(new ByteArrayResource(fileBytes), contentType);
    }

    public EventMediaUploadResponse toUploadResponse(String mediaId, ApprovalStatus moderationStatus, String message) {
        return new EventMediaUploadResponse(mediaId, moderationStatus, message);
    }

    public EventMediaCollectionResponse toCollectionResponse(
            String eventId,
            List<EventMediaItemResponse> media,
            String message) {
        return new EventMediaCollectionResponse(eventId, media, message);
    }

    public EventMediaDetailResponse toDetailResponse(String eventId, String mediaId, ApprovalStatus moderationStatus) {
        return toDetailResponse(mediaId, eventId, null, moderationStatus);
    }

    public EventMediaDetailResponse toDetailResponse(String mediaId, String eventId, MediaServiceResponse details, ApprovalStatus moderationStatus) {
        return new EventMediaDetailResponse(mediaId, eventId, details, moderationStatus);
    }

    public EventMediaModerationResponse toModerationResponse(
            ApprovalStatus status,
            String reason,
            String message) {
        return new EventMediaModerationResponse(status, reason, message);
    }

    public MessageResponse toMessageResponse(String message) {
        return new MessageResponse(message);
    }

    public EventMediaItemResponse toTimelineItem(EventMediaApprovalDocument approval) {
        if (approval == null) {
            return null;
        }
        return new EventMediaItemResponse(
                approval.getMediaId(),
                approval.getMediaId(),
                approval.getStatus(),
                approval.getUploaderUserId(),
                null,
                toIso(approval.getCreatedAt()),
                null,
                null,
                approval.getRejectionReason());
    }

    public EventMediaItemResponse toDetailedItem(EventMediaApprovalDocument approval) {
        if (approval == null) {
            return null;
        }
        return toDetailedItem(approval, approval.getUploaderUserId(), null);
    }

    public EventMediaItemResponse toDetailedItem(EventMediaApprovalDocument approval, String uploaderName, MediaServiceResponse mediaDetails) {
        if (approval == null) {
            return null;
        }
        String resolvedFileName = mediaDetails != null ? mediaDetails.resolvedTitle() : null;
        String mimeType = mediaDetails != null ? mediaDetails.mimeType() : null;
        return new EventMediaItemResponse(
                approval.getMediaId(),
                approval.getMediaId(),
                approval.getStatus(),
                approval.getUploaderUserId(),
                uploaderName,
                toIso(approval.getCreatedAt()),
                resolvedFileName,
                mimeType,
                approval.getRejectionReason());
    }

    public String toIso(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
