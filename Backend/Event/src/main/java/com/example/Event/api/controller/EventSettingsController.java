package com.example.Event.api.controller;

import com.example.Event.application.service.EventModerationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// TODO: Implement event settings feature in Phase 2
// @RestController
// @RequestMapping("/api/v1/events/{eventId}/settings")
@RequiredArgsConstructor
public class EventSettingsController {
    private final EventModerationService moderationService;

    // TODO: Implement in Phase 2
    /*
    @GetMapping
    @Operation(summary = "Get event settings")
    public ResponseEntity<Map<String, Boolean>> get(@PathVariable String eventId) {
        return ResponseEntity.ok(Map.of("moderationEnabled", moderationService.getModeration(eventId)));
    }

    @PatchMapping
    @Operation(summary = "Update event settings")
    public ResponseEntity<Map<String, Boolean>> update(@PathVariable String eventId, @RequestBody Map<String, Boolean> payload) {
        boolean updated = moderationService.setModeration(eventId, payload.getOrDefault("moderationEnabled", true));
        return ResponseEntity.ok(Map.of("moderationEnabled", updated));
    }
    */
}
