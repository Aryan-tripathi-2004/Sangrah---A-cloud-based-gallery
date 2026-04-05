package com.example.Event.api.controller;

import com.example.Event.application.service.EventModerationService;
import com.example.Event.application.service.EventService;
import com.example.Event.application.service.EventCollaboratorService;
import com.example.Event.application.service.EventAccessService;
import com.example.Event.infrastructure.client.MediaServiceClient;
import com.example.Event.infrastructure.client.UserServiceClient;
import com.example.Event.infrastructure.persistence.document.EventDocument;
import com.example.Event.infrastructure.persistence.document.EventMediaApprovalDocument;
import com.example.Event.infrastructure.persistence.repository.EventMediaApprovalRepository;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/events/{eventId}")
@RequiredArgsConstructor
public class EventMediaController {
    private final EventModerationService moderationService;
    private final EventService eventService;
    private final MediaServiceClient mediaServiceClient;
    private final UserServiceClient userServiceClient;
    private final EventMediaApprovalRepository approvalRepository;
    private final EventCollaboratorService collaboratorService;
    private final EventAccessService accessService;

    /**
     * Get media file (download/stream) for an event
     * GET /api/v1/events/{eventId}/media/{mediaId}/file
     */
    @GetMapping("/media/{mediaId}/file")
    @Operation(summary = "Get event media file", description = "Download or stream the actual media file.")
    public ResponseEntity<?> getMediaFile(
            @PathVariable String eventId,
            @PathVariable String mediaId,
            HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            log.info("🔥 [Event Media] Fetching file for media {} in event {} by user {}", mediaId, eventId, userId);

            // NEW: Check access control for protected events
            EventDocument event = eventService.getEventById(eventId);
            
            if ("PROTECTED".equals(event.getVisibility())) {
                if (userId == null || !userId.equals(event.getOwnerUserId())) {
                    // Check if user is collaborator
                    boolean isCollaborator = event.getCollaborators() != null &&
                            event.getCollaborators().stream().anyMatch(c -> c.getUserId().equals(userId));

                    if (!isCollaborator) {
                        // Check if user has approved access request
                        boolean approved = accessService != null && accessService.isUserApproved(eventId, userId);
                        if (!approved) {
                            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                    .body(Map.of("error", "You don't have permission to access this protected event"));
                        }
                    }
                }
            }

            // First check if media exists and is approved for this event
            EventMediaApprovalDocument approval = approvalRepository.findByEventIdAndMediaId(eventId, mediaId)
                    .orElseThrow(() -> new RuntimeException("Media not found in event"));

            // We stream it if:
            // 1. It's approved, OR
            // 2. The user is the one who uploaded it, OR
            // 3. The user is the event owner (for preview during approval)
            if (!"APPROVED".equals(approval.getStatus())) {
                boolean isEventOwner = userId != null && userId.equals(event.getOwnerUserId());
                boolean isUploader = userId != null && userId.equals(approval.getUploaderUserId());
                
                if (!isEventOwner && !isUploader) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "Media is pending approval and you don't have permission to view it"));
                }
            }

            // Fetch file bytes from Media Service via Feign
            byte[] fileBytes = mediaServiceClient.getMediaFile(mediaId, userId != null ? userId : "system");

            // Determine content type from Media Service
            String contentType = "application/octet-stream";
            try {
                Map<String, Object> mediaDetails = mediaServiceClient.getMediaDetails(mediaId);
                if (mediaDetails != null && mediaDetails.containsKey("mimeType")) {
                    contentType = (String) mediaDetails.get("mimeType");
                } else if (mediaDetails != null && mediaDetails.containsKey("originalFileName")) {
                    String filename = (String) mediaDetails.get("originalFileName");
                    String nameLower = filename.toLowerCase();
                    if (nameLower.endsWith(".jpg") || nameLower.endsWith(".jpeg")) contentType = "image/jpeg";
                    else if (nameLower.endsWith(".png")) contentType = "image/png";
                    else if (nameLower.endsWith(".gif")) contentType = "image/gif";
                    else if (nameLower.endsWith(".mp4")) contentType = "video/mp4";
                    else if (nameLower.endsWith(".webm")) contentType = "video/webm";
                }
            } catch (Exception ignored) { }

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .body(new org.springframework.core.io.ByteArrayResource(fileBytes));

        } catch (Exception e) {
            log.error("❌ [Event Media] Error fetching file loop: {}", e.getMessage(), e);
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error streaming media file: " + e.getMessage()));
        }
    }

    /**
     * Upload media to an event

     * POST /api/v1/events/{eventId}/media
     */
    @PostMapping("/media")
    @Operation(summary = "Upload event media")
    public ResponseEntity<?> upload(
            @PathVariable String eventId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            log.info("📤 [Event Media] Uploading to event {}, user: {}, file: {}", eventId, userId, file.getOriginalFilename());

            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User ID not found"));
            }

            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "File is empty"));
            }

            EventDocument event = eventService.getEventById(eventId);

            // NEW: Determine moderation status based on user's permissions and event policies
            String moderationStatus = determineModerationStatus(event, userId);

            // Call Media Service to upload (with eventId as entityRefId)
            Map<String, Object> mediaResponse = mediaServiceClient.uploadMedia(
                    file,
                    "EVENTS",
                    eventId,
                    userId
            );

            String mediaId = (String) mediaResponse.get("id");
            log.info("✅ Media uploaded to Media Service: {}", mediaId);

            // Create approval record with determined moderation status
            String approvalStatus = determineModerationStatus(event, userId);
            boolean isPending = "PENDING".equals(approvalStatus);
            moderationService.createMedia(eventId, mediaId, userId, isPending);
            log.info("📝 Moderation status: {} (moderationEnabled: {})",
                    approvalStatus, event.isModerationEnabled());

            String message;
            if ("APPROVED".equals(approvalStatus)) {
                message = "Media uploaded successfully and auto-approved";
            } else if ("PENDING".equals(approvalStatus)) {
                message = "Media uploaded successfully, awaiting approval";
            } else {
                message = "Media upload failed - check permissions";
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "mediaId", mediaId,
                    "moderationStatus", approvalStatus,
                    "message", message
            ));

        } catch (Exception e) {
            log.error("❌ Upload error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload media: " + e.getMessage()));
        }
    }

    /**
     * Determine moderation status based on user permissions and event visibility
     * - Event owner: always APPROVED
     * - Collaborator with canDirectUpload: APPROVED (bypasses all restrictions)
     * - Collaborator with canUploadMedia: PENDING (needs review/approval)
     * - Public/Normal user on PUBLIC event: PENDING if moderation disabled, REJECTED if moderation enabled
     * - Approved accessor on PROTECTED event: PENDING (needs review/approval)
     * - PRIVATE events: only owner and collaborators can upload
     */
    private String determineModerationStatus(EventDocument event, String userId) {
        // Owner always gets auto-approval
        if (userId.equals(event.getOwnerUserId())) {
            log.info("✅ Auto-approving media for event owner");
            return "APPROVED";
        }

        // Check event visibility
        String visibility = event.getVisibility() != null ? event.getVisibility() : "PRIVATE";

        // Check if user is a collaborator
        EventDocument.EventCollaborator collaborator = getCollaboratorIfExists(event, userId);

        // For PRIVATE events: only owner and collaborators with upload permission
        if ("PRIVATE".equals(visibility)) {
            if (collaborator == null) {
                throw new RuntimeException("Only event owner and collaborators can upload media to private events");
            }
            return processCollaboratorUpload(event, userId, collaborator);
        }

        // For PROTECTED and PUBLIC events: check collaborator permissions first
        if (collaborator != null) {
            return processCollaboratorUpload(event, userId, collaborator);
        }

        // Non-collaborator (public/normal user)
        if ("PROTECTED".equals(visibility)) {
            // For PROTECTED events: check if user has approved access request
            boolean hasApprovedAccess = accessService != null && accessService.isUserApproved(event.getId(), userId);
            if (!hasApprovedAccess) {
                throw new RuntimeException("You must request access to upload media to this protected event");
            }
            // Approved accessor to protected event goes to PENDING for review
            log.info("📝 User {} has approved access to protected event - media requires review", userId);
            return "PENDING";
        } else if ("PUBLIC".equals(visibility)) {
            // For PUBLIC events: check if moderation is enabled
            if (event.isModerationEnabled()) {
                // Moderation enabled: only owner and collaborators can upload
                throw new RuntimeException("Moderation is enabled for this event. Only owner and collaborators can upload media.");
            }
            // Moderation disabled: public users can upload but media goes to PENDING for review
            log.info("📝 Public event without moderation lock - any user media requires review");
            return "PENDING";
        }

        // Default: deny access
        throw new RuntimeException("You don't have permission to upload media to this event");
    }

    /**
     * Process collaborator upload with their specific permissions
     * - canDirectUpload: media gets APPROVED immediately
     * - canUploadMedia: media goes to PENDING for review/approval
     */
    private String processCollaboratorUpload(EventDocument event, String userId, EventDocument.EventCollaborator collaborator) {
        // Check canDirectUpload permission (bypasses all restrictions and moderation)
        if (collaborator.getCanDirectUpload() != null && collaborator.getCanDirectUpload()) {
            log.info("✅ Collaborator {} has canDirectUpload permission - auto-approving", userId);
            return "APPROVED";
        }

        // Check canUploadMedia permission
        if (collaborator.getCanUploadMedia() == null || !collaborator.getCanUploadMedia()) {
            throw new RuntimeException("Your collaborator permissions do not allow uploads to this event");
        }

        // Collaborator has canUploadMedia - media always goes to PENDING for review/approval
        // This applies regardless of moderationEnabled setting (event owner or canReviewMedia collaborators will approve)
        log.info("📝 Collaborator {} media requires approval", userId);
        return "PENDING";
    }

    /**
     * Get collaborator if exists for this event and user
     */
    private EventDocument.EventCollaborator getCollaboratorIfExists(EventDocument event, String userId) {
        if (event.getCollaborators() != null) {
            return event.getCollaborators().stream()
                    .filter(c -> c.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    /**
     * Get event timeline (all approved media in chronological order)
     * GET /api/v1/events/{eventId}/timeline
     */
    @GetMapping("/timeline")
    @Operation(summary = "Get event timeline")
    public ResponseEntity<?> timeline(@PathVariable String eventId) {
        try {
            log.info("📋 [Event Media] Fetching timeline for event: {}", eventId);

            List<EventMediaApprovalDocument> approvals = approvalRepository.findByEventIdAndStatusOrderByCreatedAtDesc(eventId, "APPROVED");
            
            List<Map<String, Object>> response = approvals.stream()
                .map(a -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", a.getMediaId());
                    map.put("mediaId", a.getMediaId());
                    map.put("status", a.getStatus());
                    map.put("uploaderUserId", a.getUploaderUserId());
                    map.put("uploadedAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : "");
                    return map;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "eventId", eventId,
                    "media", response,
                    "message", "Timeline retrieved successfully"
            ));

        } catch (Exception e) {
            log.error("❌ Timeline error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get timeline"));
        }
    }

    /**
     * Get all event media (uploaded and pending approval)
     * GET /api/v1/events/{eventId}/media
     */
    @GetMapping("/media")
    @Operation(summary = "Get all event media")
    public ResponseEntity<?> listEventMedia(@PathVariable String eventId, HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            log.info("📋 [Event Media] Fetching all media for event: {} by user: {}", eventId, userId);

            // NEW: Check access control for protected events
            EventDocument event = eventService.getEventById(eventId);
            
            if ("PROTECTED".equals(event.getVisibility())) {
                if (userId == null || !userId.equals(event.getOwnerUserId())) {
                    // Check if user is collaborator
                    boolean isCollaborator = event.getCollaborators() != null &&
                            event.getCollaborators().stream().anyMatch(c -> c.getUserId().equals(userId));

                    if (!isCollaborator) {
                        // Check if user has approved access request
                        boolean approved = accessService != null && accessService.isUserApproved(eventId, userId);
                        if (!approved) {
                            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                    .body(Map.of("error", "You don't have permission to view this event's media"));
                        }
                    }
                }
            }

            List<EventMediaApprovalDocument> approvals = approvalRepository.findByEventId(eventId);

            List<Map<String, Object>> response = approvals.stream()
                .map(a -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", a.getMediaId());
                    map.put("mediaId", a.getMediaId());
                    map.put("status", a.getStatus());
                    map.put("uploaderUserId", a.getUploaderUserId());
                    
                    // Fetch uploader display name
                    String uploaderName = userServiceClient.getUserDisplayName(a.getUploaderUserId());
                    log.info("👤 [Event Media] Uploader: {} -> {}", a.getUploaderUserId(), uploaderName);
                    map.put("uploaderName", uploaderName);
                    
                    map.put("uploadedAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : "");

                    // Enrich with media details from Media Service when available
                    try {
                        Map<String, Object> mediaDetails = mediaServiceClient.getMediaDetails(a.getMediaId());
                        if (mediaDetails != null) {
                            if (mediaDetails.containsKey("originalFileName")) {
                                map.put("originalFileName", mediaDetails.get("originalFileName"));
                            }
                            if (mediaDetails.containsKey("mimeType")) {
                                map.put("mimeType", mediaDetails.get("mimeType"));
                            }
                        }
                    } catch (Exception e) {
                        log.warn("⚠️ Could not fetch media details for {}: {}", a.getMediaId(), e.getMessage());
                    }

                    return map;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "eventId", eventId,
                    "media", response,
                    "message", "Event media loaded"
            ));

        } catch (Exception e) {
            log.error("❌ Get event media error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get event media"));
        }
    }

    /**
     * Get event media details
     * GET /api/v1/events/{eventId}/media/{mediaId}
     */
    @GetMapping("/media/{mediaId}")
    @Operation(summary = "Get event media")
    public ResponseEntity<?> get(@PathVariable String eventId, @PathVariable String mediaId) {
        try {
            log.info("🔍 [Event Media] Fetching media {} in event {}", mediaId, eventId);

            Map<String, Object> mediaDetails = mediaServiceClient.getMediaDetails(mediaId);
            String moderationStatus = moderationService.getMediaStatus(eventId, mediaId);

            return ResponseEntity.ok(Map.of(
                    "mediaId", mediaId,
                    "eventId", eventId,
                    "details", mediaDetails,
                    "moderationStatus", moderationStatus
            ));

        } catch (Exception e) {
            log.error("❌ Get media error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Media not found"));
        }
    }

    /**
     * Approve event media (event owner or collaborator with canReviewMedia)
     * PATCH /api/v1/events/{eventId}/media/{mediaId}/approve
     */
    @PatchMapping("/media/{mediaId}/approve")
    @Operation(summary = "Approve event media")
    public ResponseEntity<?> approve(
            @PathVariable String eventId,
            @PathVariable String mediaId,
            HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            log.info("✅ [Event Media] Approving media {} in event {}, by user: {}", mediaId, eventId, userId);

            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User ID not found"));
            }

            // NEW: Check permission to review media
            EventDocument event = eventService.getEventById(eventId);
            if (!userId.equals(event.getOwnerUserId())) {
                boolean hasPermission = collaboratorService.hasPermission(eventId, userId, "canReviewMedia");
                if (!hasPermission) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "You don't have permission to approve media"));
                }
            }

            String status = moderationService.approveMedia(eventId, mediaId, userId);

            return ResponseEntity.ok(Map.of(
                    "status", status,
                    "message", "Media approved successfully"
            ));

        } catch (Exception e) {
            log.error("❌ Approve error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Reject event media with reason (event owner or collaborator with canReviewMedia)
     * PATCH /api/v1/events/{eventId}/media/{mediaId}/reject
     */
    @PatchMapping("/media/{mediaId}/reject")
    @Operation(summary = "Reject event media")
    public ResponseEntity<?> reject(
            @PathVariable String eventId,
            @PathVariable String mediaId,
            @RequestParam(value = "reason", defaultValue = "Not specified") String reason,
            HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            log.info("❌ [Event Media] Rejecting media {} in event {}, reason: {}, by user: {}",
                mediaId, eventId, reason, userId);

            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User ID not found"));
            }

            // NEW: Check permission to review media
            EventDocument event = eventService.getEventById(eventId);
            if (!userId.equals(event.getOwnerUserId())) {
                boolean hasPermission = collaboratorService.hasPermission(eventId, userId, "canReviewMedia");
                if (!hasPermission) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "You don't have permission to reject media"));
                }
            }

            String status = moderationService.rejectMedia(eventId, mediaId, userId, reason);

            return ResponseEntity.ok(Map.of(
                    "status", status,
                    "reason", reason,
                    "message", "Media rejected"
            ));

        } catch (Exception e) {
            log.error("❌ Reject error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete event media
     * DELETE /api/v1/events/{eventId}/media/{mediaId}
     */
    @DeleteMapping("/media/{mediaId}")
    @Operation(summary = "Delete event media")
    public ResponseEntity<?> delete(
            @PathVariable String eventId,
            @PathVariable String mediaId,
            HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            log.info("🗑️ [Event Media] Deleting media {} in event {}, user: {}", mediaId, eventId, userId);

            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User ID not found"));
            }

            // Call Media Service to delete
            mediaServiceClient.deleteMedia(mediaId, userId);
            log.info("✅ Media deleted from Media Service");

            // Remove approval record from database
            moderationService.deleteMedia(eventId, mediaId);
            log.info("✅ Media approval record deleted from database");

            return ResponseEntity.ok(Map.of(
                    "message", "Media deleted successfully"
            ));

        } catch (Exception e) {
            log.error("❌ Delete error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to delete media: " + e.getMessage()));
        }
    }
}

