package com.example.Event.infrastructure.mapper;

import com.example.Event.api.dto.request.EventRequest;
import com.example.Event.api.dto.response.CollaboratorResponse;
import com.example.Event.api.dto.response.EventResponse;
import com.example.Event.api.dto.response.EventSummaryResponse;
import com.example.Event.infrastructure.persistence.document.EventDocument;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.time.Instant;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EventMapper {

    default EventResponse toResponse(EventDocument document) {
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
                null,
                null,
                null);
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
