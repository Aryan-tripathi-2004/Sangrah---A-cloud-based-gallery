package com.example.Event.application.service;

import com.example.Event.infrastructure.persistence.document.EventAccessRequestDocument;
import com.example.Event.infrastructure.persistence.repository.EventAccessRequestRepository;
import com.example.Event.infrastructure.client.NotificationServiceClient;
import com.example.Event.infrastructure.client.EmailServiceClient;
import com.example.Event.infrastructure.client.UserServiceClient;
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
    private final NotificationServiceClient notificationServiceClient;
    private final EmailServiceClient emailServiceClient;
    private final EventService eventService;
    private final UserServiceClient userServiceClient;

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

            try {
                var event = eventService.getEventById(eventId);
                String eventTitle = event.getTitle();
                String ownerUserId = event.getOwnerUserId();
                String requesterEmail = userServiceClient.getUserEmail(requesterUserId);

                notificationServiceClient.notifyAccessRequestReceived(
                        ownerUserId,
                        eventId,
                        eventTitle,
                        requesterEmail
                );
                log.info("✅ Access request notification sent to owner: {}", ownerUserId);
            } catch (Exception e) {
                log.warn("⚠️ Failed to send access request notification: {}", e.getMessage());
            }

            try {
                var event = eventService.getEventById(eventId);
                String eventTitle = event.getTitle();
                String ownerEmail = userServiceClient.getUserEmail(event.getOwnerUserId());
                String requesterEmail = userServiceClient.getUserEmail(requesterUserId);

                emailServiceClient.sendAccessRequestEmail(ownerEmail, eventTitle, requesterEmail);
                log.info("📧 [EventAccessService] Access request email sent to owner: {}", ownerEmail);
            } catch (Exception e) {
                log.warn("⚠️ [EventAccessService] Failed to send access request email: {}", e.getMessage());
            }

            return saved;

        } catch (Exception e) {
            log.error("❌ Failed to create access request: {}", e.getMessage());
            throw new RuntimeException("Failed to create access request: " + e.getMessage(), e);
        }
    }

    /**
     * List owner-visible access requests for an event.
     * Includes pending and approved requests so the owner can manage both tabs.
     */
    public List<EventAccessRequestDocument> listPendingRequests(String eventId, String ownerUserId) {
        try {
            return accessRequestRepository.findByEventIdAndStatusInOrderByRequestedAtDesc(
                    eventId,
                    List.of("PENDING", "APPROVED")
            );
        } catch (Exception e) {
            log.error("❌ Failed to list access requests: {}", e.getMessage());
            throw new RuntimeException("Failed to list access requests: " + e.getMessage(), e);
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
            request.setUpdatedAt(Instant.now());  // Track update time

            EventAccessRequestDocument updated = accessRequestRepository.save(request);
            log.info("✅ Access request approved: {}", requestId);

            // Send notification to requester
            try {
                String eventTitle = eventService.getEventById(eventId).getTitle();
                notificationServiceClient.notifyAccessApproved(
                        request.getRequesterUserId(),
                        eventId,
                        eventTitle
                );
                log.info("✅ Notification sent to user: {}", request.getRequesterUserId());
            } catch (Exception e) {
                log.warn("⚠️ Failed to send notification: {}", e.getMessage());
            }

            // Send email to requester
            try {
                String eventTitle = eventService.getEventById(eventId).getTitle();
                String requesterEmail = userServiceClient.getUserEmail(request.getRequesterUserId());
                emailServiceClient.sendAccessApprovedEmail(requesterEmail, eventTitle);
                log.info("📧 [EventAccessService] Approval email sent to: {}", requesterEmail);
            } catch (Exception e) {
                log.warn("⚠️ [EventAccessService] Failed to send approval email: {}", e.getMessage());
            }

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
            request.setUpdatedAt(Instant.now());  // Track update time

            EventAccessRequestDocument updated = accessRequestRepository.save(request);
            log.info("✅ Access request rejected: {}", requestId);

            // Send notification to requester
            try {
                String eventTitle = eventService.getEventById(eventId).getTitle();
                notificationServiceClient.notifyAccessRejected(
                        request.getRequesterUserId(),
                        eventId,
                        eventTitle,
                        rejectionReason
                );
                log.info("✅ Rejection notification sent to user: {}", request.getRequesterUserId());
            } catch (Exception e) {
                log.warn("⚠️ Failed to send rejection notification: {}", e.getMessage());
            }

            // Send email to requester
            try {
                String eventTitle = eventService.getEventById(eventId).getTitle();
                String requesterEmail = userServiceClient.getUserEmail(request.getRequesterUserId());
                emailServiceClient.sendAccessRejectedEmail(requesterEmail, eventTitle, rejectionReason);
                log.info("📧 [EventAccessService] Rejection email sent to: {}", requesterEmail);
            } catch (Exception e) {
                log.warn("⚠️ [EventAccessService] Failed to send rejection email: {}", e.getMessage());
            }

            return updated;

        } catch (Exception e) {
            log.error("❌ Failed to reject request: {}", e.getMessage());
            throw new RuntimeException("Failed to reject request: " + e.getMessage(), e);
        }
    }

    /**
     * Check if user has approved access to an event
     * Returns false if: REVOKED, REJECTED, PENDING, expired, or not found
     * Returns true if: APPROVED with valid/no expiration
     */
    public boolean isUserApproved(String eventId, String requesterUserId) {
        try {
            Optional<EventAccessRequestDocument> request =
                accessRequestRepository.findByEventIdAndRequesterUserId(eventId, requesterUserId);

            if (request.isPresent()) {
                EventAccessRequestDocument doc = request.get();

                // NEW: Check if REVOKED (explicit deny)
                if ("REVOKED".equals(doc.getStatus())) {
                    return false;  // ❌ Revoked users denied
                }

                if ("APPROVED".equals(doc.getStatus())) {
                    // No expiration date = FOREVER access
                    if (doc.getAccessExpiresAt() == null) {
                        return true;  // ✅ Permanent access
                    }

                    // Has expiration date = check if still valid
                    if (Instant.now().isBefore(doc.getAccessExpiresAt())) {
                        return true;  // ✅ Still valid
                    }

                    // Expired = denied (user can re-request)
                    return false;  // ❌ Access expired
                }
            }
            return false;

        } catch (Exception e) {
            log.error("❌ Failed to check approval status: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Revoke access for a user (mark as REVOKED, don't delete)
     * Sets revokedAt = now(), status = REVOKED, and tracks who revoked
     */
    public void revokeAccess(String eventId, String requesterUserId, String revokerUserId) {
        try {
            Optional<EventAccessRequestDocument> request =
                accessRequestRepository.findByEventIdAndRequesterUserId(eventId, requesterUserId);

            if (request.isPresent()) {
                EventAccessRequestDocument doc = request.get();

                // ===== MARK AS REVOKED (NOT DELETED) =====
                doc.setStatus("REVOKED");                     // Mark as revoked
                doc.setAccessExpiresAt(Instant.now());        // Set to now = immediately expired
                doc.setRevokedAt(Instant.now());              // When revoked
                doc.setRevokedByUserId(revokerUserId);        // Who revoked it
                doc.setUpdatedAt(Instant.now());              // Update timestamp

                accessRequestRepository.save(doc);
                log.info("✅ Access revoked for user: {} on event: {} by {}", requesterUserId, eventId, revokerUserId);

                // Send notification that access was revoked
                try {
                    String eventTitle = eventService.getEventById(eventId).getTitle();
                    notificationServiceClient.notifyAccessRevoked(requesterUserId, eventId, eventTitle);
                    log.info("✅ Revocation notification sent to user: {}", requesterUserId);
                } catch (Exception e) {
                    log.warn("⚠️ Failed to send revocation notification: {}", e.getMessage());
                }

                // Send email that access was revoked
                try {
                    String eventTitle = eventService.getEventById(eventId).getTitle();
                    String userEmail = userServiceClient.getUserEmail(requesterUserId);
                    emailServiceClient.sendAccessRevokedEmail(userEmail, eventTitle);
                    log.info("📧 [EventAccessService] Revocation email sent to: {}", userEmail);
                } catch (Exception e) {
                    log.warn("⚠️ [EventAccessService] Failed to send revocation email: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("❌ Failed to revoke access: {}", e.getMessage());
            throw new RuntimeException("Failed to revoke access: " + e.getMessage(), e);
        }
    }

    /**
     * User re-requests access after expiration or revocation
     * Updates existing record to PENDING status (back to waiting for owner approval)
     * CRITICAL: Re-request does NOT auto-approve - owner must manually approve again
     */
    public EventAccessRequestDocument createReRequest(String eventId, String requesterUserId, String message) {
        try {
            // Find existing record (should always exist at this point)
            EventAccessRequestDocument request = accessRequestRepository
                    .findByEventIdAndRequesterUserId(eventId, requesterUserId)
                    .orElseThrow(() -> new RuntimeException("No prior access request found"));

            // ===== UPDATE THE SAME RECORD - SET BACK TO PENDING =====
            request.setStatus("PENDING");                     // Back to PENDING (waiting for owner approval)
            request.setRequestedAt(Instant.now());            // Update request timestamp
            request.setUpdatedAt(Instant.now());              // Track update
            request.setMessage(message);                      // New message (if provided)
            request.setRejectionReason(null);                 // Clear rejection reason

            // Clear any previous decision/revocation data
            request.setDecisionAt(null);
            request.setDecidedByUserId(null);
            request.setRevokedAt(null);
            request.setRevokedByUserId(null);

            EventAccessRequestDocument saved = accessRequestRepository.save(request);
            log.info("🔄 [Re-Request] User {} requested access again to event {}", requesterUserId, eventId);

            try {
                var event = eventService.getEventById(eventId);
                String eventTitle = event.getTitle();
                String ownerUserId = event.getOwnerUserId();
                String requesterEmail = userServiceClient.getUserEmail(requesterUserId);

                notificationServiceClient.notifyAccessRequestReceived(
                        ownerUserId,
                        eventId,
                        eventTitle,
                        requesterEmail
                );
            } catch (Exception e) {
                log.warn("⚠️ Failed to send re-request notification: {}", e.getMessage());
            }

            try {
                var event = eventService.getEventById(eventId);
                String eventTitle = event.getTitle();
                String ownerEmail = userServiceClient.getUserEmail(event.getOwnerUserId());
                String requesterEmail = userServiceClient.getUserEmail(requesterUserId);

                emailServiceClient.sendAccessRequestEmail(ownerEmail, eventTitle, requesterEmail);
            } catch (Exception e) {
                log.warn("⚠️ [EventAccessService] Failed to send re-request email: {}", e.getMessage());
            }

            return saved;

        } catch (Exception e) {
            log.error("❌ Failed to create re-request: {}", e.getMessage());
            throw new RuntimeException("Failed to create re-request: " + e.getMessage(), e);
        }
    }

    /**
     * Owner approves a re-request (user's second/third/etc attempt after expiration/revocation)
     * Updates SAME record with new approval duration and expiration
     * CRITICAL: This requires owner to manually make the decision AGAIN
     */
    public EventAccessRequestDocument approveReRequest(
            String eventId,
            String requesterUserId,
            String approverUserId,
            String approvalDuration,
            Instant expiresAt) {
        try {
            EventAccessRequestDocument request = accessRequestRepository
                    .findByEventIdAndRequesterUserId(eventId, requesterUserId)
                    .orElseThrow(() -> new RuntimeException("No prior access request found"));

            // ===== UPDATE THE SAME RECORD =====
            request.setStatus("APPROVED");                    // Approve from PENDING/REVOKED
            request.setDecisionAt(Instant.now());             // When decision was made
            request.setDecidedByUserId(approverUserId);       // Who made the decision
            request.setUpdatedAt(Instant.now());              // Track update time

            // Clear revocation tracking (user is being re-approved)
            request.setRevokedAt(null);
            request.setRevokedByUserId(null);

            // Set new expiration based on approval duration
            if ("FOREVER".equals(approvalDuration)) {
                request.setAccessExpiresAt(null);  // ✅ Remove expiration = permanent access
                log.info("✅ [Re-Approve] User {} approved for FOREVER access", requesterUserId);
            } else {
                request.setAccessExpiresAt(expiresAt);  // ✅ Set new expiration date
                log.info("✅ [Re-Approve] User {} approved until {}", requesterUserId, expiresAt);
            }

            request.setApprovalDuration(approvalDuration);

            // Save the SAME record (not creating new one)
            return accessRequestRepository.save(request);

        } catch (Exception e) {
            log.error("❌ Failed to approve re-request: {}", e.getMessage());
            throw new RuntimeException("Failed to approve re-request: " + e.getMessage(), e);
        }
    }
}
