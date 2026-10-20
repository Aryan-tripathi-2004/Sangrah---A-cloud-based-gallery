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
import com.example.Event.shared.enums.EventScope;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create event")
    public ResponseEntity<EventCreateResponse> create(
            @Valid @RequestPart("eventDetails") EventRequest request,
            @RequestPart(value = "coverMedia", required = false) MultipartFile coverMedia,
            @CurrentUserId String userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request, coverMedia, userId));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create event (JSON)")
    public ResponseEntity<EventCreateResponse> createJson(
            @Valid @RequestBody EventRequest request,
            @CurrentUserId String userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request, null, userId));
    }

    @GetMapping
    @Operation(summary = "Get filtered events collection")
    public ResponseEntity<List<EventSummaryResponse>> getEvents(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(defaultValue = "GLOBAL") EventScope scope) {

        return switch (scope) {
            case MY_EVENTS -> ResponseEntity.ok(eventService.getMyEvents(userId));
            case PENDING_REQUESTS -> ResponseEntity.ok(eventService.getEventsWithPendingRequests(userId));
            default -> ResponseEntity.ok(eventService.getGlobalEvents(userId));
        };
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Get event")
    public ResponseEntity<EventResponse> byId(
            @PathVariable String eventId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(eventService.getEvent(eventId, userId));
    }

    @PutMapping(value = "/{eventId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update event")
    public ResponseEntity<EventResponse> update(
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
