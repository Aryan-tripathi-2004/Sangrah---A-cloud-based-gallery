import { Injectable, inject, signal } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { BillingApiService } from '../../core/api/services/billing-api.service';
import { CostEstimateDTO, InvoiceDTO, PaymentDTO, PagedResponse } from '../../core/api/models/api.models';
import { environment } from '../../../environments/environment';

// Re-export DTOs
export type { CostEstimateDTO, InvoiceDTO, PaymentDTO, PagedResponse };

@Injectable({ providedIn: 'root' })
export class BillingService {
  private readonly api = inject(BillingApiService);

  // ===== STRICTLY TYPED SIGNALS =====
  public readonly costEstimate = signal<CostEstimateDTO | null>(null);
  public readonly invoices = signal<InvoiceDTO[]>([]);
  public readonly currentInvoice = signal<InvoiceDTO | null>(null);
  public readonly payments = signal<PaymentDTO[]>([]);
  public readonly totalPages = signal<number>(1);
  public readonly loading = signal<boolean>(false);
  public readonly error = signal<string>('');
  public readonly paymentIntent = signal<PaymentIntentState | null>(null);

  // ===== LOAD METHODS =====

  loadCostEstimate(): Observable<CostEstimateDTO> {
    this.loading.set(true);
    return this.api.getCostEstimate().pipe(
      tap(data => {
        this.costEstimate.set(data);
        this.loading.set(false);
        this.error.set('');
      }),
      catchError(err => {
        this.error.set('Failed to load cost estimate');
        this.loading.set(false);
        return throwError(() => err);
      })
    );
  }

  loadInvoices(page: number = 0, size: number = 10): Observable<PagedResponse<InvoiceDTO>> {
    this.loading.set(true);
    return this.api.getInvoices({ page: page.toString(), size: size.toString() }).pipe(
      tap(response => {
        this.invoices.set(response.content || []);
        this.totalPages.set(response.totalPages || 1);
        this.loading.set(false);
        this.error.set('');
      }),
      catchError(err => {
        this.error.set('Failed to load invoices');
        this.loading.set(false);
        return throwError(() => err);
      })
    );
  }

  loadInvoiceDetail(invoiceId: string): Observable<InvoiceDTO> {
    this.loading.set(true);
    return this.api.getInvoiceDetail(invoiceId).pipe(
      tap(data => {
        this.currentInvoice.set(data);
        this.loading.set(false);
        this.error.set('');
      }),
      catchError(err => {
        this.error.set('Failed to load invoice details');
        this.loading.set(false);
        return throwError(() => err);
      })
    );
  }

  loadPaymentHistory(): Observable<PaymentDTO[]> {
    this.loading.set(true);
    return this.api.getPaymentHistory().pipe(
      tap(data => {
        this.payments.set(Array.isArray(data) ? data : []);
        this.loading.set(false);
        this.error.set('');
      }),
      catchError(err => {
        this.error.set('Failed to load payment history');
        this.loading.set(false);
        return throwError(() => err);
      })
    );
  }

  // ===== ACTION METHODS =====

  payInvoice(invoiceId: string): Observable<any> {
    return this.api.payInvoice(invoiceId).pipe(
      tap(() => {
        this.error.set('');
      }),
      catchError(err => {
        this.error.set(err?.error?.error || 'Payment initiation failed');
        return throwError(() => err);
      })
    );
  }

  // ===== STRIPE PAYMENT METHODS =====

  /**
   * Create Stripe payment intent for an invoice
   * Called by StripePaymentModalComponent
   */
  createPaymentIntent(invoiceId: string): Observable<CheckoutSessionResponse> {
    this.loading.set(true);
    return this.api.createCheckoutSession(invoiceId).pipe(
      tap(response => {
        this.paymentIntent.set({
          clientSecret: response.clientSecret,
          amount: response.amount,
          invoiceId: invoiceId,
          currency: response.currency
        });
        this.loading.set(false);
        this.error.set('');
      }),
      catchError(err => {
        this.error.set('Failed to create payment session');
        this.loading.set(false);
        return throwError(() => err);
      })
    );
  }

  /**
   * Get Stripe public key from environment
   */
  getStripePublicKey(): string {
    return environment.stripePublicKey;
  }

  /**
   * Get current payment intent state
   */
  getCurrentPaymentIntent(): PaymentIntentState | null {
    return this.paymentIntent();
  }

  /**
   * Clear payment intent state
   */
  clearPaymentIntent(): void {
    this.paymentIntent.set(null);
  }

  /**
   * Get payment status for an invoice
   */
  getPaymentStatus(invoiceId: string): Observable<{status: string, paidDate?: string}> {
    return this.api.getPaymentStatus(invoiceId);
  }

  /**
   * Sync payment status with Stripe API
   */
  syncPaymentStatus(invoiceId: string): Observable<{status: string, paidDate?: string}> {
    return this.api.syncPaymentStatus(invoiceId);
  }

  /**
   * Download invoice PDF
   */
  downloadInvoicePDF(invoiceId: string): Observable<Blob> {
    return this.api.downloadInvoicePDF(invoiceId);
  }

  // ===== UTILITY METHODS =====

  clearError(): void {
    this.error.set('');
  }

  getContextualCostMessage(estimate: CostEstimateDTO): string {
    const projected = estimate.currentMonthData.projectedMonthlyTotal;
    if (projected < 5) return '✅ Good - Your usage is low';
    if (projected < 20) return '⚠️ Moderate - Keep an eye on usage';
    if (projected < 50) return '📊 High - Consider optimizing storage';
    return '🔴 Very High - Review your storage usage';
  }
}

// ===== INTERFACES =====

export interface PaymentIntentState {
  clientSecret: string;
  amount: number;
  invoiceId: string;
  currency: string;
}

export interface CheckoutSessionResponse {
  invoiceId: string;
  clientSecret: string;
  amount: number;
  currency: string;
}
