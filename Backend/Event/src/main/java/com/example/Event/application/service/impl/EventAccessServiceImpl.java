package com.example.Event.application.service.impl;

import com.example.Event.api.dto.request.AccessApprovalRequest;
import com.example.Event.api.dto.request.AccessMessageRequest;
import com.example.Event.api.dto.request.AccessRejectionRequest;
import com.example.Event.api.dto.request.AccessStatusUpdateRequest;
import com.example.Event.api.dto.response.AccessApprovalResponse;
import com.example.Event.api.dto.response.AccessStatusUpdateResponse;
import com.example.Event.api.dto.response.AccessRejectionResponse;
import com.example.Event.api.dto.response.AccessRequestMutationResponse;
import com.example.Event.api.dto.response.AccessRequestResponse;
import com.example.Event.api.dto.response.AccessRequestsResponse;
import com.example.Event.api.dto.response.AccessRevocationResponse;
import com.example.Event.api.dto.response.AccessStatusResponse;
import com.example.Event.application.service.interfaces.IEventAccessService;
import com.example.Event.application.service.interfaces.IEventCollaboratorService;
import com.example.Event.application.service.interfaces.IEventInternalService;
import com.example.Event.infrastructure.client.EmailServiceClient;
import com.example.Event.infrastructure.client.NotificationIntegrationService;
import com.example.Event.infrastructure.client.UserIntegrationService;
import com.example.Event.infrastructure.mapper.EventAccessMapper;
import com.example.Event.infrastructure.persistence.document.EventAccessRequestDocument;
import com.example.Event.api.dto.response.EventInternalDTO;
import com.example.Event.infrastructure.persistence.repository.EventAccessRequestRepository;
import com.example.Event.shared.enums.AccessStatus;
import com.example.Event.shared.enums.ApprovalDuration;
import com.example.Event.shared.enums.ApprovalStatus;
import com.example.Event.shared.enums.EventVisibility;
import com.example.Event.shared.exception.DomainValidationException;
import com.example.Event.shared.exception.ForbiddenOperationException;
import com.example.Event.shared.exception.ResourceNotFoundException;
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

    private static final ApprovalStatus STATUS_PENDING = ApprovalStatus.PENDING;
    private static final ApprovalStatus STATUS_APPROVED = ApprovalStatus.APPROVED;
    private static final ApprovalStatus STATUS_REJECTED = ApprovalStatus.REJECTED;
    private static final ApprovalStatus STATUS_REVOKED = ApprovalStatus.REVOKED;

    private final EventAccessRequestRepository accessRequestRepository;
    private final NotificationIntegrationService notificationIntegrationService;
    private final EmailServiceClient emailServiceClient;
    private final IEventInternalService eventService;
    private final IEventCollaboratorService collaboratorService;
    private final UserIntegrationService userIntegrationService;
    private final EventAccessMapper accessMapper;

    @Override
    public AccessRequestMutationResponse requestAccess(
            String eventId,
            AccessMessageRequest request,
            String userId) {
        EventInternalDTO event = eventService.getEventById(eventId);

        if (userId.equals(event.ownerUserId())) {
            throw new ForbiddenOperationException("You are the event owner. No need to request access.");
        }

        if (EventVisibility.PROTECTED != event.visibility()) {
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

        return accessMapper.toMutationResponse(accessRequest, "Access request has been sent to the event owner");
    }

    @Override
    public AccessRequestsResponse listAccessRequests(String eventId, String userId) {
        EventInternalDTO event = eventService.getEventById(eventId);
        requireAccessRequestReviewer(event, userId, "Only event owner and authorized collaborators can view access requests");

        List<AccessRequestResponse> requests = accessRequestRepository.findByEventIdAndStatusInOrderByRequestedAtDesc(
                        eventId,
                        List.of(STATUS_PENDING, STATUS_APPROVED))
                .stream()
                .map(this::enrichAccessRequest)
                .toList();

        return accessMapper.toRequestsResponse(eventId, requests);
    }

    @Override
    public AccessApprovalResponse approveRequest(
            String eventId,
            String requestId,
            AccessApprovalRequest request,
            String userId) {
        EventInternalDTO event = eventService.getEventById(eventId);
        requireAccessRequestReviewer(event, userId, "You don't have permission to approve access requests");

        ApprovalDuration approvalDuration = request != null ? request.resolvedApprovalDuration() : ApprovalDuration.FOREVER;
        Instant accessExpiresAt = parseAccessExpiresAt(request);

        EventAccessRequestDocument approved = approveRequestDocument(
                eventId,
                requestId,
                userId,
                approvalDuration,
                accessExpiresAt);

        return accessMapper.toApprovalResponse(approved, approvalDuration, accessExpiresAt, "Access request approved");
    }

    @Override
    public AccessRejectionResponse rejectRequest(
            String eventId,
            String requestId,
            AccessRejectionRequest request,
            String userId) {
        EventInternalDTO event = eventService.getEventById(eventId);
        requireAccessRequestReviewer(event, userId, "You don't have permission to reject access requests");

        String reason = request != null ? request.resolvedReason() : "Request denied";
        EventAccessRequestDocument rejected = rejectRequestDocument(eventId, requestId, userId, reason);

        return accessMapper.toRejectionResponse(rejected, "Access request rejected", reason);
    }

    @Override
    public AccessRevocationResponse revokeRequest(String eventId, String requestId, String userId) {
        EventInternalDTO event = eventService.getEventById(eventId);
        requireAccessRequestReviewer(event, userId, "You don't have permission to revoke access");

        EventAccessRequestDocument accessRequest = findAccessRequest(requestId);
        if (!eventId.equals(accessRequest.getEventId())) {
            throw new DomainValidationException("Request does not belong to this event");
        }
        if (STATUS_APPROVED != accessRequest.getStatus()) {
            throw new DomainValidationException("Only approved requests can be revoked");
        }

        revokeAccess(event, accessRequest.getRequesterUserId(), userId);
        return accessMapper.toRevocationResponse(STATUS_REVOKED, "Access revoked successfully");
    }

    @Override
    public AccessStatusUpdateResponse updateAccessStatus(
            String eventId,
            String requestId,
            AccessStatusUpdateRequest request,
            String userId) {
        if (request == null || request.status() == null) {
            throw new DomainValidationException("Status is required");
        }

        return switch (request.status()) {
            case APPROVED -> {
                AccessApprovalResponse r = approveRequest(
                        eventId, requestId, request.toApprovalRequest(), userId);
                yield new AccessStatusUpdateResponse(
                        r.status(), r.message(),
                        r.approvalDuration() != null ? r.approvalDuration().name() : null,
                        r.accessExpiresAt(), null);
            }
            case REJECTED -> {
                AccessRejectionResponse r = rejectRequest(
                        eventId, requestId, request.toRejectionRequest(), userId);
                yield new AccessStatusUpdateResponse(
                        r.status(), r.message(), null, null, r.reason());
            }
            case REVOKED -> {
                AccessRevocationResponse r = revokeRequest(eventId, requestId, userId);
                yield new AccessStatusUpdateResponse(
                        r.status(), r.message(), null, null, null);
            }
            default -> throw new DomainValidationException("Invalid status transition requested. Allowed: APPROVED, REJECTED, REVOKED");
        };
    }

    @Override
    public AccessRequestMutationResponse reRequestAccess(
            String eventId,
            AccessMessageRequest request,
            String userId) {
        EventInternalDTO event = eventService.getEventById(eventId);

        if (EventVisibility.PROTECTED != event.visibility()) {
            throw new DomainValidationException("Re-requests only allowed for PROTECTED events");
        }

        EventAccessRequestDocument existing = accessRequestRepository.findByEventIdAndRequesterUserId(eventId, userId)
                .orElseThrow(() -> new DomainValidationException("No prior access request found. Please make an initial request."));

        if (STATUS_PENDING == existing.getStatus()) {
            throw new DomainValidationException("Your request is already pending approval");
        }
        if (STATUS_APPROVED == existing.getStatus() && isApprovedAndActive(existing)) {
            throw new DomainValidationException("Your access is still active");
        }

        EventAccessRequestDocument reRequest = createReRequestDocument(
                event,
                existing,
                userId,
                request != null ? request.normalizedMessage() : null);

        return accessMapper.toMutationResponse(
                reRequest,
                "Your access request has been re-submitted and is awaiting owner approval");
    }

    @Override
    public AccessStatusResponse getAccessStatus(String eventId, String userId) {
        EventInternalDTO event = eventService.getEventById(eventId);

        if (userId.equals(event.ownerUserId())) {
            return accessMapper.toSimpleStatus(AccessStatus.OWNER, true, false);
        }

        if (collaboratorService.isCollaborator(eventId, userId)) {
            return accessMapper.toSimpleStatus(AccessStatus.COLLABORATOR, true, false);
        }

        Optional<EventAccessRequestDocument> request = accessRequestRepository.findByEventIdAndRequesterUserId(eventId, userId);
        if (request.isEmpty()) {
            return accessMapper.toSimpleStatus(AccessStatus.NONE, false, EventVisibility.PROTECTED == event.visibility());
        }

        EventAccessRequestDocument accessRequest = request.get();
        if (STATUS_PENDING == accessRequest.getStatus()) {
            return accessMapper.toPendingStatus(accessRequest);
        }
        if (STATUS_APPROVED == accessRequest.getStatus()) {
            return approvedStatus(accessRequest);
        }
        if (STATUS_REJECTED == accessRequest.getStatus()) {
            return accessMapper.toRejectedStatus(accessRequest);
        }
        if (STATUS_REVOKED == accessRequest.getStatus()) {
            return accessMapper.toRevokedStatus(accessRequest);
        }
        return accessMapper.toSimpleStatus(AccessStatus.UNKNOWN, false, false);
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

    private EventAccessRequestDocument createAccessRequestDocument(EventInternalDTO event, String requesterUserId, String message) {
        Optional<EventAccessRequestDocument> existing =
                accessRequestRepository.findByEventIdAndRequesterUserId(event.id(), requesterUserId);

        if (existing.isPresent()) {
            if (STATUS_PENDING == existing.get().getStatus()) {
                throw new DomainValidationException("You have already requested access to this event");
            }
            if (STATUS_APPROVED == existing.get().getStatus()) {
                throw new DomainValidationException("You already have access to this event");
            }
        }

        EventAccessRequestDocument request = EventAccessRequestDocument.builder()
                .eventId(event.id())
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
            ApprovalDuration approvalDuration,
            Instant accessExpiresAt) {
        EventAccessRequestDocument request = findAccessRequest(requestId);
        if (!eventId.equals(request.getEventId())) {
            throw new DomainValidationException("Request does not belong to this event");
        }

        request.setStatus(STATUS_APPROVED);
        request.setApprovalDuration(approvalDuration);
        request.setAccessExpiresAt(ApprovalDuration.UNTIL_DATE == approvalDuration ? accessExpiresAt : null);
        request.setDecisionAt(Instant.now());
        request.setDecidedByUserId(approverUserId);
        request.setUpdatedAt(Instant.now());

        EventAccessRequestDocument updated = accessRequestRepository.save(request);
        EventInternalDTO event = eventService.getEventById(eventId);
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
        EventInternalDTO event = eventService.getEventById(eventId);
        sendAccessRejectedNotifications(event, request.getRequesterUserId(), rejectionReason);
        return updated;
    }

    private void revokeAccess(EventInternalDTO event, String requesterUserId, String revokerUserId) {
        EventAccessRequestDocument request = accessRequestRepository.findByEventIdAndRequesterUserId(event.id(), requesterUserId)
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
            EventInternalDTO event,
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

    private AccessStatusResponse approvedStatus(EventAccessRequestDocument accessRequest) {
        boolean expired = accessRequest.getAccessExpiresAt() != null
                && Instant.now().isAfter(accessRequest.getAccessExpiresAt());
        if (expired) {
            return accessMapper.toExpiredStatus(accessRequest);
        }
        return accessMapper.toApprovedStatus(accessRequest);
    }

    private boolean isApprovedAndActive(EventAccessRequestDocument request) {
        if (STATUS_APPROVED != request.getStatus()) {
            return false;
        }
        return request.getAccessExpiresAt() == null || Instant.now().isBefore(request.getAccessExpiresAt());
    }

    private void requireAccessRequestReviewer(EventInternalDTO event, String userId, String message) {
        if (userId.equals(event.ownerUserId())) {
            return;
        }
        if (!collaboratorService.hasPermission(event.id(), userId, "canReviewAccessRequests")) {
            throw new ForbiddenOperationException(message);
        }
    }

    private Instant parseAccessExpiresAt(AccessApprovalRequest request) {
        if (request == null || ApprovalDuration.UNTIL_DATE != request.resolvedApprovalDuration()
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

    private void sendAccessRequestNotifications(EventInternalDTO event, String requesterUserId) {
        try {
            String requesterEmail = userIntegrationService.getUserEmail(requesterUserId);
            notificationIntegrationService.notifyAccessRequestReceived(
                    event.ownerUserId(),
                    event.id(),
                    event.title(),
                    requesterEmail);
        } catch (Exception e) {
            log.warn("Failed to send access request notification: {}", e.getMessage());
        }

        try {
            String ownerEmail = userIntegrationService.getUserEmail(event.ownerUserId());
            String requesterEmail = userIntegrationService.getUserEmail(requesterUserId);
            emailServiceClient.sendAccessRequestEmail(ownerEmail, event.title(), requesterEmail);
        } catch (Exception e) {
            log.warn("Failed to send access request email: {}", e.getMessage());
        }
    }

    private void sendAccessApprovedNotifications(EventInternalDTO event, String requesterUserId) {
        try {
            notificationIntegrationService.notifyAccessApproved(requesterUserId, event.id(), event.title());
        } catch (Exception e) {
            log.warn("Failed to send access approval notification: {}", e.getMessage());
        }

        try {
            String requesterEmail = userIntegrationService.getUserEmail(requesterUserId);
            emailServiceClient.sendAccessApprovedEmail(requesterEmail, event.title());
        } catch (Exception e) {
            log.warn("Failed to send access approval email: {}", e.getMessage());
        }
    }

    private void sendAccessRejectedNotifications(EventInternalDTO event, String requesterUserId, String rejectionReason) {
        try {
            notificationIntegrationService.notifyAccessRejected(requesterUserId, event.id(), event.title(), rejectionReason);
        } catch (Exception e) {
            log.warn("Failed to send access rejection notification: {}", e.getMessage());
        }

        try {
            String requesterEmail = userIntegrationService.getUserEmail(requesterUserId);
            emailServiceClient.sendAccessRejectedEmail(requesterEmail, event.title(), rejectionReason);
        } catch (Exception e) {
            log.warn("Failed to send access rejection email: {}", e.getMessage());
        }
    }

    private void sendAccessRevokedNotifications(EventInternalDTO event, String requesterUserId) {
        try {
            notificationIntegrationService.notifyAccessRevoked(requesterUserId, event.id(), event.title());
        } catch (Exception e) {
            log.warn("Failed to send access revocation notification: {}", e.getMessage());
        }

        try {
            String userEmail = userIntegrationService.getUserEmail(requesterUserId);
            emailServiceClient.sendAccessRevokedEmail(userEmail, event.title());
        } catch (Exception e) {
            log.warn("Failed to send access revocation email: {}", e.getMessage());
        }
    }

    private AccessRequestResponse enrichAccessRequest(EventAccessRequestDocument accessRequest) {
        String displayName;
        try {
            displayName = userIntegrationService.getUserDisplayName(accessRequest.getRequesterUserId());
        } catch (Exception e) {
            log.warn("Could not fetch displayName for {}: {}", accessRequest.getRequesterUserId(), e.getMessage());
            displayName = accessRequest.getRequesterUserId();
        }
        return accessMapper.toAccessRequestResponse(accessRequest, displayName);
    }
}
