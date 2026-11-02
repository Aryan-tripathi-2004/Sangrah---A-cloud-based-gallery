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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/gallery")
@RequiredArgsConstructor
@Tag(name = "Gallery Media", description = "Gallery media management endpoints")
public class GalleryMediaController {

    private final IGalleryMediaService mediaService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload media file", description = "Upload image or video to personal gallery")
    public ResponseEntity<MediaItemResponse> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @CurrentUserId String userId) {
        
        log.info("📤 Upload request for user: {}, filename: {}", userId, file.getOriginalFilename());
        MediaItemResponse response = mediaService.uploadMedia(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/media")
    @Operation(summary = "List media", description = "Get list of all active (non-deleted) media items for user")
    public ResponseEntity<GalleryMediaListResponse> listMedia(@CurrentUserId String userId) {
        log.info("📋 List media request for user: {}", userId);
        return ResponseEntity.ok(mediaService.listUserMedia(userId));
    }

    @GetMapping(value = "/media/{mediaId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE, "image/*", "video/*"})
    @Operation(summary = "Get media details or file stream", description = "Get detailed information or download/stream the file.")
    public ResponseEntity<?> getMedia(
            @PathVariable String mediaId,
            @CurrentUserId String userId,
            @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE) String acceptHeader) {
        
        log.info("🔍 Get media {} for user: {} (Accept: {})", mediaId, userId, acceptHeader);
        return mediaService.getMediaResponse(userId, mediaId, acceptHeader);
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
    @Operation(summary = "Get storage usage", description = "Calculate total storage used by user and quota percentage")
    public ResponseEntity<StorageUsageResponse> getUsage(@CurrentUserId String userId) {
        log.info("📊 Get storage usage for user: {}", userId);
        return ResponseEntity.ok(mediaService.getStorageUsage(userId));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("UP"));
    }
}
