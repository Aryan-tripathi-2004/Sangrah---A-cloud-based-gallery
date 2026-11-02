package com.example.Billing.application.service.interfaces;

/**
 * Service contract for generating PDF invoices.
 * Adheres to Dependency Inversion Principle (DIP).
 */
public interface IPDFGenerationService {
    byte[] generateInvoicePDF(String invoiceId);
}
