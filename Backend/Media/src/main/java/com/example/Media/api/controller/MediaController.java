package com.example.Media.api.controller;

import com.example.Media.api.dto.response.MediaResponse;
import com.example.Media.infrastructure.mapper.MediaMapper;
import com.example.Media.infrastructure.persistence.document.MediaDocument;
import com.example.Media.infrastructure.persistence.document.StorageUsageLedgerDocument;
import com.example.Media.infrastructure.persistence.repository.StorageUsageLedgerRepository;
import com.example.Media.application.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;
    private final MediaMapper mediaMapper;
    private final StorageUsageLedgerRepository ledgerRepository;

    /**
     * Upload a new media file
     */
    @PostMapping("/upload")
    @Operation(summary = "Upload media file")
    public ResponseEntity<MediaResponse> uploadMedia(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file,
            @RequestParam("domain") String domain,
            @RequestParam(value = "entityRefId", required = false) String entityRefId) {

        try {
            log.info("📤 [POST /upload] Received file: {} | domain: {}", file.getOriginalFilename(), domain);

            String userId = request.getHeader("X-User-Id");
            if (userId == null || userId.isBlank()) {
                log.warn("❌ [POST /upload] No X-User-Id header found");
                return ResponseEntity.status(401).build();
            }

            MediaDocument mediaDoc = mediaService.uploadMedia(userId, domain, entityRefId, file);
            MediaResponse response = mediaMapper.toResponse(mediaDoc);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ [POST /upload] Error uploading file: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Get media file (download/stream)
     */
    @GetMapping("/{mediaId}/file")
    @Operation(summary = "Download media file")
    public ResponseEntity<Resource> getMediaFile(
            @PathVariable String mediaId) {

        try {
            log.info("📥 [GET /{}/file] Fetching file", mediaId);

            MediaDocument mediaDoc = mediaService.getMediaDetails(mediaId);
            Resource resource = mediaService.getMediaFile(mediaId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + mediaDoc.getOriginalFileName() + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, mediaDoc.getMimeType())
                    .body(resource);

        } catch (Exception e) {
            log.error("❌ [GET /{}/file] Error: {}", mediaId, e.getMessage());
            return ResponseEntity.status(404).build();
        }
    }

    /**
     * Get media details by ID
     */
    @GetMapping("/{mediaId}")
    @Operation(summary = "Get media details")
    public ResponseEntity<MediaResponse> getMediaDetails(
            @PathVariable String mediaId) {

        try {
            log.info("📋 [GET /{}] Fetching media details", mediaId);

            MediaDocument mediaDoc = mediaService.getMediaDetails(mediaId);
            MediaResponse response = mediaMapper.toResponse(mediaDoc);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ [GET /{}] Error: {}", mediaId, e.getMessage());
            return ResponseEntity.status(404).build();
        }
    }

    /**
     * Delete a media file (soft delete)
     */
    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Delete media file (soft delete)")
    public ResponseEntity<Map<String, String>> deleteMedia(
            HttpServletRequest request,
            @PathVariable String mediaId) {

        try {
            log.info("🗑️  [DELETE /{}] Deleting media", mediaId);

            String userId = request.getHeader("X-User-Id");
            if (userId == null || userId.isBlank()) {
                log.warn("❌ [DELETE /{}] No X-User-Id header found", mediaId);
                return ResponseEntity.status(401).build();
            }

            mediaService.deleteMedia(mediaId);

            return ResponseEntity.ok(Map.of("status", "success", "message", "Media deleted"));

        } catch (Exception e) {
            log.error("❌ [DELETE /{}] Error: {}", mediaId, e.getMessage());
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * List media by user and domain
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "List media by user and domain")
    public ResponseEntity<List<MediaResponse>> listMediaByUserAndDomain(
            @PathVariable String userId,
            @RequestParam(value = "domain", required = false) String domain) {

        try {
            log.info("📋 [GET /user/{}] Listing media | domain: {}", userId, domain);

            List<MediaDocument> mediaList;
            if (domain != null && !domain.isBlank()) {
                mediaList = mediaService.listMediaByUserAndDomain(userId, domain);
            } else {
                mediaList = mediaService.listMediaByUser(userId);
            }

            List<MediaResponse> responses = mediaList.stream()
                    .map(mediaMapper::toResponse)
                    .toList();

            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            log.error("❌ [GET /user/{}] Error: {}", userId, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get storage usage stats
     */
    @GetMapping("/usage/{userId}")
    @Operation(summary = "Get storage usage for user")
    public ResponseEntity<Map<String, Object>> getStorageUsage(@PathVariable String userId) {

        try {
            log.info("📊 [GET /usage/{}] Fetching storage usage", userId);

            long usedBytes = mediaService.getStorageUsageBytes(userId);
            long limitBytes = 5_368_709_120L;  // 5GB

            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "usedBytes", usedBytes,
                    "limitBytes", limitBytes,
                    "percentageUsed", (usedBytes * 100.0) / limitBytes
            ));

        } catch (Exception e) {
            log.error("❌ [GET /usage/{}] Error: {}", userId, e.getMessage());
            return ResponseEntity.status(500).build();
        }
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

        try {
            log.info("📊 [GET /ledger] Billing query | user: {} | period: {} to {}", userId, startDate, endDate);

            Instant start = Instant.parse(startDate);
            Instant end = Instant.parse(endDate);

            List<StorageUsageLedgerDocument> ledgerEntries = ledgerRepository.findByUserIdAndStartAtBetween(userId, start, end);
            log.info("📋 [GET /ledger] Found {} ledger entries", ledgerEntries.size());

            return ResponseEntity.ok(ledgerEntries);

        } catch (Exception e) {
            log.error("❌ [GET /ledger] Error: {}", e.getMessage(), e);
            return ResponseEntity.status(400).build();
        }
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
