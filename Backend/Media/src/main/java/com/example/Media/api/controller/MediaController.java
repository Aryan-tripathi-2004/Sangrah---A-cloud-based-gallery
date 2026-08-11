package com.example.Media.api.controller;

import com.example.Media.api.annotation.CurrentUserId;
import com.example.Media.api.dto.response.MediaResponse;
import com.example.Media.application.service.interfaces.IMediaService;
import com.example.Media.infrastructure.mapper.MediaMapper;
import com.example.Media.infrastructure.persistence.document.MediaDocument;
import com.example.Media.infrastructure.persistence.document.StorageUsageLedgerDocument;
import com.example.Media.shared.enums.MediaDomain;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
    private final MediaMapper mediaMapper;

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

        MediaDocument mediaDoc = mediaService.uploadMedia(userId, domain, entityRefId, file);
        MediaResponse response = mediaMapper.toResponse(mediaDoc);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get media file (download/stream)
     */
    @GetMapping("/{mediaId}/file")
    @Operation(summary = "Download media file")
    public ResponseEntity<Resource> getMediaFile(
            @PathVariable String mediaId) {

        log.info("📥 [GET /{}/file] Fetching file", mediaId);

        MediaDocument mediaDoc = mediaService.getMediaDetails(mediaId);
        Resource resource = mediaService.getMediaFile(mediaId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + mediaDoc.getOriginalFileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, mediaDoc.getMimeType())
                .body(resource);
    }

    /**
     * Get media details by ID
     */
    @GetMapping("/{mediaId}")
    @Operation(summary = "Get media details")
    public ResponseEntity<MediaResponse> getMediaDetails(
            @PathVariable String mediaId) {

        log.info("📋 [GET /{}] Fetching media details", mediaId);

        MediaDocument mediaDoc = mediaService.getMediaDetails(mediaId);
        MediaResponse response = mediaMapper.toResponse(mediaDoc);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a media file (soft delete)
     */
    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Delete media file (soft delete)")
    public ResponseEntity<Map<String, String>> deleteMedia(
            @CurrentUserId String userId,
            @PathVariable String mediaId) {

        log.info("🗑️  [DELETE /{}] Deleting media | user: {}", mediaId, userId);

        mediaService.deleteMedia(mediaId);

        return ResponseEntity.ok(Map.of("status", "success", "message", "Media deleted"));
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

        List<MediaDocument> mediaList;
        if (domain != null) {
            mediaList = mediaService.listMediaByUserAndDomain(userId, domain);
        } else {
            mediaList = mediaService.listMediaByUser(userId);
        }

        List<MediaResponse> responses = mediaList.stream()
                .map(mediaMapper::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Get storage usage stats
     */
    @GetMapping("/usage/{userId}")
    @Operation(summary = "Get storage usage for user")
    public ResponseEntity<Map<String, Object>> getStorageUsage(@PathVariable String userId) {

        log.info("📊 [GET /usage/{}] Fetching storage usage", userId);

        long usedBytes = mediaService.getStorageUsageBytes(userId);
        long limitBytes = 5_368_709_120L;  // 5GB

        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "usedBytes", usedBytes,
                "limitBytes", limitBytes,
                "percentageUsed", (usedBytes * 100.0) / limitBytes
        ));
    }

    /**
     * Get storage ledger for billing (used by Billing Service)
     */
    @GetMapping("/ledger")
    @Operation(summary = "Get storage ledger for billing")
    public ResponseEntity<List<StorageUsageLedgerDocument>> getStorageLedger(
            @RequestParam String userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        log.info("📊 [GET /ledger] Billing query | user: {} | period: {} to {}", userId, startDate, endDate);

        Instant start = Instant.parse(startDate);
        Instant end = Instant.parse(endDate);

        List<StorageUsageLedgerDocument> ledgerEntries = mediaService.getStorageLedger(userId, start, end);

        return ResponseEntity.ok(ledgerEntries);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
