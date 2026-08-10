package com.example.Event.api.controller;

import com.example.Event.api.dto.request.MediaRejectionRequest;
import com.example.Event.api.dto.response.EventMediaCollectionResponse;
import com.example.Event.api.dto.response.EventMediaDetailResponse;
import com.example.Event.api.dto.response.EventMediaFileResponse;
import com.example.Event.api.dto.response.EventMediaModerationResponse;
import com.example.Event.api.dto.response.EventMediaUploadResponse;
import com.example.Event.api.dto.response.MessageResponse;
import com.example.Event.application.service.interfaces.IEventModerationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/v1/events/{eventId}")
@RequiredArgsConstructor
public class EventMediaController {
    private final IEventModerationService moderationService;

    @GetMapping("/media/{mediaId}/file")
    @Operation(summary = "Get event media file", description = "Download or stream the actual media file.")
    public ResponseEntity<ByteArrayResource> getMediaFile(
            @PathVariable String eventId,
            @PathVariable String mediaId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        EventMediaFileResponse response = moderationService.getMediaFile(eventId, mediaId, userId);
        return ResponseEntity.ok()
                .contentType(response.contentType())
                .body(response.content());
    }

    @PostMapping("/media")
    @Operation(summary = "Upload event media")
    public ResponseEntity<EventMediaUploadResponse> upload(
            @PathVariable String eventId,
            @RequestParam("file") MultipartFile file,
            @NotBlank @RequestHeader(value = "X-User-Id", required = true) String userId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moderationService.uploadMedia(eventId, file, userId, userEmail));
    }

    @GetMapping("/timeline")
    @Operation(summary = "Get event timeline")
    public ResponseEntity<EventMediaCollectionResponse> timeline(
            @PathVariable String eventId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(moderationService.getTimeline(eventId, userId));
    }

    @GetMapping("/media")
    @Operation(summary = "Get all event media")
    public ResponseEntity<EventMediaCollectionResponse> listEventMedia(
            @PathVariable String eventId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(moderationService.listEventMedia(eventId, userId));
    }

    @GetMapping("/media/{mediaId}")
    @Operation(summary = "Get event media")
    public ResponseEntity<EventMediaDetailResponse> get(
            @PathVariable String eventId,
            @PathVariable String mediaId) {
        return ResponseEntity.ok(moderationService.getMedia(eventId, mediaId));
    }

    @PatchMapping("/media/{mediaId}/approve")
    @Operation(summary = "Approve event media")
    public ResponseEntity<EventMediaModerationResponse> approve(
            @PathVariable String eventId,
            @PathVariable String mediaId,
            @NotBlank @RequestHeader(value = "X-User-Id", required = true) String userId) {
        return ResponseEntity.ok(moderationService.approveMedia(eventId, mediaId, userId));
    }

    @PatchMapping("/media/{mediaId}/reject")
    @Operation(summary = "Reject event media")
    public ResponseEntity<EventMediaModerationResponse> reject(
            @PathVariable String eventId,
            @PathVariable String mediaId,
            @Valid @RequestBody(required = false) MediaRejectionRequest request,
            @NotBlank @RequestHeader(value = "X-User-Id", required = true) String userId) {
        return ResponseEntity.ok(moderationService.rejectMedia(eventId, mediaId, request, userId));
    }

    @DeleteMapping("/media/{mediaId}")
    @Operation(summary = "Delete event media")
    public ResponseEntity<MessageResponse> delete(
            @PathVariable String eventId,
            @PathVariable String mediaId,
            @NotBlank @RequestHeader(value = "X-User-Id", required = true) String userId) {
        return ResponseEntity.ok(moderationService.deleteMedia(eventId, mediaId, userId));
    }
}
