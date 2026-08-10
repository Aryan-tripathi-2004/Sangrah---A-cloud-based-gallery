package com.example.Event.api.controller;

import com.example.Event.api.annotation.CurrentUserId;
import com.example.Event.api.dto.request.AccessApprovalRequest;
import com.example.Event.api.dto.request.AccessMessageRequest;
import com.example.Event.api.dto.request.AccessRejectionRequest;
import com.example.Event.api.dto.response.AccessApprovalResponse;
import com.example.Event.api.dto.response.AccessRejectionResponse;
import com.example.Event.api.dto.response.AccessRequestMutationResponse;
import com.example.Event.api.dto.response.AccessRequestsResponse;
import com.example.Event.api.dto.response.AccessRevocationResponse;
import com.example.Event.api.dto.response.AccessStatusResponse;
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

    @PatchMapping("/{requestId}/approve")
    @Operation(summary = "Approve event access request (owner only)")
    public ResponseEntity<AccessApprovalResponse> approve(
            @PathVariable String eventId,
            @PathVariable String requestId,
            @Valid @RequestBody(required = false) AccessApprovalRequest request,
            @CurrentUserId String userId) {
        return ResponseEntity.ok(accessService.approveRequest(eventId, requestId, request, userId));
    }

    @PatchMapping("/{requestId}/reject")
    @Operation(summary = "Reject event access request (owner only)")
    public ResponseEntity<AccessRejectionResponse> reject(
            @PathVariable String eventId,
            @PathVariable String requestId,
            @Valid @RequestBody(required = false) AccessRejectionRequest request,
            @CurrentUserId String userId) {
        return ResponseEntity.ok(accessService.rejectRequest(eventId, requestId, request, userId));
    }

    @PatchMapping("/{requestId}/revoke")
    @Operation(summary = "Revoke approved event access (owner only)")
    public ResponseEntity<AccessRevocationResponse> revoke(
            @PathVariable String eventId,
            @PathVariable String requestId,
            @CurrentUserId String userId) {
        return ResponseEntity.ok(accessService.revokeRequest(eventId, requestId, userId));
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
