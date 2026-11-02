package com.example.Event.application.service.impl;

import com.example.Event.api.dto.request.MediaModerationRequest;
import com.example.Event.api.dto.response.EventMediaCollectionResponse;
import com.example.Event.api.dto.response.EventMediaDetailResponse;
import com.example.Event.api.dto.response.EventMediaFileResponse;
import com.example.Event.api.dto.response.EventMediaItemResponse;
import com.example.Event.api.dto.response.EventMediaModerationResponse;
import com.example.Event.api.dto.response.EventMediaUploadResponse;
import com.example.Event.api.dto.response.MessageResponse;
import com.example.Event.application.service.interfaces.IEventAccessService;
import com.example.Event.application.service.interfaces.IEventCollaboratorService;
import com.example.Event.application.service.interfaces.IEventModerationService;
import com.example.Event.application.service.interfaces.IEventService;
import com.example.Event.infrastructure.persistence.repository.EventRepository;
import com.example.Event.infrastructure.client.EmailServiceClient;
import com.example.Event.infrastructure.client.MediaServiceClient;
import com.example.Event.infrastructure.client.NotificationIntegrationService;
import com.example.Event.infrastructure.client.UserIntegrationService;
import com.example.Event.infrastructure.client.dto.MediaServiceResponse;
import com.example.Event.infrastructure.mapper.EventMediaMapper;
import com.example.Event.infrastructure.persistence.document.EventDocument;
import com.example.Event.infrastructure.persistence.document.EventMediaApprovalDocument;
import com.example.Event.infrastructure.persistence.document.OrphanedMediaLogDocument;
import com.example.Event.infrastructure.persistence.repository.EventMediaApprovalRepository;
import com.example.Event.infrastructure.persistence.repository.OrphanedMediaLogRepository;
import com.example.Event.shared.enums.ApprovalStatus;
import com.example.Event.shared.enums.EventVisibility;
import com.example.Event.shared.enums.MediaDomain;
import com.example.Event.shared.exception.DomainValidationException;
import com.example.Event.shared.exception.ForbiddenOperationException;
import com.example.Event.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventModerationServiceImpl implements IEventModerationService {

    private static final ApprovalStatus STATUS_PENDING = ApprovalStatus.PENDING;
    private static final ApprovalStatus STATUS_APPROVED = ApprovalStatus.APPROVED;
    private static final ApprovalStatus STATUS_REJECTED = ApprovalStatus.REJECTED;

    private final EventMediaApprovalRepository approvalRepository;
    private final OrphanedMediaLogRepository orphanedMediaLogRepository;
    private final NotificationIntegrationService notificationIntegrationService;
    private final EmailServiceClient emailServiceClient;
    private final UserIntegrationService userIntegrationService;
    private final IEventService eventService;
    private final EventRepository eventRepository;
    private final IEventCollaboratorService collaboratorService;
    private final IEventAccessService accessService;
    private final MediaServiceClient mediaServiceClient;
    private final EventMediaMapper mediaMapper;

    @Override
    public EventMediaFileResponse getMediaFile(String eventId, String mediaId, String userId) {
        EventDocument event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        // Visibility authorization: PRIVATE requires owner or collaborator
        if (EventVisibility.PRIVATE == event.getVisibility() && !isOwner(event, userId) && !isDocumentCollaborator(event, userId)) {
            throw new ForbiddenOperationException("You don't have permission to view media in this private event");
        }

        // Visibility authorization: PROTECTED requires owner, collaborator, or approved access
        if (EventVisibility.PROTECTED == event.getVisibility() && !isOwner(event, userId)
                && !isDocumentCollaborator(event, userId) && !accessService.isUserApproved(eventId, userId)) {
            throw new ForbiddenOperationException("You don't have permission to access this protected event");
        }

        List<EventMediaApprovalDocument> approvals = approvalRepository.findByEventIdAndMediaIdOrderByStatusAsc(eventId, mediaId);
        if (approvals.isEmpty()) {
            throw new ResourceNotFoundException("Media not found in event");
        }

        EventMediaApprovalDocument approval = approvals.stream()
                .filter(candidate -> STATUS_APPROVED == candidate.getStatus())
                .findFirst()
                .orElse(approvals.get(0));

        if (STATUS_APPROVED != approval.getStatus()) {
            boolean isEventOwner = isOwner(event, userId);
            boolean isUploader = userId != null && userId.equals(approval.getUploaderUserId());
            boolean isReviewer = userId != null && collaboratorService.hasPermission(eventId, userId, "canReviewMedia");
            if (!isEventOwner && !isUploader && !isReviewer) {
                throw new ForbiddenOperationException("Media is pending approval and you don't have permission to view it");
            }
        }

        // Retrieve binary stream on behalf of the authorized event context
        byte[] fileBytes = mediaServiceClient.getMediaFile(mediaId, event.getOwnerUserId());
        MediaType contentType = resolveContentType(mediaId);
        return mediaMapper.toFileResponse(fileBytes, contentType);
    }

    @Override
    public EventMediaUploadResponse uploadMedia(String eventId, MultipartFile file, String userId, String userEmail) {
        if (file == null || file.isEmpty()) {
            throw new DomainValidationException("File is empty");
        }

        EventDocument event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        ApprovalStatus approvalStatus = determineModerationStatus(event, userId, userEmail);

        // The storage quota and billing ledger belong to the Event Owner, not the guest/collaborator uploader
        String storageOwnerUserId = event.getOwnerUserId();
        MediaServiceResponse mediaResponse = mediaServiceClient.uploadMedia(file, MediaDomain.EVENTS, eventId, storageOwnerUserId);
        String mediaId = extractMediaId(mediaResponse);
        boolean isPending = STATUS_PENDING == approvalStatus;
        try {
            // Track the original uploader in the event approval metadata
            createMedia(eventId, mediaId, userId, isPending);
        } catch (Exception ex) {
            log.error("Failed to persist event media metadata for media {}. Compensating by deleting remote media: {}", mediaId, ex.getMessage());
            try {
                mediaServiceClient.deleteMedia(mediaId, storageOwnerUserId, "SYSTEM");
                log.warn("🧹 Cleaned up orphaned remote media {} after local database failure", mediaId);
            } catch (Exception cleanupError) {
                log.error("Compensating deleteMedia failed for media {}: {}", mediaId, cleanupError.getMessage(), cleanupError);
                try {
                    orphanedMediaLogRepository.save(OrphanedMediaLogDocument.builder()
                            .eventId(eventId)
                            .oldMediaId(mediaId)
                            .userId(userId)
                            .errorMessage(cleanupError.getMessage())
                            .createdAt(Instant.now())
                            .build());
                } catch (Exception orphanLogError) {
                    log.error("Failed to persist orphaned media log for {}: {}", mediaId, orphanLogError.getMessage(), orphanLogError);
                }
            }
            throw ex;
        }

        String message = switch (approvalStatus) {
            case APPROVED -> "Media uploaded successfully and auto-approved";
            case PENDING -> "Media uploaded successfully, awaiting approval";
            default -> "Media upload failed - check permissions";
        };

        return mediaMapper.toUploadResponse(mediaId, approvalStatus, message);
    }

    @Override
    public EventMediaCollectionResponse getMediaCollection(String eventId, String userId, String view) {
        if ("timeline".equalsIgnoreCase(view)) {
            return getTimeline(eventId, userId);
        }
        return listEventMedia(eventId, userId);
    }

    @Override
    public EventMediaCollectionResponse getTimeline(String eventId, String userId) {
        EventDocument event = eventRepository.findById(eventId).orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        requireTimelineAccess(event, userId);

        List<EventMediaItemResponse> media = approvalRepository
                .findByEventIdAndStatusOrderByCreatedAtDesc(eventId, STATUS_APPROVED)
                .stream()
                .map(this::toTimelineItem)
                .toList();

        return mediaMapper.toCollectionResponse(eventId, media, "Timeline retrieved successfully");
    }

    @Override
    public EventMediaCollectionResponse listEventMedia(String eventId, String userId) {
        EventDocument event = eventRepository.findById(eventId).orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        requireProtectedMediaListAccess(event, userId);

        List<EventMediaApprovalDocument> approvals = approvalRepository.findByEventId(eventId);
        Map<String, String> uploaderNameCache = new HashMap<>();

        List<EventMediaItemResponse> media = approvals.stream()
                .map(approval -> toDetailedMediaItem(approval, uploaderNameCache))
                .toList();

        return mediaMapper.toCollectionResponse(eventId, media, "Event media loaded");
    }

    @Override
    public EventMediaDetailResponse getMedia(String eventId, String mediaId) {
        MediaServiceResponse mediaDetails = mediaServiceClient.getMediaDetails(mediaId);
        ApprovalStatus moderationStatus = getMediaStatus(eventId, mediaId);
        return mediaMapper.toDetailResponse(mediaId, eventId, mediaDetails, moderationStatus);
    }

    @Override
    public ResponseEntity<?> getMediaResponse(String eventId, String mediaId, String acceptHeader, String userId) {
        if (acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE)) {
            return ResponseEntity.ok(getMedia(eventId, mediaId));
        }

        EventMediaFileResponse response = getMediaFile(eventId, mediaId, userId);
        return ResponseEntity.ok()
                .contentType(response.contentType())
                .body(response.content());
    }

    @Override
    public EventMediaModerationResponse approveMedia(String eventId, String mediaId, String userId) {
        requireMediaReviewer(eventId, userId, "You don't have permission to approve media");
        ApprovalStatus status = approveMediaInternal(eventId, mediaId, userId);
        return mediaMapper.toModerationResponse(status, null, "Media approved successfully");
    }

    @Override
    public EventMediaModerationResponse rejectMedia(
            String eventId,
            String mediaId,
            String reason,
            String userId) {
        if (reason == null || reason.isBlank()) { reason = "Not specified"; }
        requireMediaReviewer(eventId, userId, "You don't have permission to reject media");
        ApprovalStatus status = rejectMediaInternal(eventId, mediaId, userId, reason);
        return mediaMapper.toModerationResponse(status, reason, "Media rejected");
    }

    @Override
    public EventMediaModerationResponse moderateMedia(
            String eventId,
            String mediaId,
            MediaModerationRequest request,
            String userId) {
        if (request == null || request.status() == null) {
            throw new DomainValidationException("Moderation status is required");
        }
        return switch (request.status()) {
            case APPROVED -> approveMedia(eventId, mediaId, userId);
            case REJECTED -> rejectMedia(eventId, mediaId, request.rejectionDetails(), userId);
            default -> throw new DomainValidationException("Invalid status transition requested. Allowed: APPROVED, REJECTED");
        };
    }

    @Override
    public MessageResponse deleteMedia(String eventId, String mediaId, String userId) {
        EventDocument event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        EventMediaApprovalDocument approval = approvalRepository.findByEventIdAndMediaId(eventId, mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found in event: " + mediaId));

        // Collaborative Event Authorization:
        // 1. Event Owner can delete any media in their event
        // 2. Collaborators with 'canDeleteMedia' permission can delete media
        // 3. The original uploader can delete their own media
        boolean isEventOwner = isOwner(event, userId);
        boolean isUploader = userId != null && userId.equals(approval.getUploaderUserId());
        boolean hasDeletePermission = userId != null && collaboratorService.hasPermission(eventId, userId, "canDeleteMedia");

        if (!isEventOwner && !isUploader && !hasDeletePermission) {
            log.warn("🚨 [Event Media Delete] Unauthorized delete attempt on event {} media {} by user {}", eventId, mediaId, userId);
            throw new ForbiddenOperationException("You do not have permission to delete media from this event");
        }

        // Delegate physical media deletion using event owner context with SYSTEM service role
        mediaServiceClient.deleteMedia(mediaId, event.getOwnerUserId(), "SYSTEM");
        deleteMediaApproval(eventId, mediaId);
        return mediaMapper.toMessageResponse("Media deleted successfully");
    }

    @Override
    @Transactional
    public ApprovalStatus createMedia(String eventId, String mediaId, String uploaderUserId, boolean moderationEnabled) {
        ApprovalStatus status = moderationEnabled ? STATUS_PENDING : STATUS_APPROVED;

        Optional<EventMediaApprovalDocument> existing = approvalRepository.findByEventIdAndMediaId(eventId, mediaId);
        if (existing.isPresent()) {
            log.warn("Approval already exists for media {} in event {}", mediaId, eventId);
            return existing.get().getStatus();
        }

        EventMediaApprovalDocument approval = EventMediaApprovalDocument.builder()
                .eventId(eventId)
                .mediaId(mediaId)
                .uploaderUserId(uploaderUserId)
                .status(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        approvalRepository.save(approval);
        return status;
    }

    @Override
    public ApprovalStatus getMediaStatus(String eventId, String mediaId) {
        return approvalRepository.findByEventIdAndMediaId(eventId, mediaId)
                .map(EventMediaApprovalDocument::getStatus)
                .orElse(ApprovalStatus.NOT_FOUND);
    }

    @Override
    public boolean isMediaApproved(String eventId, String mediaId) {
        return approvalRepository.findByEventIdAndMediaId(eventId, mediaId)
                .filter(EventMediaApprovalDocument::isApproved)
                .isPresent();
    }

    private ApprovalStatus approveMediaInternal(String eventId, String mediaId, String reviewedByUserId) {
        EventMediaApprovalDocument approval = findApproval(eventId, mediaId);
        approval.setStatus(STATUS_APPROVED);
        approval.setReviewedByUserId(reviewedByUserId);
        approval.setReviewedAt(Instant.now());
        approval.setUpdatedAt(Instant.now());

        approvalRepository.save(approval);
        sendMediaApprovedNotifications(eventId, mediaId, approval.getUploaderUserId());
        return STATUS_APPROVED;
    }

    private ApprovalStatus rejectMediaInternal(String eventId, String mediaId, String reviewedByUserId, String reason) {
        EventMediaApprovalDocument approval = findApproval(eventId, mediaId);
        approval.setStatus(STATUS_REJECTED);
        approval.setReviewedByUserId(reviewedByUserId);
        approval.setReviewedAt(Instant.now());
        approval.setRejectionReason(reason);
        approval.setUpdatedAt(Instant.now());

        approvalRepository.save(approval);
        sendMediaRejectedNotifications(eventId, mediaId, approval.getUploaderUserId(), reason);
        return STATUS_REJECTED;
    }

    private void deleteMediaApproval(String eventId, String mediaId) {
        Optional<EventMediaApprovalDocument> approval = approvalRepository.findByEventIdAndMediaId(eventId, mediaId);
        if (approval.isPresent()) {
            approvalRepository.delete(approval.get());
            return;
        }
        log.warn("Approval not found for media {} in event {}", mediaId, eventId);
    }

    private ApprovalStatus determineModerationStatus(EventDocument event, String userId, String userEmail) {
        if (userId.equals(event.getOwnerUserId())) {
            return STATUS_APPROVED;
        }

        EventVisibility visibility = event.getVisibility() != null ? event.getVisibility() : EventVisibility.PRIVATE;
        EventDocument.EventCollaborator collaborator = getCollaboratorIfExists(event, userId, userEmail);

        if (collaborator == null) {
            if (EventVisibility.PRIVATE == visibility) {
                throw new ForbiddenOperationException("Upload forbidden: Only event owner and collaborators can upload to private events.");
            }
            if (EventVisibility.PROTECTED == visibility && !accessService.isUserApproved(event.getId(), userId)) {
                throw new ForbiddenOperationException("Upload forbidden: You must request access to upload media to this protected event.");
            }
        }

        boolean restrictiveMode = event.isModerationEnabled();
        if (restrictiveMode) {
            if (collaborator != null) {
                if (Boolean.TRUE.equals(collaborator.getCanDirectUpload())) {
                    return STATUS_APPROVED;
                }
                if (Boolean.TRUE.equals(collaborator.getCanUploadMedia())) {
                    return STATUS_PENDING;
                }
                throw new ForbiddenOperationException("Upload forbidden: Your collaborator permissions do not allow uploads to this event.");
            }
            throw new ForbiddenOperationException("Upload forbidden: Moderation is enabled for this event. General uploads are completely blocked.");
        }

        if (collaborator != null && Boolean.TRUE.equals(collaborator.getCanDirectUpload())) {
            return STATUS_APPROVED;
        }
        return STATUS_PENDING;
    }

    private EventDocument.EventCollaborator getCollaboratorIfExists(EventDocument event, String userId, String userEmail) {
        if (event.getCollaborators() == null) {
            return null;
        }
        return event.getCollaborators().stream()
                .filter(collaborator -> matchesCollaborator(collaborator, userId, userEmail))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesCollaborator(EventDocument.EventCollaborator collaborator, String userId, String userEmail) {
        String collaboratorId = collaborator.getUserId();
        if (collaboratorId == null) {
            return false;
        }
        return collaboratorId.equals(userId)
                || (userEmail != null && !userEmail.isBlank() && collaboratorId.equalsIgnoreCase(userEmail));
    }

    private void requireTimelineAccess(EventDocument event, String userId) {
        if (EventVisibility.PRIVATE == event.getVisibility() && !isOwner(event, userId) && !isDocumentCollaborator(event, userId)) {
            throw new ForbiddenOperationException("You don't have permission to view this event");
        }

        if (EventVisibility.PROTECTED == event.getVisibility() && userId != null && !isOwner(event, userId)
                && !isDocumentCollaborator(event, userId) && !accessService.isUserApproved(event.getId(), userId)) {
            throw new ForbiddenOperationException("Access denied - you don't have permission to view this event");
        }
    }

    private void requireProtectedMediaListAccess(EventDocument event, String userId) {
        if (EventVisibility.PROTECTED != event.getVisibility()) {
            return;
        }
        if ((userId == null || !userId.equals(event.getOwnerUserId()))
                && !isDocumentCollaborator(event, userId)
                && !accessService.isUserApproved(event.getId(), userId)) {
            throw new ForbiddenOperationException("You don't have permission to view this event's media");
        }
    }

    private void requireMediaReviewer(String eventId, String userId, String message) {
        EventDocument event = eventRepository.findById(eventId).orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        if (userId.equals(event.getOwnerUserId())) {
            return;
        }
        if (!collaboratorService.hasPermission(eventId, userId, "canReviewMedia")) {
            throw new ForbiddenOperationException(message);
        }
    }

    private EventMediaItemResponse toTimelineItem(EventMediaApprovalDocument approval) {
        return mediaMapper.toTimelineItem(approval);
    }

    private EventMediaItemResponse toDetailedMediaItem(EventMediaApprovalDocument approval) {
        return toDetailedMediaItem(approval, new HashMap<>());
    }

    private EventMediaItemResponse toDetailedMediaItem(EventMediaApprovalDocument approval, Map<String, String> uploaderNameCache) {
        String uploaderUserId = approval.getUploaderUserId();
        String uploaderName = null;
        if (uploaderUserId != null && !uploaderUserId.isBlank()) {
            uploaderName = uploaderNameCache.computeIfAbsent(uploaderUserId, id -> {
                try {
                    return userIntegrationService.getUserDisplayName(id);
                } catch (Exception e) {
                    log.warn("Could not fetch display name for user {}: {}", id, e.getMessage());
                    return "User " + id;
                }
            });
        }

        MediaServiceResponse mediaDetails = null;
        try {
            mediaDetails = mediaServiceClient.getMediaDetails(approval.getMediaId());
        } catch (Exception e) {
            log.warn("Could not fetch media details for {}: {}", approval.getMediaId(), e.getMessage());
        }

        return mediaMapper.toDetailedItem(approval, uploaderName, mediaDetails);
    }

    private EventMediaApprovalDocument findApproval(String eventId, String mediaId) {
        return approvalRepository.findByEventIdAndMediaId(eventId, mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media approval not found"));
    }

    private MediaType resolveContentType(String mediaId) {
        String contentType = "application/octet-stream";
        try {
            MediaServiceResponse mediaDetails = mediaServiceClient.getMediaDetails(mediaId);
            if (mediaDetails != null && mediaDetails.mimeType() != null && !mediaDetails.mimeType().isBlank()) {
                contentType = mediaDetails.mimeType();
            } else if (mediaDetails != null && mediaDetails.originalFileName() != null) {
                contentType = contentTypeFromFilename(mediaDetails.originalFileName());
            }
        } catch (Exception e) {
            log.warn("Could not resolve content type for {}: {}", mediaId, e.getMessage());
        }
        return MediaType.parseMediaType(contentType);
    }

    private String contentTypeFromFilename(String filename) {
        String nameLower = filename.toLowerCase();
        if (nameLower.endsWith(".jpg") || nameLower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (nameLower.endsWith(".png")) {
            return "image/png";
        }
        if (nameLower.endsWith(".gif")) {
            return "image/gif";
        }
        if (nameLower.endsWith(".mp4")) {
            return "video/mp4";
        }
        if (nameLower.endsWith(".webm")) {
            return "video/webm";
        }
        return "application/octet-stream";
    }

    private String resolveMediaTitle(String mediaId) {
        try {
            MediaServiceResponse mediaDetails = mediaServiceClient.getMediaDetails(mediaId);
            if (mediaDetails != null && mediaDetails.resolvedTitle() != null && !mediaDetails.resolvedTitle().isBlank()) {
                return mediaDetails.resolvedTitle();
            }
        } catch (Exception e) {
            log.warn("Could not resolve media title for {}: {}", mediaId, e.getMessage());
        }
        return mediaId;
    }

    private void sendMediaApprovedNotifications(String eventId, String mediaId, String uploaderUserId) {
        EventDocument event = eventRepository.findById(eventId).orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        String mediaTitle = resolveMediaTitle(mediaId);

        try {
            notificationIntegrationService.notifyMediaApproved(uploaderUserId, eventId, event.getTitle(), mediaTitle);
        } catch (Exception e) {
            log.warn("Failed to send media approved notification: {}", e.getMessage());
        }

        try {
            String uploaderEmail = userIntegrationService.getUserEmail(uploaderUserId);
            emailServiceClient.sendMediaApprovedEmail(uploaderEmail, event.getTitle(), mediaTitle);
        } catch (Exception e) {
            log.warn("Failed to send media approved email: {}", e.getMessage());
        }
    }

    private void sendMediaRejectedNotifications(String eventId, String mediaId, String uploaderUserId, String reason) {
        EventDocument event = eventRepository.findById(eventId).orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        String mediaTitle = resolveMediaTitle(mediaId);

        try {
            notificationIntegrationService.notifyMediaRejected(uploaderUserId, eventId, event.getTitle(), mediaTitle, reason);
        } catch (Exception e) {
            log.warn("Failed to send media rejected notification: {}", e.getMessage());
        }

        try {
            String uploaderEmail = userIntegrationService.getUserEmail(uploaderUserId);
            emailServiceClient.sendMediaRejectedEmail(uploaderEmail, event.getTitle(), mediaTitle, reason);
        } catch (Exception e) {
            log.warn("Failed to send media rejected email: {}", e.getMessage());
        }
    }

    private String extractMediaId(MediaServiceResponse mediaResponse) {
        if (mediaResponse == null || mediaResponse.resolvedMediaId() == null || mediaResponse.resolvedMediaId().isBlank()) {
            throw new DomainValidationException("Media upload did not return a media ID");
        }
        return mediaResponse.resolvedMediaId();
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

}
