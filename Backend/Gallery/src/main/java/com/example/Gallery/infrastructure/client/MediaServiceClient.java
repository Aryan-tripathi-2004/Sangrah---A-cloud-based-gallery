package com.example.Gallery.infrastructure.client;

import com.example.Gallery.api.dto.StorageUsageLedgerDTO;
import com.example.Gallery.infrastructure.client.dto.MediaServiceDeleteResponse;
import com.example.Gallery.infrastructure.client.dto.MediaServiceDetailResponse;
import com.example.Gallery.infrastructure.client.dto.MediaServiceStorageUsageResponse;
import com.example.Gallery.infrastructure.client.dto.MediaServiceUploadResponse;
import java.time.Instant;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
    @GetMapping(value = "/api/v1/media/{mediaId}", headers = "Accept=application/json")
    MediaServiceDetailResponse getMediaDetails(@PathVariable("mediaId") String mediaId);

    /**
     * Get storage ledger for billing
     */
    @GetMapping("/api/v1/media/ledger")
    List<StorageUsageLedgerDTO> getStorageLedger(
            @RequestParam("userId") String userId,
            @RequestParam("startDate") Instant startDate,
            @RequestParam("endDate") Instant endDate
    );

    /**
     * Delete media from Media Service
     */
    @DeleteMapping("/api/v1/media/{mediaId}")
    MediaServiceDeleteResponse deleteMedia(
            @PathVariable("mediaId") String mediaId,
            @RequestHeader("X-User-Id") String userId
    );

    /**
     * Get storage usage for user
     */
    @GetMapping("/api/v1/media/usage")
    MediaServiceStorageUsageResponse getStorageUsage(@RequestParam("userId") String userId);

    /**
     * Get media file from Media Service
     */
    @GetMapping(value = "/api/v1/media/{mediaId}", headers = "Accept=application/octet-stream")
    byte[] getMediaFile(
            @PathVariable("mediaId") String mediaId,
            @RequestHeader("X-User-Id") String userId
    );
}

