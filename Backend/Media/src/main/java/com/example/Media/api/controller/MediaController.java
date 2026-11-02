package com.example.Media.api.controller;

import com.example.Media.api.annotation.CurrentUserId;
import com.example.Media.api.dto.response.HealthResponse;
import com.example.Media.api.dto.response.MediaDeleteResponse;
import com.example.Media.api.dto.response.MediaResponse;
import com.example.Media.api.dto.response.StorageUsageLedgerResponse;
import com.example.Media.api.dto.response.StorageUsageResponse;
import com.example.Media.application.service.interfaces.IMediaService;
import com.example.Media.shared.enums.MediaDomain;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final IMediaService mediaService;

    @PostMapping("/upload")
    @Operation(summary = "Upload media file")
    public ResponseEntity<MediaResponse> uploadMedia(
            @CurrentUserId String userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("domain") MediaDomain domain,
            @RequestParam(value = "entityRefId", required = false) String entityRefId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mediaService.uploadMedia(userId, domain, entityRefId, file));
    }

    @GetMapping(value = "/{mediaId}", produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_OCTET_STREAM_VALUE,
            "image/*",
            "video/*"
    })
    @Operation(summary = "Get media details or download file stream")
    public ResponseEntity<?> getMedia(
            @PathVariable String mediaId,
            @CurrentUserId(required = false) String callerUserId,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role,
            @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE) String acceptHeader) {
        return mediaService.getMediaResponse(mediaId, callerUserId, role, acceptHeader);
    }

    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Delete media file (soft delete)")
    public ResponseEntity<MediaDeleteResponse> deleteMedia(
            @CurrentUserId String userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role,
            @PathVariable String mediaId) {
        return ResponseEntity.ok(mediaService.deleteMedia(mediaId, userId, role));
    }

    @DeleteMapping("/bulk")
    @Operation(summary = "Bulk delete media files (soft delete)")
    public ResponseEntity<MediaDeleteResponse> deleteMediaBulk(
            @CurrentUserId String userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role,
            @RequestBody List<String> mediaIds) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(mediaService.deleteMediaBulk(mediaIds, userId, role));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List media by user and domain")
    public ResponseEntity<List<MediaResponse>> listMediaByUserAndDomain(
            @PathVariable String userId,
            @CurrentUserId String callerUserId,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role,
            @RequestParam(value = "domain", required = false) MediaDomain domain) {
        return ResponseEntity.ok(mediaService.listAuthorizedMedia(userId, callerUserId, role, domain));
    }

    @GetMapping("/usage")
    @Operation(summary = "Get storage usage for a specific user")
    public ResponseEntity<StorageUsageResponse> getStorageUsage(
            @RequestParam(required = false) String userId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        return ResponseEntity.ok(mediaService.getAuthorizedStorageUsage(userId, headerUserId, role));
    }

    @GetMapping("/ledger")
    @Operation(summary = "Get storage ledger for billing")
    public ResponseEntity<List<StorageUsageLedgerResponse>> getStorageLedger(
            @RequestParam String userId,
            @RequestParam Instant startDate,
            @RequestParam Instant endDate,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        return ResponseEntity.ok(mediaService.getAuthorizedStorageLedger(userId, startDate, endDate, headerUserId, role));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(mediaService.getHealth());
    }
}
