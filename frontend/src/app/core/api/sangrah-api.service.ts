import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, Optional } from '@angular/core';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { map, tap, catchError } from 'rxjs/operators';

export interface User {
  id: string;
  email: string;
  displayName: string;
  createdAt: string;
}

export interface EventCollaborator {
  userId: string;
  canUploadMedia?: boolean;
  canReviewMedia?: boolean;
  canReviewAccessRequests?: boolean;
  canDirectUpload?: boolean;
  canDeleteMedia?: boolean;
  canEditEventDetails?: boolean;
  addedAt?: string;
  addedByUserId?: string;
}

export interface Event {
  id: string;
  ownerUserId: string;
  title: string;
  description: string;
  eventDate: string;
  visibility: string;
  moderationEnabled: boolean;
  status: string;
  coverImageId?: string;
  createdAt: string;
  uploadPolicy?: string;
  moderationRequired?: boolean;
  requiresApproval?: boolean; // NEW: Flag indicating user needs to request access
  accessStatus?: string; // NEW: Access status (NO_ACCESS, APPROVED, PENDING)
  message?: string; // NEW: Message about access status
  collaborators?: EventCollaborator[]; // NEW: List of collaborators with permissions
}

export interface Gallery {
  id: string;
  ownerUserId: string;
  originalFileName: string;
  mimeType: string;
  sizeBytes: number;
  uploadedAt: string;
  visibility: string;
}

export interface MediaItem {
  id: string;
  originalFileName: string;
  mimeType: string;
  sizeBytes: number;
  type: string;
  checksumSha256: string;
  metadata: Record<string, unknown>;
  uploadedAt: string;
  deletedAt?: string;
}

export interface StorageUsage {
  totalBytesUsed: number;
  fileCount: number;
  breakdown: {
    imageBytes: number;
    videoBytes: number;
  };
  formatted: {
    totalFormatted: string;
    imageFormatted: string;
    videoFormatted: string;
  };
  message: string;
}

export interface TimelineGroup {
  type: string;
  groupType: string;
  label: string;
  count: number;
  items: MediaItem[];
}

export interface TimelineResponse {
  view: string;
  groups: TimelineGroup[];
  totalGroups: number;
}

export interface Notification {
  id: string;
  recipientUserId: string;
  type: string;
  payload: Record<string, unknown>;
  read: boolean;
  createdAt: string;
}

// Billing interfaces
export interface CostEstimateDTO {
  currentMonthData: {
    startDate: string;
    daysElapsed: number;
    estimatedGBDays: number;
    estimatedCost: number;
    projectedMonthlyTotal: number;
  };
  storageBreakdown: {
    images: { gbDays: number; cost: number };
    videos: { gbDays: number; cost: number };
  };
  costPerDay: number;
  ratePerGBDay: number;
}

export interface InvoiceDTO {
  id: string;
  invoiceId: string;
  userId: string;
  billingPeriod: {
    startDate: string;
    endDate: string;
  };
  storageMetrics: {
    imageGBDays: number;
    imageCost: number;
    videoGBDays: number;
    videoCost: number;
    totalGBDays: number;
  };
  charges: {
    storageRate: number;
    subtotal: number;
    taxRate: number;
    tax: number;
    totalAmount: number;
  };
  status: 'PENDING' | 'PAID' | 'OVERDUE';
  issuedDate: string;
  dueDate: string;
  paidDate: string | null;
}

export interface PaymentDTO {
  id: string;
  invoiceId: string;
  amount: number;
  status: 'SUCCESS' | 'FAILED' | 'PENDING';
  transactionDate: string;
  stripeChargeId?: string;
  failureReason?: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  isEmpty: boolean;
}

export interface AuthResponse {
  token: string;
  refreshToken?: string;
  user: User;
}

