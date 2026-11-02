package com.example.Event.infrastructure.mapper;

import com.example.Event.api.dto.response.AccessApprovalResponse;
import com.example.Event.api.dto.response.AccessRejectionResponse;
import com.example.Event.api.dto.response.AccessRequestMutationResponse;
import com.example.Event.api.dto.response.AccessRequestResponse;
import com.example.Event.api.dto.response.AccessRequestsResponse;
import com.example.Event.api.dto.response.AccessRevocationResponse;
import com.example.Event.api.dto.response.AccessStatusResponse;
import com.example.Event.infrastructure.persistence.document.EventAccessRequestDocument;
import com.example.Event.shared.enums.AccessStatus;
import com.example.Event.shared.enums.ApprovalDuration;
import com.example.Event.shared.enums.ApprovalStatus;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.time.Instant;
import java.util.List;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    imports = {AccessStatus.class, ApprovalDuration.class}
)
public interface EventAccessMapper {

    @Mapping(target = "requestId", source = "document.id")
    @Mapping(target = "status", source = "document.status")
    @Mapping(target = "message", source = "message")
    AccessRequestMutationResponse toMutationResponse(EventAccessRequestDocument document, String message);

    AccessRequestsResponse toRequestsResponse(String eventId, List<AccessRequestResponse> requests);

    @Mapping(target = "status", source = "document.status")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "approvalDuration", source = "approvalDuration")
    @Mapping(target = "accessExpiresAt", expression = "java(accessExpiresAt != null ? accessExpiresAt.toString() : \"Never\")")
    AccessApprovalResponse toApprovalResponse(
            EventAccessRequestDocument document,
            ApprovalDuration approvalDuration,
            Instant accessExpiresAt,
            String message);

    @Mapping(target = "status", source = "document.status")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "reason", source = "reason")
    AccessRejectionResponse toRejectionResponse(EventAccessRequestDocument document, String message, String reason);

    AccessRevocationResponse toRevocationResponse(ApprovalStatus status, String message);

    @Mapping(target = "requestId", source = "document.id")
    @Mapping(target = "displayName", source = "resolvedDisplayName")
    @Mapping(target = "message", expression = "java(document.getMessage() != null ? document.getMessage() : \"\")")
    @Mapping(target = "requestedAt", source = "document.requestedAt")
    AccessRequestResponse toAccessRequestResponse(EventAccessRequestDocument document, String resolvedDisplayName);

    default AccessRequestResponse toAccessRequestResponse(EventAccessRequestDocument document) {
        return toAccessRequestResponse(document, document != null ? document.getRequesterUserId() : null);
    }

    default AccessStatusResponse toSimpleStatus(AccessStatus status, boolean hasAccess, boolean showRequestButton) {
        return AccessStatusResponse.simple(status, hasAccess, showRequestButton);
    }

    @Mapping(target = "status", expression = "java(AccessStatus.PENDING)")
    @Mapping(target = "hasAccess", constant = "false")
    @Mapping(target = "showRequestButton", constant = "false")
    @Mapping(target = "createdAt", source = "requestedAt")
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "revokedAt", ignore = true)
    AccessStatusResponse toPendingStatus(EventAccessRequestDocument document);

    @Mapping(target = "status", expression = "java(AccessStatus.REJECTED)")
    @Mapping(target = "hasAccess", constant = "false")
    @Mapping(target = "showRequestButton", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "rejectionReason", source = "rejectionReason")
    @Mapping(target = "revokedAt", ignore = true)
    AccessStatusResponse toRejectedStatus(EventAccessRequestDocument document);

    @Mapping(target = "status", expression = "java(AccessStatus.REVOKED)")
    @Mapping(target = "hasAccess", constant = "false")
    @Mapping(target = "showRequestButton", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "revokedAt", source = "revokedAt")
    AccessStatusResponse toRevokedStatus(EventAccessRequestDocument document);

    @Mapping(target = "status", expression = "java(AccessStatus.EXPIRED)")
    @Mapping(target = "hasAccess", constant = "false")
    @Mapping(target = "showRequestButton", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "expiresAt", source = "accessExpiresAt")
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "revokedAt", ignore = true)
    AccessStatusResponse toExpiredStatus(EventAccessRequestDocument document);

    @Mapping(target = "status", expression = "java(AccessStatus.APPROVED)")
    @Mapping(target = "hasAccess", constant = "true")
    @Mapping(target = "showRequestButton", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "expiresAt", expression = "java(document.getAccessExpiresAt() != null ? document.getAccessExpiresAt().toString() : ApprovalDuration.FOREVER.name())")
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "revokedAt", ignore = true)
    AccessStatusResponse toApprovedStatus(EventAccessRequestDocument document);

    default String toIso(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
