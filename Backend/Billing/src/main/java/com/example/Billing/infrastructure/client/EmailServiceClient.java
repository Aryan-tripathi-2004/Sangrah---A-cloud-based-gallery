package com.example.Billing.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@FeignClient(
        name = "email-service",
        url = "${email.service.url:http://localhost:8086}"
)
public interface EmailServiceClient {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class EmailSendRequest {
        private String invoiceId;
        private String userId;
        private String userEmail;
        private Double amount;
        private byte[] pdfContent;
        private String emailType;
    }

    @PostMapping(
            value = "/api/v1/email/invoices/paid",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    void sendInvoicePaidEmail(@RequestBody EmailSendRequest request);
}
