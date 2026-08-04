package com.example.Event.application.service.impl;

import com.example.Event.api.dto.request.AddCollaboratorRequest;
import com.example.Event.api.dto.request.CollaboratorPermissionsRequest;
import com.example.Event.api.dto.request.UpdateCollaboratorPermissionsRequest;
import com.example.Event.api.dto.response.CollaboratorMutationResponse;
import com.example.Event.api.dto.response.CollaboratorResponse;
import com.example.Event.api.dto.response.CollaboratorsResponse;
import com.example.Event.api.dto.response.MessageResponse;
import com.example.Event.application.service.interfaces.IEventCollaboratorService;
import com.example.Event.infrastructure.client.EmailServiceClient;
import com.example.Event.infrastructure.client.NotificationServiceClient;
import com.example.Event.infrastructure.client.UserServiceClient;
import com.example.Event.infrastructure.persistence.document.EventDocument;
import com.example.Event.infrastructure.persistence.repository.EventRepository;
import com.example.Event.shared.exception.AuthenticationRequiredException;
import com.example.Event.shared.exception.DomainValidationException;
import com.example.Event.shared.exception.ForbiddenOperationException;
import com.example.Event.shared.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventCollaboratorServiceImpl implements IEventCollaboratorService {

    private final EventRepository eventRepository;
    private final NotificationServiceClient notificationServiceClient;
    private final EmailServiceClient emailServiceClient;
    private final UserServiceClient userServiceClient;

    @Override
    public CollaboratorMutationResponse addCollaborator(
            String eventId,
            AddCollaboratorRequest request,
            HttpServletRequest httpRequest) {
        String ownerUserId = requireUserId(httpRequest, "User ID not found");
        String collaboratorId = resolveCollaboratorUserId(request.userEmail());
        CollaboratorPermissionsRequest permissions = request.resolvedPermissions();

        EventDocument.EventCollaborator collaborator = addCollaboratorDocument(
                eventId,
                collaboratorId,
                permissions.uploadMediaOrFalse(),
                permissions.reviewMediaOrFalse(),
                permissions.reviewAccessRequestsOrFalse(),
                permissions.directUploadOrFalse(),
                permissions.deleteMediaOrFalse(),
                permissions.editEventDetailsOrFalse(),
                ownerUserId);

        return new CollaboratorMutationResponse(
                enrichCollaborator(collaborator, request.userEmail()),
                "Collaborator added successfully");
    }

    @Override
    public CollaboratorsResponse getCollaborators(String eventId) {
        List<CollaboratorResponse> collaborators = new ArrayList<>();
        for (EventDocument.EventCollaborator collaborator : getCollaboratorDocuments(eventId)) {
            if (collaborator.getUserId() == null || collaborator.getUserId().isBlank()) {
                log.warn("Found collaborator with empty userId in event {}", eventId);
                continue;
            }
            collaborators.add(enrichCollaborator(collaborator, null));
        }
        return new CollaboratorsResponse(eventId, collaborators);
    }

    @Override
    public MessageResponse removeCollaborator(
            String eventId,
            String collaboratorUserId,
            HttpServletRequest httpRequest) {
        String ownerUserId = requireUserId(httpRequest, "User ID not found");
        if (collaboratorUserId == null || collaboratorUserId.isBlank() || "null".equals(collaboratorUserId)) {
            throw new DomainValidationException("Invalid collaborator ID. Cannot remove corrupted entries.");
        }

        removeCollaboratorDocument(eventId, collaboratorUserId, ownerUserId);
        return new MessageResponse("Collaborator removed successfully");
    }

    @Override
    public CollaboratorMutationResponse updateCollaboratorPermissions(
            String eventId,
            String collaboratorUserId,
            UpdateCollaboratorPermissionsRequest request,
            HttpServletRequest httpRequest) {
        String ownerUserId = requireUserId(httpRequest, "User ID not found");
        CollaboratorPermissionsRequest permissions = request.permissions();

        EventDocument.EventCollaborator updated = updateCollaboratorDocument(
                eventId,
                collaboratorUserId,
                permissions.canUploadMedia(),
                permissions.canReviewMedia(),
                permissions.canReviewAccessRequests(),
                permissions.canDirectUpload(),
                permissions.canDeleteMedia(),
                permissions.canEditEventDetails(),
                ownerUserId);

        return new CollaboratorMutationResponse(
                toResponse(updated, null, null),
                "Collaborator permissions updated successfully");
    }

    @Override
    public boolean hasPermission(String eventId, String userId, String permission) {
        EventDocument event = findEvent(eventId);
        if (userId == null || userId.isBlank()) {
            return false;
        }

        if (event.getOwnerUserId().equals(userId)) {
            return true;
        }

        if (event.getCollaborators() == null) {
            return false;
        }

        return event.getCollaborators().stream()
                .filter(collaborator -> userId.equals(collaborator.getUserId()))
                .anyMatch(collaborator -> hasPermission(collaborator, permission));
    }

    @Override
    public boolean isCollaborator(String eventId, String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }

        EventDocument event = findEvent(eventId);
        if (event.getCollaborators() == null) {
            return false;
        }

        return event.getCollaborators().stream()
                .anyMatch(collaborator -> userId.equals(collaborator.getUserId()));
    }

    @Override
    public List<EventDocument.EventCollaborator> getCollaboratorDocuments(String eventId) {
        EventDocument event = findEvent(eventId);
        return event.getCollaborators() != null ? event.getCollaborators() : Collections.emptyList();
    }

    private EventDocument.EventCollaborator addCollaboratorDocument(
            String eventId,
            String collaboratorId,
            Boolean canUploadMedia,
            Boolean canReviewMedia,
            Boolean canReviewAccessRequests,
            Boolean canDirectUpload,
            Boolean canDeleteMedia,
            Boolean canEditEventDetails,
            String ownerUserId) {
        EventDocument event = findEvent(eventId);
        validateOwner(event, ownerUserId);

        if (event.getCollaborators() != null) {
            boolean alreadyExists = event.getCollaborators().stream()
                    .anyMatch(collaborator -> collaboratorId.equals(collaborator.getUserId()));
            if (alreadyExists) {
                throw new DomainValidationException("User is already a collaborator on this event");
            }
        }

        EventDocument.EventCollaborator newCollaborator = EventDocument.EventCollaborator.builder()
                .userId(collaboratorId)
                .canUploadMedia(Boolean.TRUE.equals(canUploadMedia))
                .canReviewMedia(Boolean.TRUE.equals(canReviewMedia))
                .canReviewAccessRequests(Boolean.TRUE.equals(canReviewAccessRequests))
                .canDirectUpload(Boolean.TRUE.equals(canDirectUpload))
                .canDeleteMedia(Boolean.TRUE.equals(canDeleteMedia))
                .canEditEventDetails(Boolean.TRUE.equals(canEditEventDetails))
                .addedAt(Instant.now())
                .addedByUserId(ownerUserId)
                .build();

        if (event.getCollaborators() == null) {
            event.setCollaborators(new ArrayList<>());
        }
        event.getCollaborators().add(newCollaborator);
        eventRepository.save(event);

        notifyCollaboratorAdded(newCollaborator, event, ownerUserId);
        return newCollaborator;
    }

    private void removeCollaboratorDocument(String eventId, String collaboratorId, String ownerUserId) {
        EventDocument event = findEvent(eventId);
        validateOwner(event, ownerUserId);

        String eventTitle = event.getTitle();
        notifyCollaboratorRemoved(collaboratorId, eventId, eventTitle);

        if (event.getCollaborators() != null) {
            event.getCollaborators().removeIf(collaborator ->
                    collaborator.getUserId() == null || collaboratorId.equals(collaborator.getUserId()));
            eventRepository.save(event);
        }
    }

    private EventDocument.EventCollaborator updateCollaboratorDocument(
            String eventId,
            String collaboratorId,
            Boolean canUploadMedia,
            Boolean canReviewMedia,
            Boolean canReviewAccessRequests,
            Boolean canDirectUpload,
            Boolean canDeleteMedia,
            Boolean canEditEventDetails,
            String ownerUserId) {
        EventDocument event = findEvent(eventId);
        validateOwner(event, ownerUserId);

        Optional<EventDocument.EventCollaborator> collaborator = event.getCollaborators() == null
                ? Optional.empty()
                : event.getCollaborators().stream()
                        .filter(candidate -> collaboratorId.equals(candidate.getUserId()))
                        .findFirst();

        if (collaborator.isEmpty()) {
            throw new ResourceNotFoundException("Collaborator not found: " + collaboratorId);
        }

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

    private String resolveCollaboratorUserId(String userEmail) {
        try {
            String collaboratorId = userServiceClient.getUserIdByEmail(userEmail);
            if (collaboratorId == null || collaboratorId.isBlank()) {
                throw new DomainValidationException("User ID not found for email: " + userEmail);
            }
            return collaboratorId;
        } catch (DomainValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new DomainValidationException("Failed to find user: " + e.getMessage(), e);
        }
    }

    private CollaboratorResponse enrichCollaborator(EventDocument.EventCollaborator collaborator, String knownEmail) {
        String displayName;
        try {
            displayName = userServiceClient.getUserDisplayName(collaborator.getUserId());
        } catch (Exception e) {
            log.warn("Could not fetch displayName for {}: {}", collaborator.getUserId(), e.getMessage());
            displayName = collaborator.getUserId();
        }
        return toResponse(collaborator, knownEmail, displayName);
    }

    private CollaboratorResponse toResponse(EventDocument.EventCollaborator collaborator, String email, String displayName) {
        return new CollaboratorResponse(
                collaborator.getUserId(),
                collaborator.getCanUploadMedia(),
                collaborator.getCanReviewMedia(),
                collaborator.getCanReviewAccessRequests(),
                collaborator.getCanDirectUpload(),
                collaborator.getCanDeleteMedia(),
                collaborator.getCanEditEventDetails(),
                collaborator.getAddedAt(),
                collaborator.getAddedByUserId(),
                email,
                displayName);
    }

    private void notifyCollaboratorAdded(
            EventDocument.EventCollaborator collaborator,
            EventDocument event,
            String ownerUserId) {
        try {
            String ownerEmail = userServiceClient.getUserEmail(ownerUserId);
            notificationServiceClient.notifyCollaboratorAdded(
                    collaborator.getUserId(),
                    event.getId(),
                    event.getTitle(),
                    ownerEmail);
        } catch (Exception e) {
            log.warn("Failed to send collaborator added notification: {}", e.getMessage());
        }

        try {
            String ownerName = userServiceClient.getUserDisplayName(ownerUserId);
            String collaboratorEmail = userServiceClient.getUserEmail(collaborator.getUserId());
            emailServiceClient.sendCollaboratorAddedEmail(collaboratorEmail, event.getTitle(), ownerName);
        } catch (Exception e) {
            log.warn("Failed to send collaborator added email: {}", e.getMessage());
        }
    }

    private void notifyCollaboratorRemoved(String collaboratorId, String eventId, String eventTitle) {
        try {
            notificationServiceClient.notifyCollaboratorRemoved(collaboratorId, eventId, eventTitle);
        } catch (Exception e) {
            log.warn("Failed to send collaborator removed notification: {}", e.getMessage());
        }

        try {
            String collaboratorEmail = userServiceClient.getUserEmail(collaboratorId);
            emailServiceClient.sendCollaboratorRemovedEmail(collaboratorEmail, eventTitle);
        } catch (Exception e) {
            log.warn("Failed to send collaborator removed email: {}", e.getMessage());
        }
    }

    private boolean hasPermission(EventDocument.EventCollaborator collaborator, String permission) {
        return switch (permission) {
            case "canUploadMedia" -> Boolean.TRUE.equals(collaborator.getCanUploadMedia());
            case "canReviewMedia" -> Boolean.TRUE.equals(collaborator.getCanReviewMedia());
            case "canReviewAccessRequests" -> Boolean.TRUE.equals(collaborator.getCanReviewAccessRequests());
            case "canDirectUpload" -> Boolean.TRUE.equals(collaborator.getCanDirectUpload());
            case "canDeleteMedia" -> Boolean.TRUE.equals(collaborator.getCanDeleteMedia());
            case "canEditEventDetails" -> Boolean.TRUE.equals(collaborator.getCanEditEventDetails());
            default -> false;
        };
    }

    private void validateOwner(EventDocument event, String ownerUserId) {
        if (!event.getOwnerUserId().equals(ownerUserId)) {
            throw new ForbiddenOperationException("Only event owner can manage collaborators");
        }
    }

    private EventDocument findEvent(String eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
    }

    private String requireUserId(HttpServletRequest request, String message) {
        String userId = request == null ? null : request.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            throw new AuthenticationRequiredException(message);
        }
        return userId;
    }
}
