package com.example.Event.api.controller;

import com.example.Event.api.annotation.CurrentUserId;
import com.example.Event.api.dto.request.EventSettingsRequest;
import com.example.Event.api.dto.response.EventSettingsResponse;
import com.example.Event.application.service.interfaces.IEventService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/events/{eventId}/settings")
@RequiredArgsConstructor
public class EventSettingsController {
    private final IEventService eventService;

    @GetMapping
    @Operation(summary = "Get event settings")
    public ResponseEntity<EventSettingsResponse> get(@PathVariable String eventId) {
        return ResponseEntity.ok(eventService.getSettings(eventId));
    }

    @PatchMapping
    @Operation(summary = "Update event settings")
    public ResponseEntity<EventSettingsResponse> update(
            @PathVariable String eventId,
            @Valid @RequestBody EventSettingsRequest request,
            @CurrentUserId String userId) {
        return ResponseEntity.ok(eventService.updateSettings(eventId, request, userId));
    }
}
