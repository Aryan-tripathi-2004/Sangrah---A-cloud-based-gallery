package com.example.Billing.api.controller;

import com.example.Billing.api.dto.response.DomainUsageResponse;
import com.example.Billing.application.service.interfaces.IBillingUsageService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing/usage")
@RequiredArgsConstructor
public class BillingUsageController {

    private final IBillingUsageService billingUsageService;

    /**
     * Get storage usage details for the Gallery domain
     */
    @GetMapping("/gallery")
    @Operation(summary = "Get gallery usage details")
    public ResponseEntity<DomainUsageResponse> galleryUsage() {
        return ResponseEntity.ok(billingUsageService.getGalleryUsage());
    }

    /**
     * Get storage usage details for the Events domain
     */
    @GetMapping("/events")
    @Operation(summary = "Get event usage details")
    public ResponseEntity<DomainUsageResponse> eventUsage() {
        return ResponseEntity.ok(billingUsageService.getEventUsage());
    }
}
