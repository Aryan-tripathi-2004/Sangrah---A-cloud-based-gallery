package com.example.Event.infrastructure.persistence.repository;

import com.example.Event.infrastructure.persistence.document.OrphanedMediaLogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrphanedMediaLogRepository extends MongoRepository<OrphanedMediaLogDocument, String> {
}