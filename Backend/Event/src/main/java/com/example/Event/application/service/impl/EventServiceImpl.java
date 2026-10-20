package com.example.Event.application.service.impl;

import com.example.Event.api.dto.request.EventRequest;
import com.example.Event.api.dto.request.EventSettingsRequest;
import com.example.Event.api.dto.request.EventUpdateRequest;
import com.example.Event.api.dto.response.EventCreateResponse;
import com.example.Event.api.dto.response.EventDeleteResponse;
import com.example.Event.api.dto.response.EventInternalDTO;
import com.example.Event.api.dto.response.EventResponse;
import com.example.Event.api.dto.response.EventSettingsResponse;
import com.example.Event.api.dto.response.EventSummaryResponse;
import com.example.Event.api.dto.response.EventUpdateResponse;
import com.example.Event.application.service.interfaces.IEventCollaboratorService;
import com.example.Event.application.service.interfaces.IEventInternalService;
import com.example.Event.application.service.interfaces.IEventService;
import com.example.Event.infrastructure.client.MediaServiceClient;
import com.example.Event.infrastructure.client.dto.MediaServiceResponse;
import com.example.Event.infrastructure.mapper.EventInternalMapper;
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
import com.example.Event.shared.enums.AccessStatus;
import com.example.Event.shared.enums.ApprovalStatus;
import com.example.Event.shared.enums.EventStatus;
import com.example.Event.shared.enums.EventVisibility;
import com.example.Event.shared.exception.DomainValidationException;
import com.example.Event.shared.exception.ForbiddenOperationException;
import com.example.Event.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.Event.shared.enums.MediaDomain;
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
public class EventServiceImpl implements IEventService, IEventInternalService {

    private final EventRepository eventRepository;
    private final EventMediaRepository eventMediaRepository;
    private final EventMediaApprovalRepository eventMediaApprovalRepository;
    private final EventAccessRequestRepository eventAccessRequestRepository;
    private final MediaServiceClient mediaServiceClient;
    private final IEventCollaboratorService collaboratorService;
    private final OrphanedMediaLogRepository orphanedMediaLogRepository;
    private final EventMapper eventMapper;
    private final EventInternalMapper eventInternalMapper;

    @Override
    public EventCreateResponse createEvent(EventRequest request, MultipartFile coverMedia, String userId) {
        validateCreatePayload(request);

        String coverMediaId = null;

        // Step 1: Perform network I/O OUTSIDE any database transaction
        if (coverMedia != null && !coverMedia.isEmpty()) {
            MediaServiceResponse uploadResponse = mediaServiceClient.uploadMedia(
                    coverMedia,
                    MediaDomain.EVENT_COVER,
                    null,
                    userId);
            coverMediaId = extractMediaId(uploadResponse);
        }

        // Delegate translation strictly to the Mapper layer
        EventDocument event = eventMapper.toDocument(request);

        // Assign internally generated service-level defaults post-mapping
        event.setOwnerUserId(userId);
        event.setEventDate(parseEventDate(request.eventDate(), true));
        event.setStatus(EventStatus.ACTIVE);
        event.setCoverImageId(coverMediaId);
        Instant now = Instant.now();
        event.setCreatedAt(now);
        event.setUpdatedAt(now);

        // Step 2: Delegate database operations with compensation rollback
        EventDocument savedEvent;
        try {
            savedEvent = eventRepository.save(event);
            log.info("Event created successfully: {} with ID: {}", savedEvent.getTitle(), savedEvent.getId());
        } catch (Exception ex) {
            // Compensation: If database transaction fails, clean up the newly uploaded remote binary
            if (coverMediaId != null) {
                try {
                    mediaServiceClient.deleteMedia(coverMediaId, userId);
                    log.warn("🧹 Cleaned up newly uploaded cover media {} after event creation failure", coverMediaId);
                } catch (Exception cleanupError) {
                    log.error("Failed to clean up newly uploaded cover media {}: {}", coverMediaId, cleanupError.getMessage(), cleanupError);
                    try {
                        orphanedMediaLogRepository.save(OrphanedMediaLogDocument.builder()
                                .oldMediaId(coverMediaId)
                                .errorMessage(cleanupError.getMessage())
                                .createdAt(Instant.now())
                                .build());
                    } catch (Exception orphanLogError) {
                        log.error("Failed to persist orphaned media log for {}: {}", coverMediaId, orphanLogError.getMessage(), orphanLogError);
                    }
                }
            }
            throw ex;
        }

        return eventMapper.toCreateResponse(savedEvent, "Event created successfully");
    }

    @Override
    public EventCreateResponse createEvent(EventRequest request, String userId) {
        return createEvent(request, null, userId);
    }

