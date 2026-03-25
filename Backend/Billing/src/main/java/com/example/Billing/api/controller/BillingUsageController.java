package com.example.Billing.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing/usage")
public class BillingUsageController {

    @GetMapping("/gallery")
    @Operation(summary = "Get gallery usage details")
    public ResponseEntity<Map<String, Object>> galleryUsage() {
        return ResponseEntity.ok(Map.of("domain", "GALLERY", "bytesSeconds", 0));
    }

    @GetMapping("/events")
    @Operation(summary = "Get event usage details")
    public ResponseEntity<Map<String, Object>> eventUsage() {
        return ResponseEntity.ok(Map.of("domain", "EVENT", "bytesSeconds", 0));
    }
}
