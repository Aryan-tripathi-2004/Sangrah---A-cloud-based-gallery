package com.example.Gallery.api.controller;

import com.example.Gallery.api.annotation.CurrentUserId;
import com.example.Gallery.api.dto.response.MediaItemResponse;
import com.example.Gallery.api.dto.response.StorageUsageResponse;
import com.example.Gallery.application.service.interfaces.IGalleryMediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Max file size: 500MB (configurable)
        long maxFileSize = 500 * 1024 * 1024;  // 500MB
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File too large. Max: 500MB");
        }

        MediaItemResponse response = mediaService.uploadMedia(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/media")
    @Operation(summary = "List user's media", description = "Get all non-deleted media files (paginated, newest first)")
    public ResponseEntity<List<MediaItemResponse>> listMedia(@CurrentUserId String userId) {
        log.info("📋 List media request from user: {}", userId);
        
        List<MediaItemResponse> media = mediaService.listUserMedia(userId);
        return ResponseEntity.ok(media);
    }

    @GetMapping("/media/{mediaId}")
    @Operation(summary = "Get media details", description = "Get detailed information for a specific media file")
    public ResponseEntity<MediaItemResponse> getMedia(
            @PathVariable String mediaId,
            @CurrentUserId String userId) {
        
        log.info("🔍 Get media {} for user: {}", mediaId, userId);
        
        MediaItemResponse media = mediaService.getMedia(userId, mediaId);
        return ResponseEntity.ok(media);
    }

    @GetMapping("/media/{mediaId}/file")
    @Operation(summary = "Get media file", description = "Download or stream the actual media file. Token can be passed as query parameter for <img> and <video> tags.")
    public ResponseEntity<Resource> getMediaFile(
            @PathVariable String mediaId,
            @CurrentUserId String userId) {
        
        log.info("📥 Get file request for media {} from user: {}", mediaId, userId);

        Resource resource = mediaService.getMediaFile(userId, mediaId);
        if (resource == null || !resource.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String contentType = "application/octet-stream";
        String filename = resource.getFilename();
        if (filename != null) {
            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (filename.endsWith(".png")) {
                contentType = "image/png";
            } else if (filename.endsWith(".gif")) {
                contentType = "image/gif";
            } else if (filename.endsWith(".mp4")) {
                contentType = "video/mp4";
            } else if (filename.endsWith(".webm")) {
                contentType = "video/webm";
            }
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @DeleteMapping("/media/{mediaId}")
    @Operation(summary = "Delete media", description = "Soft delete - marks file as deleted but keeps for 30 days for restore capability")
    public ResponseEntity<Void> deleteMedia(
            @PathVariable String mediaId,
            @CurrentUserId String userId) {
        
        log.info("🗑️ Delete media {} for user: {}", mediaId, userId);
        mediaService.deleteMedia(userId, mediaId);
        return ResponseEntity.noContent().build();
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
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("UP");
    }
}
