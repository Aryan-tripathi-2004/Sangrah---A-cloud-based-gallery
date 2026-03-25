package com.example.Event.api.controller;

import com.example.Event.application.service.EventModerationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events/{eventId}/access-requests")
@RequiredArgsConstructor
public class EventAccessRequestController {
    private final EventModerationService moderationService;

    @PostMapping
    @Operation(summary = "Request access to protected event")
    public ResponseEntity<Map<String, String>> request(@PathVariable String eventId) {
        String requestId = UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "requestId", requestId,
                "status", moderationService.createAccessRequest(requestId))
        );
    }

    @GetMapping
    @Operation(summary = "List event access requests")
    public ResponseEntity<List<Map<String, String>>> list(@PathVariable String eventId) {
        return ResponseEntity.ok(List.of());
    }

    @PatchMapping("/{requestId}/approve")
    @Operation(summary = "Approve event access request")
    public ResponseEntity<Map<String, String>> approve(@PathVariable String eventId, @PathVariable String requestId) {
        return ResponseEntity.ok(Map.of("status", moderationService.approveAccess(requestId)));
    }

    @PatchMapping("/{requestId}/reject")
    @Operation(summary = "Reject event access request")
    public ResponseEntity<Map<String, String>> reject(@PathVariable String eventId, @PathVariable String requestId) {
        return ResponseEntity.ok(Map.of("status", moderationService.rejectAccess(requestId)));
    }
}
