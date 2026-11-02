package com.example.Event.api.controller;

import com.example.Event.api.annotation.CurrentUserId;
import com.example.Event.api.dto.request.AccessMessageRequest;
import com.example.Event.api.dto.request.AccessStatusUpdateRequest;
import com.example.Event.api.dto.response.AccessRequestMutationResponse;
import com.example.Event.api.dto.response.AccessRequestsResponse;
import com.example.Event.api.dto.response.AccessStatusResponse;
import com.example.Event.api.dto.response.AccessStatusUpdateResponse;
import com.example.Event.application.service.interfaces.IEventAccessService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/events/{eventId}/access-requests")
@RequiredArgsConstructor
public class EventAccessRequestController {
    private final IEventAccessService accessService;

    @PostMapping
    @Operation(summary = "Request access to protected event")
    public ResponseEntity<AccessRequestMutationResponse> request(
            @PathVariable String eventId,
            @Valid @RequestBody(required = false) AccessMessageRequest request,
            @CurrentUserId String userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accessService.requestAccess(eventId, request, userId));
    }

    @GetMapping
    @Operation(summary = "List event access requests (owner only)")
    public ResponseEntity<AccessRequestsResponse> list(
            @PathVariable String eventId,
            @CurrentUserId String userId) {
        return ResponseEntity.ok(accessService.listAccessRequests(eventId, userId));
    }

    @PatchMapping("/{requestId}/status")
    @Operation(summary = "Update access request status (approve / reject / revoke)")
    public ResponseEntity<AccessStatusUpdateResponse> updateStatus(
            @PathVariable String eventId,
            @PathVariable String requestId,
            @Valid @RequestBody AccessStatusUpdateRequest request,
            @CurrentUserId String userId) {
        return ResponseEntity.ok(accessService.updateAccessStatus(eventId, requestId, request, userId));
    }

    @PatchMapping("/re-request")
    @Operation(summary = "User re-requests access after expiration or revocation")
    public ResponseEntity<AccessRequestMutationResponse> reRequest(
            @PathVariable String eventId,
            @Valid @RequestBody(required = false) AccessMessageRequest request,
            @CurrentUserId String userId) {
        return ResponseEntity.ok(accessService.reRequestAccess(eventId, request, userId));
    }

    @GetMapping("/access-status")
    @Operation(summary = "Get current user's access status for event")
    public ResponseEntity<AccessStatusResponse> getAccessStatus(
            @PathVariable String eventId,
            @CurrentUserId String userId) {
        return ResponseEntity.ok(accessService.getAccessStatus(eventId, userId));
    }
}
