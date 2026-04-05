package com.example.Event.application.service;

import com.example.Event.infrastructure.client.MediaServiceClient;
import com.example.Event.infrastructure.persistence.document.EventDocument;
import com.example.Event.infrastructure.persistence.document.EventMediaDocument;
import com.example.Event.infrastructure.persistence.document.EventMediaApprovalDocument;
import com.example.Event.infrastructure.persistence.repository.EventAccessRequestRepository;
import com.example.Event.infrastructure.persistence.repository.EventMediaApprovalRepository;
import com.example.Event.infrastructure.persistence.repository.EventMediaRepository;
import com.example.Event.infrastructure.persistence.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventMediaRepository eventMediaRepository;
    private final EventMediaApprovalRepository eventMediaApprovalRepository;
    private final EventAccessRequestRepository eventAccessRequestRepository;
    private final MediaServiceClient mediaServiceClient;

    /**
     * Create a new event
     */
    public EventDocument createEvent(String ownerUserId, String title, String description,
                                     String eventDateStr, String visibility, boolean moderationEnabled) {
        try {
            log.info("?? Creating event: {} by user: {}", title, ownerUserId);

            // Parse event date from ISO string (ISO 8601 format from frontend)
            Instant eventDate;
            if (eventDateStr != null && !eventDateStr.isEmpty()) {
                try {
                    // Handle ISO 8601 format: 2026-03-31T18:30
                    LocalDateTime ldt = LocalDateTime.parse(eventDateStr.replace(" ", "T"));
                    eventDate = ldt.atZone(ZoneId.systemDefault()).toInstant();
                } catch (Exception e) {
                    log.warn("Failed to parse event date: {}, using current time", eventDateStr);
                    eventDate = Instant.now();
                }
            } else {
                eventDate = Instant.now();
            }

            EventDocument event = EventDocument.builder()
                    .ownerUserId(ownerUserId)
                    .title(title)
                    .description(description)
                    .eventDate(eventDate)
                    .visibility(visibility != null ? visibility : "PRIVATE")
                    .moderationEnabled(moderationEnabled)
                    .status("ACTIVE")
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
    public EventDocument updateEvent(String eventId, String title, String description,
                                     String eventDateStr, String visibility, boolean moderationEnabled) {
        try {
            log.info("?? Updating event: {}", eventId);

            EventDocument event = getEventById(eventId);

            // Update fields if provided
            if (title != null && !title.isEmpty()) {
                event.setTitle(title);
            }
            if (description != null && !description.isEmpty()) {
                event.setDescription(description);
            }
            if (eventDateStr != null && !eventDateStr.isEmpty()) {
                try {
                    LocalDateTime ldt = LocalDateTime.parse(eventDateStr.replace(" ", "T"));
                    event.setEventDate(ldt.atZone(ZoneId.systemDefault()).toInstant());
                } catch (Exception e) {
                    log.warn("Failed to parse event date: {}", eventDateStr);
                }
            }
            if (visibility != null && !visibility.isEmpty()) {
                event.setVisibility(visibility);
            }
            event.setModerationEnabled(moderationEnabled);
            event.setUpdatedAt(Instant.now());

            EventDocument updatedEvent = eventRepository.save(event);
            log.info("? Event updated successfully: {}", eventId);

            return updatedEvent;

        } catch (Exception e) {
            log.error("? Failed to update event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update event: " + e.getMessage(), e);
        }
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