@Injectable({ providedIn: 'root' })
export class SangrahApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/v1';
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  // Auth endpoints
  login(payload: { email: string; password: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/login`, payload).pipe(
      tap(response => {
        // Only access localStorage in browser environment (SSR safe)
        if (typeof window !== 'undefined') {
          localStorage.setItem('token', response.token);
          // ⭐ NEW: Store refresh token for automatic token refresh
          if (response.refreshToken) {
            localStorage.setItem('refreshToken', response.refreshToken);
          }
        }
        this.currentUserSubject.next(response.user);
      })
    );
  }

  register(payload: { email: string; password: string; displayName: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/register`, payload).pipe(
      tap(response => {
        // Only access localStorage in browser environment (SSR safe)
        if (typeof window !== 'undefined') {
          localStorage.setItem('token', response.token);
          // ⭐ NEW: Store refresh token for automatic token refresh
          if (response.refreshToken) {
            localStorage.setItem('refreshToken', response.refreshToken);
          }
        }
        this.currentUserSubject.next(response.user);
      })
    );
  }

  logout(): void {
    // Only access localStorage in browser environment (SSR safe)
    if (typeof window !== 'undefined') {
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken'); // ⭐ NEW: Clear refresh token too
    }
    this.currentUserSubject.next(null);
  }

  // ⭐ NEW: Refresh access token using refresh token
  refreshAccessToken(): Observable<AuthResponse> {
    const refreshToken = typeof window !== 'undefined' ? localStorage.getItem('refreshToken') : null;
    if (!refreshToken) {
      console.error('❌ No refresh token available in localStorage');
      throw new Error('No refresh token available');
    }

    console.log('🔄 [refreshAccessToken] Attempting to refresh with token ending in:', refreshToken.substring(refreshToken.length - 10));

    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/token/refresh`, { refreshToken }).pipe(
      tap(response => {
        console.log('✅ [refreshAccessToken] Token refreshed successfully');
        if (typeof window !== 'undefined') {
          localStorage.setItem('token', response.token);
          if (response.refreshToken) {
            localStorage.setItem('refreshToken', response.refreshToken);
          }
        }
      }),
      catchError((err) => {
        console.error('❌ [refreshAccessToken] HTTP error:', {
          status: err.status,
          statusText: err.statusText,
          message: err.message,
          errorBody: err.error
        });
        return throwError(() => err);
      })
    );
  }

  getProfile(): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/users/profile`);
  }

  updateProfile(payload: Partial<User>): Observable<User> {
    return this.http.put<User>(`${this.baseUrl}/users/profile`, payload);
  }

  deleteAccout(): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/users/profile`);
  }

  // Gallery endpoints
  uploadGalleryMedia(file: File): Observable<MediaItem> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<MediaItem>(`${this.baseUrl}/gallery/media`, formData);
  }

  listGalleryMedia(): Observable<{ count: number; items: MediaItem[] }> {
    return this.http.get<{ count: number; items: MediaItem[] }>(`${this.baseUrl}/gallery/media`);
  }

  getGalleryMedia(mediaId: string): Observable<MediaItem> {
    return this.http.get<MediaItem>(`${this.baseUrl}/gallery/media/${mediaId}`);
  }

  deleteGalleryMedia(mediaId: string): Observable<{ message: string; mediaId: string }> {
    return this.http.delete<{ message: string; mediaId: string }>(`${this.baseUrl}/gallery/media/${mediaId}`);
  }

  getGalleryMediaFile(mediaId: string): string {
    const token = this.getTokenFromStorage();
    const url = `${this.baseUrl}/gallery/media/${mediaId}/file`;
    return token ? `${url}?token=${encodeURIComponent(token)}` : url;
  }

  getEventMediaFile(eventId: string, mediaId: string): string {
    const token = this.getTokenFromStorage();
    const url = `${this.baseUrl}/events/${eventId}/media/${mediaId}/file`;
    return token ? `${url}?token=${encodeURIComponent(token)}` : url;
  }

  /**
   * Get JWT token from localStorage (for image/video URLs)
   */
  private getTokenFromStorage(): string | null {
    if (typeof window === 'undefined') {
      return null;
    }
    return localStorage.getItem('token');
  }

  getStorageUsageGallery(): Observable<StorageUsage> {
    return this.http.get<StorageUsage>(`${this.baseUrl}/gallery/usage`);
  }

  getGalleryDailyTimeline(): Observable<TimelineResponse> {
    return this.http.get<TimelineResponse>(`${this.baseUrl}/gallery/timeline/daily`);
  }

  getGalleryMonthlyTimeline(): Observable<TimelineResponse> {
    return this.http.get<TimelineResponse>(`${this.baseUrl}/gallery/timeline/monthly`);
  }

  getGalleryYearlyTimeline(): Observable<TimelineResponse> {
    return this.http.get<TimelineResponse>(`${this.baseUrl}/gallery/timeline/yearly`);
  }

  // Backward compatibility methods
  /**
   * @deprecated Use getGalleryMonthlyTimeline() instead
   */
  getGalleryTimeline(): Observable<TimelineResponse> {
    return this.getGalleryMonthlyTimeline();
  }

  /**
   * @deprecated Use getStorageUsageGallery() instead
   */
  getStorageUsage(): Observable<StorageUsage> {
    return this.getStorageUsageGallery();
  }

  // Event endpoints
  getGlobalEvents(): Observable<Event[]> {
    return this.http.get<Event[]>(`${this.baseUrl}/events/global`);
  }

  getPublicEvents(): Observable<Event[]> {
    return this.http.get<Event[]>(`${this.baseUrl}/events/global?visibility=PUBLIC`);
  }

  getMyEvents(): Observable<Event[]> {
    return this.http.get<Event[]>(`${this.baseUrl}/events/my-events`);
  }

  getEventById(eventId: string): Observable<Event> {
    return this.http.get<Event>(`${this.baseUrl}/events/${eventId}`);
  }

  createEvent(payload: Partial<Event>): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/events`, payload).pipe(
      map(response => ({ ...response, id: response.eventId || response.id })),
      catchError(error => {
        console.error('Create event failed:', error);
        return throwError(() => error);
      })
    );
  }

  updateEvent(eventId: string, payload: Partial<Event>): Observable<Event> {
    return this.http.patch<Event>(`${this.baseUrl}/events/${eventId}`, payload);
  }

  deleteEvent(eventId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/events/${eventId}`);
  }

  getEventMedia(eventId: string): Observable<Gallery[]> {
    return this.http.get<Gallery[]>(`${this.baseUrl}/events/${eventId}/media`);
  }

  uploadEventMedia(eventId: string, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<any>(`${this.baseUrl}/events/${eventId}/media`, formData);
  }

  approveEventMedia(eventId: string, mediaId: string): Observable<any> {
    return this.http.patch<any>(`${this.baseUrl}/events/${eventId}/media/${mediaId}/approve`, {});
  }

  rejectEventMedia(eventId: string, mediaId: string, reason: string): Observable<any> {
    return this.http.patch<any>(`${this.baseUrl}/events/${eventId}/media/${mediaId}/reject`, { reason });
  }

  deleteEventMedia(eventId: string, mediaId: string): Observable<any> {
    return this.http.delete<any>(`${this.baseUrl}/events/${eventId}/media/${mediaId}`);
  }

  // Billing endpoints
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

  // ===== STRIPE PAYMENT METHODS =====

  createCheckoutSession(invoiceId: string): Observable<{ invoiceId: string; clientSecret: string; amount: number; currency: string }> {
    return this.http.post<{ invoiceId: string; clientSecret: string; amount: number; currency: string }>(
      `${this.baseUrl}/billing/payments/${invoiceId}/checkout`,
      {}
    );
  }

  getPaymentStatus(invoiceId: string): Observable<{status: string, paidDate?: string}> {
    return this.http.get<{status: string, paidDate?: string}>(
      `${this.baseUrl}/billing/payments/${invoiceId}/status`
    );
  }

  // ⭐ NEW: Sync payment status with Stripe API
  syncPaymentStatus(invoiceId: string): Observable<{status: string, paidDate?: string}> {
    console.log('🔄 [syncPaymentStatus] Syncing with Stripe for invoice:', invoiceId);
    return this.http.post<{status: string, paidDate?: string}>(
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

  // ⭐ NEW: Download invoice PDF
  downloadInvoicePDF(invoiceId: string): Observable<Blob> {
    console.log('📥 [downloadInvoicePDF] Downloading PDF for invoice:', invoiceId);
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

  // ===== EVENT ACCESS REQUEST METHODS =====

  /**
   * Get the URL for accessing event media file (image/video).
   * This includes the token as a query parameter for authenticated access.
   */
  getEventMediaFileUrl(eventId: string, mediaId: string): string {
    const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
    const baseUrl = `${this.baseUrl}/events/${eventId}/media/${mediaId}/file`;
    return token ? `${baseUrl}?token=${encodeURIComponent(token)}` : baseUrl;
  }

  getPendingAccessRequests(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/events/pending-requests`);
  }

  getEventAccessRequests(eventId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/events/${eventId}/access-requests`);
  }

  listAccessRequests(eventId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/events/${eventId}/access-requests`);
  }

  approveAccessRequest(eventId: string, requestId: string, payload?: any): Observable<any> {
    return this.http.patch(`${this.baseUrl}/events/${eventId}/access-requests/${requestId}/approve`,
      payload || {});
  }

  rejectAccessRequest(eventId: string, requestId: string, payload?: any): Observable<any> {
    return this.http.patch(`${this.baseUrl}/events/${eventId}/access-requests/${requestId}/reject`,
      payload || {});
  }

  // Revoke granted access (owner action)
  revokeAccessRequest(eventId: string, requestId: string, payload?: any): Observable<any> {
    return this.http.patch(`${this.baseUrl}/events/${eventId}/access-requests/${requestId}/revoke`,
      payload || {});
  }

  // NEW: Request access to protected event
  requestEventAccess(eventId: string, message?: string): Observable<any> {
    const payload = message ? { message } : {};
    return this.http.post(`${this.baseUrl}/events/${eventId}/access-requests`, payload);
  }

  // NEW: Get current user's access status for event
  getAccessStatus(eventId: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/events/${eventId}/access-requests/access-status`);
  }

  // NEW: User re-requests access after expiration or revocation
  reRequestAccess(eventId: string, message?: string): Observable<any> {
    const payload = message ? { message } : {};
    return this.http.patch(`${this.baseUrl}/events/${eventId}/access-requests/re-request`, payload);
  }

  // ===== EVENT COLLABORATOR METHODS =====

  getEventCollaborators(eventId: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/events/${eventId}/collaborators`);
  }

  addEventCollaborator(eventId: string, payload: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/events/${eventId}/collaborators`, payload);
  }

  removeEventCollaborator(eventId: string, userId: string): Observable<any> {
    return this.http.delete(`${this.baseUrl}/events/${eventId}/collaborators/${userId}`);
  }

  updateCollaboratorPermissions(eventId: string, userId: string, payload: any): Observable<any> {
    return this.http.patch(`${this.baseUrl}/events/${eventId}/collaborators/${userId}`, payload);
  }

  // ===== NOTIFICATION METHODS =====

  getNotifications(skip: number = 0, limit: number = 50): Observable<any> {
    let params = new HttpParams()
      .set('skip', skip.toString())
      .set('limit', limit.toString());
    return this.http.get(`${this.baseUrl}/notifications`, { params });
  }

  markNotificationRead(notificationId: string): Observable<any> {
    return this.http.patch(`${this.baseUrl}/notifications/${notificationId}/read`, {});
  }

  markAllNotificationsRead(): Observable<any> {
    return this.http.post(`${this.baseUrl}/notifications/mark-all-read`, {});
  }

  // ===== EVENT POLICY METHODS =====

  getEventPolicies(eventId: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/events/${eventId}/policies`);
  }

  updateEventPolicies(eventId: string, payload: any): Observable<any> {
    return this.http.patch(`${this.baseUrl}/events/${eventId}/policies`, payload);
  }
}
