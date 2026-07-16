package com.example.Event.application.service;

import com.example.Event.api.dto.request.EventUpdateRequest;
import com.example.Event.infrastructure.client.MediaServiceClient;
import com.example.Event.infrastructure.persistence.document.EventDocument;
import com.example.Event.infrastructure.persistence.document.EventMediaDocument;
import com.example.Event.infrastructure.persistence.document.EventMediaApprovalDocument;
import com.example.Event.infrastructure.persistence.document.OrphanedMediaLogDocument;
import com.example.Event.infrastructure.persistence.repository.EventAccessRequestRepository;
import com.example.Event.infrastructure.persistence.repository.EventMediaApprovalRepository;
import com.example.Event.infrastructure.persistence.repository.EventMediaRepository;
import com.example.Event.infrastructure.persistence.repository.EventRepository;
import com.example.Event.infrastructure.persistence.repository.OrphanedMediaLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventMediaRepository eventMediaRepository;
    private final EventMediaApprovalRepository eventMediaApprovalRepository;
    private final EventAccessRequestRepository eventAccessRequestRepository;
    private final MediaServiceClient mediaServiceClient;
    private final EventCollaboratorService collaboratorService;
    private final OrphanedMediaLogRepository orphanedMediaLogRepository;

    /**
     * Create a new event
     */
    public EventDocument createEvent(String ownerUserId, String title, String description,
                                     String eventDateStr, String visibility,String coverImageId, boolean moderationEnabled) {
        try {
            log.info("?? Creating event: {} by user: {}", title, ownerUserId);

            // Parse event date from ISO string (ISO 8601 format from frontend)
            Instant eventDate = parseEventDate(eventDateStr, true);

            EventDocument event = EventDocument.builder()
                    .ownerUserId(ownerUserId)
                    .title(title)
                    .description(description)
                    .eventDate(eventDate)
                    .visibility(visibility != null ? visibility : "PRIVATE")
                    .moderationEnabled(moderationEnabled)
                    .status("ACTIVE")
                    .coverImageId(coverImageId)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            EventDocument savedEvent = eventRepository.save(event);
            log.info("? Event created successfully: {} with ID: {}", title, savedEvent.getId());

            return savedEvent;

        } catch (Exception e) {
            log.error("? Failed to create event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create event: " + e.getMessage(), e);
        }
    }

    /**
     * Get event by ID
     */
    public EventDocument getEventById(String eventId) {
        log.info("?? Fetching event: {}", eventId);

        return eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("? Event not found: {}", eventId);
                    return new RuntimeException("Event not found: " + eventId);
                });
    }

    /**
     * Get all events by owner
     */
    public List<EventDocument> getEventsByOwner(String ownerUserId) {
        log.info("?? Fetching events for user: {}", ownerUserId);

        List<EventDocument> events = eventRepository.findByOwnerUserId(ownerUserId);
        log.info("Found {} events for user {}", events.size(), ownerUserId);

        return events;
    }

    /**
     * Get all public events
     */
    public List<EventDocument> getPublicEvents() {
        log.info("?? Fetching public events");

        List<EventDocument> events = eventRepository.findByVisibility("PUBLIC");
        log.info("Found {} public events", events.size());

        return events;
    }

    /**
     * Get all protected events
     */
    public List<EventDocument> getProtectedEvents() {
        log.info("?? Fetching protected events");

        List<EventDocument> events = eventRepository.findByVisibility("PROTECTED");
        log.info("Found {} protected events", events.size());

        return events;
    }

    /**
     * Update an event
     */
    public EventDocument updateEvent(String eventId, String userId, EventUpdateRequest eventDetails, MultipartFile coverMedia) {
        try {
            log.info("?? Updating event: {}", eventId);

            EventDocument event = getEventById(eventId);

            if (userId == null || userId.isBlank()) {
                throw new SecurityException("User ID not found");
            }

            boolean canEditDetails = collaboratorService.hasPermission(eventId, userId, "canEditEventDetails");
            if (!canEditDetails) {
                throw new SecurityException("You don't have permission to edit this event");
            }

            validateUpdatePayload(eventDetails);

            String oldMediaId = event.getCoverImageId();
            String newMediaId = oldMediaId;

            event.setTitle(eventDetails.getTitle().trim());
            event.setDescription(eventDetails.getDescription() != null ? eventDetails.getDescription().trim() : "");
            event.setEventDate(parseEventDate(eventDetails.getEventDate(), false));
            event.setVisibility(eventDetails.getVisibility().trim().toUpperCase());
            event.setModerationEnabled(Boolean.TRUE.equals(eventDetails.getModerationEnabled()));

            if (coverMedia != null && !coverMedia.isEmpty()) {
                Map<String, Object> uploadResponse = mediaServiceClient.uploadMedia(
                        coverMedia,
                        "EVENT_COVER",
                        eventId,
                        userId
                );
                newMediaId = extractMediaId(uploadResponse);
                event.setCoverImageId(newMediaId);
            }

            event.setUpdatedAt(Instant.now());

            EventDocument updatedEvent = eventRepository.save(event);

            if (oldMediaId != null && !oldMediaId.isBlank() && !oldMediaId.equals(newMediaId)) {
                try {
                    //throw new RuntimeException("This is a fake error!");
                    mediaServiceClient.deleteMedia(oldMediaId, userId);
                } catch (Exception deleteError) {
                    log.error("SEVERE: Failed to delete old cover media {} for event {}: {}",
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

            log.info("? Event updated successfully: {}", eventId);

            return updatedEvent;

        } catch (IllegalArgumentException | SecurityException e) {
            log.warn("? Failed to update event: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("? Failed to update event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update event: " + e.getMessage(), e);
        }
    }

    private void validateUpdatePayload(EventUpdateRequest eventDetails) {
        if (eventDetails.getTitle() == null || eventDetails.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }

        if (eventDetails.getDescription() != null && eventDetails.getDescription().length() > 2000) {
            throw new IllegalArgumentException("Description must not exceed 2000 characters");
        }

        if (eventDetails.getEventDate() == null || eventDetails.getEventDate().trim().isEmpty()) {
            throw new IllegalArgumentException("Event date is required");
        }

        if (eventDetails.getVisibility() == null || eventDetails.getVisibility().trim().isEmpty()) {
            throw new IllegalArgumentException("Visibility is required");
        }

        Set<String> allowedVisibility = Set.of("PUBLIC", "PROTECTED", "PRIVATE");
        if (!allowedVisibility.contains(eventDetails.getVisibility().trim().toUpperCase())) {
            throw new IllegalArgumentException("Visibility must be PUBLIC, PROTECTED, or PRIVATE");
        }
    }

    private String extractMediaId(Map<String, Object> uploadResponse) {
        if (uploadResponse == null) {
            throw new IllegalStateException("Media upload response was empty");
        }

        Object mediaId = uploadResponse.get("mediaId");
        if (mediaId == null) {
            mediaId = uploadResponse.get("id");
        }

        if (mediaId == null || mediaId.toString().isBlank()) {
            throw new IllegalStateException("Media upload did not return a media ID");
        }

        return mediaId.toString();
    }

    private Instant parseEventDate(String eventDateStr, boolean allowFallbackToNow) {
        if (eventDateStr == null || eventDateStr.trim().isEmpty()) {
            if (allowFallbackToNow) {
                return Instant.now();
            }
            throw new IllegalArgumentException("Event date is required");
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

        throw new IllegalArgumentException("Invalid event date format");
    }

    /**
     * Delete an event
     */
    @Transactional
    public void deleteEvent(String eventId) {
        try {
            log.info("??? Deleting event and all associated data: {}", eventId);

            // Verify event exists first
            EventDocument event = getEventById(eventId);
            String ownerId = event.getOwnerUserId();

            // 1. Fetch approval records to determine Media Service IDs and delete remote files
            List<EventMediaApprovalDocument> approvals = eventMediaApprovalRepository.findByEventId(eventId);
            log.info("Found {} approval records (media refs) to delete for event: {}", approvals.size(), eventId);

            for (EventMediaApprovalDocument approval : approvals) {
                try {
                    String mediaId = approval.getMediaId();
                    log.info("Deleting remote media: {}", mediaId);
                    mediaServiceClient.deleteMedia(mediaId, ownerId);
                } catch (Exception e) {
                    log.warn("? Failed to delete remote media {}, continuing: {}", approval.getMediaId(), e.getMessage());
                }
            }

            // 2. Cascade delete records in mongoDB (EventMedia, EventMediaApprovals, EventAccessRequests)
            // Delete approval records (these reference Media Service IDs)
            eventMediaApprovalRepository.deleteAll(approvals);
            log.info("? Event moderation approvals deleted for event: {}", eventId);

            // Delete any local EventMedia records (metadata)
            List<EventMediaDocument> mediaList = eventMediaRepository.findByEventId(eventId);
            eventMediaRepository.deleteAll(mediaList);
            log.info("? Event media records deleted for event: {}", eventId);

            eventAccessRequestRepository.deleteAll(eventAccessRequestRepository.findByEventId(eventId));
            log.info("? Event access requests deleted for event: {}", eventId);

            // 3. Finally delete the event document itself
            eventRepository.deleteById(eventId);
            log.info("? Event deleted successfully: {}", eventId);

        } catch (Exception e) {
            log.error("? Failed to delete event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete event: " + e.getMessage(), e);
        }
    }

    /**
     * Update event directly (for policy/collaborator changes)
     */
    public EventDocument updateEventDirectly(EventDocument event) {
        try {
            event.setUpdatedAt(Instant.now());
            EventDocument updated = eventRepository.save(event);
            log.info("? Event updated directly: {}", event.getId());
            return updated;
        } catch (Exception e) {
            log.error("? Failed to update event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update event: " + e.getMessage(), e);
        }
    }
}
