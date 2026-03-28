package com.example.Notification.infrastructure.persistence.repository;

import com.example.Notification.infrastructure.persistence.document.NotificationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Notification persistence in MongoDB.
 */
@Repository
public interface NotificationRepository extends MongoRepository<NotificationDocument, String> {
    /**
     * Find notifications by recipient user ID
     */
    List<NotificationDocument> findByRecipientUserId(String recipientUserId);

    /**
     * Find unread notifications for a user
     */
    List<NotificationDocument> findByRecipientUserIdAndReadFalse(String recipientUserId);

    /**
     * Find notifications by user and type
     */
    List<NotificationDocument> findByRecipientUserIdAndType(String recipientUserId, String type);

    /**
     * Find notifications by user sorted by creation date descending
     */
    List<NotificationDocument> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId);
}
