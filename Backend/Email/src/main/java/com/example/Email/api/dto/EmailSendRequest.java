package com.example.Email.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailSendRequest {

    @NotBlank(message = "Invoice ID is required")
    private String invoiceId;

    @NotBlank(message = "User ID is required")
    private String userId;

    @Email(message = "Valid email is required")
    private String userEmail;

    private Double amount;

    private byte[] pdfContent;  // Optional: PDF attachment as byte array

    private String emailType;  // invoice-paid, invoice-created, payment-failed
}
