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
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST controller for media lifecycle operations.
 *
 * <p>Design notes:
 * <ul>
 *   <li>All {@code HttpServletRequest} parameters have been replaced by
 *       {@link CurrentUserId @CurrentUserId} — a custom argument resolver that
 *       extracts and validates the {@code X-User-Id} header before the method
 *       body executes. Missing headers throw a {@code MissingRequestHeaderException}
 *       caught centrally by {@code GlobalExceptionHandler}.</li>
 *   <li>All {@code try-catch} blocks have been deleted. The controller only returns
 *       {@code ResponseEntity.ok(...)} or {@code ResponseEntity.status(CREATED)}.
 *       Service-layer exceptions propagate unchecked to the
 *       {@code GlobalExceptionHandler}, which produces strict RFC-9457
 *       {@code ProblemDetail} responses.</li>
 *   <li>The direct {@code StorageUsageLedgerRepository} injection has been removed.
 *       Ledger queries are now delegated to {@code IMediaService#getStorageLedger},
 *       respecting the layered architecture boundary.</li>
 *   <li>The {@code domain} parameter on endpoints that accept it is now typed as
 *       {@link MediaDomain} enum. Spring's {@code ConversionService} automatically
 *       converts the incoming string to the enum constant.</li>
 * </ul>
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final IMediaService mediaService;

    /**
     * Upload a new media file
     */
    @PostMapping("/upload")
    @Operation(summary = "Upload media file")
    public ResponseEntity<MediaResponse> uploadMedia(
            @CurrentUserId String userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("domain") MediaDomain domain,
            @RequestParam(value = "entityRefId", required = false) String entityRefId) {

        log.info("📤 [POST /upload] Received file: {} | domain: {}", file.getOriginalFilename(), domain);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                mediaService.uploadMedia(userId, domain, entityRefId, file));
    }

    @GetMapping(value = "/{mediaId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE, "image/*", "video/*"})
    @Operation(summary = "Get media details or download file stream")
    public ResponseEntity<?> getMedia(
            @PathVariable String mediaId,
            @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE) String acceptHeader) {
            
        log.info("📋 [GET /{}] Fetching media (Accept: {})", mediaId, acceptHeader);
        
        boolean wantsBinary = acceptHeader != null && (
                acceptHeader.contains("image/")
                || acceptHeader.contains("video/")
                || acceptHeader.contains(MediaType.APPLICATION_OCTET_STREAM_VALUE)
        );

        if (!wantsBinary || (acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE))) {
            return ResponseEntity.ok(mediaService.getMediaDetails(mediaId));
        }

        Resource resource = mediaService.getMediaFile(mediaId);
        MediaResponse details = mediaService.getMediaDetails(mediaId);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + details.originalFileName() + "\"")
                .contentType(MediaType.parseMediaType(details.mimeType()))
                .body(resource);
    }

    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Delete media file (soft delete)")
    public ResponseEntity<MediaDeleteResponse> deleteMedia(
            @CurrentUserId String userId,
            @PathVariable String mediaId) {

        log.info("🗑️  [DELETE /{}] Deleting media | user: {}", mediaId, userId);
        return ResponseEntity.ok(mediaService.deleteMedia(mediaId));
    }

    @DeleteMapping("/bulk")
    @Operation(summary = "Bulk delete media files (soft delete)")
    public ResponseEntity<MediaDeleteResponse> deleteMediaBulk(
            @CurrentUserId String userId,
            @RequestBody List<String> mediaIds) {
        log.info("🗑️  [DELETE /bulk] Bulk deleting media files | count: {} | user: {}", mediaIds != null ? mediaIds.size() : 0, userId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mediaService.deleteMediaBulk(mediaIds));
    }

    /**
     * List media by user and domain
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "List media by user and domain")
    public ResponseEntity<List<MediaResponse>> listMediaByUserAndDomain(
            @PathVariable String userId,
            @RequestParam(value = "domain", required = false) MediaDomain domain) {

        log.info("📋 [GET /user/{}] Listing media | domain: {}", userId, domain);

        List<MediaResponse> responses;
        if (domain != null) {
            responses = mediaService.listMediaByUserAndDomain(userId, domain);
        } else {
            responses = mediaService.listMediaByUser(userId);
        }

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/usage")
    @Operation(summary = "Get storage usage for a specific user")
    public ResponseEntity<StorageUsageResponse> getStorageUsage(@RequestParam String userId) {
        log.info("  [GET /usage] Fetching storage usage for user: {}", userId);
        return ResponseEntity.ok(mediaService.getStorageUsage(userId));
    }

    /**
     * Get storage ledger for billing (used by Billing Service)
     */
    @GetMapping("/ledger")
    @Operation(summary = "Get storage ledger for billing")
    public ResponseEntity<List<StorageUsageLedgerResponse>> getStorageLedger(
            @RequestParam String userId,
            @RequestParam Instant startDate,
            @RequestParam Instant endDate) {

        log.info("📊 [GET /ledger] Billing query | user: {} | period: {} to {}", userId, startDate, endDate);
        return ResponseEntity.ok(mediaService.getStorageLedger(userId, startDate, endDate));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(mediaService.getHealth());
    }
}
