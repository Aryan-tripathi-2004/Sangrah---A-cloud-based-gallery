import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import {
  CostEstimateDTO,
  InvoiceDTO,
  PagedResponse,
  PaymentDTO,
  CheckoutSessionResponse,
  PaymentStatusResponse
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class BillingApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/v1';

  getCostEstimate(): Observable<CostEstimateDTO> {
    return this.http.get<CostEstimateDTO>(`${this.baseUrl}/billing/cost-estimate`);
  }

  getInvoices(params?: { page?: string; size?: string }): Observable<PagedResponse<InvoiceDTO>> {
    let url = `${this.baseUrl}/billing/invoices`;
    if (params && (params.page || params.size)) {
      const queryParams = [];
      if (params.page) queryParams.push(`page=${params.page}`);
      if (params.size) queryParams.push(`size=${params.size}`);
      url += '?' + queryParams.join('&');
    }
    return this.http.get<PagedResponse<InvoiceDTO>>(url);
  }

  getInvoiceDetail(invoiceId: string): Observable<InvoiceDTO> {
    return this.http.get<InvoiceDTO>(`${this.baseUrl}/billing/invoices/${invoiceId}`);
  }

  payInvoice(invoiceId: string): Observable<{ sessionUrl?: string; message?: string }> {
    return this.http.post<{ sessionUrl?: string; message?: string }>(`${this.baseUrl}/billing/invoices/${invoiceId}/pay`, {});
  }

  getPaymentHistory(): Observable<PaymentDTO[]> {
    return this.http.get<PaymentDTO[]>(`${this.baseUrl}/billing/payments`);
  }

  getBillingSummary(yearMonth: string): Observable<unknown> {
    return this.http.get(`${this.baseUrl}/billing/monthly/${yearMonth}/summary`);
  }

  createCheckoutSession(invoiceId: string): Observable<CheckoutSessionResponse> {
    return this.http.post<CheckoutSessionResponse>(
      `${this.baseUrl}/billing/payments/${invoiceId}/checkout`,
      {}
    );
  }

  getPaymentStatus(invoiceId: string): Observable<PaymentStatusResponse> {
    return this.http.get<PaymentStatusResponse>(
      `${this.baseUrl}/billing/payments/${invoiceId}/status`
    );
  }

  syncPaymentStatus(invoiceId: string): Observable<PaymentStatusResponse> {
    return this.http.post<PaymentStatusResponse>(
      `${this.baseUrl}/billing/payments/${invoiceId}/sync`,
      {}
    ).pipe(
      tap((response) => {
        console.log('✅ [syncPaymentStatus] Sync response - Status:', response.status);
      }),
      catchError((err) => {
        console.error('❌ [syncPaymentStatus] Sync failed:', err);
        return throwError(() => err);
      })
    );
  }

  downloadInvoicePDF(invoiceId: string): Observable<Blob> {
    return this.http.get(
      `${this.baseUrl}/billing/invoices/${invoiceId}/pdf`,
      { responseType: 'blob' }
    ).pipe(
      tap(() => {
        console.log('✅ [downloadInvoicePDF] PDF downloaded successfully');
      }),
      catchError((err) => {
        console.error('❌ [downloadInvoicePDF] Download failed:', err);
        return throwError(() => err);
      })
    );
  }
}
