package com.example.Billing.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "email-service",
        url = "${email.service.url:http://localhost:8086}"
)
public interface EmailServiceClient {

    @PostMapping(
            value = "/api/v1/email/invoices/paid",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    void sendInvoicePaidEmail(
            @RequestParam String invoiceId,
            @RequestParam String userId,
            @RequestParam Double amount,
            @RequestBody byte[] pdfContent,
            @RequestParam String userEmail
    );
}
