import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  User,
  LoginPayload,
  RegisterPayload,
  AuthResponse,
  EventCollaborator,
  AddCollaboratorPayload,
  UpdateCollaboratorPermissionsPayload,
  Event,
  UpdateEventPayload,
  EventMediaUploadResponse,
  EventAccessRequest,
  AccessRequestResponse,
  AccessStatusResponse,
  EventPolicies,
  Gallery,
  MediaItem,
  StorageUsage,
  TimelineGroup,
  TimelineResponse,
  Notification,
  NotificationListResponse,
  CostEstimateDTO,
  InvoiceDTO,
  PaymentDTO,
  CheckoutSessionResponse,
  PaymentStatusResponse,
  PagedResponse
} from './models/api.models';
import { AuthApiService } from './services/auth-api.service';
import { UserApiService } from './services/user-api.service';
import { GalleryApiService } from './services/gallery-api.service';
import { EventApiService } from './services/event-api.service';
import { BillingApiService } from './services/billing-api.service';
import { NotificationApiService } from './services/notification-api.service';

// Re-export all models for seamless backward compatibility
export * from './models/api.models';
export * from './services/auth-api.service';
export * from './services/user-api.service';
export * from './services/gallery-api.service';
export * from './services/event-api.service';
export * from './services/billing-api.service';
export * from './services/notification-api.service';

/**
 * SangrahApiService: Enterprise Facade adhering to the Single Responsibility Principle.
 * Decomposes domain logic into dedicated microservices (Auth, User, Gallery, Event, Billing, Notification)
 * while preserving backward compatibility for existing consumers across the application.
 */
@Injectable({ providedIn: 'root' })
export class SangrahApiService {
  private readonly authApi = inject(AuthApiService);
  private readonly userApi = inject(UserApiService);
  private readonly galleryApi = inject(GalleryApiService);
  private readonly eventApi = inject(EventApiService);
  private readonly billingApi = inject(BillingApiService);
  private readonly notificationApi = inject(NotificationApiService);

  public readonly currentUser$ = this.authApi.currentUser$;

  // ===== AUTH DOMAIN =====

  login(payload: LoginPayload): Observable<AuthResponse> {
    return this.authApi.login(payload);
  }

  register(payload: RegisterPayload): Observable<AuthResponse> {
    return this.authApi.register(payload);
  }

  logout(): void {
    this.authApi.logout();
  }

  refreshAccessToken(): Observable<AuthResponse> {
    return this.authApi.refreshAccessToken();
  }

  // ===== USER DOMAIN =====

  getProfile(): Observable<User> {
    return this.userApi.getProfile();
  }

  updateProfile(payload: Partial<User>): Observable<User> {
    return this.userApi.updateProfile(payload);
  }

  uploadProfileAvatar(file: File): Observable<User> {
    return this.userApi.uploadProfileAvatar(file);
  }

  deleteAccount(): Observable<void> {
    return this.userApi.deleteAccount();
  }

  // ===== GALLERY & ASSET DOMAIN =====

  uploadGalleryMedia(file: File): Observable<MediaItem> {
    return this.galleryApi.uploadGalleryMedia(file);
  }

  listGalleryMedia(): Observable<{ count: number; items: MediaItem[] }> {
    return this.galleryApi.listGalleryMedia();
  }

  getGalleryMedia(mediaId: string): Observable<MediaItem> {
    return this.galleryApi.getGalleryMedia(mediaId);
  }

  deleteGalleryMedia(mediaId: string): Observable<{ message: string; mediaId: string }> {
    return this.galleryApi.deleteGalleryMedia(mediaId);
  }

  getGalleryMediaFile(mediaId: string): string {
    return this.galleryApi.getGalleryMediaFile(mediaId);
  }

  getMediaFileUrl(mediaId: string): string {
    return this.galleryApi.getMediaFileUrl(mediaId);
  }

  getAvatarUrl(avatarUrlOrMediaId?: string | null): string | null {
    return this.galleryApi.getAvatarUrl(avatarUrlOrMediaId);
  }

  getStorageUsageGallery(): Observable<StorageUsage> {
    return this.galleryApi.getStorageUsageGallery();
  }

  getGalleryTimeline(groupBy: 'DAILY' | 'MONTHLY' | 'YEARLY' = 'DAILY'): Observable<TimelineResponse> {
    return this.galleryApi.getGalleryTimeline(groupBy);
  }

  getStorageUsage(): Observable<StorageUsage> {
    return this.galleryApi.getStorageUsage();
  }

  // ===== EVENT DOMAIN =====

  getGlobalEvents(): Observable<Event[]> {
    return this.eventApi.getGlobalEvents();
  }

  getPublicEvents(): Observable<Event[]> {
    return this.eventApi.getPublicEvents();
  }

  getMyEvents(): Observable<Event[]> {
    return this.eventApi.getMyEvents();
  }

  getEventById(eventId: string): Observable<Event> {
    return this.eventApi.getEventById(eventId);
  }

  createEvent(payloadOrFormData: FormData | Partial<Event>, coverMediaFile?: File): Observable<Event & { id: string }> {
    return this.eventApi.createEvent(payloadOrFormData, coverMediaFile);
  }

  updateEvent(eventId: string, eventData: UpdateEventPayload, coverMediaFile?: File): Observable<Event> {
    return this.eventApi.updateEvent(eventId, eventData, coverMediaFile);
  }

  deleteEvent(eventId: string): Observable<void> {
    return this.eventApi.deleteEvent(eventId);
  }

  getEventMedia(eventId: string): Observable<Gallery[]> {
    return this.eventApi.getEventMedia(eventId);
  }

