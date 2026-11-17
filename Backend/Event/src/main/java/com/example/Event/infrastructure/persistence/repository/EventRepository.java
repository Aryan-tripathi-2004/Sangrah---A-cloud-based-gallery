package com.example.Event.infrastructure.persistence.repository;

import com.example.Event.infrastructure.persistence.document.EventDocument;
import com.example.Event.shared.enums.EventVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Event persistence in MongoDB.
 */
@Repository
public interface EventRepository extends MongoRepository<EventDocument, String> {
    /**
     * Find events by owner user ID
     */
    List<EventDocument> findByOwnerUserId(String ownerUserId);

    Page<EventDocument> findByOwnerUserId(String ownerUserId, Pageable pageable);

    /**
     * Find events by visibility
     */
    List<EventDocument> findByVisibility(EventVisibility visibility);

    Page<EventDocument> findByVisibility(EventVisibility visibility, Pageable pageable);

    /**
     * Find events matching any of the specified visibilities using a single MongoDB $in query.
     */
    List<EventDocument> findByVisibilityIn(List<EventVisibility> visibilities);

    Page<EventDocument> findByVisibilityIn(List<EventVisibility> visibilities, Pageable pageable);

    /**
     * Find global events visible to authenticated user: PUBLIC, PROTECTED, or owned by user (including user's PRIVATE).
     */
    @Query("{ $or: [ { 'visibility': { $in: ['PUBLIC', 'PROTECTED'] } }, { 'ownerUserId': ?0 } ] }")
    Page<EventDocument> findGlobalVisibleEvents(String ownerUserId, Pageable pageable);

    @Query("{ $or: [ { 'visibility': { $in: ['PUBLIC', 'PROTECTED'] } }, { 'ownerUserId': ?0 } ] }")
    List<EventDocument> findGlobalVisibleEvents(String ownerUserId);

    Page<EventDocument> findByOwnerUserIdAndVisibility(String ownerUserId, EventVisibility visibility, Pageable pageable);

    List<EventDocument> findByOwnerUserIdAndVisibility(String ownerUserId, EventVisibility visibility);

    Page<EventDocument> findByOwnerUserIdAndIdIn(String ownerUserId, List<String> eventIds, Pageable pageable);
}
