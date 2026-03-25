package com.example.Event.api.controller;

import com.example.Event.application.service.EventModerationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events/{eventId}")
@RequiredArgsConstructor
public class EventMediaController {
    private final EventModerationService moderationService;

    @PostMapping("/media")
    @Operation(summary = "Upload event media")
    public ResponseEntity<Map<String, String>> upload(@PathVariable String eventId, @RequestParam("file") MultipartFile file) {
        String mediaId = UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mediaId", mediaId,
                "status", moderationService.createMedia(eventId, mediaId))
        );
    }

    @GetMapping("/timeline")
    @Operation(summary = "Get event timeline")
    public ResponseEntity<List<Map<String, String>>> timeline(@PathVariable String eventId) {
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/media/{mediaId}")
    @Operation(summary = "Get event media")
    public ResponseEntity<Map<String, String>> get(@PathVariable String eventId, @PathVariable String mediaId) {
        return ResponseEntity.ok(Map.of("mediaId", mediaId));
    }

    @PatchMapping("/media/{mediaId}/approve")
    @Operation(summary = "Approve event media")
    public ResponseEntity<Map<String, String>> approve(@PathVariable String eventId, @PathVariable String mediaId) {
        return ResponseEntity.ok(Map.of("status", moderationService.approveMedia(mediaId)));
    }

    @PatchMapping("/media/{mediaId}/reject")
    @Operation(summary = "Reject event media")
    public ResponseEntity<Map<String, String>> reject(@PathVariable String eventId, @PathVariable String mediaId) {
        return ResponseEntity.ok(Map.of("status", moderationService.rejectMedia(mediaId)));
    }

    @DeleteMapping("/media/{mediaId}")
    @Operation(summary = "Delete event media")
    public ResponseEntity<Void> delete(@PathVariable String eventId, @PathVariable String mediaId) {
        return ResponseEntity.noContent().build();
    }
}
