package com.example.Event.infrastructure.persistence.repository;

import com.example.Event.infrastructure.persistence.document.EventAccessRequestDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for EventAccessRequest persistence in MongoDB.
 */
@Repository
public interface EventAccessRequestRepository extends MongoRepository<EventAccessRequestDocument, String> {
    /**
     * Find access requests by event ID
     */
    List<EventAccessRequestDocument> findByEventId(String eventId);

    /**
     * Find access request by event and requester user
     */
    Optional<EventAccessRequestDocument> findByEventIdAndRequesterUserId(String eventId, String requesterUserId);

    /**
     * Find pending requests for an event
     */
    List<EventAccessRequestDocument> findByEventIdAndStatus(String eventId, String status);

    /**
     * Find pending requests for an event (ordered by timestamp)
     */
    List<EventAccessRequestDocument> findByEventIdAndStatusOrderByRequestedAtDesc(String eventId, String status);

    /**
     * Check for duplicate pending request from same requester
     */
    Optional<EventAccessRequestDocument> findByEventIdAndRequesterUserIdAndStatus(String eventId, String requesterUserId, String status);
}
