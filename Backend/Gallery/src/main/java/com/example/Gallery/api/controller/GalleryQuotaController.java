package com.example.Gallery.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Gallery Quota (Deprecated)", description = "⚠️ DEPRECATED - Use /api/v1/gallery/usage instead. No quotas in pay-as-you-use model.")
@Deprecated
public class GalleryQuotaController {

    public record QuotaResponse(String message, long limitBytes, long usedBytes, boolean deprecated, String useInstead) {}

    @GetMapping("/quota")
    @Operation(summary = "Get gallery quota (DEPRECATED)", description = "⚠️ DEPRECATED: Use /api/v1/gallery/usage instead. This endpoint returns empty as there are no quotas.")
    public ResponseEntity<QuotaResponse> getQuota() {
        log.warn("⚠️ DEPRECATED endpoint /quota called. Use /usage instead. No quotas in pay-as-you-use model.");
        return ResponseEntity.ok(new QuotaResponse(
                "PAY-AS-YOU-USE model: No storage quotas",
                -1L,
                0L,
                true,
                "/api/v1/gallery/usage"
        ));
    }
}
