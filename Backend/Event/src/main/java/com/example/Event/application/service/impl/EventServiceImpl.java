package com.example.Event.application.service.impl;

import com.example.Event.api.dto.request.EventRequest;
import com.example.Event.api.dto.request.EventSettingsRequest;
import com.example.Event.api.dto.request.EventUpdateRequest;
import com.example.Event.api.dto.response.CollaboratorResponse;
import com.example.Event.api.dto.response.EventCreateResponse;
import com.example.Event.api.dto.response.EventDeleteResponse;
import com.example.Event.api.dto.response.EventResponse;
import com.example.Event.api.dto.response.EventSettingsResponse;
import com.example.Event.api.dto.response.EventSummaryResponse;
import com.example.Event.api.dto.response.EventUpdateResponse;
import com.example.Event.application.service.interfaces.IEventCollaboratorService;
import com.example.Event.application.service.interfaces.IEventService;
import com.example.Event.infrastructure.client.MediaServiceClient;
import com.example.Event.infrastructure.client.dto.MediaServiceResponse;
import com.example.Event.infrastructure.mapper.EventMapper;
import com.example.Event.infrastructure.persistence.document.EventAccessRequestDocument;
import com.example.Event.infrastructure.persistence.document.EventDocument;
import com.example.Event.infrastructure.persistence.document.EventMediaApprovalDocument;
import com.example.Event.infrastructure.persistence.document.EventMediaDocument;
import com.example.Event.infrastructure.persistence.document.OrphanedMediaLogDocument;
import com.example.Event.infrastructure.persistence.repository.EventAccessRequestRepository;
import com.example.Event.infrastructure.persistence.repository.EventMediaApprovalRepository;
import com.example.Event.infrastructure.persistence.repository.EventMediaRepository;
import com.example.Event.infrastructure.persistence.repository.EventRepository;
import com.example.Event.infrastructure.persistence.repository.OrphanedMediaLogRepository;
import com.example.Event.shared.exception.AuthenticationRequiredException;
import com.example.Event.shared.exception.DomainValidationException;
import com.example.Event.shared.exception.ForbiddenOperationException;
import com.example.Event.shared.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements IEventService {

    private static final Set<String> ALLOWED_VISIBILITY = Set.of("PUBLIC", "PROTECTED", "PRIVATE");

    private final EventRepository eventRepository;
    private final EventMediaRepository eventMediaRepository;
    private final EventMediaApprovalRepository eventMediaApprovalRepository;
    private final EventAccessRequestRepository eventAccessRequestRepository;
    private final MediaServiceClient mediaServiceClient;
    private final IEventCollaboratorService collaboratorService;
    private final OrphanedMediaLogRepository orphanedMediaLogRepository;
    private final EventMapper eventMapper;

    @Override
    public EventCreateResponse createEvent(EventRequest request, HttpServletRequest httpRequest) {
        String ownerUserId = requireUserId(httpRequest, "User ID not found");
        validateCreatePayload(request);

        String title = request.title().trim();
        Instant now = Instant.now();
        EventDocument event = EventDocument.builder()
                .ownerUserId(ownerUserId)
                .title(title)
                .description(request.description().trim())
                .eventDate(parseEventDate(request.eventDate(), true))
                .visibility(request.resolvedVisibility())
                .moderationEnabled(request.moderationEnabledOrDefault())
                .status("ACTIVE")
                .coverImageId(normalizeBlankToNull(request.coverImageId()))
                .createdAt(now)
                .updatedAt(now)
                .build();

        EventDocument savedEvent = eventRepository.save(event);
        log.info("Event created successfully: {} with ID: {}", title, savedEvent.getId());

        return new EventCreateResponse(
                savedEvent.getId(),
                savedEvent.getId(),
                savedEvent.getTitle(),
                "Event created successfully");
    }

    @Override
    public List<EventSummaryResponse> getGlobalEvents(HttpServletRequest httpRequest) {
        String userId = optionalUserId(httpRequest);

        List<EventDocument> allEvents = new ArrayList<>(getPublicEvents());
        allEvents.addAll(getProtectedEvents());

        if (userId != null) {
            List<EventDocument> userEvents = getEventsByOwner(userId);
            Set<String> visibleEventIds = allEvents.stream()
                    .map(EventDocument::getId)
                    .collect(Collectors.toSet());

            for (EventDocument event : userEvents) {
                if (!visibleEventIds.contains(event.getId())) {
                    allEvents.add(event);
                }
            }
        }

        return allEvents.stream()
                .map(this::toGlobalSummary)
                .toList();
    }

    @Override
    public List<EventSummaryResponse> getMyEvents(HttpServletRequest httpRequest) {
        String userId = requireUserId(httpRequest, "User ID not found");
        return getEventsByOwner(userId).stream()
                .map(this::toOwnerSummary)
                .toList();
    }

    @Override
    public EventResponse getEvent(String eventId, HttpServletRequest httpRequest) {
        String userId = optionalUserId(httpRequest);
        EventDocument event = getEventById(eventId);

        if ("PRIVATE".equals(event.getVisibility()) && !isOwner(event, userId) && !isDocumentCollaborator(event, userId)) {
            throw new ForbiddenOperationException("You don't have permission to view this event");
        }

        if ("PROTECTED".equals(event.getVisibility()) && userId != null && !isOwner(event, userId)
                && !isDocumentCollaborator(event, userId) && !isUserApproved(eventId, userId)) {
            return new EventResponse(
                    event.getId(),
                    event.getId(),
                    event.getOwnerUserId(),
                    event.getTitle(),
                    event.getDescription(),
                    event.getCoverImageId(),
                    toIso(event.getEventDate()),
                    event.getVisibility(),
                    event.isModerationEnabled(),
                    eventMapper.toCollaboratorResponses(event.getCollaborators()),
                    event.getStatus(),
                    toIso(event.getCreatedAt()),
                    "NO_ACCESS",
                    true,
                    "This event is protected. You need to request access to view media.");
        }

        return new EventResponse(
                event.getId(),
                event.getId(),
                event.getOwnerUserId(),
                event.getTitle(),
                event.getDescription(),
                event.getCoverImageId(),
                toIso(event.getEventDate()),
                event.getVisibility(),
                event.isModerationEnabled(),
                eventMapper.toCollaboratorResponses(event.getCollaborators()),
                event.getStatus(),
                toIso(event.getCreatedAt()),
                "APPROVED",
                null,
                null);
    }

    @Override
    public EventUpdateResponse updateEvent(
            String eventId,
            EventUpdateRequest eventDetails,
            MultipartFile coverMedia,
            HttpServletRequest httpRequest) {
        String userId = requireUserId(httpRequest, "User ID not found");
        EventDocument updated = updateEventDocument(eventId, userId, eventDetails, coverMedia);

        return new EventUpdateResponse(
                "success",
                "Event updated successfully",
                updated.getId(),
                updated.getId(),
                updated.getTitle(),
                updated.getDescription(),
                toIso(updated.getEventDate()),
                updated.getVisibility(),
                updated.isModerationEnabled(),
                updated.getCoverImageId(),
                eventMapper.toCollaboratorResponses(updated.getCollaborators()));
    }

    @Override
    @Transactional
    public EventDeleteResponse deleteEvent(String eventId, HttpServletRequest httpRequest) {
        String userId = requireUserId(httpRequest, "User ID not found");
        EventDocument event = getEventById(eventId);
        if (!event.getOwnerUserId().equals(userId)) {
            throw new ForbiddenOperationException("Only the event owner can delete this event");
        }

        deleteEventCascade(event);
        return new EventDeleteResponse(eventId, "Event deleted successfully");
    }

    @Override
    public EventSettingsResponse getSettings(String eventId) {
        EventDocument event = getEventById(eventId);
        return new EventSettingsResponse(event.isModerationEnabled());
    }

    @Override
    public EventSettingsResponse updateSettings(
            String eventId,
            EventSettingsRequest request,
            HttpServletRequest httpRequest) {
        String userId = requireUserId(httpRequest, "User ID not found");
        if (!collaboratorService.hasPermission(eventId, userId, "canEditEventDetails")) {
            throw new ForbiddenOperationException("You don't have permission to edit this event");
        }

        EventDocument event = getEventById(eventId);
        event.setModerationEnabled(Boolean.TRUE.equals(request.moderationEnabled()));
        updateEventDirectly(event);
        return new EventSettingsResponse(event.isModerationEnabled());
    }

    @Override
    public EventDocument getEventById(String eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
    }

    @Override
    public List<EventDocument> getEventsByOwner(String ownerUserId) {
        return eventRepository.findByOwnerUserId(ownerUserId);
    }

    @Override
    public List<EventDocument> getPublicEvents() {
        return eventRepository.findByVisibility("PUBLIC");
    }

    @Override
    public List<EventDocument> getProtectedEvents() {
        return eventRepository.findByVisibility("PROTECTED");
    }

    @Override
    public EventDocument updateEventDirectly(EventDocument event) {
        event.setUpdatedAt(Instant.now());
        EventDocument updated = eventRepository.save(event);
        log.info("Event updated directly: {}", event.getId());
        return updated;
    }

    private EventDocument updateEventDocument(
            String eventId,
            String userId,
            EventUpdateRequest eventDetails,
            MultipartFile coverMedia) {
        EventDocument event = getEventById(eventId);

        if (!collaboratorService.hasPermission(eventId, userId, "canEditEventDetails")) {
            throw new ForbiddenOperationException("You don't have permission to edit this event");
        }

        validateUpdatePayload(eventDetails);

        String oldMediaId = event.getCoverImageId();
        String newMediaId = oldMediaId;

        event.setTitle(eventDetails.title().trim());
        event.setDescription(eventDetails.description() != null ? eventDetails.description().trim() : "");
        event.setEventDate(parseEventDate(eventDetails.eventDate(), false));
        event.setVisibility(eventDetails.visibility().trim().toUpperCase());
        event.setModerationEnabled(Boolean.TRUE.equals(eventDetails.moderationEnabled()));

        if (coverMedia != null && !coverMedia.isEmpty()) {
            MediaServiceResponse uploadResponse = mediaServiceClient.uploadMedia(
                    coverMedia,
                    "EVENT_COVER",
                    eventId,
                    userId);
            newMediaId = extractMediaId(uploadResponse);
            event.setCoverImageId(newMediaId);
        }

        event.setUpdatedAt(Instant.now());
        EventDocument updatedEvent = eventRepository.save(event);
        deleteOldCoverMediaIfNeeded(eventId, userId, oldMediaId, newMediaId);
        return updatedEvent;
    }

    private void deleteEventCascade(EventDocument event) {
        String eventId = event.getId();
        String ownerId = event.getOwnerUserId();
        log.info("Deleting event and associated data: {}", eventId);

        List<EventMediaApprovalDocument> approvals = eventMediaApprovalRepository.findByEventId(eventId);
        for (EventMediaApprovalDocument approval : approvals) {
            try {
                mediaServiceClient.deleteMedia(approval.getMediaId(), ownerId);
            } catch (Exception e) {
                log.warn("Failed to delete remote media {}, continuing: {}", approval.getMediaId(), e.getMessage());
            }
        }

        eventMediaApprovalRepository.deleteAll(approvals);
        List<EventMediaDocument> mediaList = eventMediaRepository.findByEventId(eventId);
        eventMediaRepository.deleteAll(mediaList);
        eventAccessRequestRepository.deleteAll(eventAccessRequestRepository.findByEventId(eventId));
        eventRepository.deleteById(eventId);
    }

    private void deleteOldCoverMediaIfNeeded(String eventId, String userId, String oldMediaId, String newMediaId) {
        if (oldMediaId == null || oldMediaId.isBlank() || oldMediaId.equals(newMediaId)) {
            return;
        }

        try {
            mediaServiceClient.deleteMedia(oldMediaId, userId);
        } catch (Exception deleteError) {
            log.error(
                    "Failed to delete old cover media {} for event {}: {}",
                    oldMediaId,
                    eventId,
                    deleteError.getMessage(),
                    deleteError);

            try {
                orphanedMediaLogRepository.save(OrphanedMediaLogDocument.builder()
                        .eventId(eventId)
                        .oldMediaId(oldMediaId)
                        .errorMessage(deleteError.getMessage())
                        .createdAt(Instant.now())
                        .build());
            } catch (Exception orphanLogError) {
                log.error("Failed to persist orphaned media log for {}: {}", oldMediaId, orphanLogError.getMessage(), orphanLogError);
            }
        }
    }

    private void validateCreatePayload(EventRequest request) {
        if (request.title() == null || request.title().trim().isEmpty()
                || request.description() == null || request.description().trim().isEmpty()) {
            throw new DomainValidationException("Title and description are required");
        }

        String visibility = request.resolvedVisibility();
        if (!ALLOWED_VISIBILITY.contains(visibility)) {
            throw new DomainValidationException("Visibility must be PUBLIC, PROTECTED, or PRIVATE");
        }
    }

    private void validateUpdatePayload(EventUpdateRequest eventDetails) {
        if (eventDetails.title() == null || eventDetails.title().trim().isEmpty()) {
            throw new DomainValidationException("Title is required");
        }
        if (eventDetails.description() != null && eventDetails.description().length() > 2000) {
            throw new DomainValidationException("Description must not exceed 2000 characters");
        }
        if (eventDetails.eventDate() == null || eventDetails.eventDate().trim().isEmpty()) {
            throw new DomainValidationException("Event date is required");
        }
        if (eventDetails.visibility() == null || eventDetails.visibility().trim().isEmpty()) {
            throw new DomainValidationException("Visibility is required");
        }
        if (!ALLOWED_VISIBILITY.contains(eventDetails.visibility().trim().toUpperCase())) {
            throw new DomainValidationException("Visibility must be PUBLIC, PROTECTED, or PRIVATE");
        }
    }

    private String extractMediaId(MediaServiceResponse uploadResponse) {
        if (uploadResponse == null || uploadResponse.resolvedMediaId() == null || uploadResponse.resolvedMediaId().isBlank()) {
            throw new DomainValidationException("Media upload did not return a media ID");
        }
        return uploadResponse.resolvedMediaId();
    }

    private Instant parseEventDate(String eventDateStr, boolean allowFallbackToNow) {
        if (eventDateStr == null || eventDateStr.trim().isEmpty()) {
            if (allowFallbackToNow) {
                return Instant.now();
            }
            throw new DomainValidationException("Event date is required");
        }

        String normalized = eventDateStr.trim().replace(" ", "T");
        try {
            LocalDateTime ldt = LocalDateTime.parse(normalized);
            return ldt.atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception ignored) {
        }

        try {
            LocalDate date = LocalDate.parse(normalized);
            return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        } catch (Exception ignored) {
        }

        if (allowFallbackToNow) {
            log.warn("Failed to parse event date: {}, using current time", eventDateStr);
            return Instant.now();
        }
        throw new DomainValidationException("Invalid event date format");
    }

    private EventSummaryResponse toGlobalSummary(EventDocument event) {
        return new EventSummaryResponse(
                event.getId(),
                event.getId(),
                event.getOwnerUserId(),
                event.getTitle(),
                event.getDescription(),
                event.getCoverImageId(),
                toIso(event.getEventDate()),
                event.getVisibility(),
                null,
                null,
                toIso(event.getCreatedAt()));
    }

    private EventSummaryResponse toOwnerSummary(EventDocument event) {
        return new EventSummaryResponse(
                event.getId(),
                event.getId(),
                event.getOwnerUserId(),
                event.getTitle(),
                event.getDescription(),
                event.getCoverImageId(),
                toIso(event.getEventDate()),
                event.getVisibility(),
                event.isModerationEnabled(),
                eventMapper.toCollaboratorResponses(event.getCollaborators()),
                toIso(event.getCreatedAt()));
    }

    private boolean isUserApproved(String eventId, String requesterUserId) {
        return eventAccessRequestRepository.findByEventIdAndRequesterUserId(eventId, requesterUserId)
                .filter(this::isApprovedAndActive)
                .isPresent();
    }

    private boolean isApprovedAndActive(EventAccessRequestDocument request) {
        if (!"APPROVED".equals(request.getStatus())) {
            return false;
        }
        return request.getAccessExpiresAt() == null || Instant.now().isBefore(request.getAccessExpiresAt());
    }

    private boolean isOwner(EventDocument event, String userId) {
        return userId != null && userId.equals(event.getOwnerUserId());
    }

    private boolean isDocumentCollaborator(EventDocument event, String userId) {
        if (userId == null || event.getCollaborators() == null) {
            return false;
        }
        return event.getCollaborators().stream()
                .anyMatch(collaborator -> userId.equals(collaborator.getUserId()));
    }

    private String requireUserId(HttpServletRequest request, String message) {
        String userId = optionalUserId(request);
        if (userId == null) {
            throw new AuthenticationRequiredException(message);
        }
        return userId;
    }

    private String optionalUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String userId = request.getHeader("X-User-Id");
        return userId == null || userId.isBlank() ? null : userId;
    }

    private String normalizeBlankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String toIso(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
