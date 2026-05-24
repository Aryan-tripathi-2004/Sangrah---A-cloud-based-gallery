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

    @PatchMapping("/{requestId}/revoke")
    @Operation(summary = "Revoke approved event access (owner only)")
    public ResponseEntity<?> revoke(
            @PathVariable String eventId,
            @PathVariable String requestId,
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
                            .body(Map.of("error", "You don't have permission to revoke access"));
                }
            }

            EventAccessRequestDocument accessRequest = accessRequestRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));

            if (!eventId.equals(accessRequest.getEventId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Request does not belong to this event"));
            }

            if (!"APPROVED".equals(accessRequest.getStatus())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Only approved requests can be revoked"));
            }

            accessService.revokeAccess(eventId, accessRequest.getRequesterUserId(), userId);

            return ResponseEntity.ok(Map.of(
                    "status", "REVOKED",
                    "message", "Access revoked successfully"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * NEW: User re-requests access after expiration or revocation
     * PATCH /api/v1/events/{eventId}/access-requests/re-request
     * CRITICAL: Re-request goes BACK to PENDING (requires manual owner approval, not auto-approved)
     */
    @PatchMapping("/re-request")
    @Operation(summary = "User re-requests access after expiration or revocation")
    public ResponseEntity<?> reRequest(
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

            // Only allow for PROTECTED events
            if (!"PROTECTED".equals(event.getVisibility())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Re-requests only allowed for PROTECTED events"));
            }

            // Check if user has prior access request (should exist)
            Optional<EventAccessRequestDocument> existing =
                    accessRequestRepository.findByEventIdAndRequesterUserId(eventId, userId);
            if (!existing.isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No prior access request found. Please make an initial request."));
            }

            // Check if current status allows re-request (REVOKED or expired)
            EventAccessRequestDocument doc = existing.get();
            if ("PENDING".equals(doc.getStatus())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Your request is already pending approval"));
            }
            if ("APPROVED".equals(doc.getStatus()) &&
                (doc.getAccessExpiresAt() == null || Instant.now().isBefore(doc.getAccessExpiresAt()))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Your access is still active"));
            }

            String message = payload != null ? payload.get("message") : null;
            EventAccessRequestDocument reRequest = accessService.createReRequest(eventId, userId, message);

            return ResponseEntity.ok(Map.of(
                    "requestId", reRequest.getId(),
                    "status", "PENDING",
                    "message", "Your access request has been re-submitted and is awaiting owner approval"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * NEW: Get current user's access status for an event
     * GET /api/v1/events/{eventId}/access-status
     */
    @GetMapping("/access-status")
    @Operation(summary = "Get current user's access status for event")
    public ResponseEntity<?> getAccessStatus(
            @PathVariable String eventId,
            HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            EventDocument event = eventService.getEventById(eventId);

            // Check if user is owner
            if (userId.equals(event.getOwnerUserId())) {
                return ResponseEntity.ok(Map.of(
                        "status", "OWNER",
                        "hasAccess", true,
                        "showRequestButton", false
                ));
            }

            // Check if user is collaborator
            boolean isCollaborator = event.getCollaborators() != null &&
                    event.getCollaborators().stream().anyMatch(c -> c.getUserId().equals(userId));
            if (isCollaborator) {
                return ResponseEntity.ok(Map.of(
                        "status", "COLLABORATOR",
                        "hasAccess", true,
                        "showRequestButton", false
                ));
            }

            // Check access request status
            Optional<EventAccessRequestDocument> request_doc =
                    accessRequestRepository.findByEventIdAndRequesterUserId(eventId, userId);

            if (!request_doc.isPresent()) {
                // No request yet
                return ResponseEntity.ok(Map.of(
                        "status", "NONE",
                        "hasAccess", false,
                        "showRequestButton", "PROTECTED".equals(event.getVisibility())
                ));
            }

            EventAccessRequestDocument accessRequest = request_doc.get();
            String status = accessRequest.getStatus();

            if ("PENDING".equals(status)) {
                return ResponseEntity.ok(Map.of(
                        "status", "PENDING",
                        "hasAccess", false,
                        "showRequestButton", false,
                        "createdAt", accessRequest.getRequestedAt().toString()
                ));
            } else if ("APPROVED".equals(status)) {
                boolean expired = accessRequest.getAccessExpiresAt() != null &&
                        Instant.now().isAfter(accessRequest.getAccessExpiresAt());
                if (expired) {
                    return ResponseEntity.ok(Map.of(
                            "status", "EXPIRED",
                            "hasAccess", false,
                            "showRequestButton", true,
                            "expiresAt", accessRequest.getAccessExpiresAt().toString()
                    ));
                } else {
                    return ResponseEntity.ok(Map.of(
                            "status", "APPROVED",
                            "hasAccess", true,
                            "showRequestButton", false,
                            "expiresAt", accessRequest.getAccessExpiresAt() != null ?
                                    accessRequest.getAccessExpiresAt().toString() : "FOREVER"
                    ));
                }
            } else if ("REJECTED".equals(status)) {
                return ResponseEntity.ok(Map.of(
                        "status", "REJECTED",
                        "hasAccess", false,
                        "showRequestButton", true,
                        "rejectionReason", accessRequest.getRejectionReason()
                ));
            } else if ("REVOKED".equals(status)) {
                return ResponseEntity.ok(Map.of(
                        "status", "REVOKED",
                        "hasAccess", false,
                        "showRequestButton", true,
                        "revokedAt", accessRequest.getRevokedAt().toString()
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "status", "UNKNOWN",
                    "hasAccess", false,
                    "showRequestButton", false
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
