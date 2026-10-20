package com.example.Event.api.controller;

import com.example.Event.api.annotation.CurrentUserId;
import com.example.Event.api.dto.request.MediaModerationRequest;
import com.example.Event.api.dto.response.EventMediaCollectionResponse;
import com.example.Event.api.dto.response.EventMediaDetailResponse;
import com.example.Event.shared.enums.ApprovalStatus;
import com.example.Event.api.dto.response.EventMediaFileResponse;
import com.example.Event.api.dto.response.EventMediaModerationResponse;
import com.example.Event.api.dto.response.EventMediaUploadResponse;
import com.example.Event.api.dto.response.MessageResponse;
import com.example.Event.application.service.interfaces.IEventModerationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import com.example.Event.shared.exception.DomainValidationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;





















@Validated
@RestController
@RequestMapping("/api/v1/events/{eventId}")
@RequiredArgsConstructor
public class EventMediaController {
    private final IEventModerationService moderationService;

    @PostMapping("/media")
    @Operation(summary = "Upload event media")
    public ResponseEntity<EventMediaUploadResponse> upload(
            @PathVariable String eventId,
            @RequestParam("file") MultipartFile file,
            @CurrentUserId String userId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moderationService.uploadMedia(eventId, file, userId, userEmail));
    }

    @GetMapping("/media")
    @Operation(summary = "Get event media collection", description = "Use ?view=timeline to group media sequentially")
    public ResponseEntity<EventMediaCollectionResponse> getMediaCollection(
            @PathVariable String eventId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(defaultValue = "list") String view) {
            
        return ResponseEntity.ok(moderationService.getMediaCollection(eventId, userId, view));
    }

    @GetMapping(value = "/media/{mediaId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE, "image/*", "video/*"})
    @Operation(summary = "Get event media details or file stream")
    public ResponseEntity<?> getMedia(
            @PathVariable String eventId,
            @PathVariable String mediaId,
            @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE) String acceptHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
            
        if (acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE)) {
            return ResponseEntity.ok(moderationService.getMedia(eventId, mediaId));
        }

        EventMediaFileResponse response = moderationService.getMediaFile(eventId, mediaId, userId);
        return ResponseEntity.ok()
                .contentType(response.contentType())
                .body(response.content());
    }

    @PatchMapping("/media/{mediaId}/status")
    @Operation(summary = "Update media moderation status")
    public ResponseEntity<EventMediaModerationResponse> updateMediaStatus(
            @PathVariable String eventId,
            @PathVariable String mediaId,
            @Valid @RequestBody MediaModerationRequest request,
            @CurrentUserId String userId) {
        
        return switch (request.status()) {
            case APPROVED -> ResponseEntity.ok(moderationService.approveMedia(eventId, mediaId, userId));
            case REJECTED -> ResponseEntity.ok(moderationService.rejectMedia(eventId, mediaId, request.rejectionDetails(), userId));
            default -> throw new DomainValidationException("Invalid status transition requested. Allowed: APPROVED, REJECTED");
        };
    }

    @DeleteMapping("/media/{mediaId}")
    @Operation(summary = "Delete event media")
    public ResponseEntity<MessageResponse> delete(
            @PathVariable String eventId,
            @PathVariable String mediaId,
            @CurrentUserId String userId) {
        return ResponseEntity.ok(moderationService.deleteMedia(eventId, mediaId, userId));
    }
}
