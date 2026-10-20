package com.example.Notification.infrastructure.persistence.repository;

import com.example.Notification.infrastructure.persistence.document.NotificationDocument;
import com.example.Notification.shared.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
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
    List<NotificationDocument> findByRecipientUserIdAndType(String recipientUserId, NotificationType type);

    /**
     * Find notifications by user sorted by creation date descending
     */
    List<NotificationDocument> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId);

    Page<NotificationDocument> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId, Pageable pageable);

    long countByRecipientUserId(String recipientUserId);

    long countByRecipientUserIdAndReadFalse(String recipientUserId);

    /**
     * Mark all unread notifications as read for a user directly in MongoDB.
     *
     * @param recipientUserId the recipient user ID
     * @return the number of documents modified
     */
    @Query("{ 'recipientUserId' : ?0, 'read' : false }")
    @Update("{ '$set' : { 'read' : true } }")
    long markAllAsReadByUserId(String recipientUserId);
}
