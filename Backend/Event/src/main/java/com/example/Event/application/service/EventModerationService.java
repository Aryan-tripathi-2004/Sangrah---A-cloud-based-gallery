package com.example.Event.application.service;

import com.example.Event.infrastructure.persistence.document.EventMediaApprovalDocument;
import com.example.Event.infrastructure.persistence.repository.EventMediaApprovalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventModerationService {

    private final EventMediaApprovalRepository approvalRepository;

    /**
     * Create media approval record (store in MongoDB instead of in-memory)
     * Determine status based on event moderation setting
     */
    public String createMedia(String eventId, String mediaId, String uploaderUserId, boolean moderationEnabled) {
        log.info("📝 [Moderation] Creating approval for media {} in event {}", mediaId, eventId);

        String status = moderationEnabled ? "PENDING" : "APPROVED";

        // Check if approval record already exists to avoid duplicates
        Optional<EventMediaApprovalDocument> existing = approvalRepository.findByEventIdAndMediaId(eventId, mediaId);
        if (existing.isPresent()) {
            log.warn("⚠️ [Moderation] Approval already exists for media {} in event {}. Returning existing status.", mediaId, eventId);
            return existing.get().getStatus();
        }

        EventMediaApprovalDocument approval = EventMediaApprovalDocument.builder()
                .eventId(eventId)
                .mediaId(mediaId)
                .uploaderUserId(uploaderUserId)
                .status(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        approvalRepository.save(approval);
        log.info("✅ [Moderation] Approval created with status: {}", status);

        return status;
    }

    /**
     * Approve media (requires event owner/moderator)
     */
    public String approveMedia(String eventId, String mediaId, String reviewedByUserId) {
        log.info("✅ [Moderation] Approving media {} in event {}", mediaId, eventId);

        Optional<EventMediaApprovalDocument> existing = approvalRepository.findByEventIdAndMediaId(eventId, mediaId);

        if (existing.isEmpty()) {
            log.error("❌ [Moderation] Approval not found for media {} in event {}", mediaId, eventId);
            throw new RuntimeException("Media approval not found");
        }

        EventMediaApprovalDocument approval = existing.get();
        approval.setStatus("APPROVED");
        approval.setReviewedByUserId(reviewedByUserId);
        approval.setReviewedAt(Instant.now());
        approval.setUpdatedAt(Instant.now());

        approvalRepository.save(approval);
        log.info("✅ [Moderation] Media approved successfully");

        return "APPROVED";
    }

    /**
     * Reject media with reason
     */
    public String rejectMedia(String eventId, String mediaId, String reviewedByUserId, String reason) {
        log.info("❌ [Moderation] Rejecting media {} in event {}, reason: {}", mediaId, eventId, reason);

        Optional<EventMediaApprovalDocument> existing = approvalRepository.findByEventIdAndMediaId(eventId, mediaId);

        if (existing.isEmpty()) {
            log.error("❌ [Moderation] Approval not found for media {} in event {}", mediaId, eventId);
            throw new RuntimeException("Media approval not found");
        }

        EventMediaApprovalDocument approval = existing.get();
        approval.setStatus("REJECTED");
        approval.setReviewedByUserId(reviewedByUserId);
        approval.setReviewedAt(Instant.now());
        approval.setRejectionReason(reason);
        approval.setUpdatedAt(Instant.now());

        approvalRepository.save(approval);
        log.info("✅ [Moderation] Media rejected");

        return "REJECTED";
    }

    /**
     * Get approval status for a media
     */
    public String getMediaStatus(String eventId, String mediaId) {
        Optional<EventMediaApprovalDocument> approval = approvalRepository.findByEventIdAndMediaId(eventId, mediaId);

        if (approval.isEmpty()) {
            log.warn("⚠️ [Moderation] Approval not found for media {}", mediaId);
            return "NOT_FOUND";
        }

        return approval.get().getStatus();
    }

    /**
     * Check if media is approved
     */
    public boolean isMediaApproved(String eventId, String mediaId) {
        Optional<EventMediaApprovalDocument> approval = approvalRepository.findByEventIdAndMediaId(eventId, mediaId);
        return approval.isPresent() && approval.get().isApproved();
    }

    /**
     * Set event moderation requirement
     * Note: This is stored in EventDocument, not here
     * This service only manages media approvals
     */
    public void setModeration(String eventId, boolean enabled) {
        log.info("📋 [Moderation] Event {} moderation set to: {}", eventId, enabled);
        // This would be set in EventService/EventDocument
        // Not stored in this service
    }

    /**
     * Delete media approval record
     */
    public void deleteMedia(String eventId, String mediaId) {
        try {
            log.info("🗑️ [Moderation] Deleting approval for media {} in event {}", mediaId, eventId);

            Optional<EventMediaApprovalDocument> approval = approvalRepository.findByEventIdAndMediaId(eventId, mediaId);
            
            if (approval.isPresent()) {
                approvalRepository.delete(approval.get());
                log.info("✅ [Moderation] Approval deleted for media {}", mediaId);
            } else {
                log.warn("⚠️ [Moderation] Approval not found for media {}", mediaId);
            }
        } catch (Exception e) {
            log.error("❌ [Moderation] Failed to delete approval: {}", e.getMessage());
            throw new RuntimeException("Failed to delete media approval: " + e.getMessage());
        }
    }
}

