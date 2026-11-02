package com.example.Gallery.api.controller;

import com.example.Gallery.api.dto.response.QuotaResponse;
import com.example.Gallery.application.service.interfaces.IGalleryMediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DEPRECATED: Use /api/v1/gallery/usage endpoint instead
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/gallery")
@RequiredArgsConstructor
@Tag(name = "Gallery Quota (Deprecated)", description = "⚠️ DEPRECATED - Use /api/v1/gallery/usage instead. No quotas in pay-as-you-use model.")
@Deprecated
public class GalleryQuotaController {

    private final IGalleryMediaService mediaService;

    @GetMapping("/quota")
    @Operation(summary = "Get gallery quota (DEPRECATED)", description = "⚠️ DEPRECATED: Use /api/v1/gallery/usage instead. This endpoint returns empty as there are no quotas.")
    public ResponseEntity<QuotaResponse> getQuota() {
        log.warn("⚠️ DEPRECATED endpoint /quota called. Use /usage instead. No quotas in pay-as-you-use model.");
        return ResponseEntity.ok(mediaService.getQuota());
    }
}
