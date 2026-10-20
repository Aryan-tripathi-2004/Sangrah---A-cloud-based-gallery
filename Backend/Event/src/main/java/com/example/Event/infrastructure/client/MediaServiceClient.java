package com.example.Event.infrastructure.client;

import com.example.Event.infrastructure.client.dto.MediaDeleteResponse;
import com.example.Event.infrastructure.client.dto.MediaServiceResponse;
import com.example.Event.shared.enums.MediaDomain;
import java.time.Instant;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "media-service-event", url = "${media.service.url:http://localhost:8087}")
public interface MediaServiceClient {

    /**
     * Upload media file to Media Service (for Events domain)
     */
    @PostMapping(value = "/api/v1/media/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    MediaServiceResponse uploadMedia(
            @RequestPart("file") MultipartFile file,
            @RequestParam("domain") MediaDomain domain,
            @RequestParam(value = "entityRefId", required = false) String entityRefId,
            @RequestHeader("X-User-Id") String userId
    );

    /**
     * Get media details
     */
    @GetMapping(value = "/api/v1/media/{mediaId}", headers = "Accept=application/json")
    MediaServiceResponse getMediaDetails(@PathVariable("mediaId") String mediaId);

    /**
     * Delete media
     */
    @DeleteMapping("/api/v1/media/{mediaId}")
    MediaDeleteResponse deleteMedia(
            @PathVariable("mediaId") String mediaId,
            @RequestHeader("X-User-Id") String userId
    );

    /**
     * Bulk delete media files
     */
    @DeleteMapping("/api/v1/media/bulk")
    MediaDeleteResponse deleteMediaBulk(
            @RequestBody List<String> mediaIds,
            @RequestHeader("X-User-Id") String userId
    );

    /**
     * Get storage ledger for billing
     */
    @GetMapping("/api/v1/media/ledger")
    Object getStorageLedger(
            @RequestParam("userId") String userId,
            @RequestParam("startDate") Instant startDate,
            @RequestParam("endDate") Instant endDate
    );

    /**
     * Get media file from Media Service
     */
    @GetMapping(value = "/api/v1/media/{mediaId}", headers = "Accept=application/octet-stream")
    byte[] getMediaFile(
            @PathVariable("mediaId") String mediaId,
            @RequestHeader("X-User-Id") String userId
    );
}
