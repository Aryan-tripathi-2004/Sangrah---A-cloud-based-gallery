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
public class EmailServiceClient {

    private final RestTemplate restTemplate;

    @Value("${email-service.url:http://localhost:8086}")
    private String emailServiceUrl;

    /**
     * Send email when user is added as collaborator
     */
    public void sendCollaboratorAddedEmail(String collaboratorEmail, String eventTitle, String ownerName) {
        try {
            log.info("📧 [Email Client] Sending collaborator added email to: {}", collaboratorEmail);

            Map<String, Object> payload = new HashMap<>();
            payload.put("userEmail", collaboratorEmail);
            payload.put("type", "collaborator-added");
            payload.put("subject", "Added as Collaborator: " + eventTitle);
            payload.put("body", String.format("You have been added as a collaborator to the event '%s' by %s.", 
                    eventTitle, ownerName != null ? ownerName : "the event owner"));

            restTemplate.postForObject(
                    emailServiceUrl + "/api/v1/email/notify",
                    payload,
                    String.class
            );

            log.info("✅ [Email Client] Collaborator added email sent successfully to: {}", collaboratorEmail);
        } catch (Exception e) {
            log.warn("⚠️ [Email Client] Failed to send collaborator added email: {}", e.getMessage());
        }
    }

    /**
     * Send email when access request is received
     */
    public void sendAccessRequestEmail(String ownerEmail, String eventTitle, String requesterEmail) {
        try {
            log.info("📧 [Email Client] Sending access request email to: {}", ownerEmail);

            Map<String, Object> payload = new HashMap<>();
            payload.put("userEmail", ownerEmail);
            payload.put("type", "access-request");
            payload.put("subject", "New Access Request: " + eventTitle);
            payload.put("body", String.format("User %s has requested access to your event '%s'. Please review the request in the application.", 
                    requesterEmail, eventTitle));

            restTemplate.postForObject(
                    emailServiceUrl + "/api/v1/email/notify",
                    payload,
                    String.class
            );

            log.info("✅ [Email Client] Access request email sent successfully to: {}", ownerEmail);
        } catch (Exception e) {
            log.warn("⚠️ [Email Client] Failed to send access request email: {}", e.getMessage());
        }
    }

    /**
     * Send email when access request is approved
     */
    public void sendAccessApprovedEmail(String requesterEmail, String eventTitle) {
        try {
            log.info("📧 [Email Client] Sending access approved email to: {}", requesterEmail);

            Map<String, Object> payload = new HashMap<>();
            payload.put("userEmail", requesterEmail);
            payload.put("type", "access-approved");
            payload.put("subject", "Access Approved: " + eventTitle);
            payload.put("body", "Your access request to the event '" + eventTitle + "' has been approved. You can now access the event and its resources.");

            restTemplate.postForObject(
                    emailServiceUrl + "/api/v1/email/notify",
                    payload,
                    String.class
            );

            log.info("✅ [Email Client] Access approved email sent successfully to: {}", requesterEmail);
        } catch (Exception e) {
            log.warn("⚠️ [Email Client] Failed to send access approved email: {}", e.getMessage());
        }
    }

    /**
     * Send email when media is approved
     */
    public void sendMediaApprovedEmail(String uploaderEmail, String eventTitle, String mediaTitle) {
        try {
            log.info("📧 [Email Client] Sending media approved email to: {}", uploaderEmail);

            Map<String, Object> payload = new HashMap<>();
            payload.put("userEmail", uploaderEmail);
            payload.put("type", "media-approved");
            payload.put("subject", "Media Approved: " + mediaTitle);
            payload.put("body", String.format("Your media '%s' for event '%s' has been approved and is now visible in the gallery.", 
                    mediaTitle, eventTitle));

            restTemplate.postForObject(
                    emailServiceUrl + "/api/v1/email/notify",
                    payload,
                    String.class
            );

            log.info("✅ [Email Client] Media approved email sent successfully to: {}", uploaderEmail);
        } catch (Exception e) {
            log.warn("⚠️ [Email Client] Failed to send media approved email: {}", e.getMessage());
        }
    }

