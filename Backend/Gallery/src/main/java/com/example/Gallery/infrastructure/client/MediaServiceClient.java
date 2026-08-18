package com.example.Gallery.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.Gallery.infrastructure.client.dto.MediaServiceUploadResponse;
import com.example.Gallery.infrastructure.client.dto.MediaServiceDetailResponse;

import java.util.Map;

@FeignClient(name = "media-service", url = "${media.service.url:http://localhost:8087}")
public interface MediaServiceClient {

    /**
     * Upload media file to Media Service
     */
    @PostMapping(value = "/api/v1/media/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    MediaServiceUploadResponse uploadMedia(
            @RequestPart("file") MultipartFile file,
            @RequestParam("domain") String domain,
            @RequestParam(value = "entityRefId", required = false) String entityRefId,
            @RequestHeader("X-User-Id") String userId
    );

    /**
     * Get media details from Media Service
     */
    @GetMapping("/api/v1/media/{mediaId}")
    MediaServiceDetailResponse getMediaDetails(@PathVariable("mediaId") String mediaId);

    /**
     * Get storage ledger for billing
     */
    @GetMapping("/api/v1/media/ledger")
    Object getStorageLedger(
            @RequestParam("userId") String userId,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate
    );

    /**
     * Delete media from Media Service
     */
    @DeleteMapping("/api/v1/media/{mediaId}")
    Map<String, String> deleteMedia(
            @PathVariable("mediaId") String mediaId,
            @RequestHeader("X-User-Id") String userId
    );

    /**
     * Get storage usage for user
     */
    @GetMapping("/api/v1/media/usage/{userId}")
    Map<String, Object> getStorageUsage(@PathVariable("userId") String userId);

    /**
     * Get media file from Media Service
     */
    @GetMapping("/api/v1/media/{mediaId}/file")
    byte[] getMediaFile(
            @PathVariable("mediaId") String mediaId,
            @RequestHeader("X-User-Id") String userId
    );
}
