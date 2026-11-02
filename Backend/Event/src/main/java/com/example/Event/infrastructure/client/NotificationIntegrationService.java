package com.example.Event.infrastructure.client;

import com.example.Event.infrastructure.client.dto.NotificationPayload;
import com.example.Event.infrastructure.client.dto.NotificationRequest;
import com.example.Event.shared.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Integration service for dispatching notifications to Notification Service.
 * Decouples notification dispatch orchestration and payload building from declarative Feign HTTP contract.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationIntegrationService {

    private final NotificationServiceClient notificationServiceClient;

    public void notifyCollaboratorAdded(String collaboratorUserId, String eventId, String eventTitle, String addedByUserEmail) {
        send(
                collaboratorUserId,
                NotificationType.COLLABORATOR_ADDED,
                new NotificationPayload(
                        eventId,
                        eventTitle,
                        addedByUserEmail,
                        null,
                        null,
                        null,
                        "You've been added as a collaborator to: " + eventTitle,
                        "/event/" + eventId));
    }

    public void notifyAccessRequestReceived(String eventOwnerId, String eventId, String eventTitle, String requesterEmail) {
        send(
                eventOwnerId,
                NotificationType.ACCESS_REQUEST_RECEIVED,
                new NotificationPayload(
                        eventId,
                        eventTitle,
                        null,
                        requesterEmail,
                        null,
                        null,
                        "New access request for: " + eventTitle,
                        "/event/" + eventId + "?tab=access-requests"));
    }

    public void notifyAccessApproved(String requesterUserId, String eventId, String eventTitle) {
        send(
                requesterUserId,
                NotificationType.ACCESS_REQUEST_APPROVED,
                new NotificationPayload(
                        eventId,
                        eventTitle,
                        null,
                        null,
                        null,
                        null,
                        "Your access request has been approved for: " + eventTitle,
                        "/event/" + eventId));
    }

    public void notifyCollaboratorRemoved(String collaboratorUserId, String eventId, String eventTitle) {
        send(
                collaboratorUserId,
                NotificationType.COLLABORATOR_REMOVED,
                new NotificationPayload(
                        eventId,
                        eventTitle,
                        null,
                        null,
                        null,
                        null,
                        "You have been removed from: " + eventTitle,
                        null));
    }

    public void notifyMediaApproved(String uploaderUserId, String eventId, String eventTitle, String mediaTitle) {
        send(
                uploaderUserId,
                NotificationType.MEDIA_APPROVED,
                new NotificationPayload(
                        eventId,
                        eventTitle,
                        null,
                        null,
                        mediaTitle,
                        null,
                        "Your media has been approved for: " + eventTitle,
                        "/event/" + eventId + "?tab=gallery"));
    }

    public void notifyMediaRejected(String uploaderUserId, String eventId, String eventTitle, String mediaTitle, String rejectionReason) {
        send(
                uploaderUserId,
                NotificationType.MEDIA_REJECTED,
                new NotificationPayload(
                        eventId,
                        eventTitle,
                        null,
                        null,
                        mediaTitle,
                        rejectionReason,
                        "Your media was rejected for: " + eventTitle,
                        "/event/" + eventId + "?tab=gallery"));
    }

    public void notifyAccessRejected(String requesterUserId, String eventId, String eventTitle, String rejectionReason) {
        send(
                requesterUserId,
                NotificationType.ACCESS_REQUEST_REJECTED,
                new NotificationPayload(
                        eventId,
                        eventTitle,
                        null,
                        null,
                        null,
                        rejectionReason,
                        "Your access request to " + eventTitle + " was rejected",
                        "/event/" + eventId));
    }

    public void notifyAccessRevoked(String requesterUserId, String eventId, String eventTitle) {
        send(
                requesterUserId,
                NotificationType.ACCESS_REVOKED,
                new NotificationPayload(
                        eventId,
                        eventTitle,
                        null,
                        null,
                        null,
                        null,
                        "Your access to has been revoked: " + eventTitle,
                        null));
    }

    public void send(String recipientUserId, NotificationType type, NotificationPayload payload) {
        try {
            log.info("Sending {} notification to {}", type.name(), recipientUserId);
            NotificationRequest request = new NotificationRequest(recipientUserId, type.name(), payload);
            notificationServiceClient.sendNotification(request);
            log.info("{} notification sent successfully", type.name());
        } catch (Exception e) {
            log.warn("Failed to send {} notification: {}", type.name(), e.getMessage());
        }
    }
}
