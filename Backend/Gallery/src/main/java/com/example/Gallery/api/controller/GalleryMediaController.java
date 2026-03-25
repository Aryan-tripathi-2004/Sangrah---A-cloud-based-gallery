package com.example.Gallery.api.controller;

import com.example.Gallery.api.dto.response.MediaItemResponse;
import com.example.Gallery.api.dto.response.StorageUsageResponse;
import com.example.Gallery.application.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;

@Slf4j
@RestController
@RequestMapping("/api/v1/gallery")
@RequiredArgsConstructor
@Tag(name = "Gallery Management", description = "APIs for media upload, management, and storage tracking")
public class GalleryMediaController {

    private final MediaService mediaService;

    @Value("${jwt.secret:sangrah_secret_key_for_jwt_token_validation_please_change_in_production}")
    private String jwtSecret;

    /**
     * Upload single or multiple media files
     * POST /api/v1/gallery/upload
     * No quota limits (pay-as-you-use model)
     */
    @PostMapping("/upload")
    @Operation(summary = "Upload media file(s)", description = "Upload one or more images/videos. No quota limits in pay-as-you-use model.")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            log.info("📤 Upload request from user: {}, file: {}", userId, file.getOriginalFilename());

            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User ID not found in context"));
            }

            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "File is empty"));
            }

            // Max file size: 500MB (configurable)
            long maxFileSize = 500 * 1024 * 1024;  // 500MB
            if (file.getSize() > maxFileSize) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body(Map.of("error", "File too large. Max: 500MB"));
            }

            MediaItemResponse response = mediaService.uploadMedia(userId, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IOException e) {
            log.error("❌ Upload error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Unexpected error during upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /**
     * List all non-deleted media for the user
     * GET /api/v1/gallery/media
     */
    @GetMapping("/media")
    @Operation(summary = "List user's media", description = "Get all non-deleted media files (paginated, newest first)")
    public ResponseEntity<?> listMedia(HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            log.info("📋 List media request from user: {}", userId);

            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User ID not found in context"));
            }

            List<MediaItemResponse> media = mediaService.listUserMedia(userId);
            return ResponseEntity.ok(Map.of(
                    "count", media.size(),
                    "items", media
            ));

        } catch (Exception e) {
            log.error("❌ List media error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to list media"));
        }
    }

    /**
     * Get single media details
     * GET /api/v1/gallery/media/:id
     */
    @GetMapping("/media/{mediaId}")
    @Operation(summary = "Get media details", description = "Get detailed information for a specific media file")
    public ResponseEntity<?> getMedia(
            @PathVariable String mediaId,
            HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            log.info("🔍 Get media {} for user: {}", mediaId, userId);

            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User ID not found in context"));
            }

            MediaItemResponse media = mediaService.getMedia(userId, mediaId);
            return ResponseEntity.ok(media);

        } catch (RuntimeException e) {
            log.warn("⚠️ Media not found: {}", mediaId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Get media error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get media"));
        }
    }

    /**
     * Get media file (download/stream)
     * GET /api/v1/gallery/media/:id/file?token=JWT_TOKEN
     * Token can be passed as query parameter for image/video tags
     */
    @GetMapping("/media/{mediaId}/file")
    @Operation(summary = "Get media file", description = "Download or stream the actual media file. Token can be passed as query parameter for <img> and <video> tags.")
    public ResponseEntity<?> getMediaFile(
            @PathVariable String mediaId,
            @RequestParam(value = "token", required = false) String tokenParam,
            HttpServletRequest request) {
        try {
            log.info("🔥🔥🔥 FILE ENDPOINT REACHED - mediaId: {}, hasToken: {}", mediaId, tokenParam != null && !tokenParam.isEmpty());
            log.debug("   All headers: {}", Collections.list(request.getHeaderNames()).stream().collect(Collectors.toMap(h -> h, request::getHeader)));

            // Try to get userId from X-User-Id header (from API Gateway)
            String userId = request.getHeader("X-User-Id");
            log.info("   X-User-Id header: {}", userId);

            // If not in header, extract from JWT token in query parameter
            if (userId == null || userId.isEmpty()) {
                if (tokenParam == null || tokenParam.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("error", "User ID not found and no token provided"));
                }

                // Validate JWT token and extract user ID
                try {
                    userId = validateTokenAndGetUserId(tokenParam);
                    log.info("✅ User ID extracted from token: {}", userId);
                } catch (Exception e) {
                    log.error("❌ Token validation failed: {}", e.getMessage());
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("error", "Invalid or expired token"));
                }
            }

            log.info("📥 Get file request for media {} from user: {}", mediaId, userId);

            Resource resource = mediaService.getMediaFile(userId, mediaId);
            if (resource == null || !resource.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Media file not found"));
            }

            // Determine content type from file
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

        } catch (RuntimeException e) {
            log.warn("⚠️ Get file failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Get media file error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get media file"));
        }
    }

    /**
     * Validate JWT token and extract user ID
     */
    private String validateTokenAndGetUserId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userId = claims.getSubject();
            if (userId == null || userId.isEmpty()) {
                throw new RuntimeException("User ID not found in token");
            }

            return userId;
        } catch (Exception e) {
            log.error("❌ JWT validation failed: {}", e.getMessage());
            throw new RuntimeException("Invalid token: " + e.getMessage());
        }
    }

    /**
     * Delete media (soft delete - marks deletedAt, doesn't remove file)
     * DELETE /api/v1/gallery/media/:id
     */
    @DeleteMapping("/media/{mediaId}")
    @Operation(summary = "Delete media", description = "Soft delete - marks file as deleted but keeps for 30 days for restore capability")
    public ResponseEntity<?> deleteMedia(
            @PathVariable String mediaId,
            HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            log.info("🗑️ Delete media {} for user: {}", mediaId, userId);

            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User ID not found in context"));
            }

            mediaService.deleteMedia(userId, mediaId);
            return ResponseEntity.ok(Map.of(
                    "message", "Media deleted successfully",
                    "mediaId", mediaId
            ));

        } catch (RuntimeException e) {
            log.warn("⚠️ Delete failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Delete media error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete media"));
        }
    }

    /**
     * Get current storage usage (informational, NOT a quota limit)
     * GET /api/v1/gallery/usage
     *
     * PAY-AS-YOU-USE MODEL:
     * - No quota enforcement
     * - Users can upload unlimited
     * - Only charged for what they use (byte-days)
     * - This endpoint shows stats for reference only
     */
    @GetMapping("/usage")
    @Operation(summary = "Get storage usage stats", description = "Get current storage usage information (informational, no quota limits)")
    public ResponseEntity<?> getStorageUsage(HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            log.info("📊 Storage usage request from user: {}", userId);

            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User ID not found in context"));
            }

            StorageUsageResponse usage = mediaService.getStorageUsage(userId);
            return ResponseEntity.ok(Map.of(
                    "totalBytesUsed", usage.getTotalBytesUsed(),
                    "fileCount", usage.getFileCount(),
                    "breakdown", Map.of(
                            "imageBytes", usage.getImageBytes(),
                            "videoBytes", usage.getVideoBytes()
                    ),
                    "formatted", usage.getFormattedUsage(),
                    "message", "Pay-as-you-use model: No quota limits, charged only for bytes-days used"
            ));

        } catch (Exception e) {
            log.error("❌ Storage usage error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get storage usage"));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verify Gallery service is running")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Gallery",
                "timestamp", java.time.Instant.now()
        ));
    }
}
