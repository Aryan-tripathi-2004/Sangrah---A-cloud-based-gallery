package com.example.Event.infrastructure.persistence.repository;

import com.example.Event.infrastructure.persistence.document.EventMediaApprovalDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
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

    // Find all media uploads by a user in an event
    List<EventMediaApprovalDocument> findByEventIdAndUploaderUserId(String eventId, String uploaderUserId);

    // Find all PENDING approvals for an event (for moderation review)
    List<EventMediaApprovalDocument> findByEventIdAndStatus(String eventId, String status);

    // Find all media that was APPROVED in an event
    List<EventMediaApprovalDocument> findByEventIdAndStatusOrderByCreatedAtDesc(String eventId, String status);

    // Delete approvals for a media (if media is deleted)
    void deleteByMediaId(String mediaId);
}
