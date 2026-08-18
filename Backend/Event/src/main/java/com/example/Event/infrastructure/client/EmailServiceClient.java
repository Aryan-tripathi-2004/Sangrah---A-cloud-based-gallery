package com.example.Event.infrastructure.client;

import com.example.Event.infrastructure.client.dto.EmailNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailServiceClient {

    private final RestTemplate restTemplate;

    @Value("${email-service.url:http://localhost:8086}")
    private String emailServiceUrl;

    public void sendCollaboratorAddedEmail(String collaboratorEmail, String eventTitle, String ownerName) {
        send(new EmailNotificationRequest(
                collaboratorEmail,
                "collaborator-added",
                "Added as Collaborator: " + eventTitle,
                String.format(
                        "You have been added as a collaborator to the event '%s' by %s.",
                        eventTitle,
                        ownerName != null ? ownerName : "the event owner")));
    }

    public void sendAccessRequestEmail(String ownerEmail, String eventTitle, String requesterEmail) {
        send(new EmailNotificationRequest(
                ownerEmail,
                "access-request",
                "New Access Request: " + eventTitle,
                String.format(
                        "User %s has requested access to your event '%s'. Please review the request in the application.",
                        requesterEmail,
                        eventTitle)));
    }

    public void sendAccessApprovedEmail(String requesterEmail, String eventTitle) {
        send(new EmailNotificationRequest(
                requesterEmail,
                "access-approved",
                "Access Approved: " + eventTitle,
                "Your access request to the event '" + eventTitle + "' has been approved. You can now access the event and its resources."));
    }

    public void sendMediaApprovedEmail(String uploaderEmail, String eventTitle, String mediaTitle) {
        send(new EmailNotificationRequest(
                uploaderEmail,
                "media-approved",
                "Media Approved: " + mediaTitle,
                String.format(
                        "Your media '%s' for event '%s' has been approved and is now visible in the gallery.",
                        mediaTitle,
                        eventTitle)));
    }

    public void sendMediaRejectedEmail(String uploaderEmail, String eventTitle, String mediaTitle, String rejectionReason) {
        String resolvedReason = rejectionReason == null || rejectionReason.isBlank()
                ? "No specific reason was provided by the event owner"
                : rejectionReason.trim();

        send(new EmailNotificationRequest(
                uploaderEmail,
                "media-rejected",
                "Media Rejected: " + mediaTitle,
                String.format(
                        "Your media '%s' for event '%s' was not approved. Reason: %s",
                        mediaTitle,
                        eventTitle,
                        resolvedReason)));
    }

    public void sendCollaboratorRemovedEmail(String collaboratorEmail, String eventTitle) {
        send(new EmailNotificationRequest(
                collaboratorEmail,
                "collaborator-removed",
                "Removed from Collaborator Access: " + eventTitle,
                String.format(
                        "You have been removed as a collaborator from the event '%s'. You will no longer be able to access collaborator-only actions or content for this event. If you believe this was a mistake, please contact the event owner.",
                        eventTitle)));
    }

    public void sendAccessRejectedEmail(String requesterEmail, String eventTitle, String rejectionReason) {
        send(new EmailNotificationRequest(
                requesterEmail,
                "access-rejected",
                "Access Request Denied: " + eventTitle,
                "Your access request to the event '" + eventTitle + "' has been denied. Reason: "
                        + (rejectionReason != null ? rejectionReason : "Request denied")));
    }

    public void sendAccessRevokedEmail(String userEmail, String eventTitle) {
        send(new EmailNotificationRequest(
                userEmail,
                "access-revoked",
                "Access Revoked: " + eventTitle,
                "Your access to the event '" + eventTitle + "' has been revoked. You will no longer be able to access this event and its resources."));
    }

    private void send(EmailNotificationRequest payload) {
        try {
            log.info("Sending {} email to {}", payload.type(), payload.userEmail());
            restTemplate.postForObject(
                    emailServiceUrl + "/api/v1/email/notify",
                    payload,
                    String.class);
            log.info("{} email sent successfully to {}", payload.type(), payload.userEmail());
        } catch (Exception e) {
            log.warn("Failed to send {} email: {}", payload.type(), e.getMessage());
        }
    }
}
