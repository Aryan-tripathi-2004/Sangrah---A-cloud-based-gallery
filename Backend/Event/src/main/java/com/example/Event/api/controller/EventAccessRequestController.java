package com.example.Event.api.controller;

import com.example.Event.application.service.EventAccessService;
import com.example.Event.application.service.EventCollaboratorService;
import com.example.Event.application.service.EventService;
import com.example.Event.infrastructure.persistence.document.EventAccessRequestDocument;
import com.example.Event.infrastructure.persistence.document.EventDocument;
import com.example.Event.infrastructure.persistence.repository.EventAccessRequestRepository;
import com.example.Event.infrastructure.client.UserServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/events/{eventId}/access-requests")
@RequiredArgsConstructor
public class EventAccessRequestController {
    private final EventAccessService accessService;
    private final EventService eventService;
    private final EventCollaboratorService collaboratorService;
    private final EventAccessRequestRepository accessRequestRepository;
    private final UserServiceClient userServiceClient;

    @PostMapping
    @Operation(summary = "Request access to protected event")
    public ResponseEntity<?> request(
            @PathVariable String eventId,
            @RequestBody(required = false) Map<String, String> payload,
            HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            EventDocument event = eventService.getEventById(eventId);

            // Prevent requesting your own event
            if (userId.equals(event.getOwnerUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You are the event owner. No need to request access."));
            }

            // Only allow for PROTECTED events
            if (!"PROTECTED".equals(event.getVisibility())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Access requests only allowed for PROTECTED events."));
            }

            // Prevent duplicate pending requests
            Optional<EventAccessRequestDocument> existingPendingRequest =
                    accessRequestRepository.findByEventIdAndRequesterUserIdAndStatus(eventId, userId, "PENDING");
            if (existingPendingRequest.isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "You have already requested access to this event. Waiting for approval."));
            }

            String message = payload != null ? payload.get("message") : null;
            EventAccessRequestDocument accessRequest = accessService.requestAccess(eventId, userId, message);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "requestId", accessRequest.getId(),
                    "status", "PENDING",
                    "message", "Access request has been sent to the event owner"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "List event access requests (owner only)")
    public ResponseEntity<?> list(
            @PathVariable String eventId,
            HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            EventDocument event = eventService.getEventById(eventId);

            // Only owner or collaborators with canReviewAccessRequests can view
            if (!userId.equals(event.getOwnerUserId())) {
                boolean hasPermission = collaboratorService.hasPermission(eventId, userId, "canReviewAccessRequests");
                if (!hasPermission) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "Only event owner and authorized collaborators can view access requests"));
                }
            }

            List<EventAccessRequestDocument> requests = accessService.listPendingRequests(eventId, userId);

            // Enrich requests with display names
            List<Map<String, Object>> enrichedRequests = new java.util.ArrayList<>();
            for (EventAccessRequestDocument r : requests) {
                Map<String, Object> enriched = new java.util.HashMap<>();
                enriched.put("requestId", r.getId());
                enriched.put("requesterUserId", r.getRequesterUserId());
                
                // Fetch display name from Auth service
                try {
                    String displayName = userServiceClient.getUserDisplayName(r.getRequesterUserId());
                    enriched.put("displayName", displayName);
                } catch (Exception e) {
                    // Fallback to user ID if service call fails
                    enriched.put("displayName", r.getRequesterUserId());
                }
                
                enriched.put("message", r.getMessage() != null ? r.getMessage() : "");
                enriched.put("status", r.getStatus());
                enriched.put("requestedAt", r.getRequestedAt().toString());
                enrichedRequests.add(enriched);
            }

            return ResponseEntity.ok(Map.of(
                    "eventId", eventId,
                    "requests", enrichedRequests
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{requestId}/approve")
    @Operation(summary = "Approve event access request (owner only)")
    public ResponseEntity<?> approve(
            @PathVariable String eventId,
            @PathVariable String requestId,
            @RequestBody(required = false) Map<String, Object> payload,
            HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            EventDocument event = eventService.getEventById(eventId);

            // Verify authority
            if (!userId.equals(event.getOwnerUserId())) {
                boolean hasPermission = collaboratorService.hasPermission(eventId, userId, "canReviewAccessRequests");
                if (!hasPermission) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "You don't have permission to approve access requests"));
                }
            }

            // Get approval parameters
            String approvalDuration = "FOREVER";
            Instant accessExpiresAt = null;
            if (payload != null) {
                approvalDuration = (String) payload.getOrDefault("approvalDuration", "FOREVER");
                if ("UNTIL_DATE".equals(approvalDuration) && payload.containsKey("accessExpiresAt")) {
                    accessExpiresAt = Instant.parse((String) payload.get("accessExpiresAt"));
                }
            }

            EventAccessRequestDocument approved = accessService.approveRequest(
                    eventId, requestId, userId, approvalDuration, accessExpiresAt);

            return ResponseEntity.ok(Map.of(
                    "status", "APPROVED",
                    "message", "Access request approved",
                    "approvalDuration", approvalDuration,
                    "accessExpiresAt", accessExpiresAt != null ? accessExpiresAt.toString() : "Never"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{requestId}/reject")
    @Operation(summary = "Reject event access request (owner only)")
    public ResponseEntity<?> reject(
            @PathVariable String eventId,
            @PathVariable String requestId,
            @RequestBody(required = false) Map<String, String> payload,
            HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            EventDocument event = eventService.getEventById(eventId);

            // Verify authority
            if (!userId.equals(event.getOwnerUserId())) {
                boolean hasPermission = collaboratorService.hasPermission(eventId, userId, "canReviewAccessRequests");
                if (!hasPermission) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "You don't have permission to reject access requests"));
                }
            }

            String reason = payload != null ? payload.get("reason") : "Request denied";
            EventAccessRequestDocument rejected = accessService.rejectRequest(eventId, requestId, userId, reason);

            return ResponseEntity.ok(Map.of(
                    "status", "REJECTED",
                    "message", "Access request rejected",
                    "reason", reason
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
