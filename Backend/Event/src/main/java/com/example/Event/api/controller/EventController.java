package com.example.Event.api.controller;

import com.example.Event.application.service.EventService;
import com.example.Event.application.service.EventCollaboratorService;
import com.example.Event.application.service.EventAccessService;
import com.example.Event.infrastructure.client.UserServiceClient;
import com.example.Event.infrastructure.persistence.document.EventDocument;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;
    private final EventCollaboratorService collaboratorService;
    private final EventAccessService accessService;
    private final UserServiceClient userServiceClient;

    /**
     * Create a new event
     * POST /api/v1/events
     */
    @PostMapping
    @Operation(summary = "Create event")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            log.info("📝 [Event Create] User: {}, Payload: {}", userId, payload);

            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User ID not found"));
            }

            String title = (String) payload.get("title");
            String description = (String) payload.get("description");
            String eventDate = (String) payload.get("eventDate");
            String visibility = (String) payload.getOrDefault("visibility", "PRIVATE");
            boolean moderationEnabled = (boolean) payload.getOrDefault("moderationEnabled", false);

            // Validate required fields
            if (title == null || title.isEmpty() || description == null || description.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Title and description are required"));
            }

            // Create event
            EventDocument event = eventService.createEvent(userId, title, description, eventDate, visibility, moderationEnabled);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "eventId", event.getId(),
                    "id", event.getId(),
                    "title", event.getTitle(),
                    "message", "Event created successfully"
            ));

        } catch (Exception e) {
            log.error("❌ Create event error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create event: " + e.getMessage()));
        }
    }

    /**
     * Get all public events + user's own events
     * GET /api/v1/events/global
     */
    @GetMapping("/global")
    @Operation(summary = "Get global events")
    public ResponseEntity<?> global(HttpServletRequest request) {
        try {
            log.info("🌐 [Event List] Fetching global events (PUBLIC + PROTECTED)");

            String userId = request.getHeader("X-User-Id");

            // Get both PUBLIC and PROTECTED events
            List<EventDocument> publicEvents = eventService.getPublicEvents();
            List<EventDocument> protectedEvents = eventService.getProtectedEvents();
            List<EventDocument> allEvents = new java.util.ArrayList<>(publicEvents);
            allEvents.addAll(protectedEvents);

            // Add user's own events (if logged in) - avoid duplicates by checking IDs
            if (userId != null && !userId.isEmpty()) {
                List<EventDocument> userEvents = eventService.getEventsByOwner(userId);
                java.util.Set<String> visibleEventIds = allEvents.stream()
                        .map(EventDocument::getId)
                        .collect(java.util.stream.Collectors.toSet());

                for (EventDocument event : userEvents) {
                    // Don't add if already in public/protected events (compare by ID)
                    if (!visibleEventIds.contains(event.getId())) {
                        allEvents.add(event);
                    }
                }
            }

            List<Map<String, Object>> response = allEvents.stream()
                    .map(e -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("eventId", e.getId());
                        map.put("id", e.getId());
                        map.put("ownerUserId", e.getOwnerUserId());
                        map.put("title", e.getTitle());
                        map.put("description", e.getDescription());
                        map.put("eventDate", e.getEventDate().toString());
                        map.put("visibility", e.getVisibility());
                        map.put("createdAt", e.getCreatedAt().toString());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Get global events error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get events"));
        }
    }

    /**
     * Get user's events
     * GET /api/v1/events/my-events
     */
    @GetMapping("/my-events")
    @Operation(summary = "Get user's events")
    public ResponseEntity<?> myEvents(HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            log.info("📋 [Event List] Fetching events for user: {}", userId);

            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User ID not found"));
            }

            List<EventDocument> events = eventService.getEventsByOwner(userId);
            List<Map<String, Object>> response = events.stream()
                    .map(e -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("eventId", e.getId());
                        map.put("id", e.getId());
                        map.put("ownerUserId", e.getOwnerUserId());
                        map.put("title", e.getTitle());
                        map.put("description", e.getDescription());
                        map.put("eventDate", e.getEventDate().toString());
                        map.put("visibility", e.getVisibility());
                        map.put("moderationEnabled", e.isModerationEnabled());
                        map.put("collaborators", e.getCollaborators());
                        map.put("createdAt", e.getCreatedAt().toString());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Get user's events error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get events"));
        }
    }

    /**
     * Get single event by ID with visibility enforcement
     * GET /api/v1/events/{eventId}
     */
    @GetMapping("/{eventId}")
    @Operation(summary = "Get event")
    public ResponseEntity<?> byId(@PathVariable String eventId, HttpServletRequest request) {
        try {
            log.info("🔍 [Event Get] Fetching event: {}", eventId);

            String userId = request.getHeader("X-User-Id");
            EventDocument event = eventService.getEventById(eventId);

            Map<String, Object> response = new HashMap<>();
            response.put("eventId", event.getId());
            response.put("id", event.getId());
            response.put("ownerUserId", event.getOwnerUserId());
            response.put("title", event.getTitle());
            response.put("description", event.getDescription());
            response.put("eventDate", event.getEventDate() != null ? event.getEventDate().toString() : null);
            response.put("visibility", event.getVisibility());
            response.put("moderationEnabled", event.isModerationEnabled());

            // NEW: Include collaborators in response so frontend can check permissions
            response.put("collaborators", event.getCollaborators());

            // DEBUG: Log collaborators for troubleshooting
            if (event.getCollaborators() != null && !event.getCollaborators().isEmpty()) {
                log.info("👥 [Event Detail] Event {} has {} collaborators: {}",
                    eventId,
                    event.getCollaborators().size(),
                    event.getCollaborators().stream()
                        .map(c -> c.getUserId())
                        .collect(Collectors.toList()));

                // DETAILED DEBUG: Log each collaborator's full details
                event.getCollaborators().forEach(c -> {
                    log.info("  👤 Collaborator userId='{}' (type: String) - canReviewMedia={}, canReviewAccessRequests={}, canEditEventDetails={}",
                        c.getUserId(),
                        c.getCanReviewMedia(),
                        c.getCanReviewAccessRequests(),
                        c.getCanEditEventDetails());
                });
            } else {
                log.info("👥 [Event Detail] Event {} has no collaborators", eventId);
            }

            response.put("status", event.getStatus());
            response.put("createdAt", event.getCreatedAt() != null ? event.getCreatedAt().toString() : null);

            // NEW: Visibility enforcement with collaborator checking
            if ("PRIVATE".equals(event.getVisibility())) {
                // Only owner and collaborators can view PRIVATE events
                if (userId == null || !userId.equals(event.getOwnerUserId())) {
                    boolean isCollaborator = event.getCollaborators() != null &&
                            event.getCollaborators().stream().anyMatch(c -> c.getUserId().equals(userId));
                    if (!isCollaborator) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", "You don't have permission to view this event"));
                    }
                }
            }

            if ("PROTECTED".equals(event.getVisibility())) {
                if (userId != null && !userId.equals(event.getOwnerUserId())) {
                    // Check if user is collaborator
                    boolean isCollaborator = event.getCollaborators() != null &&
                            event.getCollaborators().stream().anyMatch(c -> c.getUserId().equals(userId));

                    if (!isCollaborator) {
                        // Check if user has approved access request
                        boolean approved = accessService != null && accessService.isUserApproved(eventId, userId);
                        if (!approved) {
                            log.info("👤 User {} requested access to protected event {}, showing access request UI", userId, eventId);
                            // Return event basic info with accessStatus flag so frontend can show access request UI
                            response.put("accessStatus", "NO_ACCESS");
                            response.put("requiresApproval", true);
                            response.put("message", "This event is protected. You need to request access to view media.");
                            return ResponseEntity.ok(response);
                        }
                    }
                }
            }

            // PUBLIC: anyone can view (no restrictions)
            response.put("accessStatus", "APPROVED");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Get event error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update an event
     * PATCH /api/v1/events/{eventId}
     */
    @PatchMapping("/{eventId}")
    @Operation(summary = "Update event")
    public ResponseEntity<?> update(@PathVariable String eventId, @RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            log.info("✏️ [Event Update] Updating event: {} by user: {}", eventId, userId);

            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User ID not found"));
            }

            // Verify ownership
            EventDocument event = eventService.getEventById(eventId);
            if (!event.getOwnerUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You don't have permission to update this event"));
            }

            String title = (String) payload.get("title");
            String description = (String) payload.get("description");
            String eventDate = (String) payload.get("eventDate");
            String visibility = (String) payload.get("visibility");
            Object modObj = payload.get("moderationEnabled");
            boolean moderationEnabled = modObj instanceof Boolean ? (boolean) modObj : false;

            EventDocument updated = eventService.updateEvent(eventId, title, description, eventDate, visibility, moderationEnabled);

            return ResponseEntity.ok(Map.of(
                    "eventId", updated.getId(),
                    "message", "Event updated successfully"
            ));

        } catch (Exception e) {
            log.error("❌ Update event error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete an event
     * DELETE /api/v1/events/{eventId}
     */
    @DeleteMapping("/{eventId}")
    @Operation(summary = "Delete event")
    public ResponseEntity<?> deleteEvent(@PathVariable String eventId, HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            log.info("🗑️ [Event Delete] Deleting event: {} by user: {}", eventId, userId);

            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User ID not found"));
            }

            // Verify ownership
            EventDocument event = eventService.getEventById(eventId);
            if (!event.getOwnerUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only the event owner can delete this event"));
            }

            eventService.deleteEvent(eventId);

            return ResponseEntity.ok(Map.of(
                    "eventId", eventId,
                    "message", "Event deleted successfully"
            ));

        } catch (Exception e) {
            log.error("❌ Delete event error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Add collaborator to event
     * POST /api/v1/events/{eventId}/collaborators
     */
    @PostMapping("/{eventId}/collaborators")
    @Operation(summary = "Add collaborator to event")
    public ResponseEntity<?> addCollaborator(
            @PathVariable String eventId,
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {
        try {
            String ownerUserId = request.getHeader("X-User-Id");
            log.info("👥 [Add Collaborator] Event: {}, By user: {}", eventId, ownerUserId);

            if (ownerUserId == null || ownerUserId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User ID not found"));
            }

            String userEmail = (String) payload.get("userEmail");

            // FIXED: Look up the actual user ID by email instead of using email as ID
            String collaboratorId = null;
            try {
                // Call user service to get actual user ID from email
                collaboratorId = userServiceClient.getUserIdByEmail(userEmail);
                log.info("🔍 [Add Collaborator] Resolved email '{}' to user ID '{}'", userEmail, collaboratorId);
            } catch (Exception e) {
                log.error("❌ [Add Collaborator] Error looking up user by email '{}': {}", userEmail, e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Failed to find user: " + e.getMessage()));
            }

            if (collaboratorId == null || collaboratorId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "User ID not found for email: " + userEmail));
            }

            // Extract permissions (handle both old and new format)
            Boolean canUploadMedia = false;
            Boolean canReviewMedia = false;
            Boolean canReviewAccessRequests = false;
            Boolean canDirectUpload = false;
            Boolean canDeleteMedia = false;
            Boolean canEditEventDetails = false;

            // Try new format: permissions object
            Object permissionsObj = payload.get("permissions");
            if (permissionsObj instanceof Map) {
                Map<String, Object> permissions = (Map<String, Object>) permissionsObj;
                canUploadMedia = (Boolean) permissions.getOrDefault("canUploadMedia", false);
                canReviewMedia = (Boolean) permissions.getOrDefault("canReviewMedia", false);
                canReviewAccessRequests = (Boolean) permissions.getOrDefault("canReviewAccessRequests", false);
                canDirectUpload = (Boolean) permissions.getOrDefault("canDirectUpload", false);
                canDeleteMedia = (Boolean) permissions.getOrDefault("canDeleteMedia", false);
                canEditEventDetails = (Boolean) permissions.getOrDefault("canEditEventDetails", false);
            } else {
                // Try old format: direct properties
                canUploadMedia = (Boolean) payload.getOrDefault("canUploadMedia", false);
                canReviewMedia = (Boolean) payload.getOrDefault("canReviewMedia", false);
                canReviewAccessRequests = (Boolean) payload.getOrDefault("canReviewAccessRequests", false);
                canDirectUpload = (Boolean) payload.getOrDefault("canDirectUpload", false);
                canDeleteMedia = (Boolean) payload.getOrDefault("canDeleteMedia", false);
                canEditEventDetails = (Boolean) payload.getOrDefault("canEditEventDetails", false);
            }

            EventDocument.EventCollaborator collaborator = collaboratorService.addCollaborator(
                    eventId, collaboratorId, canUploadMedia, canReviewMedia, canReviewAccessRequests, canDirectUpload, canDeleteMedia, canEditEventDetails, ownerUserId);

            // Enrich response with user details
            Map<String, Object> enrichedCollab = new java.util.HashMap<>();
            enrichedCollab.put("userId", collaborator.getUserId());
            enrichedCollab.put("canUploadMedia", collaborator.getCanUploadMedia());
            enrichedCollab.put("canReviewMedia", collaborator.getCanReviewMedia());
            enrichedCollab.put("canReviewAccessRequests", collaborator.getCanReviewAccessRequests());
            enrichedCollab.put("canDirectUpload", collaborator.getCanDirectUpload());
            enrichedCollab.put("canDeleteMedia", collaborator.getCanDeleteMedia());
            enrichedCollab.put("canEditEventDetails", collaborator.getCanEditEventDetails());
            enrichedCollab.put("addedAt", collaborator.getAddedAt());
            enrichedCollab.put("addedByUserId", collaborator.getAddedByUserId());

            // Try to fetch email and displayName
            if (userEmail != null && !userEmail.isEmpty()) {
                enrichedCollab.put("email", userEmail);
            }
            try {
                String displayName = userServiceClient.getUserDisplayName(collaboratorId);
                enrichedCollab.put("displayName", displayName);
            } catch (Exception e) {
                log.warn("⚠️ Could not fetch displayName: {}", e.getMessage());
                enrichedCollab.put("displayName", collaboratorId);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "collaborator", enrichedCollab,
                    "message", "Collaborator added successfully"
            ));

        } catch (Exception e) {
            log.error("❌ Add collaborator error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get event collaborators
     * GET /api/v1/events/{eventId}/collaborators
     */
    @GetMapping("/{eventId}/collaborators")
    @Operation(summary = "Get event collaborators")
    public ResponseEntity<?> getCollaborators(@PathVariable String eventId) {
        try {
            log.info("👥 [Get Collaborators] Event: {}", eventId);

            List<EventDocument.EventCollaborator> collaborators = collaboratorService.getCollaborators(eventId);
            
            // Enrich collaborators with email and displayName from Auth service
            List<Map<String, Object>> enrichedCollaborators = new java.util.ArrayList<>();
            for (EventDocument.EventCollaborator collab : collaborators) {
                // Skip invalid collaborators with null ID
                if (collab.getUserId() == null || collab.getUserId().isEmpty()) {
                    log.warn("⚠️ Found corrupted collaborator with null userId in event: {}", eventId);
                    continue;
                }

                Map<String, Object> enriched = new java.util.HashMap<>();
                enriched.put("userId", collab.getUserId());
                enriched.put("canUploadMedia", collab.getCanUploadMedia());
                enriched.put("canReviewMedia", collab.getCanReviewMedia());
                enriched.put("canReviewAccessRequests", collab.getCanReviewAccessRequests());
                // Include newer permission fields so frontend can reflect them
                enriched.put("canDirectUpload", collab.getCanDirectUpload());
                enriched.put("canDeleteMedia", collab.getCanDeleteMedia());
                enriched.put("canEditEventDetails", collab.getCanEditEventDetails());
                enriched.put("addedAt", collab.getAddedAt());
                enriched.put("addedByUserId", collab.getAddedByUserId());
                
                // Try to fetch email and displayName from Auth service
                if (collab.getUserId() != null && !collab.getUserId().isEmpty()) {
                    try {
                        String displayName = userServiceClient.getUserDisplayName(collab.getUserId());
                        enriched.put("displayName", displayName);
                        log.info("✅ Enriched collaborator {} with displayName: {}", collab.getUserId(), displayName);
                    } catch (Exception e) {
                        log.warn("⚠️ Could not enrich collaborator {}: {}", collab.getUserId(), e.getMessage());
                        enriched.put("displayName", collab.getUserId());
                    }
                }
                
                enrichedCollaborators.add(enriched);
            }

            return ResponseEntity.ok(Map.of(
                    "eventId", eventId,
                    "collaborators", enrichedCollaborators
            ));

        } catch (Exception e) {
            log.error("❌ Get collaborators error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Remove collaborator from event
     * DELETE /api/v1/events/{eventId}/collaborators/{userId}
     */
    @DeleteMapping("/{eventId}/collaborators/{userId}")
    @Operation(summary = "Remove collaborator from event")
    public ResponseEntity<?> removeCollaborator(
            @PathVariable String eventId,
            @PathVariable String userId,
            HttpServletRequest request) {
        try {
            String ownerUserId = request.getHeader("X-User-Id");
            log.info("🗑️ [Remove Collaborator] Event: {}, Collaborator: {}, By user: {}", eventId, userId, ownerUserId);

            if (ownerUserId == null || ownerUserId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User ID not found"));
            }

            // Validate that userId is not null or "null" string
            if (userId == null || userId.isEmpty() || "null".equals(userId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid collaborator ID. Cannot remove corrupted entries."));
            }

            collaboratorService.removeCollaborator(eventId, userId, ownerUserId);

            return ResponseEntity.ok(Map.of("message", "Collaborator removed successfully"));

        } catch (Exception e) {
            log.error("❌ Remove collaborator error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update collaborator permissions
     * PATCH /api/v1/events/{eventId}/collaborators/{userId}
     */
    @PatchMapping("/{eventId}/collaborators/{userId}")
    @Operation(summary = "Update collaborator permissions")
    public ResponseEntity<?> updateCollaboratorPermissions(
            @PathVariable String eventId,
            @PathVariable String userId,
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {
        try {
            String ownerUserId = request.getHeader("X-User-Id");
            log.info("✏️ [Update Collaborator] Event: {}, Collaborator: {}, By user: {}", eventId, userId, ownerUserId);

            if (ownerUserId == null || ownerUserId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User ID not found"));
            }

            Map<String, Object> permissions = (Map<String, Object>) payload.get("permissions");
            Boolean canUploadMedia = permissions != null ? (Boolean) permissions.get("canUploadMedia") : null;
            Boolean canReviewMedia = permissions != null ? (Boolean) permissions.get("canReviewMedia") : null;
            Boolean canReviewAccessRequests = permissions != null ? (Boolean) permissions.get("canReviewAccessRequests") : null;
              Boolean canDirectUpload = permissions != null ? (Boolean) permissions.get("canDirectUpload") : null;
              Boolean canDeleteMedia = permissions != null ? (Boolean) permissions.get("canDeleteMedia") : null;
              Boolean canEditEventDetails = permissions != null ? (Boolean) permissions.get("canEditEventDetails") : null;

            EventDocument.EventCollaborator updated = collaboratorService.updateCollaboratorPermissions(
                      eventId, userId, canUploadMedia, canReviewMedia, canReviewAccessRequests, canDirectUpload, canDeleteMedia, canEditEventDetails, ownerUserId);

            return ResponseEntity.ok(Map.of(
                    "collaborator", updated,
                    "message", "Collaborator permissions updated successfully"
            ));

        } catch (Exception e) {
            log.error("❌ Update collaborator error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Policies feature removed - no longer part of the application design
}


