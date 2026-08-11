package com.example.Email.api.controller;

import com.example.Email.api.dto.EmailSendRequest;
import com.example.Email.api.dto.EmailSendResponse;
import com.example.Email.api.dto.response.EmailServiceInfoResponse;
import com.example.Email.application.service.interfaces.IEmailService;
import com.example.Email.shared.enums.EmailStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
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

    // ─────────────────────────────────────────────────────────────────────────
    // Billing endpoints
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Send a payment-confirmation email with an optional PDF invoice attachment.
     * Called by the Billing microservice after a successful payment.
     */
    @PostMapping("/invoices/paid")
    @Operation(
            summary     = "Send invoice paid email",
            description = "Send payment confirmation email with optional PDF attachment"
    )
    public ResponseEntity<EmailSendResponse> sendInvoicePaidEmail(
            @Valid @RequestBody EmailSendRequest request
    ) throws MessagingException {

        log.info("📧 ═══════════════════════════════════════════════════════════════");
        log.info("📧 [CONTROLLER] Invoice-paid request | to={} invoice={} amount=${}",
                request.userEmail(), request.invoiceId(), request.amount());
        log.info("📧 ═══════════════════════════════════════════════════════════════");

        EmailSendResponse response = emailService.sendInvoicePaidEmail(
                request.invoiceId(),
                request.userId(),
                request.userEmail(),
                request.amount(),
                request.pdfContent()
        );

        log.info("✅ [CONTROLLER] Invoice-paid email dispatched | emailId={}", response.emailId());
        return ResponseEntity
                .status(EmailStatus.SENT.equals(response.status()) ? HttpStatus.OK : HttpStatus.ACCEPTED)
                .body(response);
    }

    /**
     * Send a new-invoice notification with a payment link.
     */
    @PostMapping("/invoices/created")
    @Operation(
            summary     = "Send invoice created email",
            description = "Send invoice notification with payment link"
    )
    public ResponseEntity<EmailSendResponse> sendInvoiceCreatedEmail(
            @Valid @RequestBody EmailSendRequest request
    ) throws MessagingException {

        log.info("📧 [CONTROLLER] Invoice-created request | to={} invoice={}",
                request.userEmail(), request.invoiceId());

        EmailSendResponse response = emailService.sendInvoiceCreatedEmail(
                request.invoiceId(),
                request.userId(),
                request.userEmail(),
                request.amount(),
                "Invoice Due"   // TODO: promote dueDate into EmailSendRequest when required
        );

        log.info("✅ [CONTROLLER] Invoice-created email dispatched | emailId={}", response.emailId());
        return ResponseEntity
                .status(EmailStatus.SENT.equals(response.status()) ? HttpStatus.OK : HttpStatus.ACCEPTED)
                .body(response);
    }

    /**
     * Send a payment-failure alert with a retry link.
     */
    @PostMapping("/payments/failed")
    @Operation(
            summary     = "Send payment failed email",
            description = "Send payment failure notification"
    )
    public ResponseEntity<EmailSendResponse> sendPaymentFailedEmail(
            @Valid @RequestBody EmailSendRequest request
    ) throws MessagingException {

        log.info("📧 [CONTROLLER] Payment-failed request | to={} invoice={}",
                request.userEmail(), request.invoiceId());

        EmailSendResponse response = emailService.sendPaymentFailedEmail(
                request.invoiceId(),
                request.userId(),
                request.userEmail(),
                "Payment processing failed"  // TODO: promote failureReason into EmailSendRequest when required
        );

        log.info("✅ [CONTROLLER] Payment-failed email dispatched | emailId={}", response.emailId());
        return ResponseEntity
                .status(EmailStatus.SENT.equals(response.status()) ? HttpStatus.OK : HttpStatus.ACCEPTED)
                .body(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Generic notification endpoint (Event Service)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Send a generic event-driven notification email.
     * Called by the Event Service for: access-approved, access-rejected, media-approved, etc.
     */
    @PostMapping("/notify")
    @Operation(
            summary     = "Send generic notification email",
            description = "Send event notification emails"
    )
    public ResponseEntity<EmailSendResponse> sendNotificationEmail(
            @Valid @RequestBody EmailSendRequest request
    ) throws MessagingException {

        log.info("📧 [CONTROLLER] Notification request | to={} type={}",
                request.userEmail(), request.emailType());

        EmailSendResponse response = emailService.sendNotificationEmail(
                request.userEmail(),
                request.emailType() != null ? request.emailType().name() : null,
                request.subject()  != null ? request.subject()  : "Notification",
                request.body()     != null ? request.body()     : ""
        );

        log.info("✅ [CONTROLLER] Notification email dispatched | emailId={}", response.emailId());
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
    public ResponseEntity<String> health() {
        log.info("✅ [CONTROLLER] Email service health check");
        return ResponseEntity.ok("Email service is running");
    }

    /**
     * Service capabilities descriptor – returns supported email types derived
     * from the canonical {@link com.example.Email.shared.enums.EmailType} enum.
     */
    @GetMapping("/info")
    @Operation(summary = "Service info", description = "Get email service information")
    public ResponseEntity<EmailServiceInfoResponse> info() {
        log.info("ℹ️ [CONTROLLER] Email service info requested");
        return ResponseEntity.ok(EmailServiceInfoResponse.defaults());
    }
}
