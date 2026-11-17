package com.example.Event.api.controller;

import com.example.Event.api.annotation.CurrentUserId;
import com.example.Event.api.dto.response.EventParticipantsResponse;
import com.example.Event.application.service.interfaces.IEventParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events/{eventId}/participants")
@RequiredArgsConstructor
public class EventParticipantController {

    private final IEventParticipantService participantService;

    @GetMapping
    @Operation(summary = "List participants")
    public ResponseEntity<EventParticipantsResponse> list(
            @PathVariable String eventId,
            @CurrentUserId String userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null || size != null) {
            int p = page != null ? Math.max(0, page) : 0;
            int s = size != null ? Math.min(100, Math.max(1, size)) : 50;
            return ResponseEntity.ok(participantService.listParticipants(eventId, userId, PageRequest.of(p, s)));
        }
        return ResponseEntity.ok(participantService.listParticipants(eventId, userId));
    }

    @DeleteMapping("/{participantUserId}")
    @Operation(summary = "Remove participant")
    public ResponseEntity<Void> remove(
            @PathVariable String eventId,
            @PathVariable String participantUserId,
            @CurrentUserId String requesterUserId) {
        participantService.removeParticipant(eventId, participantUserId, requesterUserId);
        return ResponseEntity.noContent().build();
    }
}
