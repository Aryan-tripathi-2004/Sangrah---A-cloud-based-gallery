package com.example.Event.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {
    @PostMapping
    @Operation(summary = "Create event")
    public ResponseEntity<Map<String, String>> create(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("eventId", "generated-id"));
    }

    @GetMapping("/global")
    @Operation(summary = "Get global events")
    public ResponseEntity<List<Map<String, String>>> global() {
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Get event")
    public ResponseEntity<Map<String, String>> byId(@PathVariable String eventId) {
        return ResponseEntity.ok(Map.of("eventId", eventId));
    }

    @PatchMapping("/{eventId}")
    @Operation(summary = "Update event")
    public ResponseEntity<Map<String, String>> update(@PathVariable String eventId, @RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(Map.of("eventId", eventId));
    }

    @DeleteMapping("/{eventId}")
    @Operation(summary = "Delete event")
    public ResponseEntity<Void> delete(@PathVariable String eventId) {
        return ResponseEntity.noContent().build();
    }
}
