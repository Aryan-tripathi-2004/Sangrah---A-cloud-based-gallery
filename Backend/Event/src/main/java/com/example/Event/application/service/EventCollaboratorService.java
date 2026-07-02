package com.example.Event.application.service;

import com.example.Event.infrastructure.client.EmailServiceClient;
import com.example.Event.infrastructure.client.NotificationServiceClient;
import com.example.Event.infrastructure.client.UserServiceClient;
import com.example.Event.infrastructure.persistence.document.EventDocument;
import com.example.Event.infrastructure.persistence.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventCollaboratorService {

    private final EventRepository eventRepository;
    private final NotificationServiceClient notificationServiceClient;
    private final EmailServiceClient emailServiceClient;
    private final UserServiceClient userServiceClient;

    /**
     * Add a collaborator to an event with specific permissions
     */
    public EventDocument.EventCollaborator addCollaborator(
            String eventId,
            String collaboratorId,
            Boolean canUploadMedia,
            Boolean canReviewMedia,
            Boolean canReviewAccessRequests,
            Boolean canDirectUpload,
            Boolean canDeleteMedia,
            Boolean canEditEventDetails,
            String ownerUserId) {

        EventDocument event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));

        // Verify ownership
        if (!event.getOwnerUserId().equals(ownerUserId)) {
            throw new RuntimeException("Only event owner can manage collaborators");
        }

        // Check if collaborator already exists
        if (event.getCollaborators() != null) {
            boolean alreadyExists = event.getCollaborators().stream()
                    .anyMatch(c -> c.getUserId() != null && c.getUserId().equals(collaboratorId));
            if (alreadyExists) {
                throw new RuntimeException("User is already a collaborator on this event");
            }
        }

        // Create new collaborator with permissions
        EventDocument.EventCollaborator newCollaborator = EventDocument.EventCollaborator.builder()
                .userId(collaboratorId)
                .canUploadMedia(canUploadMedia != null ? canUploadMedia : false)
                .canReviewMedia(canReviewMedia != null ? canReviewMedia : false)
                .canReviewAccessRequests(canReviewAccessRequests != null ? canReviewAccessRequests : false)
                .canDirectUpload(canDirectUpload != null ? canDirectUpload : false)
                .canDeleteMedia(canDeleteMedia != null ? canDeleteMedia : false)
                .canEditEventDetails(canEditEventDetails != null ? canEditEventDetails : false)
                .addedAt(Instant.now())
                .addedByUserId(ownerUserId)
                .build();

        // Add to event's collaborators list
        if (event.getCollaborators() == null) {
            event.setCollaborators(new java.util.ArrayList<>());
        }
        event.getCollaborators().add(newCollaborator);

        eventRepository.save(event);

        try {
            String eventTitle = event.getTitle();
            String ownerEmail = userServiceClient.getUserEmail(ownerUserId);
            notificationServiceClient.notifyCollaboratorAdded(
                    collaboratorId,
                    eventId,
                    eventTitle,
                    ownerEmail
            );
            log.info("✅ Collaborator added notification sent to user: {}", collaboratorId);
        } catch (Exception e) {
            log.warn("⚠️ Failed to send collaborator added notification: {}", e.getMessage());
        }

        try {
            String eventTitle = event.getTitle();
            String ownerName = userServiceClient.getUserDisplayName(ownerUserId);
            String collaboratorEmail = userServiceClient.getUserEmail(collaboratorId);
            emailServiceClient.sendCollaboratorAddedEmail(collaboratorEmail, eventTitle, ownerName);
            log.info("📧 [EventCollaboratorService] Collaborator added email sent to: {}", collaboratorEmail);
        } catch (Exception e) {
            log.warn("⚠️ [EventCollaboratorService] Failed to send collaborator added email: {}", e.getMessage());
        }

        return newCollaborator;
    }

    /**
     * Remove a collaborator from an event
     */
    public void removeCollaborator(String eventId, String collaboratorId, String ownerUserId) {
        EventDocument event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));

        // Verify ownership
        if (!event.getOwnerUserId().equals(ownerUserId)) {
            throw new RuntimeException("Only event owner can manage collaborators");
        }

        String eventTitle = event.getTitle();

        try {
            notificationServiceClient.notifyCollaboratorRemoved(collaboratorId, eventId, eventTitle);
            log.info("✅ Collaborator removed notification sent to user: {}", collaboratorId);
        } catch (Exception e) {
            log.warn("⚠️ Failed to send collaborator removed notification: {}", e.getMessage());
        }

        try {
            String collaboratorEmail = userServiceClient.getUserEmail(collaboratorId);
            emailServiceClient.sendCollaboratorRemovedEmail(collaboratorEmail, eventTitle);
            log.info("📧 [EventCollaboratorService] Collaborator removed email sent to: {}", collaboratorEmail);
        } catch (Exception e) {
            log.warn("⚠️ [EventCollaboratorService] Failed to send collaborator removed email: {}", e.getMessage());
        }

        // Remove collaborator
        if (event.getCollaborators() != null) {
            event.getCollaborators().removeIf(c -> c.getUserId() == null || c.getUserId().equals(collaboratorId) || "null".equals(collaboratorId));
            eventRepository.save(event);
        }
    }

    /**
     * Update collaborator permissions
     */
    public EventDocument.EventCollaborator updateCollaboratorPermissions(
            String eventId,
            String collaboratorId,
            Boolean canUploadMedia,
            Boolean canReviewMedia,
            Boolean canReviewAccessRequests,
            Boolean canDirectUpload,
            Boolean canDeleteMedia,
            Boolean canEditEventDetails,
            String ownerUserId) {

        EventDocument event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));

        // Verify ownership
        if (!event.getOwnerUserId().equals(ownerUserId)) {
            throw new RuntimeException("Only event owner can manage collaborators");
        }

        // Find and update collaborator
        if (event.getCollaborators() != null) {
            Optional<EventDocument.EventCollaborator> collaborator = event.getCollaborators().stream()
                    .filter(c -> c.getUserId() != null && c.getUserId().equals(collaboratorId))
                    .findFirst();

            if (collaborator.isPresent()) {
                EventDocument.EventCollaborator updated = collaborator.get();
                if (canUploadMedia != null) {
                    updated.setCanUploadMedia(canUploadMedia);
                }
                if (canReviewMedia != null) {
                    updated.setCanReviewMedia(canReviewMedia);
                }
                if (canReviewAccessRequests != null) {
                    updated.setCanReviewAccessRequests(canReviewAccessRequests);
                }
                if (canDirectUpload != null) {
                    updated.setCanDirectUpload(canDirectUpload);
                }
                if (canDeleteMedia != null) {
                    updated.setCanDeleteMedia(canDeleteMedia);
                }
                if (canEditEventDetails != null) {
                    updated.setCanEditEventDetails(canEditEventDetails);
                }
                eventRepository.save(event);
                return updated;
            }
        }

        throw new RuntimeException("Collaborator not found: " + collaboratorId);
    }

    /**
     * Get all collaborators for an event
     */
    public List<EventDocument.EventCollaborator> getCollaborators(String eventId) {
        EventDocument event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));
        return event.getCollaborators() != null ? event.getCollaborators() : java.util.Collections.emptyList();
    }

    /**
     * Check if user has specific permission
     */
    public boolean hasPermission(String eventId, String userId, String permission) {
        EventDocument event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));

        // Owner always has all permissions
        if (event.getOwnerUserId().equals(userId)) {
            return true;
        }

        if (event.getCollaborators() == null) {
            return false;
        }

        return event.getCollaborators().stream()
                .filter(c -> c.getUserId() != null && c.getUserId().equals(userId))
                .anyMatch(c -> {
                    switch (permission) {
                        case "canUploadMedia":
                            return c.getCanUploadMedia() != null && c.getCanUploadMedia();
                        case "canReviewMedia":
                            return c.getCanReviewMedia() != null && c.getCanReviewMedia();
                        case "canReviewAccessRequests":
                            return c.getCanReviewAccessRequests() != null && c.getCanReviewAccessRequests();
                        default:
                            return false;
                    }
                });
    }

    /**
     * Check if user is a collaborator
     */
    public boolean isCollaborator(String eventId, String userId) {
        EventDocument event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));

        if (event.getCollaborators() == null) {
            return false;
        }

        return event.getCollaborators().stream()
                .anyMatch(c -> c.getUserId() != null && c.getUserId().equals(userId));
    }
}
