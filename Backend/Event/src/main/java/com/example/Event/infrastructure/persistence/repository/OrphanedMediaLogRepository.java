package com.example.Event.infrastructure.persistence.repository;

import com.example.Event.infrastructure.persistence.document.OrphanedMediaLogDocument;
import com.example.Event.shared.enums.OrphanedMediaStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrphanedMediaLogRepository extends MongoRepository<OrphanedMediaLogDocument, String> {
    List<OrphanedMediaLogDocument> findByStatus(OrphanedMediaStatus status);
}
