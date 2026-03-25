package com.example.Event.infrastructure.persistence.repository;

import com.example.Event.infrastructure.persistence.document.EventMediaDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for EventMedia persistence in MongoDB.
 */
@Repository
public interface EventMediaRepository extends MongoRepository<EventMediaDocument, String> {
    /**
     * Find media by event ID
     */
    List<EventMediaDocument> findByEventId(String eventId);

    /**
     * Find media by uploader user ID
     */
    List<EventMediaDocument> findByUploaderUserId(String uploaderUserId);

    /**
     * Find media by event and MIME type
     */
    List<EventMediaDocument> findByEventIdAndMimeType(String eventId, String mimeType);
}
