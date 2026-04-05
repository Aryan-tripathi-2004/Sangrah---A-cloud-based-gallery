package com.example.Event.application.service;

import com.example.Event.infrastructure.persistence.document.EventAccessRequestDocument;
import com.example.Event.infrastructure.persistence.repository.EventAccessRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventAccessService {

    private final EventAccessRequestRepository accessRequestRepository;

    /**
     * Create an access request for a PROTECTED event
     */
    public EventAccessRequestDocument requestAccess(String eventId, String requesterUserId, String message) {
        try {
            // Check if already requested
            Optional<EventAccessRequestDocument> existing =
                accessRequestRepository.findByEventIdAndRequesterUserId(eventId, requesterUserId);

            if (existing.isPresent()) {
                if ("PENDING".equals(existing.get().getStatus())) {
                    throw new RuntimeException("You have already requested access to this event");
                }
                if ("APPROVED".equals(existing.get().getStatus())) {
                    throw new RuntimeException("You already have access to this event");
                }
            }

            // Create new request
            EventAccessRequestDocument request = EventAccessRequestDocument.builder()
                    .eventId(eventId)
                    .requesterUserId(requesterUserId)
                    .message(message)
                    .status("PENDING")
                    .requestedAt(Instant.now())
                    .build();

            EventAccessRequestDocument saved = accessRequestRepository.save(request);
            log.info("✅ Access request created: {} for event: {} by user: {}", saved.getId(), eventId, requesterUserId);
            return saved;

        } catch (Exception e) {
            log.error("❌ Failed to create access request: {}", e.getMessage());
            throw new RuntimeException("Failed to create access request: " + e.getMessage(), e);
        }
    }

    /**
     * List pending access requests for an event (owner only)
     */
    public List<EventAccessRequestDocument> listPendingRequests(String eventId, String ownerUserId) {
        try {
            return accessRequestRepository.findByEventIdAndStatusOrderByRequestedAtDesc(eventId, "PENDING");
        } catch (Exception e) {
            log.error("❌ Failed to list pending requests: {}", e.getMessage());
            throw new RuntimeException("Failed to list pending requests: " + e.getMessage(), e);
        }
    }

    /**
     * Approve an access request
     */
    public EventAccessRequestDocument approveRequest(
            String eventId,
            String requestId,
            String approverUserId,
            String approvalDuration,
            Instant accessExpiresAt) {
        try {
            EventAccessRequestDocument request = accessRequestRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));

            // Update status
            request.setStatus("APPROVED");
            request.setApprovalDuration(approvalDuration != null ? approvalDuration : "FOREVER");
            if ("UNTIL_DATE".equals(approvalDuration) && accessExpiresAt != null) {
                request.setAccessExpiresAt(accessExpiresAt);
            }
            request.setDecisionAt(Instant.now());
            request.setDecidedByUserId(approverUserId);

            EventAccessRequestDocument updated = accessRequestRepository.save(request);
            log.info("✅ Access request approved: {}", requestId);

            // TODO: Send email notification
            // TODO: Create in-app notification
            request.setEmailNotificationSent(true);
            request.setInAppNotificationCreated(true);

            return updated;

        } catch (Exception e) {
            log.error("❌ Failed to approve request: {}", e.getMessage());
            throw new RuntimeException("Failed to approve request: " + e.getMessage(), e);
        }
    }

    /**
     * Reject an access request
     */
    public EventAccessRequestDocument rejectRequest(
            String eventId,
            String requestId,
            String approverUserId,
            String rejectionReason) {
        try {
            EventAccessRequestDocument request = accessRequestRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));

            // Update status
            request.setStatus("REJECTED");
            request.setRejectionReason(rejectionReason);
            request.setDecisionAt(Instant.now());
            request.setDecidedByUserId(approverUserId);

            EventAccessRequestDocument updated = accessRequestRepository.save(request);
            log.info("✅ Access request rejected: {}", requestId);

            // TODO: Send email notification
            // TODO: Create in-app notification
            request.setEmailNotificationSent(true);
            request.setInAppNotificationCreated(true);

            return updated;

        } catch (Exception e) {
            log.error("❌ Failed to reject request: {}", e.getMessage());
            throw new RuntimeException("Failed to reject request: " + e.getMessage(), e);
        }
    }

    /**
     * Check if user has approved access to an event
     */
    public boolean isUserApproved(String eventId, String requesterUserId) {
        try {
            Optional<EventAccessRequestDocument> request =
                accessRequestRepository.findByEventIdAndRequesterUserId(eventId, requesterUserId);

            if (request.isPresent()) {
                EventAccessRequestDocument doc = request.get();
                if ("APPROVED".equals(doc.getStatus())) {
                    // Check if access is not expired
                    if (doc.getAccessExpiresAt() != null && Instant.now().isAfter(doc.getAccessExpiresAt())) {
                        return false; // Access expired
                    }
                    return true;
                }
            }
            return false;

        } catch (Exception e) {
            log.error("❌ Failed to check approval status: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Revoke access for a user
     */
    public void revokeAccess(String eventId, String requesterUserId) {
        try {
            Optional<EventAccessRequestDocument> request =
                accessRequestRepository.findByEventIdAndRequesterUserId(eventId, requesterUserId);

            if (request.isPresent()) {
                accessRequestRepository.deleteById(request.get().getId());
                log.info("✅ Access revoked for user: {} on event: {}", requesterUserId, eventId);
            }

        } catch (Exception e) {
            log.error("❌ Failed to revoke access: {}", e.getMessage());
            throw new RuntimeException("Failed to revoke access: " + e.getMessage(), e);
        }
    }
}
