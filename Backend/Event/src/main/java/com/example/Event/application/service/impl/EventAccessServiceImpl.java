package com.example.Event.application.service.impl;

import com.example.Event.api.dto.request.AccessApprovalRequest;
import com.example.Event.api.dto.request.AccessMessageRequest;
import com.example.Event.api.dto.request.AccessRejectionRequest;
import com.example.Event.api.dto.response.AccessApprovalResponse;
import com.example.Event.api.dto.response.AccessRejectionResponse;
import com.example.Event.api.dto.response.AccessRequestMutationResponse;
import com.example.Event.api.dto.response.AccessRequestResponse;
import com.example.Event.api.dto.response.AccessRequestsResponse;
import com.example.Event.api.dto.response.AccessRevocationResponse;
import com.example.Event.api.dto.response.AccessStatusResponse;
import com.example.Event.application.service.interfaces.IEventAccessService;
import com.example.Event.application.service.interfaces.IEventCollaboratorService;
import com.example.Event.application.service.interfaces.IEventService;
import com.example.Event.infrastructure.client.EmailServiceClient;
import com.example.Event.infrastructure.client.NotificationServiceClient;
import com.example.Event.infrastructure.client.UserServiceClient;
import com.example.Event.infrastructure.persistence.document.EventAccessRequestDocument;
import com.example.Event.infrastructure.persistence.document.EventDocument;
import com.example.Event.infrastructure.persistence.repository.EventAccessRequestRepository;
import com.example.Event.shared.exception.AuthenticationRequiredException;
import com.example.Event.shared.exception.DomainValidationException;
import com.example.Event.shared.exception.ForbiddenOperationException;
import com.example.Event.shared.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventAccessServiceImpl implements IEventAccessService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_REVOKED = "REVOKED";

    private final EventAccessRequestRepository accessRequestRepository;
    private final NotificationServiceClient notificationServiceClient;
    private final EmailServiceClient emailServiceClient;
    private final IEventService eventService;
    private final IEventCollaboratorService collaboratorService;
    private final UserServiceClient userServiceClient;

    @Override
    public AccessRequestMutationResponse requestAccess(
            String eventId,
            AccessMessageRequest request,
            HttpServletRequest httpRequest) {
        String userId = requireUserId(httpRequest, "User not authenticated");
        EventDocument event = eventService.getEventById(eventId);

        if (userId.equals(event.getOwnerUserId())) {
            throw new ForbiddenOperationException("You are the event owner. No need to request access.");
        }

        if (!"PROTECTED".equals(event.getVisibility())) {
            throw new DomainValidationException("Access requests only allowed for PROTECTED events.");
        }

        Optional<EventAccessRequestDocument> existingPendingRequest =
                accessRequestRepository.findByEventIdAndRequesterUserIdAndStatus(eventId, userId, STATUS_PENDING);
        if (existingPendingRequest.isPresent()) {
            throw new DomainValidationException("You have already requested access to this event. Waiting for approval.");
        }

        EventAccessRequestDocument accessRequest = createAccessRequestDocument(
                event,
                userId,
                request != null ? request.normalizedMessage() : null);

        return new AccessRequestMutationResponse(
                accessRequest.getId(),
                STATUS_PENDING,
                "Access request has been sent to the event owner");
    }

    @Override
    public AccessRequestsResponse listAccessRequests(String eventId, HttpServletRequest httpRequest) {
        String userId = requireUserId(httpRequest, "User not authenticated");
        EventDocument event = eventService.getEventById(eventId);
        requireAccessRequestReviewer(event, userId, "Only event owner and authorized collaborators can view access requests");

        List<AccessRequestResponse> requests = accessRequestRepository.findByEventIdAndStatusInOrderByRequestedAtDesc(
                        eventId,
                        List.of(STATUS_PENDING, STATUS_APPROVED))
                .stream()
                .map(this::toAccessRequestResponse)
                .toList();

        return new AccessRequestsResponse(eventId, requests);
    }

    @Override
    public AccessApprovalResponse approveRequest(
            String eventId,
            String requestId,
            AccessApprovalRequest request,
            HttpServletRequest httpRequest) {
        String userId = requireUserId(httpRequest, "User not authenticated");
        EventDocument event = eventService.getEventById(eventId);
        requireAccessRequestReviewer(event, userId, "You don't have permission to approve access requests");

        String approvalDuration = request != null ? request.resolvedApprovalDuration() : "FOREVER";
        Instant accessExpiresAt = parseAccessExpiresAt(request);

        EventAccessRequestDocument approved = approveRequestDocument(
                eventId,
                requestId,
                userId,
                approvalDuration,
                accessExpiresAt);

        return new AccessApprovalResponse(
                approved.getStatus(),
                "Access request approved",
                approvalDuration,
                accessExpiresAt != null ? accessExpiresAt.toString() : "Never");
    }

    @Override
    public AccessRejectionResponse rejectRequest(
            String eventId,
            String requestId,
            AccessRejectionRequest request,
            HttpServletRequest httpRequest) {
        String userId = requireUserId(httpRequest, "User not authenticated");
        EventDocument event = eventService.getEventById(eventId);
        requireAccessRequestReviewer(event, userId, "You don't have permission to reject access requests");

        String reason = request != null ? request.resolvedReason() : "Request denied";
        EventAccessRequestDocument rejected = rejectRequestDocument(eventId, requestId, userId, reason);

        return new AccessRejectionResponse(rejected.getStatus(), "Access request rejected", reason);
    }

    @Override
    public AccessRevocationResponse revokeRequest(String eventId, String requestId, HttpServletRequest httpRequest) {
        String userId = requireUserId(httpRequest, "User not authenticated");
        EventDocument event = eventService.getEventById(eventId);
        requireAccessRequestReviewer(event, userId, "You don't have permission to revoke access");

        EventAccessRequestDocument accessRequest = findAccessRequest(requestId);
        if (!eventId.equals(accessRequest.getEventId())) {
            throw new DomainValidationException("Request does not belong to this event");
        }
        if (!STATUS_APPROVED.equals(accessRequest.getStatus())) {
            throw new DomainValidationException("Only approved requests can be revoked");
        }

        revokeAccess(event, accessRequest.getRequesterUserId(), userId);
        return new AccessRevocationResponse(STATUS_REVOKED, "Access revoked successfully");
    }

    @Override
    public AccessRequestMutationResponse reRequestAccess(
            String eventId,
            AccessMessageRequest request,
            HttpServletRequest httpRequest) {
        String userId = requireUserId(httpRequest, "User not authenticated");
        EventDocument event = eventService.getEventById(eventId);

        if (!"PROTECTED".equals(event.getVisibility())) {
            throw new DomainValidationException("Re-requests only allowed for PROTECTED events");
        }

        EventAccessRequestDocument existing = accessRequestRepository.findByEventIdAndRequesterUserId(eventId, userId)
                .orElseThrow(() -> new DomainValidationException("No prior access request found. Please make an initial request."));

        if (STATUS_PENDING.equals(existing.getStatus())) {
            throw new DomainValidationException("Your request is already pending approval");
        }
        if (STATUS_APPROVED.equals(existing.getStatus()) && isApprovedAndActive(existing)) {
            throw new DomainValidationException("Your access is still active");
        }

        EventAccessRequestDocument reRequest = createReRequestDocument(
                event,
                existing,
                userId,
                request != null ? request.normalizedMessage() : null);

        return new AccessRequestMutationResponse(
                reRequest.getId(),
                STATUS_PENDING,
                "Your access request has been re-submitted and is awaiting owner approval");
    }

    @Override
    public AccessStatusResponse getAccessStatus(String eventId, HttpServletRequest httpRequest) {
        String userId = requireUserId(httpRequest, "User not authenticated");
        EventDocument event = eventService.getEventById(eventId);

        if (userId.equals(event.getOwnerUserId())) {
            return AccessStatusResponse.simple("OWNER", true, false);
        }

        if (collaboratorService.isCollaborator(eventId, userId)) {
            return AccessStatusResponse.simple("COLLABORATOR", true, false);
        }

        Optional<EventAccessRequestDocument> request = accessRequestRepository.findByEventIdAndRequesterUserId(eventId, userId);
        if (request.isEmpty()) {
            return AccessStatusResponse.simple("NONE", false, "PROTECTED".equals(event.getVisibility()));
        }

        EventAccessRequestDocument accessRequest = request.get();
        return switch (accessRequest.getStatus()) {
            case STATUS_PENDING -> new AccessStatusResponse(
                    STATUS_PENDING,
                    false,
                    false,
                    toIso(accessRequest.getRequestedAt()),
                    null,
                    null,
                    null);
            case STATUS_APPROVED -> approvedStatus(accessRequest);
            case STATUS_REJECTED -> new AccessStatusResponse(
                    STATUS_REJECTED,
                    false,
                    true,
                    null,
                    null,
                    accessRequest.getRejectionReason(),
                    null);
            case STATUS_REVOKED -> new AccessStatusResponse(
                    STATUS_REVOKED,
                    false,
                    true,
                    null,
                    null,
                    null,
                    toIso(accessRequest.getRevokedAt()));
            default -> AccessStatusResponse.simple("UNKNOWN", false, false);
        };
    }

    @Override
    public boolean isUserApproved(String eventId, String requesterUserId) {
        try {
            return accessRequestRepository.findByEventIdAndRequesterUserId(eventId, requesterUserId)
                    .filter(this::isApprovedAndActive)
                    .isPresent();
        } catch (Exception e) {
            log.error("Failed to check approval status: {}", e.getMessage());
            return false;
        }
    }

    private EventAccessRequestDocument createAccessRequestDocument(EventDocument event, String requesterUserId, String message) {
        Optional<EventAccessRequestDocument> existing =
                accessRequestRepository.findByEventIdAndRequesterUserId(event.getId(), requesterUserId);

        if (existing.isPresent()) {
            if (STATUS_PENDING.equals(existing.get().getStatus())) {
                throw new DomainValidationException("You have already requested access to this event");
            }
            if (STATUS_APPROVED.equals(existing.get().getStatus())) {
                throw new DomainValidationException("You already have access to this event");
            }
        }

        EventAccessRequestDocument request = EventAccessRequestDocument.builder()
                .eventId(event.getId())
                .requesterUserId(requesterUserId)
                .message(message)
                .status(STATUS_PENDING)
                .requestedAt(Instant.now())
                .build();

        EventAccessRequestDocument saved = accessRequestRepository.save(request);
        sendAccessRequestNotifications(event, requesterUserId);
        return saved;
    }

    private EventAccessRequestDocument approveRequestDocument(
            String eventId,
            String requestId,
            String approverUserId,
            String approvalDuration,
            Instant accessExpiresAt) {
        EventAccessRequestDocument request = findAccessRequest(requestId);
        if (!eventId.equals(request.getEventId())) {
            throw new DomainValidationException("Request does not belong to this event");
        }

        request.setStatus(STATUS_APPROVED);
        request.setApprovalDuration(approvalDuration);
        request.setAccessExpiresAt("UNTIL_DATE".equals(approvalDuration) ? accessExpiresAt : null);
        request.setDecisionAt(Instant.now());
        request.setDecidedByUserId(approverUserId);
        request.setUpdatedAt(Instant.now());

        EventAccessRequestDocument updated = accessRequestRepository.save(request);
        EventDocument event = eventService.getEventById(eventId);
        sendAccessApprovedNotifications(event, request.getRequesterUserId());
        return updated;
    }

    private EventAccessRequestDocument rejectRequestDocument(
            String eventId,
            String requestId,
            String approverUserId,
            String rejectionReason) {
        EventAccessRequestDocument request = findAccessRequest(requestId);
        if (!eventId.equals(request.getEventId())) {
            throw new DomainValidationException("Request does not belong to this event");
        }

        request.setStatus(STATUS_REJECTED);
        request.setRejectionReason(rejectionReason);
        request.setDecisionAt(Instant.now());
        request.setDecidedByUserId(approverUserId);
        request.setUpdatedAt(Instant.now());

        EventAccessRequestDocument updated = accessRequestRepository.save(request);
        EventDocument event = eventService.getEventById(eventId);
        sendAccessRejectedNotifications(event, request.getRequesterUserId(), rejectionReason);
        return updated;
    }

    private void revokeAccess(EventDocument event, String requesterUserId, String revokerUserId) {
        EventAccessRequestDocument request = accessRequestRepository.findByEventIdAndRequesterUserId(event.getId(), requesterUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found for user: " + requesterUserId));

        Instant now = Instant.now();
        request.setStatus(STATUS_REVOKED);
        request.setAccessExpiresAt(now);
        request.setRevokedAt(now);
        request.setRevokedByUserId(revokerUserId);
        request.setUpdatedAt(now);
        accessRequestRepository.save(request);

        sendAccessRevokedNotifications(event, requesterUserId);
    }

    private EventAccessRequestDocument createReRequestDocument(
            EventDocument event,
            EventAccessRequestDocument request,
            String requesterUserId,
            String message) {
        request.setStatus(STATUS_PENDING);
        request.setRequestedAt(Instant.now());
        request.setUpdatedAt(Instant.now());
        request.setMessage(message);
        request.setRejectionReason(null);
        request.setDecisionAt(null);
        request.setDecidedByUserId(null);
        request.setRevokedAt(null);
        request.setRevokedByUserId(null);

        EventAccessRequestDocument saved = accessRequestRepository.save(request);
        sendAccessRequestNotifications(event, requesterUserId);
        return saved;
    }

    private AccessRequestResponse toAccessRequestResponse(EventAccessRequestDocument request) {
        String displayName;
        try {
            displayName = userServiceClient.getUserDisplayName(request.getRequesterUserId());
        } catch (Exception e) {
            displayName = request.getRequesterUserId();
        }

        return new AccessRequestResponse(
                request.getId(),
                request.getRequesterUserId(),
                displayName,
                request.getMessage() != null ? request.getMessage() : "",
                request.getStatus(),
                toIso(request.getRequestedAt()));
    }

    private AccessStatusResponse approvedStatus(EventAccessRequestDocument accessRequest) {
        boolean expired = accessRequest.getAccessExpiresAt() != null
                && Instant.now().isAfter(accessRequest.getAccessExpiresAt());
        if (expired) {
            return new AccessStatusResponse(
                    "EXPIRED",
                    false,
                    true,
                    null,
                    accessRequest.getAccessExpiresAt().toString(),
                    null,
                    null);
        }
        return new AccessStatusResponse(
                STATUS_APPROVED,
                true,
                false,
                null,
                accessRequest.getAccessExpiresAt() != null ? accessRequest.getAccessExpiresAt().toString() : "FOREVER",
                null,
                null);
    }

    private boolean isApprovedAndActive(EventAccessRequestDocument request) {
        if (!STATUS_APPROVED.equals(request.getStatus())) {
            return false;
        }
        return request.getAccessExpiresAt() == null || Instant.now().isBefore(request.getAccessExpiresAt());
    }

    private void requireAccessRequestReviewer(EventDocument event, String userId, String message) {
        if (userId.equals(event.getOwnerUserId())) {
            return;
        }
        if (!collaboratorService.hasPermission(event.getId(), userId, "canReviewAccessRequests")) {
            throw new ForbiddenOperationException(message);
        }
    }

    private Instant parseAccessExpiresAt(AccessApprovalRequest request) {
        if (request == null || !"UNTIL_DATE".equals(request.resolvedApprovalDuration())
                || request.accessExpiresAt() == null || request.accessExpiresAt().isBlank()) {
            return null;
        }

        try {
            return Instant.parse(request.accessExpiresAt().trim());
        } catch (DateTimeParseException e) {
            throw new DomainValidationException("Invalid access expiration date", e);
        }
    }

    private EventAccessRequestDocument findAccessRequest(String requestId) {
        return accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));
    }

    private void sendAccessRequestNotifications(EventDocument event, String requesterUserId) {
        try {
            String requesterEmail = userServiceClient.getUserEmail(requesterUserId);
            notificationServiceClient.notifyAccessRequestReceived(
                    event.getOwnerUserId(),
                    event.getId(),
                    event.getTitle(),
                    requesterEmail);
        } catch (Exception e) {
            log.warn("Failed to send access request notification: {}", e.getMessage());
        }

        try {
            String ownerEmail = userServiceClient.getUserEmail(event.getOwnerUserId());
            String requesterEmail = userServiceClient.getUserEmail(requesterUserId);
            emailServiceClient.sendAccessRequestEmail(ownerEmail, event.getTitle(), requesterEmail);
        } catch (Exception e) {
            log.warn("Failed to send access request email: {}", e.getMessage());
        }
    }

    private void sendAccessApprovedNotifications(EventDocument event, String requesterUserId) {
        try {
            notificationServiceClient.notifyAccessApproved(requesterUserId, event.getId(), event.getTitle());
        } catch (Exception e) {
            log.warn("Failed to send access approval notification: {}", e.getMessage());
        }

        try {
            String requesterEmail = userServiceClient.getUserEmail(requesterUserId);
            emailServiceClient.sendAccessApprovedEmail(requesterEmail, event.getTitle());
        } catch (Exception e) {
            log.warn("Failed to send access approval email: {}", e.getMessage());
        }
    }

    private void sendAccessRejectedNotifications(EventDocument event, String requesterUserId, String rejectionReason) {
        try {
            notificationServiceClient.notifyAccessRejected(requesterUserId, event.getId(), event.getTitle(), rejectionReason);
        } catch (Exception e) {
            log.warn("Failed to send access rejection notification: {}", e.getMessage());
        }

        try {
            String requesterEmail = userServiceClient.getUserEmail(requesterUserId);
            emailServiceClient.sendAccessRejectedEmail(requesterEmail, event.getTitle(), rejectionReason);
        } catch (Exception e) {
            log.warn("Failed to send access rejection email: {}", e.getMessage());
        }
    }

    private void sendAccessRevokedNotifications(EventDocument event, String requesterUserId) {
        try {
            notificationServiceClient.notifyAccessRevoked(requesterUserId, event.getId(), event.getTitle());
        } catch (Exception e) {
            log.warn("Failed to send access revocation notification: {}", e.getMessage());
        }

        try {
            String userEmail = userServiceClient.getUserEmail(requesterUserId);
            emailServiceClient.sendAccessRevokedEmail(userEmail, event.getTitle());
        } catch (Exception e) {
            log.warn("Failed to send access revocation email: {}", e.getMessage());
        }
    }

    private String requireUserId(HttpServletRequest request, String message) {
        String userId = request == null ? null : request.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            throw new AuthenticationRequiredException(message);
        }
        return userId;
    }

    private String toIso(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
