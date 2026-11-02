package com.example.Billing.application.service.interfaces;

import com.example.Billing.api.dto.response.InvoiceGenerationResultResponse;

public interface IInvoiceGenerationService {

    InvoiceGenerationResultResponse processMonthlyInvoiceGeneration();

    InvoiceGenerationResultResponse processCurrentMonthInvoiceGeneration();

    InvoiceGenerationResultResponse triggerMonthlyInvoiceGenerationForAdmin(String role);

    InvoiceGenerationResultResponse triggerCurrentMonthInvoiceGenerationForAdmin(String role);
}