    /**
     * Send email when media is rejected
     */
    public void sendMediaRejectedEmail(String uploaderEmail, String eventTitle, String mediaTitle, String rejectionReason) {
        try {
            log.info("📧 [Email Client] Sending media rejected email to: {}", uploaderEmail);

            Map<String, Object> payload = new HashMap<>();
            payload.put("userEmail", uploaderEmail);
            payload.put("type", "media-rejected");
            payload.put("subject", "Media Rejected: " + mediaTitle);
            payload.put("body", String.format("Your media '%s' for event '%s' was not approved. Reason: %s", 
                    mediaTitle, eventTitle, rejectionReason != null ? rejectionReason : "Does not meet community guidelines"));

            restTemplate.postForObject(
                    emailServiceUrl + "/api/v1/email/notify",
                    payload,
                    String.class
            );

            log.info("✅ [Email Client] Media rejected email sent successfully to: {}", uploaderEmail);
        } catch (Exception e) {
            log.warn("⚠️ [Email Client] Failed to send media rejected email: {}", e.getMessage());
        }
    }

    /**
     * Send email when collaborator is removed
     */
    public void sendCollaboratorRemovedEmail(String collaboratorEmail, String eventTitle) {
        try {
            log.info("📧 [Email Client] Sending collaborator removed email to: {}", collaboratorEmail);

            Map<String, Object> payload = new HashMap<>();
            payload.put("userEmail", collaboratorEmail);
            payload.put("type", "collaborator-removed");
            payload.put("eventTitle", eventTitle);

            restTemplate.postForObject(
                    emailServiceUrl + "/api/v1/email/notify",
                    payload,
                    String.class
            );

            log.info("✅ [Email Client] Collaborator removed email sent successfully to: {}", collaboratorEmail);
        } catch (Exception e) {
            log.warn("⚠️ [Email Client] Failed to send collaborator removed email: {}", e.getMessage());
        }
    }

    /**
     * Send email when access request is rejected
     */
    public void sendAccessRejectedEmail(String requesterEmail, String eventTitle, String rejectionReason) {
        try {
            log.info("📧 [Email Client] Sending access rejected email to: {}", requesterEmail);

            Map<String, Object> payload = new HashMap<>();
            payload.put("userEmail", requesterEmail);
            payload.put("type", "access-rejected");
            payload.put("subject", "Access Request Denied: " + eventTitle);
            payload.put("body", "Your access request to the event '" + eventTitle + "' has been denied. Reason: " + (rejectionReason != null ? rejectionReason : "Request denied"));

            restTemplate.postForObject(
                    emailServiceUrl + "/api/v1/email/notify",
                    payload,
                    String.class
            );

            log.info("✅ [Email Client] Access rejected email sent successfully to: {}", requesterEmail);
        } catch (Exception e) {
            log.warn("⚠️ [Email Client] Failed to send access rejected email: {}", e.getMessage());
        }
    }

    /**
     * Send email when access is revoked
     */
    public void sendAccessRevokedEmail(String userEmail, String eventTitle) {
        try {
            log.info("📧 [Email Client] Sending access revoked email to: {}", userEmail);

            Map<String, Object> payload = new HashMap<>();
            payload.put("userEmail", userEmail);
            payload.put("type", "access-revoked");
            payload.put("subject", "Access Revoked: " + eventTitle);
            payload.put("body", "Your access to the event '" + eventTitle + "' has been revoked. You will no longer be able to access this event and its resources.");

            restTemplate.postForObject(
                    emailServiceUrl + "/api/v1/email/notify",
                    payload,
                    String.class
            );

            log.info("✅ [Email Client] Access revoked email sent successfully to: {}", userEmail);
        } catch (Exception e) {
            log.warn("⚠️ [Email Client] Failed to send access revoked email: {}", e.getMessage());
        }
    }
}
