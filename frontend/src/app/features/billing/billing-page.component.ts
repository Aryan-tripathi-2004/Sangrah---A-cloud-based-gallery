import { Component, OnInit, ChangeDetectionStrategy, inject, signal, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutComponent } from '../../shared/layout/layout.component';
import { BillingService, InvoiceDTO } from './billing.service';
import { BillingSummaryComponent } from './components/billing-summary/billing-summary.component';
import { InvoiceListComponent } from './components/invoice-list/invoice-list.component';
import { PaymentHistoryComponent } from './components/payment-history/payment-history.component';
import { BillingSettingsComponent } from './components/billing-settings/billing-settings.component';
import { InvoiceDetailModalComponent } from './components/invoice-detail-modal/invoice-detail-modal.component';
import { StripePaymentModalComponent } from './components/stripe-payment-modal/stripe-payment-modal.component';

type Tab = 'dashboard' | 'invoices' | 'payments' | 'settings';

@Component({
  selector: 'app-billing-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    LayoutComponent,
    BillingSummaryComponent,
    InvoiceListComponent,
    PaymentHistoryComponent,
    BillingSettingsComponent,
    InvoiceDetailModalComponent,
    StripePaymentModalComponent
  ],
  templateUrl: './billing-page.component.html'
})
export class BillingPageComponent implements OnInit {
  private readonly billingService = inject(BillingService);
  private readonly destroyRef = inject(DestroyRef);

  readonly activeTab = signal<Tab>('dashboard');
  readonly showInvoiceModal = signal<boolean>(false);
  readonly selectedInvoice = signal<InvoiceDTO | null>(null);
  readonly showStripeModal = signal<boolean>(false);
  readonly stripeInvoiceId = signal<string | null>(null);

  readonly tabs = [
    { id: 'dashboard', label: '📊 Dashboard' },
    { id: 'invoices', label: '📄 Invoices' },
    { id: 'payments', label: '💳 Payments' },
    { id: 'settings', label: '⚙️ Settings' }
  ] as const;

  readonly loading = this.billingService.loading;
  readonly error = this.billingService.error;

  ngOnInit(): void {
    // Load initial data with destroyRef lifecycle safety
    this.billingService.loadCostEstimate()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe();

    this.billingService.loadInvoices()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe();
  }

  switchTab(tabId: Tab): void {
    this.activeTab.set(tabId);
    if (tabId === 'payments') {
      this.billingService.loadPaymentHistory()
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe();
    }
  }

  openInvoiceDetail(invoice: InvoiceDTO): void {
    this.selectedInvoice.set(invoice);
    this.showInvoiceModal.set(true);
  }

  closeInvoiceModal(): void {
    this.showInvoiceModal.set(false);
    this.selectedInvoice.set(null);
  }

  onPayInvoice(invoiceId: string): void {
    this.stripeInvoiceId.set(invoiceId);
    this.showStripeModal.set(true);
  }

  onStripePaymentSuccess(): void {
    this.showStripeModal.set(false);
    this.showInvoiceModal.set(false);
    this.stripeInvoiceId.set(null);
    this.selectedInvoice.set(null);

    this.billingService.loadInvoices()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe();

    console.log('✅ Payment processed successfully');
  }

  onStripeModalClose(): void {
    this.showStripeModal.set(false);
    this.stripeInvoiceId.set(null);
  }

  dismissError(): void {
    this.billingService.clearError();
  }
}
