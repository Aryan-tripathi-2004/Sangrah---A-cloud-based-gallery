package com.example.Event.infrastructure.client;

import com.example.Event.infrastructure.client.dto.EmailNotificationRequest;
import com.example.Event.shared.enums.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "email-service", url = "${email-service.url:http://localhost:8086}")
public interface EmailServiceClient {

    Logger log = LoggerFactory.getLogger(EmailServiceClient.class);

    @PostMapping("/api/v1/email")
    String sendEmail(@RequestBody EmailNotificationRequest request);

    default void sendCollaboratorAddedEmail(String collaboratorEmail, String eventTitle, String ownerName) {
        send(new EmailNotificationRequest(
                collaboratorEmail,
                NotificationType.COLLABORATOR_ADDED,
                "Added as Collaborator: " + eventTitle,
                String.format(
                        "You have been added as a collaborator to the event '%s' by %s.",
                        eventTitle,
                        ownerName != null ? ownerName : "the event owner")));
    }

    default void sendAccessRequestEmail(String ownerEmail, String eventTitle, String requesterEmail) {
        send(new EmailNotificationRequest(
                ownerEmail,
                NotificationType.ACCESS_REQUEST,
                "New Access Request: " + eventTitle,
                String.format(
                        "User %s has requested access to your event '%s'. Please review the request in the application.",
                        requesterEmail,
                        eventTitle)));
    }

    default void sendAccessApprovedEmail(String requesterEmail, String eventTitle) {
        send(new EmailNotificationRequest(
                requesterEmail,
                NotificationType.ACCESS_APPROVED,
                "Access Approved: " + eventTitle,
                "Your access request to the event '" + eventTitle + "' has been approved. You can now access the event and its resources."));
    }

    default void sendMediaApprovedEmail(String uploaderEmail, String eventTitle, String mediaTitle) {
        send(new EmailNotificationRequest(
                uploaderEmail,
                NotificationType.MEDIA_APPROVED,
                "Media Approved: " + mediaTitle,
                String.format(
                        "Your media '%s' for event '%s' has been approved and is now visible in the gallery.",
                        mediaTitle,
                        eventTitle)));
    }

    default void sendMediaRejectedEmail(String uploaderEmail, String eventTitle, String mediaTitle, String rejectionReason) {
        String resolvedReason = rejectionReason == null || rejectionReason.isBlank()
                ? "No specific reason was provided by the event owner"
                : rejectionReason.trim();

        send(new EmailNotificationRequest(
                uploaderEmail,
                NotificationType.MEDIA_REJECTED,
                "Media Rejected: " + mediaTitle,
                String.format(
                        "Your media '%s' for event '%s' was not approved. Reason: %s",
                        mediaTitle,
                        eventTitle,
                        resolvedReason)));
    }

    default void sendCollaboratorRemovedEmail(String collaboratorEmail, String eventTitle) {
        send(new EmailNotificationRequest(
                collaboratorEmail,
                NotificationType.NOTIFICATION,
                "Removed from Collaborator Access: " + eventTitle,
                String.format(
                        "You have been removed as a collaborator from the event '%s'. You will no longer be able to access collaborator-only actions or content for this event. If you believe this was a mistake, please contact the event owner.",
                        eventTitle)));
    }

    default void sendAccessRejectedEmail(String requesterEmail, String eventTitle, String rejectionReason) {
        send(new EmailNotificationRequest(
                requesterEmail,
                NotificationType.ACCESS_REJECTED,
                "Access Request Denied: " + eventTitle,
                "Your access request to the event '" + eventTitle + "' has been denied. Reason: "
                        + (rejectionReason != null ? rejectionReason : "Request denied")));
    }

    default void sendAccessRevokedEmail(String userEmail, String eventTitle) {
        send(new EmailNotificationRequest(
                userEmail,
                NotificationType.ACCESS_REVOKED,
                "Access Revoked: " + eventTitle,
                "Your access to the event '" + eventTitle + "' has been revoked. You will no longer be able to access this event and its resources."));
    }

    default void send(EmailNotificationRequest payload) {
        try {
            log.info("Sending {} email to {}", payload.emailType(), payload.userEmail());
            sendEmail(payload);
            log.info("{} email sent successfully to {}", payload.emailType(), payload.userEmail());
        } catch (Exception e) {
            log.warn("Failed to send {} email: {}", payload.emailType(), e.getMessage());
        }
    }
}
