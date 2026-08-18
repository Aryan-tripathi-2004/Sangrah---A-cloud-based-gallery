package com.example.Event.application.service.impl;

import com.example.Event.api.dto.request.EventRequest;
import com.example.Event.api.dto.request.EventSettingsRequest;
import com.example.Event.api.dto.request.EventUpdateRequest;
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
import com.example.Event.shared.enums.AccessStatus;
import com.example.Event.shared.enums.ApprovalStatus;
import com.example.Event.shared.enums.EventStatus;
import com.example.Event.shared.enums.EventVisibility;
import com.example.Event.shared.exception.DomainValidationException;
import com.example.Event.shared.exception.ForbiddenOperationException;
import com.example.Event.shared.exception.ResourceNotFoundException;
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

    private final EventRepository eventRepository;
    private final EventMediaRepository eventMediaRepository;
    private final EventMediaApprovalRepository eventMediaApprovalRepository;
    private final EventAccessRequestRepository eventAccessRequestRepository;
    private final MediaServiceClient mediaServiceClient;
    private final IEventCollaboratorService collaboratorService;
    private final OrphanedMediaLogRepository orphanedMediaLogRepository;
    private final EventMapper eventMapper;

    @Override
    public EventCreateResponse createEvent(EventRequest request, String userId) {
        validateCreatePayload(request);

        String title = request.title().trim();
        Instant now = Instant.now();
        EventDocument event = EventDocument.builder()
                .ownerUserId(userId)
                .title(title)
                .description(request.description().trim())
                .eventDate(parseEventDate(request.eventDate(), true))
                .visibility(request.resolvedVisibility())
                .moderationEnabled(request.moderationEnabledOrDefault())
                .status(EventStatus.ACTIVE)
                .coverImageId(normalizeBlankToNull(request.coverImageId()))
                .createdAt(now)
                .updatedAt(now)
                .build();

        EventDocument savedEvent = eventRepository.save(event);
        log.info("Event created successfully: {} with ID: {}", title, savedEvent.getId());

        return eventMapper.toCreateResponse(savedEvent, "Event created successfully");
    }

    @Override
    public List<EventSummaryResponse> getGlobalEvents(String userId) {
        List<EventDocument> allEvents = new ArrayList<>(getPublicEvents());
        allEvents.addAll(getProtectedEvents());

        if (hasText(userId)) {
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
                .map(eventMapper::toPublicSummary)
                .toList();
    }

    @Override
    public List<EventSummaryResponse> getMyEvents(String userId) {
        return getEventsByOwner(userId).stream()
                .map(eventMapper::toOwnerSummary)
                .toList();
    }

    @Override
    public EventResponse getEvent(String eventId, String userId) {
        EventDocument event = getEventById(eventId);

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
    public EventUpdateResponse updateEvent(
            String eventId,
            EventUpdateRequest eventDetails,
            MultipartFile coverMedia,
            String userId) {
        EventDocument updated = updateEventDocument(eventId, userId, eventDetails, coverMedia);

        return eventMapper.toUpdateResponse(updated, "success", "Event updated successfully");
    }

    @Override
    @Transactional
    public EventDeleteResponse deleteEvent(String eventId, String userId) {
        EventDocument event = getEventById(eventId);
        if (!event.getOwnerUserId().equals(userId)) {
            throw new ForbiddenOperationException("Only the event owner can delete this event");
        }

        deleteEventCascade(event);
        return eventMapper.toDeleteResponse(eventId, "Event deleted successfully");
    }

    @Override
    public EventSettingsResponse getSettings(String eventId) {
        EventDocument event = getEventById(eventId);
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

        EventDocument event = getEventById(eventId);
        event.setModerationEnabled(Boolean.TRUE.equals(request.moderationEnabled()));
        updateEventDirectly(event);
        return eventMapper.toSettingsResponse(event);
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
        return eventRepository.findByVisibility(EventVisibility.PUBLIC);
    }

    @Override
    public List<EventDocument> getProtectedEvents() {
        return eventRepository.findByVisibility(EventVisibility.PROTECTED);
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
        event.setVisibility(eventDetails.visibility());
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
