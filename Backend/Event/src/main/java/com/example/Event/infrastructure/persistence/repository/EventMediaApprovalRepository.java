package com.example.Event.infrastructure.persistence.repository;

import com.example.Event.infrastructure.persistence.document.EventMediaApprovalDocument;
import com.example.Event.shared.enums.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventMediaApprovalRepository extends MongoRepository<EventMediaApprovalDocument, String> {

    // Find approval for a specific media in an event
    Optional<EventMediaApprovalDocument> findByEventIdAndMediaId(String eventId, String mediaId);

    // Find all approvals for a specific media in an event (handles duplicates)
    List<EventMediaApprovalDocument> findByEventIdAndMediaIdOrderByStatusAsc(String eventId, String mediaId);

    // Find all approvals for an event
    List<EventMediaApprovalDocument> findByEventId(String eventId);

    // Paginated find all approvals for an event
    Page<EventMediaApprovalDocument> findByEventId(String eventId, Pageable pageable);

    // Find all media uploads by a user in an event
    List<EventMediaApprovalDocument> findByEventIdAndUploaderUserId(String eventId, String uploaderUserId);

    // Find all PENDING approvals for an event (for moderation review)
    List<EventMediaApprovalDocument> findByEventIdAndStatus(String eventId, ApprovalStatus status);

    // Find all media that was APPROVED in an event
    List<EventMediaApprovalDocument> findByEventIdAndStatusOrderByCreatedAtDesc(String eventId, ApprovalStatus status);

    // Paginated find all media that was APPROVED in an event
    Page<EventMediaApprovalDocument> findByEventIdAndStatusOrderByCreatedAtDesc(String eventId, ApprovalStatus status, Pageable pageable);

    // Count approvals by status for badge counts across all pages
    long countByEventIdAndStatus(String eventId, ApprovalStatus status);

    long countByEventId(String eventId);

    // Delete approvals for a media (if media is deleted)
    void deleteByMediaId(String mediaId);
}
