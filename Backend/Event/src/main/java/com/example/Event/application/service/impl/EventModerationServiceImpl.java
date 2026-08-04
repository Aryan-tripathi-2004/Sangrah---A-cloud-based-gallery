package com.example.Event.application.service.impl;

import com.example.Event.api.dto.request.MediaRejectionRequest;
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
import com.example.Event.infrastructure.client.EmailServiceClient;
import com.example.Event.infrastructure.client.MediaServiceClient;
import com.example.Event.infrastructure.client.NotificationServiceClient;
import com.example.Event.infrastructure.client.UserServiceClient;
import com.example.Event.infrastructure.client.dto.MediaServiceResponse;
import com.example.Event.infrastructure.persistence.document.EventDocument;
import com.example.Event.infrastructure.persistence.document.EventMediaApprovalDocument;
import com.example.Event.infrastructure.persistence.repository.EventMediaApprovalRepository;
import com.example.Event.shared.exception.AuthenticationRequiredException;
import com.example.Event.shared.exception.DomainValidationException;
import com.example.Event.shared.exception.ForbiddenOperationException;
import com.example.Event.shared.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventModerationServiceImpl implements IEventModerationService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final EventMediaApprovalRepository approvalRepository;
    private final NotificationServiceClient notificationServiceClient;
    private final EmailServiceClient emailServiceClient;
    private final UserServiceClient userServiceClient;
    private final IEventService eventService;
    private final IEventCollaboratorService collaboratorService;
    private final IEventAccessService accessService;
    private final MediaServiceClient mediaServiceClient;

    @Override
    public EventMediaFileResponse getMediaFile(String eventId, String mediaId, HttpServletRequest httpRequest) {
        String userId = optionalUserId(httpRequest);
        EventDocument event = eventService.getEventById(eventId);

        if ("PROTECTED".equals(event.getVisibility()) && (userId == null || !userId.equals(event.getOwnerUserId()))
                && !isDocumentCollaborator(event, userId) && !accessService.isUserApproved(eventId, userId)) {
            throw new ForbiddenOperationException("You don't have permission to access this protected event");
        }

        List<EventMediaApprovalDocument> approvals = approvalRepository.findByEventIdAndMediaIdOrderByStatusAsc(eventId, mediaId);
        if (approvals.isEmpty()) {
            throw new ResourceNotFoundException("Media not found in event");
        }

        EventMediaApprovalDocument approval = approvals.stream()
                .filter(candidate -> STATUS_APPROVED.equals(candidate.getStatus()))
                .findFirst()
                .orElse(approvals.get(0));

        if (!STATUS_APPROVED.equals(approval.getStatus())) {
            boolean isEventOwner = userId != null && userId.equals(event.getOwnerUserId());
            boolean isUploader = userId != null && userId.equals(approval.getUploaderUserId());
            if (!isEventOwner && !isUploader) {
                throw new ForbiddenOperationException("Media is pending approval and you don't have permission to view it");
            }
        }

        byte[] fileBytes = mediaServiceClient.getMediaFile(mediaId, userId != null ? userId : "system");
        MediaType contentType = resolveContentType(mediaId);
        return new EventMediaFileResponse(new ByteArrayResource(fileBytes), contentType);
    }

    @Override
    public EventMediaUploadResponse uploadMedia(String eventId, MultipartFile file, HttpServletRequest httpRequest) {
        String userId = requireUserId(httpRequest, "User ID not found");
        String userEmail = optionalUserEmail(httpRequest);

        if (file == null || file.isEmpty()) {
            throw new DomainValidationException("File is empty");
        }

        EventDocument event = eventService.getEventById(eventId);
        String approvalStatus = determineModerationStatus(event, userId, userEmail);

        MediaServiceResponse mediaResponse = mediaServiceClient.uploadMedia(file, "EVENTS", eventId, userId);
        String mediaId = extractMediaId(mediaResponse);
        boolean isPending = STATUS_PENDING.equals(approvalStatus);
        createMedia(eventId, mediaId, userId, isPending);

        String message = switch (approvalStatus) {
            case STATUS_APPROVED -> "Media uploaded successfully and auto-approved";
            case STATUS_PENDING -> "Media uploaded successfully, awaiting approval";
            default -> "Media upload failed - check permissions";
        };

        return new EventMediaUploadResponse(mediaId, approvalStatus, message);
    }

    @Override
    public EventMediaCollectionResponse getTimeline(String eventId, HttpServletRequest httpRequest) {
        String userId = optionalUserId(httpRequest);
        EventDocument event = eventService.getEventById(eventId);
        requireTimelineAccess(event, userId);

        List<EventMediaItemResponse> media = approvalRepository
                .findByEventIdAndStatusOrderByCreatedAtDesc(eventId, STATUS_APPROVED)
                .stream()
                .map(this::toTimelineItem)
                .toList();

        return new EventMediaCollectionResponse(eventId, media, "Timeline retrieved successfully");
    }

    @Override
    public EventMediaCollectionResponse listEventMedia(String eventId, HttpServletRequest httpRequest) {
        String userId = optionalUserId(httpRequest);
        EventDocument event = eventService.getEventById(eventId);
        requireProtectedMediaListAccess(event, userId);

        List<EventMediaItemResponse> media = approvalRepository.findByEventId(eventId).stream()
                .map(this::toDetailedMediaItem)
                .toList();

        return new EventMediaCollectionResponse(eventId, media, "Event media loaded");
    }

    @Override
    public EventMediaDetailResponse getMedia(String eventId, String mediaId) {
        MediaServiceResponse mediaDetails = mediaServiceClient.getMediaDetails(mediaId);
        String moderationStatus = getMediaStatus(eventId, mediaId);
        return new EventMediaDetailResponse(mediaId, eventId, mediaDetails, moderationStatus);
    }

    @Override
    public EventMediaModerationResponse approveMedia(String eventId, String mediaId, HttpServletRequest httpRequest) {
        String userId = requireUserId(httpRequest, "User ID not found");
        requireMediaReviewer(eventId, userId, "You don't have permission to approve media");
        String status = approveMediaInternal(eventId, mediaId, userId);
        return new EventMediaModerationResponse(status, null, "Media approved successfully");
    }

    @Override
    public EventMediaModerationResponse rejectMedia(
            String eventId,
            String mediaId,
            MediaRejectionRequest request,
            HttpServletRequest httpRequest) {
        String userId = requireUserId(httpRequest, "User ID not found");
        String reason = request != null ? request.resolvedReason() : "Not specified";
        requireMediaReviewer(eventId, userId, "You don't have permission to reject media");
        String status = rejectMediaInternal(eventId, mediaId, userId, reason);
        return new EventMediaModerationResponse(status, reason, "Media rejected");
    }

    @Override
    public MessageResponse deleteMedia(String eventId, String mediaId, HttpServletRequest httpRequest) {
        String userId = requireUserId(httpRequest, "User ID not found");
        mediaServiceClient.deleteMedia(mediaId, userId);
        deleteMediaApproval(eventId, mediaId);
        return new MessageResponse("Media deleted successfully");
    }

    @Override
    public String createMedia(String eventId, String mediaId, String uploaderUserId, boolean moderationEnabled) {
        String status = moderationEnabled ? STATUS_PENDING : STATUS_APPROVED;

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
    public String getMediaStatus(String eventId, String mediaId) {
        return approvalRepository.findByEventIdAndMediaId(eventId, mediaId)
                .map(EventMediaApprovalDocument::getStatus)
                .orElse("NOT_FOUND");
    }

    @Override
    public boolean isMediaApproved(String eventId, String mediaId) {
        return approvalRepository.findByEventIdAndMediaId(eventId, mediaId)
                .filter(EventMediaApprovalDocument::isApproved)
                .isPresent();
    }

    private String approveMediaInternal(String eventId, String mediaId, String reviewedByUserId) {
        EventMediaApprovalDocument approval = findApproval(eventId, mediaId);
        approval.setStatus(STATUS_APPROVED);
        approval.setReviewedByUserId(reviewedByUserId);
        approval.setReviewedAt(Instant.now());
        approval.setUpdatedAt(Instant.now());

        approvalRepository.save(approval);
        sendMediaApprovedNotifications(eventId, mediaId, approval.getUploaderUserId());
        return STATUS_APPROVED;
    }

    private String rejectMediaInternal(String eventId, String mediaId, String reviewedByUserId, String reason) {
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

    private String determineModerationStatus(EventDocument event, String userId, String userEmail) {
        if (userId.equals(event.getOwnerUserId())) {
            return STATUS_APPROVED;
        }

        String visibility = event.getVisibility() != null ? event.getVisibility() : "PRIVATE";
        EventDocument.EventCollaborator collaborator = getCollaboratorIfExists(event, userId, userEmail);

        if (collaborator == null) {
            if ("PRIVATE".equals(visibility)) {
                throw new ForbiddenOperationException("Upload forbidden: Only event owner and collaborators can upload to private events.");
            }
            if ("PROTECTED".equals(visibility) && !accessService.isUserApproved(event.getId(), userId)) {
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
        if ("PRIVATE".equals(event.getVisibility()) && !isOwner(event, userId) && !isDocumentCollaborator(event, userId)) {
            throw new ForbiddenOperationException("You don't have permission to view this event");
        }

        if ("PROTECTED".equals(event.getVisibility()) && userId != null && !isOwner(event, userId)
                && !isDocumentCollaborator(event, userId) && !accessService.isUserApproved(event.getId(), userId)) {
            throw new ForbiddenOperationException("Access denied - you don't have permission to view this event");
        }
    }

    private void requireProtectedMediaListAccess(EventDocument event, String userId) {
        if (!"PROTECTED".equals(event.getVisibility())) {
            return;
        }
        if ((userId == null || !userId.equals(event.getOwnerUserId()))
                && !isDocumentCollaborator(event, userId)
                && !accessService.isUserApproved(event.getId(), userId)) {
            throw new ForbiddenOperationException("You don't have permission to view this event's media");
        }
    }

    private void requireMediaReviewer(String eventId, String userId, String message) {
        EventDocument event = eventService.getEventById(eventId);
        if (userId.equals(event.getOwnerUserId())) {
            return;
        }
        if (!collaboratorService.hasPermission(eventId, userId, "canReviewMedia")) {
            throw new ForbiddenOperationException(message);
        }
    }

    private EventMediaItemResponse toTimelineItem(EventMediaApprovalDocument approval) {
        return new EventMediaItemResponse(
                approval.getMediaId(),
                approval.getMediaId(),
                approval.getStatus(),
                approval.getUploaderUserId(),
                null,
                toIso(approval.getCreatedAt()),
                null,
                null);
    }

    private EventMediaItemResponse toDetailedMediaItem(EventMediaApprovalDocument approval) {
        String uploaderName = userServiceClient.getUserDisplayName(approval.getUploaderUserId());
        MediaServiceResponse mediaDetails = null;
        try {
            mediaDetails = mediaServiceClient.getMediaDetails(approval.getMediaId());
        } catch (Exception e) {
            log.warn("Could not fetch media details for {}: {}", approval.getMediaId(), e.getMessage());
        }

        return new EventMediaItemResponse(
                approval.getMediaId(),
                approval.getMediaId(),
                approval.getStatus(),
                approval.getUploaderUserId(),
                uploaderName,
                toIso(approval.getCreatedAt()),
                mediaDetails != null ? mediaDetails.originalFileName() : null,
                mediaDetails != null ? mediaDetails.mimeType() : null);
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
        EventDocument event = eventService.getEventById(eventId);
        String mediaTitle = resolveMediaTitle(mediaId);

        try {
            notificationServiceClient.notifyMediaApproved(uploaderUserId, eventId, event.getTitle(), mediaTitle);
        } catch (Exception e) {
            log.warn("Failed to send media approved notification: {}", e.getMessage());
        }

        try {
            String uploaderEmail = userServiceClient.getUserEmail(uploaderUserId);
            emailServiceClient.sendMediaApprovedEmail(uploaderEmail, event.getTitle(), mediaTitle);
        } catch (Exception e) {
            log.warn("Failed to send media approved email: {}", e.getMessage());
        }
    }

    private void sendMediaRejectedNotifications(String eventId, String mediaId, String uploaderUserId, String reason) {
        EventDocument event = eventService.getEventById(eventId);
        String mediaTitle = resolveMediaTitle(mediaId);

        try {
            notificationServiceClient.notifyMediaRejected(uploaderUserId, eventId, event.getTitle(), mediaTitle, reason);
        } catch (Exception e) {
            log.warn("Failed to send media rejected notification: {}", e.getMessage());
        }

        try {
            String uploaderEmail = userServiceClient.getUserEmail(uploaderUserId);
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

    private String optionalUserEmail(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String userEmail = request.getHeader("X-User-Email");
        return userEmail == null || userEmail.isBlank() ? null : userEmail;
    }

    private String toIso(Instant instant) {
        return instant == null ? "" : instant.toString();
    }
}
