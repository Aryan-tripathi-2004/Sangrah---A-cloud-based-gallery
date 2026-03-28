import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
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
  activeTab: Tab = 'dashboard';

  showInvoiceModal = false;
  selectedInvoice: InvoiceDTO | null = null;

  showStripeModal = false;
  stripeInvoiceId: string | null = null;

  tabs = [
    { id: 'dashboard', label: '📊 Dashboard' },
    { id: 'invoices', label: '📄 Invoices' },
    { id: 'payments', label: '💳 Payments' },
    { id: 'settings', label: '⚙️ Settings' }
  ] as const;

  loading$;
  error$;

  constructor(private billingService: BillingService) {
    this.loading$ = this.billingService.loading$;
    this.error$ = this.billingService.error$;
  }

  ngOnInit(): void {
    // Load initial data
    this.billingService.loadCostEstimate().subscribe();
    this.billingService.loadInvoices().subscribe();
  }

  switchTab(tabId: Tab): void {
    this.activeTab = tabId;
    // Load specific data when switching tabs
    if (tabId === 'payments') {
      this.billingService.loadPaymentHistory().subscribe();
    }
  }

  openInvoiceDetail(invoice: InvoiceDTO): void {
    this.selectedInvoice = invoice;
    this.showInvoiceModal = true;
  }

  closeInvoiceModal(): void {
    this.showInvoiceModal = false;
    this.selectedInvoice = null;
  }

  onPayInvoice(invoiceId: string): void {
    // Show Stripe payment modal
    this.stripeInvoiceId = invoiceId;
    this.showStripeModal = true;
  }

  onStripePaymentSuccess(): void {
    // Close modals and reload invoices
    this.showStripeModal = false;
    this.showInvoiceModal = false;
    this.stripeInvoiceId = null;
    this.selectedInvoice = null;

    // Reload invoice list to reflect updated payment status
    this.billingService.loadInvoices().subscribe();

    // Show success message (optional)
    console.log('✅ Payment processed successfully');
  }

  onStripeModalClose(): void {
    this.showStripeModal = false;
    this.stripeInvoiceId = null;
  }

  dismissError(): void {
    this.billingService.clearError();
  }
}
