package com.example.Email.api.controller;

import com.example.Email.api.dto.EmailSendRequest;
import com.example.Email.api.dto.EmailSendResponse;
import com.example.Email.api.dto.response.EmailServiceInfoResponse;
import com.example.Email.api.dto.response.HealthResponse;
import com.example.Email.application.service.interfaces.IEmailService;
import com.example.Email.shared.enums.EmailStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
 *   <li>Map the service response to an appropriate HTTP status code.</li>
 * </ol>
 *
 * <p>All exception handling is centralised in
 * {@link com.example.Email.shared.exception.GlobalExceptionHandler}.
 * No try-catch blocks appear in this class.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
@Validated
@Tag(name = "Email", description = "Email Notification Management")
public class EmailSendController {

    /** Dependency declared against the interface, not the concrete implementation (DIP). */
    private final IEmailService emailService;

    @PostMapping
    @Operation(summary = "Dispatch email notification", description = "Routes to the specific email template based on the EmailType payload")
    public ResponseEntity<EmailSendResponse> sendEmail(@Valid @RequestBody EmailSendRequest request) {
        
        log.info("📧 [CONTROLLER] Email dispatch request | to={} type={}", request.userEmail(), request.emailType());
        
        EmailSendResponse response = switch (request.emailType()) {
            case INVOICE_PAID -> emailService.sendInvoicePaidEmail(
                    request.invoiceId(), request.userId(), request.userEmail(), request.amount(), request.pdfContent());
            case INVOICE_CREATED -> emailService.sendInvoiceCreatedEmail(
                    request.invoiceId(), request.userId(), request.userEmail(), request.amount(), "Invoice Due");
            case PAYMENT_FAILED -> emailService.sendPaymentFailedEmail(
                    request.invoiceId(), request.userId(), request.userEmail(), "Payment processing failed");
            default -> emailService.sendNotificationEmail(
                    request.userEmail(), request.emailType(), request.subject(), request.body());
        };

        return ResponseEntity
                .status(EmailStatus.SENT.equals(response.status()) ? HttpStatus.OK : HttpStatus.ACCEPTED)
                .body(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Operational endpoints
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Liveness probe – confirms the service JVM is responsive.
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if Email service is running")
    public ResponseEntity<HealthResponse> health() {
        log.info("✅ [CONTROLLER] Email service health check");
        return ResponseEntity.ok(new HealthResponse(emailService.getHealth()));
    }

    /**
     * Service capabilities descriptor – returns supported email types derived
     * from the canonical {@link com.example.Email.shared.enums.EmailType} enum.
     */
    @GetMapping("/info")
    @Operation(summary = "Service info", description = "Get email service information")
    public ResponseEntity<EmailServiceInfoResponse> info() {
        log.info("ℹ️ [CONTROLLER] Email service info requested");
        return ResponseEntity.ok(emailService.getServiceInfo());
    }
}
