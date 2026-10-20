package com.example.Gallery.infrastructure.persistence.repository;

import com.example.Gallery.infrastructure.persistence.document.OrphanedMediaLogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for persisting and querying orphaned media log documents.
 */
@Repository
public interface OrphanedMediaLogRepository extends MongoRepository<OrphanedMediaLogDocument, String> {
}
