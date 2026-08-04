package com.example.Event.api.controller;

import com.example.Event.api.dto.response.EventParticipantsResponse;
import com.example.Event.application.service.interfaces.IEventParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events/{eventId}/participants")
@RequiredArgsConstructor
public class EventParticipantController {
    private final IEventParticipantService participantService;

    @GetMapping
    @Operation(summary = "List participants")
    public ResponseEntity<EventParticipantsResponse> list(@PathVariable String eventId) {
        return ResponseEntity.ok(participantService.listParticipants(eventId));
    }

    @DeleteMapping("/{participantUserId}")
    @Operation(summary = "Remove participant")
    public ResponseEntity<Void> remove(
            @PathVariable String eventId,
            @PathVariable String participantUserId) {
        participantService.removeParticipant(eventId, participantUserId);
        return ResponseEntity.noContent().build();
    }
}
