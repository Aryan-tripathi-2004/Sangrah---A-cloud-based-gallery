package com.example.Email.api.controller;

import com.example.Email.api.dto.EmailSendRequest;
import com.example.Email.api.dto.EmailSendResponse;
import com.example.Email.api.dto.response.EmailServiceInfoResponse;
import com.example.Email.api.dto.response.HealthResponse;
import com.example.Email.application.service.interfaces.IEmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Lightweight HTTP router for the Email microservice.
 *
 * <p>This controller owns zero business logic. Its sole responsibilities are:
 * <ol>
 *   <li>Accept and validate the incoming HTTP request.</li>
 *   <li>Delegate execution to {@link IEmailService}.</li>
 * </ol>
 */
@Slf4j
@RestController
@RequestMapping({"/api/v1/email", "/internal/api/v1/email"})
@RequiredArgsConstructor
@Validated
@Tag(name = "Email", description = "Email Notification Management")
public class EmailSendController {

    private final IEmailService emailService;

    @PostMapping
    @Operation(summary = "Dispatch email notification", description = "Routes to the specific email template based on the EmailType payload")
    public ResponseEntity<EmailSendResponse> sendEmail(@Valid @RequestBody EmailSendRequest request) {
        log.info("📧 [CONTROLLER] Email dispatch request | to={} type={}", request.userEmail(), request.emailType());
        return emailService.dispatchEmailResponse(request);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if Email service is running")
    public ResponseEntity<HealthResponse> health() {
        log.info("✅ [CONTROLLER] Email service health check");
        return ResponseEntity.ok(emailService.getHealthResponse());
    }

    @GetMapping("/info")
    @Operation(summary = "Service info", description = "Get email service information")
    public ResponseEntity<EmailServiceInfoResponse> info() {
        log.info("ℹ️ [CONTROLLER] Email service info requested");
        return ResponseEntity.ok(emailService.getServiceInfo());
    }
}
