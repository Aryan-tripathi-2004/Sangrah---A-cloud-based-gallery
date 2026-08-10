package com.example.Event.api.controller;

import com.example.Event.api.annotation.CurrentUserId;
import com.example.Event.api.dto.request.AddCollaboratorRequest;
import com.example.Event.api.dto.request.EventRequest;
import com.example.Event.api.dto.request.EventUpdateRequest;
import com.example.Event.api.dto.request.UpdateCollaboratorPermissionsRequest;
import com.example.Event.api.dto.response.CollaboratorMutationResponse;
import com.example.Event.api.dto.response.CollaboratorsResponse;
import com.example.Event.api.dto.response.EventCreateResponse;
import com.example.Event.api.dto.response.EventDeleteResponse;
import com.example.Event.api.dto.response.EventResponse;
import com.example.Event.api.dto.response.EventSummaryResponse;
import com.example.Event.api.dto.response.EventUpdateResponse;
import com.example.Event.api.dto.response.MessageResponse;
import com.example.Event.application.service.interfaces.IEventCollaboratorService;
import com.example.Event.application.service.interfaces.IEventService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {
    private final IEventService eventService;
    private final IEventCollaboratorService collaboratorService;

    @PostMapping
    @Operation(summary = "Create event")
    public ResponseEntity<EventCreateResponse> create(
            @Valid @RequestBody EventRequest request,
            @CurrentUserId String userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request, userId));
    }

    @GetMapping("/global")
    @Operation(summary = "Get global events")
    public ResponseEntity<List<EventSummaryResponse>> global(
            @CurrentUserId(required = false) String userId) {
        return ResponseEntity.ok(eventService.getGlobalEvents(userId));
    }

    @GetMapping("/my-events")
    @Operation(summary = "Get user's events")
    public ResponseEntity<List<EventSummaryResponse>> myEvents(
            @CurrentUserId String userId) {
        return ResponseEntity.ok(eventService.getMyEvents(userId));
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Get event")
    public ResponseEntity<EventResponse> byId(
            @PathVariable String eventId,
            @CurrentUserId(required = false) String userId) {
        return ResponseEntity.ok(eventService.getEvent(eventId, userId));
    }

    @PutMapping(value = "/{eventId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update event")
    public ResponseEntity<EventUpdateResponse> update(
            @PathVariable String eventId,
            @Valid @RequestPart("eventDetails") EventUpdateRequest eventDetails,
            @RequestPart(value = "coverMedia", required = false) MultipartFile coverMedia,
            @CurrentUserId String userId) {
        return ResponseEntity.ok(eventService.updateEvent(eventId, eventDetails, coverMedia, userId));
    }

    @DeleteMapping("/{eventId}")
    @Operation(summary = "Delete event")
    public ResponseEntity<EventDeleteResponse> deleteEvent(
            @PathVariable String eventId,
            @CurrentUserId String userId) {
        return ResponseEntity.ok(eventService.deleteEvent(eventId, userId));
    }

    @PostMapping("/{eventId}/collaborators")
    @Operation(summary = "Add collaborator to event")
    public ResponseEntity<CollaboratorMutationResponse> addCollaborator(
            @PathVariable String eventId,
            @Valid @RequestBody AddCollaboratorRequest request,
            @CurrentUserId String userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(collaboratorService.addCollaborator(eventId, request, userId));
    }

    @GetMapping("/{eventId}/collaborators")
    @Operation(summary = "Get event collaborators")
    public ResponseEntity<CollaboratorsResponse> getCollaborators(@PathVariable String eventId) {
        return ResponseEntity.ok(collaboratorService.getCollaborators(eventId));
    }

    @DeleteMapping("/{eventId}/collaborators/{userId}")
    @Operation(summary = "Remove collaborator from event")
    public ResponseEntity<MessageResponse> removeCollaborator(
            @PathVariable String eventId,
            @PathVariable String userId,
            @CurrentUserId String requesterUserId) {
        return ResponseEntity.ok(collaboratorService.removeCollaborator(eventId, userId, requesterUserId));
    }

    @PatchMapping("/{eventId}/collaborators/{userId}")
    @Operation(summary = "Update collaborator permissions")
    public ResponseEntity<CollaboratorMutationResponse> updateCollaboratorPermissions(
            @PathVariable String eventId,
            @PathVariable String userId,
            @Valid @RequestBody UpdateCollaboratorPermissionsRequest request,
            @CurrentUserId String requesterUserId) {
        return ResponseEntity.ok(collaboratorService.updateCollaboratorPermissions(eventId, userId, request, requesterUserId));
    }
}
