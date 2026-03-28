package com.example.Gallery.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * DEPRECATED: Use /api/v1/gallery/usage endpoint instead
 *
 * This controller is maintained for backwards compatibility only.
 * The Gallery service uses a PAY-AS-YOU-USE model with NO QUOTAS.
 * There are no storage limits - users can upload unlimited files.
 * They are charged only for what they use (by byte-days).
 *
 * Use GET /api/v1/gallery/usage for complete usage information.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/gallery")
@Tag(name = "Gallery Quota (Deprecated)", description = "⚠️ DEPRECATED - Use /api/v1/gallery/usage instead. No quotas in pay-as-you-use model.")
@Deprecated
public class GalleryQuotaController {

    @GetMapping("/quota")
    @Operation(summary = "Get gallery quota (DEPRECATED)", description = "⚠️ DEPRECATED: Use /api/v1/gallery/usage instead. This endpoint returns empty as there are no quotas.")
    public ResponseEntity<Map<String, Object>> getQuota() {
        log.warn("⚠️ DEPRECATED endpoint /quota called. Use /usage instead. No quotas in pay-as-you-use model.");
        return ResponseEntity.ok(Map.of(
                "message", "PAY-AS-YOU-USE model: No storage quotas",
                "limitBytes", -1L,  // -1 = unlimited
                "usedBytes", 0L,
                "deprecated", true,
                "useInstead", "/api/v1/gallery/usage"
        ));
    }
}
