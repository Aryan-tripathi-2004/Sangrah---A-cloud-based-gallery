package com.example.Event.infrastructure.client;

import com.example.Event.infrastructure.client.dto.MediaDeleteResponse;
import com.example.Event.infrastructure.client.dto.MediaServiceResponse;
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
            @RequestParam("domain") String domain,
            @RequestParam(value = "entityRefId", required = false) String entityRefId,
            @RequestHeader("X-User-Id") String userId
    );

    /**
     * Get media details
     */
    @GetMapping("/api/v1/media/{mediaId}")
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
     * Get storage ledger for billing
     */
    @GetMapping("/api/v1/media/ledger")
    Object getStorageLedger(
            @RequestParam("userId") String userId,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate
    );

    /**
     * Get media file from Media Service
     */
    @GetMapping("/api/v1/media/{mediaId}/file")
    byte[] getMediaFile(
            @PathVariable("mediaId") String mediaId,
            @RequestHeader("X-User-Id") String userId
    );
}
