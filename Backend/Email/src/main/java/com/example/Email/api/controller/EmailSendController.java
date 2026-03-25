package com.example.Email.api.controller;

import com.example.Email.api.dto.EmailSendRequest;
import com.example.Email.api.dto.EmailSendResponse;
import com.example.Email.application.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
@Validated
@Tag(name = "Email", description = "Email Notification Management")
public class EmailSendController {

    private final EmailService emailService;

    /**
     * Send Invoice Paid Email (called by Billing service)
     */
    @PostMapping("/invoices/paid")
    @Operation(summary = "Send invoice paid email", description = "Send payment confirmation email with PDF")
    public ResponseEntity<EmailSendResponse> sendInvoicePaidEmail(
            @Valid @RequestBody EmailSendRequest request
    ) {
        log.info("📧 Received invoice paid email request for: {}", request.getUserEmail());

        try {
            EmailSendResponse response = emailService.sendInvoicePaidEmail(
                    request.getInvoiceId(),
                    request.getUserId(),
                    request.getUserEmail(),
                    request.getAmount(),
                    request.getPdfContent()
            );

            return ResponseEntity
                    .status("sent".equals(response.getStatus()) ? HttpStatus.OK : HttpStatus.ACCEPTED)
                    .body(response);

        } catch (Exception e) {
            log.error("❌ Error sending invoice paid email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(EmailSendResponse.builder()
                            .status("failed")
                            .message("Failed to send email")
                            .errorReason(e.getMessage())
                            .build());
        }
    }

    /**
     * Send Invoice Created Email
     */
    @PostMapping("/invoices/created")
    @Operation(summary = "Send invoice created email", description = "Send invoice notification with payment link")
    public ResponseEntity<EmailSendResponse> sendInvoiceCreatedEmail(
            @Valid @RequestBody EmailSendRequest request
    ) {
        log.info("📧 Received invoice created email request for: {}", request.getUserEmail());

        try {
            EmailSendResponse response = emailService.sendInvoiceCreatedEmail(
                    request.getInvoiceId(),
                    request.getUserId(),
                    request.getUserEmail(),
                    request.getAmount(),
                    "Invoice Due"  // TODO: Accept dueDate in request
            );

            return ResponseEntity
                    .status("sent".equals(response.getStatus()) ? HttpStatus.OK : HttpStatus.ACCEPTED)
                    .body(response);

        } catch (Exception e) {
            log.error("❌ Error sending invoice created email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(EmailSendResponse.builder()
                            .status("failed")
                            .message("Failed to send email")
                            .errorReason(e.getMessage())
                            .build());
        }
    }

    /**
     * Send Payment Failed Email
     */
    @PostMapping("/payments/failed")
    @Operation(summary = "Send payment failed email", description = "Send payment failure notification")
    public ResponseEntity<EmailSendResponse> sendPaymentFailedEmail(
            @Valid @RequestBody EmailSendRequest request
    ) {
        log.info("📧 Received payment failed email request for: {}", request.getUserEmail());

        try {
            EmailSendResponse response = emailService.sendPaymentFailedEmail(
                    request.getInvoiceId(),
                    request.getUserId(),
                    request.getUserEmail(),
                    "Payment processing failed"  // TODO: Accept failureReason in request
            );

            return ResponseEntity
                    .status("sent".equals(response.getStatus()) ? HttpStatus.OK : HttpStatus.ACCEPTED)
                    .body(response);

        } catch (Exception e) {
            log.error("❌ Error sending payment failed email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(EmailSendResponse.builder()
                            .status("failed")
                            .message("Failed to send email")
                            .errorReason(e.getMessage())
                            .build());
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if Email service is running")
    public ResponseEntity<String> health() {
        log.info("✅ Email service health check");
        return ResponseEntity.ok("Email service is running");
    }

    /**
     * Get email service info
     */
    @GetMapping("/info")
    @Operation(summary = "Service info", description = "Get email service information")
    public ResponseEntity<?> info() {
        log.info("ℹ️ Email service info requested");
        return ResponseEntity.ok(new EmailServiceInfo());
    }

    // Helper class for service info
    @lombok.Data
    public static class EmailServiceInfo {
        private String serviceName = "Email Notification Service";
        private String version = "1.0.0";
        private String status = "active";
        private String[] supportedEmailTypes = {"invoice-paid", "invoice-created", "payment-failed", "otp"};
    }
}
