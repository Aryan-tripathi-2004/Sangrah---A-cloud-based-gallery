package com.example.Event.infrastructure.mapper;

import com.example.Event.api.dto.response.AccessApprovalResponse;
import com.example.Event.api.dto.response.AccessRejectionResponse;
import com.example.Event.api.dto.response.AccessRequestMutationResponse;
import com.example.Event.api.dto.response.AccessRequestResponse;
import com.example.Event.api.dto.response.AccessRequestsResponse;
import com.example.Event.api.dto.response.AccessRevocationResponse;
import com.example.Event.api.dto.response.AccessStatusResponse;
import com.example.Event.infrastructure.client.UserServiceClient;
import com.example.Event.infrastructure.persistence.document.EventAccessRequestDocument;
import com.example.Event.shared.enums.AccessStatus;
import com.example.Event.shared.enums.ApprovalDuration;
import com.example.Event.shared.enums.ApprovalStatus;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class EventAccessMapper {

    @Autowired
    private UserServiceClient userServiceClient;

    public AccessRequestMutationResponse toMutationResponse(EventAccessRequestDocument document, String message) {
        if (document == null) {
            return null;
        }
        return new AccessRequestMutationResponse(document.getId(), document.getStatus(), message);
    }

    public AccessRequestsResponse toRequestsResponse(String eventId, List<AccessRequestResponse> requests) {
        return new AccessRequestsResponse(eventId, requests);
    }

    public AccessApprovalResponse toApprovalResponse(
            EventAccessRequestDocument document,
            ApprovalDuration approvalDuration,
            Instant accessExpiresAt,
            String message) {
        if (document == null) {
            return null;
        }
        return new AccessApprovalResponse(
                document.getStatus(),
                message,
                approvalDuration,
                accessExpiresAt != null ? accessExpiresAt.toString() : "Never");
    }

    public AccessRejectionResponse toRejectionResponse(EventAccessRequestDocument document, String message, String reason) {
        if (document == null) {
            return null;
        }
        return new AccessRejectionResponse(document.getStatus(), message, reason);
    }

    public AccessRevocationResponse toRevocationResponse(ApprovalStatus status, String message) {
        return new AccessRevocationResponse(status, message);
    }

    public AccessRequestResponse toAccessRequestResponse(EventAccessRequestDocument document) {
        if (document == null) {
            return null;
        }
        String displayName;
        try {
            displayName = userServiceClient.getUserDisplayName(document.getRequesterUserId());
        } catch (Exception e) {
            displayName = document.getRequesterUserId();
        }
        return new AccessRequestResponse(
                document.getId(),
                document.getRequesterUserId(),
                displayName,
                document.getMessage() != null ? document.getMessage() : "",
                document.getStatus(),
                toIso(document.getRequestedAt()));
    }

    public AccessStatusResponse toSimpleStatus(AccessStatus status, boolean hasAccess, boolean showRequestButton) {
        return AccessStatusResponse.simple(status, hasAccess, showRequestButton);
    }

    public AccessStatusResponse toPendingStatus(EventAccessRequestDocument document) {
        return new AccessStatusResponse(
                AccessStatus.PENDING,
                false,
                false,
                toIso(document.getRequestedAt()),
                null,
                null,
                null);
    }

    public AccessStatusResponse toRejectedStatus(EventAccessRequestDocument document) {
        return new AccessStatusResponse(
                AccessStatus.REJECTED,
                false,
                true,
                null,
                null,
                document.getRejectionReason(),
                null);
    }

    public AccessStatusResponse toRevokedStatus(EventAccessRequestDocument document) {
        return new AccessStatusResponse(
                AccessStatus.REVOKED,
                false,
                true,
                null,
                null,
                null,
                toIso(document.getRevokedAt()));
    }

    public AccessStatusResponse toExpiredStatus(EventAccessRequestDocument document) {
        return new AccessStatusResponse(
                AccessStatus.EXPIRED,
                false,
                true,
                null,
                document.getAccessExpiresAt().toString(),
                null,
                null);
    }

    public AccessStatusResponse toApprovedStatus(EventAccessRequestDocument document) {
        return new AccessStatusResponse(
                AccessStatus.APPROVED,
                true,
                false,
                null,
                document.getAccessExpiresAt() != null ? document.getAccessExpiresAt().toString() : ApprovalDuration.FOREVER.name(),
                null,
                null);
    }

    public String toIso(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
