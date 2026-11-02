package com.example.Event.infrastructure.persistence.repository;

import com.example.Event.infrastructure.persistence.document.EventDocument;
import com.example.Event.shared.enums.EventVisibility;
import org.springframework.data.mongodb.repository.MongoRepository;
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

    /**
     * Find events by visibility
     */
    List<EventDocument> findByVisibility(EventVisibility visibility);

    /**
     * Find events matching any of the specified visibilities using a single MongoDB $in query.
     */
    List<EventDocument> findByVisibilityIn(List<EventVisibility> visibilities);
}
