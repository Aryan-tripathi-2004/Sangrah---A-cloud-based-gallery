package com.example.Event.infrastructure.mapper;

import com.example.Event.api.dto.request.EventRequest;
import com.example.Event.api.dto.response.CollaboratorResponse;
import com.example.Event.api.dto.response.EventCreateResponse;
import com.example.Event.api.dto.response.EventDeleteResponse;
import com.example.Event.api.dto.response.EventResponse;
import com.example.Event.api.dto.response.EventSettingsResponse;
import com.example.Event.api.dto.response.EventSummaryResponse;
import com.example.Event.api.dto.response.EventUpdateResponse;
import com.example.Event.infrastructure.persistence.document.EventDocument;
import com.example.Event.shared.enums.AccessStatus;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.time.Instant;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EventMapper {

    default EventResponse toResponse(EventDocument document) {
        return toResponse(document, null, null, null);
    }

    default EventResponse toResponse(
            EventDocument document,
            AccessStatus accessStatus,
            Boolean requiresApproval,
            String message) {
        if (document == null) {
            return null;
        }
        return new EventResponse(
                document.getId(),
                document.getId(),
                document.getOwnerUserId(),
                document.getTitle(),
                document.getDescription(),
                document.getCoverImageId(),
                toIso(document.getEventDate()),
                document.getVisibility(),
                document.isModerationEnabled(),
                toCollaboratorResponses(document.getCollaborators()),
                document.getStatus(),
                toIso(document.getCreatedAt()),
                accessStatus,
                requiresApproval,
                message);
    }

    default EventCreateResponse toCreateResponse(EventDocument document, String message) {
        if (document == null) {
            return null;
        }
        return new EventCreateResponse(
                document.getId(),
                document.getId(),
                document.getTitle(),
                message);
    }

    default EventSummaryResponse toPublicSummary(EventDocument document) {
        if (document == null) {
            return null;
        }
        return new EventSummaryResponse(
                document.getId(),
                document.getId(),
                document.getOwnerUserId(),
                document.getTitle(),
                document.getDescription(),
                document.getCoverImageId(),
                toIso(document.getEventDate()),
                document.getVisibility(),
                null,
                null,
                toIso(document.getCreatedAt()));
    }

    default EventSummaryResponse toOwnerSummary(EventDocument document) {
        if (document == null) {
            return null;
        }
        return new EventSummaryResponse(
                document.getId(),
                document.getId(),
                document.getOwnerUserId(),
                document.getTitle(),
                document.getDescription(),
                document.getCoverImageId(),
                toIso(document.getEventDate()),
                document.getVisibility(),
                document.isModerationEnabled(),
                toCollaboratorResponses(document.getCollaborators()),
                toIso(document.getCreatedAt()));
    }

    default EventUpdateResponse toUpdateResponse(EventDocument document, String status, String message) {
        if (document == null) {
            return null;
        }
        return new EventUpdateResponse(
                status,
                message,
                document.getId(),
                document.getId(),
                document.getTitle(),
                document.getDescription(),
                toIso(document.getEventDate()),
                document.getVisibility(),
                document.isModerationEnabled(),
                document.getCoverImageId(),
                toCollaboratorResponses(document.getCollaborators()));
    }

    default EventDeleteResponse toDeleteResponse(String eventId, String message) {
        return new EventDeleteResponse(eventId, message);
    }

    default EventSettingsResponse toSettingsResponse(EventDocument document) {
        if (document == null) {
            return null;
        }
        return new EventSettingsResponse(document.isModerationEnabled());
    }

    default EventDocument toDocument(EventRequest request) {
        if (request == null) {
            return null;
        }
        return EventDocument.builder()
                .title(request.title())
                .description(request.description())
                .visibility(request.resolvedVisibility())
                .moderationEnabled(request.moderationEnabledOrDefault())
                .coverImageId(request.coverImageId())
                .build();
    }

    default CollaboratorResponse toCollaboratorResponse(EventDocument.EventCollaborator collaborator) {
        if (collaborator == null) {
            return null;
        }
        return new CollaboratorResponse(
                collaborator.getUserId(),
                collaborator.getCanUploadMedia(),
                collaborator.getCanReviewMedia(),
                collaborator.getCanReviewAccessRequests(),
                collaborator.getCanDirectUpload(),
                collaborator.getCanDeleteMedia(),
                collaborator.getCanEditEventDetails(),
                collaborator.getAddedAt(),
                collaborator.getAddedByUserId(),
                null,
                null);
    }

    default List<CollaboratorResponse> toCollaboratorResponses(List<EventDocument.EventCollaborator> collaborators) {
        if (collaborators == null) {
            return null;
        }
        return collaborators.stream()
                .map(this::toCollaboratorResponse)
                .toList();
    }

    default String toIso(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
