import { Injectable, Inject, Optional } from '@angular/core';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { SangrahApiService, CostEstimateDTO, InvoiceDTO, PaymentDTO, PagedResponse } from '../../core/api/sangrah-api.service';
import { environment } from '../../../environments/environment';

// Re-export DTOs
export type { CostEstimateDTO, InvoiceDTO, PaymentDTO, PagedResponse };

@Injectable({ providedIn: 'root' })
export class BillingService {
  // ===== COST ESTIMATE STATE =====
  private costEstimateSubject = new BehaviorSubject<CostEstimateDTO | null>(null);
  public costEstimate$ = this.costEstimateSubject.asObservable();

  // ===== INVOICES STATE =====
  private invoicesSubject = new BehaviorSubject<InvoiceDTO[]>([]);
  public invoices$ = this.invoicesSubject.asObservable();

  private currentInvoiceSubject = new BehaviorSubject<InvoiceDTO | null>(null);
  public currentInvoice$ = this.currentInvoiceSubject.asObservable();

  // ===== PAYMENTS STATE =====
  private paymentsSubject = new BehaviorSubject<PaymentDTO[]>([]);
  public payments$ = this.paymentsSubject.asObservable();

  // ===== PAGINATION STATE =====
  private totalPagesSubject = new BehaviorSubject<number>(1);
  public totalPages$ = this.totalPagesSubject.asObservable();

  // ===== LOADING STATE =====
  private loadingSubject = new BehaviorSubject<boolean>(false);
  public loading$ = this.loadingSubject.asObservable();

  // ===== ERROR STATE =====
  private errorSubject = new BehaviorSubject<string>('');
  public error$ = this.errorSubject.asObservable();

  // ===== STRIPE PAYMENT INTENT STATE =====
  private paymentIntentSubject = new BehaviorSubject<PaymentIntentState | null>(null);
  public paymentIntent$ = this.paymentIntentSubject.asObservable();

  constructor(private api: SangrahApiService) {}

  // ===== LOAD METHODS =====

  loadCostEstimate(): Observable<CostEstimateDTO> {
    this.loadingSubject.next(true);
    return this.api.getCostEstimate().pipe(
      tap(data => {
        this.costEstimateSubject.next(data);
        this.loadingSubject.next(false);
        this.errorSubject.next('');
      }),
      catchError(err => {
        this.errorSubject.next('Failed to load cost estimate');
        this.loadingSubject.next(false);
        return throwError(() => err);
      })
    );
  }

  loadInvoices(page: number = 0, size: number = 10): Observable<PagedResponse<InvoiceDTO>> {
    this.loadingSubject.next(true);
    return this.api.getInvoices({ page: page.toString(), size: size.toString() }).pipe(
      tap(response => {
        this.invoicesSubject.next(response.content);
        this.totalPagesSubject.next(response.totalPages);
        this.loadingSubject.next(false);
        this.errorSubject.next('');
      }),
      catchError(err => {
        this.errorSubject.next('Failed to load invoices');
        this.loadingSubject.next(false);
        return throwError(() => err);
      })
    );
  }

  loadInvoiceDetail(invoiceId: string): Observable<InvoiceDTO> {
    this.loadingSubject.next(true);
    return this.api.getInvoiceDetail(invoiceId).pipe(
      tap(data => {
        this.currentInvoiceSubject.next(data);
        this.loadingSubject.next(false);
        this.errorSubject.next('');
      }),
      catchError(err => {
        this.errorSubject.next('Failed to load invoice details');
        this.loadingSubject.next(false);
        return throwError(() => err);
      })
    );
  }

  loadPaymentHistory(): Observable<PaymentDTO[]> {
    this.loadingSubject.next(true);
    return this.api.getPaymentHistory().pipe(
      tap(data => {
        this.paymentsSubject.next(data);
        this.loadingSubject.next(false);
        this.errorSubject.next('');
      }),
      catchError(err => {
        this.errorSubject.next('Failed to load payment history');
        this.loadingSubject.next(false);
        return throwError(() => err);
      })
    );
  }

  // ===== ACTION METHODS =====

  payInvoice(invoiceId: string): Observable<any> {
    // PLACEHOLDER: Returns mock session URL
    // Real Stripe integration in Phase 2
    return this.api.payInvoice(invoiceId).pipe(
      tap(() => {
        this.errorSubject.next('');
      }),
      catchError(err => {
        this.errorSubject.next(err?.error?.error || 'Payment initiation failed');
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
    this.loadingSubject.next(true);
    return this.api.createCheckoutSession(invoiceId).pipe(
      tap(response => {
        this.paymentIntentSubject.next({
          clientSecret: response.clientSecret,
          amount: response.amount,
          invoiceId: invoiceId,
          currency: response.currency
        });
        this.loadingSubject.next(false);
        this.errorSubject.next('');
      }),
      catchError(err => {
        this.errorSubject.next('Failed to create payment session');
        this.loadingSubject.next(false);
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
    return this.paymentIntentSubject.value;
  }

  /**
   * Clear payment intent state
   */
  clearPaymentIntent(): void {
    this.paymentIntentSubject.next(null);
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

  // ===== UTILITY METHODS =====

  clearError(): void {
    this.errorSubject.next('');
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
