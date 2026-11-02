package com.example.Gallery.infrastructure.persistence.repository;

import com.example.Gallery.infrastructure.persistence.document.OrphanedMediaLogDocument;
import com.example.Gallery.shared.enums.OrphanedMediaStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrphanedMediaLogRepository extends MongoRepository<OrphanedMediaLogDocument, String> {
    List<OrphanedMediaLogDocument> findByStatus(OrphanedMediaStatus status);
}
