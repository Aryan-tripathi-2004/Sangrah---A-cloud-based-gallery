package com.example.Notification.infrastructure.persistence.repository;

import com.example.Notification.infrastructure.persistence.document.NotificationPreferenceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends MongoRepository<NotificationPreferenceDocument, String> {
    Optional<NotificationPreferenceDocument> findByUserId(String userId);
}
