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
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.util.List;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface EventMapper {

    @Mapping(target = "eventId", source = "document.id")
    @Mapping(target = "id", source = "document.id")
    @Mapping(target = "eventDate", source = "document.eventDate")
    @Mapping(target = "createdAt", source = "document.createdAt")
    @Mapping(target = "status", source = "document.status")
    @Mapping(target = "accessStatus", source = "accessStatus")
    @Mapping(target = "requiresApproval", source = "requiresApproval")
    @Mapping(target = "message", source = "message")
    EventResponse toResponse(
            EventDocument document,
            AccessStatus accessStatus,
            Boolean requiresApproval,
            String message);

    default EventResponse toResponse(EventDocument document) {
        return toResponse(document, null, null, null);
    }

    @Mapping(target = "eventId", source = "document.id")
    @Mapping(target = "id", source = "document.id")
    @Mapping(target = "title", source = "document.title")
    @Mapping(target = "message", source = "message")
    EventCreateResponse toCreateResponse(EventDocument document, String message);

    @Mapping(target = "eventId", source = "document.id")
    @Mapping(target = "id", source = "document.id")
    @Mapping(target = "eventDate", source = "document.eventDate")
    @Mapping(target = "createdAt", source = "document.createdAt")
    @Mapping(target = "moderationEnabled", expression = "java(isOwner ? document.isModerationEnabled() : null)")
    @Mapping(target = "collaborators", expression = "java(isOwner ? toCollaboratorResponses(document.getCollaborators()) : null)")
    @Mapping(target = "accessStatus", source = "accessStatus")
    @Mapping(target = "pendingRequestsCount", source = "pendingRequestsCount")
    EventSummaryResponse toSummaryWithAccess(
            EventDocument document,
            String accessStatus,
            boolean isOwner,
            Integer pendingRequestsCount);

    default EventSummaryResponse toPublicSummary(EventDocument document) {
        return toSummaryWithAccess(document, null, false, null);
    }

    default EventSummaryResponse toOwnerSummary(EventDocument document) {
        return toSummaryWithAccess(document, "OWNER", true, null);
    }

    default EventSummaryResponse toOwnerSummaryWithPendingCount(EventDocument document, Integer pendingRequestsCount) {
        return toSummaryWithAccess(document, "OWNER", true, pendingRequestsCount);
    }

    default EventSummaryResponse toSummaryWithAccess(EventDocument document, String accessStatus, boolean isOwner) {
        return toSummaryWithAccess(document, accessStatus, isOwner, null);
    }

    @Mapping(target = "eventId", source = "document.id")
    @Mapping(target = "id", source = "document.id")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "message", source = "message")
    EventUpdateResponse toUpdateResponse(EventDocument document, String status, String message);

    EventDeleteResponse toDeleteResponse(String eventId, String message);

    EventSettingsResponse toSettingsResponse(EventDocument document);

    @Mapping(target = "title", expression = "java(request.title() != null ? request.title().trim() : null)")
    @Mapping(target = "description", expression = "java(request.description() != null ? request.description().trim() : null)")
    @Mapping(target = "visibility", expression = "java(request.resolvedVisibility())")
    @Mapping(target = "moderationEnabled", expression = "java(request.moderationEnabledOrDefault())")
    EventDocument toDocument(EventRequest request);

    CollaboratorResponse toCollaboratorResponse(EventDocument.EventCollaborator collaborator);

    List<CollaboratorResponse> toCollaboratorResponses(List<EventDocument.EventCollaborator> collaborators);

    default String toIso(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
