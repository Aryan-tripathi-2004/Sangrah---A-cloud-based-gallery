package com.example.Gallery.api.controller;

import com.example.Gallery.api.annotation.CurrentUserId;
import com.example.Gallery.api.dto.response.GalleryMediaListResponse;
import com.example.Gallery.api.dto.response.HealthResponse;
import com.example.Gallery.api.dto.response.MediaDeleteResponse;
import com.example.Gallery.api.dto.response.MediaItemResponse;
import com.example.Gallery.api.dto.response.StorageUsageResponse;
import com.example.Gallery.application.service.interfaces.IGalleryMediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/v1/gallery")
@RequiredArgsConstructor
@Tag(name = "Gallery Management", description = "APIs for media upload, management, and storage tracking")
public class GalleryMediaController {

    private final IGalleryMediaService mediaService;

    @PostMapping("/media")
    @Operation(summary = "Upload media file(s)", description = "Upload one or more images/videos. No quota limits in pay-as-you-use model.")
    public ResponseEntity<MediaItemResponse> upload(
            @RequestParam("file") MultipartFile file,
            @CurrentUserId String userId) throws IOException {
        
        log.info("📤 Upload request from user: {}, file: {}", userId, file.getOriginalFilename());

        MediaItemResponse response = mediaService.uploadMedia(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/media")
    @Operation(summary = "List user's media", description = "Get all non-deleted media files (paginated, newest first)")
    public ResponseEntity<GalleryMediaListResponse> listMedia(@CurrentUserId String userId) {
        log.info("📋 List media request from user: {}", userId);
        return ResponseEntity.ok(mediaService.listUserMedia(userId));
    }

    @GetMapping(value = "/media/{mediaId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE, "image/*", "video/*"})
    @Operation(summary = "Get media details or file stream", description = "Get detailed information or download/stream the file.")
    public ResponseEntity<?> getMedia(
            @PathVariable String mediaId,
            @CurrentUserId String userId,
            @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE) String acceptHeader) {
        
        log.info("🔍 Get media {} for user: {} (Accept: {})", mediaId, userId, acceptHeader);
        
        if (acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE)) {
            MediaItemResponse media = mediaService.getMedia(userId, mediaId);
            return ResponseEntity.ok(media);
        }

        Resource resource = mediaService.getMediaFile(userId, mediaId);
        MediaItemResponse details = mediaService.getMedia(userId, mediaId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + details.originalFileName() + "\"")
                .contentType(MediaType.parseMediaType(details.mimeType()))
                .body(resource);
    }

    @DeleteMapping("/media/{mediaId}")
    @Operation(summary = "Delete media", description = "Soft delete - marks file as deleted but keeps for 30 days for restore capability")
    public ResponseEntity<MediaDeleteResponse> deleteMedia(
            @PathVariable String mediaId,
            @CurrentUserId String userId) {
        
        log.info("🗑️ Delete media {} for user: {}", mediaId, userId);
        return ResponseEntity.ok(mediaService.deleteMedia(userId, mediaId));
    }

    @GetMapping("/usage")
    @Operation(summary = "Get storage usage stats", description = "Get current storage usage information (informational, no quota limits)")
    public ResponseEntity<StorageUsageResponse> getStorageUsage(@CurrentUserId String userId) {
        log.info("📊 Storage usage request from user: {}", userId);
        StorageUsageResponse usage = mediaService.getStorageUsage(userId);
        return ResponseEntity.ok(usage);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verify Gallery service is running")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("UP"));
    }
}
