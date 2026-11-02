package com.example.Billing.infrastructure.event;

import com.example.Billing.application.service.interfaces.IInvoiceLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePaidEventListener {

    private final IInvoiceLifecycleService invoiceLifecycleService;

    @Async
    @EventListener
    public void onInvoicePaid(InvoicePaidEvent event) {
        log.info("[PAYMENT EVENT] Processing InvoicePaidEvent for invoice: {} | user: {}",
                event.getInvoiceId(), event.getUserId());
        invoiceLifecycleService.processPaidInvoiceReceiptAndNotification(
                event.getUserId(), event.getInvoiceId());
    }
}
