package com.example.Event.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceClient {

    private final RestTemplate restTemplate;

    @Value("${notification-service.url:http://localhost:8085}")
    private String notificationServiceUrl;

    /**
     * Send notification when user is added as collaborator
     */
    public void notifyCollaboratorAdded(String collaboratorUserId, String eventId, String eventTitle, String addedByUserEmail) {
        try {
            log.info("🔔 [Notification Client] Sending collaborator added notification to: {}", collaboratorUserId);

            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", eventId);
            payload.put("eventTitle", eventTitle);
            payload.put("addedByUserEmail", addedByUserEmail);
            payload.put("message", "You've been added as a collaborator to: " + eventTitle);
            payload.put("actionUrl", "/event/" + eventId);

            Map<String, Object> request = new HashMap<>();
            request.put("recipientUserId", collaboratorUserId);
            request.put("type", "COLLABORATOR_ADDED");
            request.put("payload", payload);

            restTemplate.postForObject(
                    notificationServiceUrl + "/api/v1/notifications/internal/notifications",
                    request,
                    String.class
            );

            log.info("✅ [Notification Client] Collaborator added notification sent successfully");
        } catch (Exception e) {
            log.warn("⚠️ [Notification Client] Failed to send collaborator added notification: {}", e.getMessage());
        }
    }

    /**
     * Send notification when access request is received
     */
    public void notifyAccessRequestReceived(String eventOwnerId, String eventId, String eventTitle, String requesterEmail) {
        try {
            log.info("🔔 [Notification Client] Sending access request notification to owner: {}", eventOwnerId);

            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", eventId);
            payload.put("eventTitle", eventTitle);
            payload.put("requesterEmail", requesterEmail);
            payload.put("message", "New access request for: " + eventTitle);
            payload.put("actionUrl", "/event/" + eventId + "?tab=access-requests");

            Map<String, Object> request = new HashMap<>();
            request.put("recipientUserId", eventOwnerId);
            request.put("type", "ACCESS_REQUEST_RECEIVED");
            request.put("payload", payload);

            restTemplate.postForObject(
                    notificationServiceUrl + "/api/v1/notifications/internal/notifications",
                    request,
                    String.class
            );

            log.info("✅ [Notification Client] Access request notification sent successfully");
        } catch (Exception e) {
            log.warn("⚠️ [Notification Client] Failed to send access request notification: {}", e.getMessage());
        }
    }

    /**
     * Send notification when access request is approved
     */
    public void notifyAccessApproved(String requesterUserId, String eventId, String eventTitle) {
        try {
            log.info("🔔 [Notification Client] Sending access approved notification to: {}", requesterUserId);

            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", eventId);
            payload.put("eventTitle", eventTitle);
            payload.put("message", "Your access request has been approved for: " + eventTitle);
            payload.put("actionUrl", "/event/" + eventId);

            Map<String, Object> request = new HashMap<>();
            request.put("recipientUserId", requesterUserId);
            request.put("type", "ACCESS_REQUEST_APPROVED");
            request.put("payload", payload);

            restTemplate.postForObject(
                    notificationServiceUrl + "/api/v1/notifications/internal/notifications",
                    request,
                    String.class
            );

            log.info("✅ [Notification Client] Access approved notification sent successfully");
        } catch (Exception e) {
            log.warn("⚠️ [Notification Client] Failed to send access approved notification: {}", e.getMessage());
        }
    }

    /**
     * Send notification when collaborator is removed
     */
    public void notifyCollaboratorRemoved(String collaboratorUserId, String eventId, String eventTitle) {
        try {
            log.info("🔔 [Notification Client] Sending collaborator removed notification to: {}", collaboratorUserId);

            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", eventId);
            payload.put("eventTitle", eventTitle);
            payload.put("message", "You have been removed from: " + eventTitle);

            Map<String, Object> request = new HashMap<>();
            request.put("recipientUserId", collaboratorUserId);
            request.put("type", "COLLABORATOR_REMOVED");
            request.put("payload", payload);

            restTemplate.postForObject(
                    notificationServiceUrl + "/api/v1/notifications/internal/notifications",
                    request,
                    String.class
            );

            log.info("✅ [Notification Client] Collaborator removed notification sent successfully");
        } catch (Exception e) {
            log.warn("⚠️ [Notification Client] Failed to send collaborator removed notification: {}", e.getMessage());
        }
    }

    /**
     * Send notification when media is approved
     */
    public void notifyMediaApproved(String uploaderUserId, String eventId, String eventTitle, String mediaTitle) {
        try {
            log.info("🔔 [Notification Client] Sending media approved notification to: {}", uploaderUserId);

            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", eventId);
            payload.put("eventTitle", eventTitle);
            payload.put("mediaTitle", mediaTitle);
            payload.put("message", "Your media has been approved for: " + eventTitle);
            payload.put("actionUrl", "/event/" + eventId + "?tab=gallery");

            Map<String, Object> request = new HashMap<>();
            request.put("recipientUserId", uploaderUserId);
            request.put("type", "MEDIA_APPROVED");
            request.put("payload", payload);

            restTemplate.postForObject(
                    notificationServiceUrl + "/api/v1/notifications/internal/notifications",
                    request,
                    String.class
            );

            log.info("✅ [Notification Client] Media approved notification sent successfully");
        } catch (Exception e) {
            log.warn("⚠️ [Notification Client] Failed to send media approved notification: {}", e.getMessage());
        }
    }

    /**
     * Send notification when media is rejected
     */
    public void notifyMediaRejected(String uploaderUserId, String eventId, String eventTitle, String mediaTitle, String rejectionReason) {
        try {
            log.info("🔔 [Notification Client] Sending media rejected notification to: {}", uploaderUserId);

            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", eventId);
            payload.put("eventTitle", eventTitle);
            payload.put("mediaTitle", mediaTitle);
            payload.put("rejectionReason", rejectionReason);
            payload.put("message", "Your media was rejected for: " + eventTitle);
            payload.put("actionUrl", "/event/" + eventId + "?tab=gallery");

            Map<String, Object> request = new HashMap<>();
            request.put("recipientUserId", uploaderUserId);
            request.put("type", "MEDIA_REJECTED");
            request.put("payload", payload);

            restTemplate.postForObject(
                    notificationServiceUrl + "/api/v1/notifications/internal/notifications",
                    request,
                    String.class
            );

            log.info("✅ [Notification Client] Media rejected notification sent successfully");
        } catch (Exception e) {
            log.warn("⚠️ [Notification Client] Failed to send media rejected notification: {}", e.getMessage());
        }
    }

    /**
     * Send notification when access request is rejected
     */
    public void notifyAccessRejected(String requesterUserId, String eventId, String eventTitle, String rejectionReason) {
        try {
            log.info("🔔 [Notification Client] Sending access rejected notification to: {}", requesterUserId);

            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", eventId);
            payload.put("eventTitle", eventTitle);
            payload.put("rejectionReason", rejectionReason);
            payload.put("message", "Your access request to " + eventTitle + " was rejected");
            payload.put("actionUrl", "/event/" + eventId);

            Map<String, Object> request = new HashMap<>();
            request.put("recipientUserId", requesterUserId);
            request.put("type", "ACCESS_REQUEST_REJECTED");
            request.put("payload", payload);

            restTemplate.postForObject(
                    notificationServiceUrl + "/api/v1/notifications/internal/notifications",
                    request,
                    String.class
            );

            log.info("✅ [Notification Client] Access rejected notification sent successfully");
        } catch (Exception e) {
            log.warn("⚠️ [Notification Client] Failed to send access rejected notification: {}", e.getMessage());
        }
    }

    /**
     * Send notification when access is revoked
     */
    public void notifyAccessRevoked(String userUserId, String eventId, String eventTitle) {
        try {
            log.info("🔔 [Notification Client] Sending access revoked notification to: {}", userUserId);

            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", eventId);
            payload.put("eventTitle", eventTitle);
            payload.put("message", "Your access to has been revoked: " + eventTitle);

            Map<String, Object> request = new HashMap<>();
            request.put("recipientUserId", userUserId);
            request.put("type", "ACCESS_REVOKED");
            request.put("payload", payload);

            restTemplate.postForObject(
                    notificationServiceUrl + "/api/v1/notifications/internal/notifications",
                    request,
                    String.class
            );

            log.info("✅ [Notification Client] Access revoked notification sent successfully");
        } catch (Exception e) {
            log.warn("⚠️ [Notification Client] Failed to send access revoked notification: {}", e.getMessage());
        }
    }
}
