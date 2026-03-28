package com.example.Event.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/events/{eventId}/participants")
public class EventParticipantController {
    @GetMapping
    @Operation(summary = "List participants")
    public ResponseEntity<List<Map<String, String>>> list(@PathVariable String eventId) {
        return ResponseEntity.ok(List.of());
    }

    @DeleteMapping("/{participantUserId}")
    @Operation(summary = "Remove participant")
    public ResponseEntity<Void> remove(@PathVariable String eventId, @PathVariable String participantUserId) {
        return ResponseEntity.noContent().build();
    }
}