    @Override
    public List<EventSummaryResponse> getGlobalEvents(String userId) {
        List<EventDocument> allEvents = new ArrayList<>(eventRepository.findByVisibility(EventVisibility.PUBLIC));
        allEvents.addAll(eventRepository.findByVisibility(EventVisibility.PROTECTED));

        if (hasText(userId)) {
            List<EventDocument> userEvents = eventRepository.findByOwnerUserId(userId);
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
                .map(eventMapper::toPublicSummary)
                .toList();
    }

    @Override
    public List<EventSummaryResponse> getMyEvents(String userId) {
        return eventRepository.findByOwnerUserId(userId).stream()
                .map(eventMapper::toOwnerSummary)
                .toList();
    }

    @Override
    public EventResponse getEvent(String eventId, String userId) {
        EventDocument event = findEventEntity(eventId);

        if (EventVisibility.PRIVATE == event.getVisibility() && !isOwner(event, userId) && !isDocumentCollaborator(event, userId)) {
            throw new ForbiddenOperationException("You don't have permission to view this event");
        }

        if (EventVisibility.PROTECTED == event.getVisibility() && userId != null && !isOwner(event, userId)
                && !isDocumentCollaborator(event, userId) && !isUserApproved(eventId, userId)) {
            return eventMapper.toResponse(
                    event,
                    AccessStatus.NO_ACCESS,
                    true,
                    "This event is protected. You need to request access to view media.");
        }

        return eventMapper.toResponse(
                event,
                AccessStatus.APPROVED,
                null,
                null);
    }

    @Override
    public EventResponse updateEvent(
            String eventId,
            EventUpdateRequest eventDetails,
            MultipartFile coverMedia,
            String userId) {
        EventDocument existingEvent = findEventEntity(eventId);

        if (!collaboratorService.hasPermission(eventId, userId, "canEditEventDetails")) {
            throw new ForbiddenOperationException("You don't have permission to edit this event");
        }

        validateUpdatePayload(eventDetails);

        String oldMediaId = existingEvent.getCoverImageId();
        String newMediaId = null;

        // Step 1: Perform network I/O OUTSIDE any database transaction
        if (coverMedia != null && !coverMedia.isEmpty()) {
            MediaServiceResponse uploadResponse = mediaServiceClient.uploadMedia(
                    coverMedia,
                    MediaDomain.EVENT_COVER,
                    eventId,
                    userId);
            newMediaId = extractMediaId(uploadResponse);
        }

        // Step 2: Delegate pure database operations to transactional boundary with compensation rollback
        EventDocument updatedEvent;
        try {
            updatedEvent = executeEventUpdate(eventId, eventDetails, newMediaId);
        } catch (Exception ex) {
            // Compensation: If database transaction fails, clean up the newly uploaded remote binary
            if (newMediaId != null) {
                try {
                    mediaServiceClient.deleteMedia(newMediaId, userId);
                    log.warn("🧹 Cleaned up newly uploaded cover media {} after database failure", newMediaId);
                } catch (Exception cleanupError) {
                    log.error("Failed to clean up newly uploaded media {}: {}", newMediaId, cleanupError.getMessage(), cleanupError);
                }
            }
            throw ex;
        }

        // Step 3: Clean up previous cover media OUTSIDE the transaction
        if (newMediaId != null && !newMediaId.equals(oldMediaId)) {
            deleteOldCoverMediaIfNeeded(eventId, userId, oldMediaId, newMediaId);
        }

        return eventMapper.toResponse(updatedEvent);
    }

    @Override
    public EventDeleteResponse deleteEvent(String eventId, String userId) {
        log.info("Deleting event and associated data: {}", eventId);
        EventDocument event = findEventEntity(eventId);
        if (!event.getOwnerUserId().equals(userId)) {
            throw new ForbiddenOperationException("Only the event owner can delete this event");
        }

        deleteEventCascade(event);
        log.info("Event deleted successfully: {}", eventId);
        return eventMapper.toDeleteResponse(eventId, "Event deleted successfully");
    }

    @Override
    public EventSettingsResponse getSettings(String eventId) {
        EventDocument event = findEventEntity(eventId);
        return eventMapper.toSettingsResponse(event);
    }

    @Override
    public EventSettingsResponse updateSettings(
            String eventId,
            EventSettingsRequest request,
            String userId) {
        if (!collaboratorService.hasPermission(eventId, userId, "canEditEventDetails")) {
            throw new ForbiddenOperationException("You don't have permission to edit this event");
        }

        EventDocument event = findEventEntity(eventId);
        event.setModerationEnabled(Boolean.TRUE.equals(request.moderationEnabled()));
        event.setUpdatedAt(Instant.now());
        eventRepository.save(event);
        return eventMapper.toSettingsResponse(event);
    }

    @Override
    public List<EventSummaryResponse> getEventsWithPendingRequests(String userId) {
        log.info("🔍 Getting events with pending requests for user: {}", userId);
        List<EventDocument> userEvents = eventRepository.findByOwnerUserId(userId);
        if (userEvents.isEmpty()) {
            return List.of();
        }

        List<String> eventIds = userEvents.stream().map(EventDocument::getId).toList();

        // Fetch all pending requests for all relevant events in exactly ONE batched query
        Set<String> eventsWithPendingIds = eventAccessRequestRepository
                .findByEventIdInAndStatus(eventIds, ApprovalStatus.PENDING)
                .stream()
                .map(EventAccessRequestDocument::getEventId)
                .collect(Collectors.toSet());

        return userEvents.stream()
                .filter(event -> eventsWithPendingIds.contains(event.getId()))
                .map(eventMapper::toOwnerSummary)
                .toList();
    }

    private EventDocument findEventEntity(String eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
    }

    @Override
    public EventInternalDTO getEventById(String eventId) {
        return eventInternalMapper.toDTO(findEventEntity(eventId));
    }

    @Override
    public List<EventInternalDTO> getEventsByOwner(String ownerUserId) {
        return eventRepository.findByOwnerUserId(ownerUserId).stream()
                .map(eventInternalMapper::toDTO)
                .toList();
    }

    @Override
    public List<EventInternalDTO> getPublicEvents() {
        return eventRepository.findByVisibility(EventVisibility.PUBLIC).stream()
                .map(eventInternalMapper::toDTO)
                .toList();
    }

    @Override
    public List<EventInternalDTO> getProtectedEvents() {
        return eventRepository.findByVisibility(EventVisibility.PROTECTED).stream()
                .map(eventInternalMapper::toDTO)
                .toList();
    }

    @Transactional
    protected EventDocument executeEventUpdate(
            String eventId,
            EventUpdateRequest eventDetails,
            String newMediaId) {
        EventDocument event = findEventEntity(eventId);

        event.setTitle(eventDetails.title().trim());
        event.setDescription(eventDetails.description() != null ? eventDetails.description().trim() : "");
        event.setEventDate(parseEventDate(eventDetails.eventDate(), false));
        event.setVisibility(eventDetails.visibility());
        event.setModerationEnabled(Boolean.TRUE.equals(eventDetails.moderationEnabled()));

        if (newMediaId != null && !newMediaId.isBlank()) {
            event.setCoverImageId(newMediaId);
        }

        event.setUpdatedAt(Instant.now());
        EventDocument updatedEvent = eventRepository.save(event);
        log.info("Event database record updated successfully: {}", eventId);
        return updatedEvent;
    }

    private void deleteEventCascade(EventDocument event) {
        String eventId = event.getId();
        String ownerId = event.getOwnerUserId();
        log.info("Deleting event and associated data: {}", eventId);

        List<EventMediaApprovalDocument> approvals = eventMediaApprovalRepository.findByEventId(eventId);
        if (!approvals.isEmpty()) {
            List<String> mediaIds = approvals.stream()
                    .map(EventMediaApprovalDocument::getMediaId)
                    .filter(id -> id != null && !id.isBlank())
                    .toList();

            if (!mediaIds.isEmpty()) {
                try {
                    mediaServiceClient.deleteMediaBulk(mediaIds, ownerId);
                    log.info("Bulk deleted {} remote media files for event {}", mediaIds.size(), eventId);
                } catch (Exception e) {
                    log.error("Failed to bulk delete remote media for event {}: {}", eventId, e.getMessage(), e);
                }
            }
        }

        executeDatabaseDeletions(eventId, approvals);
    }

    @Transactional
    protected void executeDatabaseDeletions(String eventId, List<EventMediaApprovalDocument> approvals) {
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

        request.resolvedVisibility();
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
        if (eventDetails.visibility() == null) {
            throw new DomainValidationException("Visibility is required");
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
            return Instant.parse(normalized);
        } catch (Exception ignored) {
        }

        try {
            LocalDateTime ldt = LocalDateTime.parse(normalized);
            return ldt.atZone(ZoneId.of("UTC")).toInstant();
        } catch (Exception ignored) {
        }

        try {
            LocalDate date = LocalDate.parse(normalized);
            return date.atStartOfDay(ZoneId.of("UTC")).toInstant();
        } catch (Exception ignored) {
        }

        if (allowFallbackToNow) {
            log.warn("Failed to parse event date: {}, using current time", eventDateStr);
            return Instant.now();
        }
        throw new DomainValidationException("Invalid event date format");
    }

    private boolean isUserApproved(String eventId, String requesterUserId) {
        return eventAccessRequestRepository.findByEventIdAndRequesterUserId(eventId, requesterUserId)
                .filter(this::isApprovedAndActive)
                .isPresent();
    }

    private boolean isApprovedAndActive(EventAccessRequestDocument request) {
        if (ApprovalStatus.APPROVED != request.getStatus()) {
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

    private String normalizeBlankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