  uploadEventMedia(eventId: string, file: File): Observable<EventMediaUploadResponse> {
    return this.eventApi.uploadEventMedia(eventId, file);
  }

  updateEventMediaStatus(
    eventId: string,
    mediaId: string,
    status: 'APPROVED' | 'REJECTED',
    reason?: string
  ): Observable<{ status: string; reason?: string }> {
    return this.eventApi.updateEventMediaStatus(eventId, mediaId, status, reason);
  }

  deleteEventMedia(eventId: string, mediaId: string): Observable<{ message: string }> {
    return this.eventApi.deleteEventMedia(eventId, mediaId);
  }

  getEventMediaFile(eventId: string, mediaId: string): string {
    return this.eventApi.getEventMediaFile(eventId, mediaId);
  }

  getEventMediaFileUrl(eventId: string, mediaId: string): string {
    return this.eventApi.getEventMediaFileUrl(eventId, mediaId);
  }

  getPendingAccessRequests(): Observable<Event[]> {
    return this.eventApi.getPendingAccessRequests();
  }

  getEventAccessRequests(eventId: string): Observable<EventAccessRequest[]> {
    return this.eventApi.getEventAccessRequests(eventId);
  }

  listAccessRequests(eventId: string): Observable<EventAccessRequest[]> {
    return this.eventApi.listAccessRequests(eventId);
  }

  updateAccessRequestStatus(
    eventId: string,
    requestId: string,
    status: 'APPROVED' | 'REJECTED' | 'REVOKED',
    details?: { approvalDuration?: string; accessExpiresAt?: string; reason?: string }
  ): Observable<AccessRequestResponse> {
    return this.eventApi.updateAccessRequestStatus(eventId, requestId, status, details);
  }

  requestEventAccess(eventId: string, message?: string): Observable<AccessRequestResponse> {
    return this.eventApi.requestEventAccess(eventId, message);
  }

  getAccessStatus(eventId: string): Observable<AccessStatusResponse> {
    return this.eventApi.getAccessStatus(eventId);
  }

  reRequestAccess(eventId: string, message?: string): Observable<AccessRequestResponse> {
    return this.eventApi.reRequestAccess(eventId, message);
  }

  getEventCollaborators(eventId: string): Observable<EventCollaborator[]> {
    return this.eventApi.getEventCollaborators(eventId);
  }

  addEventCollaborator(eventId: string, payload: AddCollaboratorPayload | Partial<EventCollaborator>): Observable<EventCollaborator> {
    return this.eventApi.addEventCollaborator(eventId, payload);
  }

  removeEventCollaborator(eventId: string, userId: string): Observable<{ message: string }> {
    return this.eventApi.removeEventCollaborator(eventId, userId);
  }

  updateCollaboratorPermissions(
    eventId: string,
    userId: string,
    payload: UpdateCollaboratorPermissionsPayload | Partial<EventCollaborator>
  ): Observable<EventCollaborator> {
    return this.eventApi.updateCollaboratorPermissions(eventId, userId, payload);
  }

  getEventPolicies(eventId: string): Observable<EventPolicies> {
    return this.eventApi.getEventPolicies(eventId);
  }

  updateEventPolicies(eventId: string, payload: Partial<EventPolicies>): Observable<EventPolicies> {
    return this.eventApi.updateEventPolicies(eventId, payload);
  }

  // ===== BILLING DOMAIN =====

  getCostEstimate(): Observable<CostEstimateDTO> {
    return this.billingApi.getCostEstimate();
  }

  getInvoices(params?: { page?: string; size?: string }): Observable<PagedResponse<InvoiceDTO>> {
    return this.billingApi.getInvoices(params);
  }

  getInvoiceDetail(invoiceId: string): Observable<InvoiceDTO> {
    return this.billingApi.getInvoiceDetail(invoiceId);
  }

  payInvoice(invoiceId: string): Observable<{ sessionUrl?: string; message?: string }> {
    return this.billingApi.payInvoice(invoiceId);
  }

  getPaymentHistory(): Observable<PaymentDTO[]> {
    return this.billingApi.getPaymentHistory();
  }

  getBillingSummary(yearMonth: string): Observable<unknown> {
    return this.billingApi.getBillingSummary(yearMonth);
  }

  createCheckoutSession(invoiceId: string): Observable<CheckoutSessionResponse> {
    return this.billingApi.createCheckoutSession(invoiceId);
  }

  getPaymentStatus(invoiceId: string): Observable<PaymentStatusResponse> {
    return this.billingApi.getPaymentStatus(invoiceId);
  }

  syncPaymentStatus(invoiceId: string): Observable<PaymentStatusResponse> {
    return this.billingApi.syncPaymentStatus(invoiceId);
  }

  downloadInvoicePDF(invoiceId: string): Observable<Blob> {
    return this.billingApi.downloadInvoicePDF(invoiceId);
  }

  // ===== NOTIFICATION DOMAIN =====

  getUnreadNotificationCount(): Observable<{ unreadCount: number }> {
    return this.notificationApi.getUnreadNotificationCount();
  }

  getNotifications(skip: number = 0, limit: number = 50): Observable<NotificationListResponse> {
    return this.notificationApi.getNotifications(skip, limit);
  }

  markNotificationRead(notificationId: string): Observable<{ success?: boolean; [key: string]: unknown }> {
    return this.notificationApi.markNotificationRead(notificationId);
  }

  markAllNotificationsRead(): Observable<{ success?: boolean; [key: string]: unknown }> {
    return this.notificationApi.markAllNotificationsRead();
  }
}
