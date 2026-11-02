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
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.List;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface EventMediaMapper {

    @Mapping(target = "id", source = "document.id")
    @Mapping(target = "mediaId", source = "document.id")
    @Mapping(target = "uploaderId", source = "document.uploaderUserId")
    @Mapping(target = "mediaType", source = "document.mimeType")
    @Mapping(target = "fileName", source = "document.originalFileName")
    @Mapping(target = "fileSize", source = "document.sizeBytes")
    @Mapping(target = "storagePath", source = "document.storageKey")
    @Mapping(target = "createdAt", source = "document.uploadedAt")
    EventMediaResponse toResponse(EventMediaDocument document);

    @Mapping(target = "eventId", source = "request.eventId")
    @Mapping(target = "originalFileName", source = "request.fileName")
    @Mapping(target = "mimeType", source = "request.mediaType")
    @Mapping(target = "sizeBytes", expression = "java(request.fileSize() == null ? 0L : request.fileSize())")
    @Mapping(target = "storageKey", source = "request.storagePath")
    EventMediaDocument toDocument(EventMediaRequest request);

    default EventMediaFileResponse toFileResponse(byte[] fileBytes, MediaType contentType) {
        return new EventMediaFileResponse(new ByteArrayResource(fileBytes), contentType);
    }

    EventMediaUploadResponse toUploadResponse(String mediaId, ApprovalStatus moderationStatus, String message);

    EventMediaCollectionResponse toCollectionResponse(
            String eventId,
            List<EventMediaItemResponse> media,
            String message);

    default EventMediaDetailResponse toDetailResponse(String eventId, String mediaId, ApprovalStatus moderationStatus) {
        return toDetailResponse(mediaId, eventId, null, moderationStatus);
    }

    @Mapping(target = "mediaId", source = "mediaId")
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "details", source = "details")
    @Mapping(target = "moderationStatus", source = "moderationStatus")
    EventMediaDetailResponse toDetailResponse(
            String mediaId,
            String eventId,
            MediaServiceResponse details,
            ApprovalStatus moderationStatus);

    EventMediaModerationResponse toModerationResponse(
            ApprovalStatus status,
            String reason,
            String message);

    MessageResponse toMessageResponse(String message);

    @Mapping(target = "id", source = "approval.mediaId")
    @Mapping(target = "mediaId", source = "approval.mediaId")
    @Mapping(target = "status", source = "approval.status")
    @Mapping(target = "uploaderId", source = "approval.uploaderUserId")
    @Mapping(target = "uploaderName", ignore = true)
    @Mapping(target = "uploadedAt", source = "approval.createdAt")
    @Mapping(target = "fileName", ignore = true)
    @Mapping(target = "mimeType", ignore = true)
    @Mapping(target = "reason", source = "approval.rejectionReason")
    EventMediaItemResponse toTimelineItem(EventMediaApprovalDocument approval);

    default EventMediaItemResponse toDetailedItem(EventMediaApprovalDocument approval) {
        return toDetailedItem(approval, approval != null ? approval.getUploaderUserId() : null, null);
    }

    @Mapping(target = "id", source = "approval.mediaId")
    @Mapping(target = "mediaId", source = "approval.mediaId")
    @Mapping(target = "status", source = "approval.status")
    @Mapping(target = "uploaderId", source = "approval.uploaderUserId")
    @Mapping(target = "uploaderName", source = "uploaderName")
    @Mapping(target = "uploadedAt", source = "approval.createdAt")
    @Mapping(target = "fileName", expression = "java(mediaDetails != null ? mediaDetails.resolvedTitle() : null)")
    @Mapping(target = "mimeType", source = "mediaDetails.mimeType")
    @Mapping(target = "reason", source = "approval.rejectionReason")
    EventMediaItemResponse toDetailedItem(EventMediaApprovalDocument approval, String uploaderName, MediaServiceResponse mediaDetails);

    default String toIso(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
