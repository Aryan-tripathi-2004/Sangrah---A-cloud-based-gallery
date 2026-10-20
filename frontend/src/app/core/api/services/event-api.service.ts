import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import {
  Event,
  UpdateEventPayload,
  Gallery,
  EventMediaUploadResponse,
  EventCollaborator,
  AddCollaboratorPayload,
  UpdateCollaboratorPermissionsPayload,
  EventAccessRequest,
  AccessRequestResponse,
  AccessStatusResponse,
  EventPolicies
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class EventApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/v1';

  private getTokenFromStorage(): string | null {
    if (typeof window === 'undefined') {
      return null;
    }
    return localStorage.getItem('token');
  }

  getGlobalEvents(): Observable<Event[]> {
    return this.http.get<Event[]>(`${this.baseUrl}/events?scope=GLOBAL`);
  }

  getPublicEvents(): Observable<Event[]> {
    return this.http.get<Event[]>(`${this.baseUrl}/events?scope=GLOBAL&visibility=PUBLIC`);
  }

  getMyEvents(): Observable<Event[]> {
    return this.http.get<Event[]>(`${this.baseUrl}/events?scope=MY_EVENTS`);
  }

  getEventById(eventId: string): Observable<Event> {
    return this.http.get<Event>(`${this.baseUrl}/events/${eventId}`);
  }

  createEvent(payloadOrFormData: FormData | Partial<Event>, coverMediaFile?: File): Observable<Event & { id: string }> {
    let body: FormData | any;
    if (payloadOrFormData instanceof FormData) {
      body = payloadOrFormData;
    } else {
      const formData = new FormData();
      formData.append(
        'eventDetails',
        new Blob([JSON.stringify(payloadOrFormData)], { type: 'application/json' })
      );
      if (coverMediaFile) {
        formData.append('coverMedia', coverMediaFile);
      }
      body = formData;
    }

    return this.http.post<any>(`${this.baseUrl}/events`, body).pipe(
      map(response => ({ ...response, id: response.eventId || response.id })),
      catchError(error => {
        console.error('Create event failed:', error);
        return throwError(() => error);
      })
    );
  }

  updateEvent(eventId: string, eventData: UpdateEventPayload, coverMediaFile?: File): Observable<Event> {
    const formData = new FormData();
    formData.append(
      'eventDetails',
      new Blob([JSON.stringify(eventData)], { type: 'application/json' })
    );

    if (coverMediaFile) {
      formData.append('coverMedia', coverMediaFile);
    }

    return this.http.put<Event>(`${this.baseUrl}/events/${eventId}`, formData);
  }

  deleteEvent(eventId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/events/${eventId}`);
  }

  getEventMedia(eventId: string): Observable<Gallery[]> {
    return this.http.get<Gallery[]>(`${this.baseUrl}/events/${eventId}/media`);
  }

  uploadEventMedia(eventId: string, file: File): Observable<EventMediaUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<EventMediaUploadResponse>(`${this.baseUrl}/events/${eventId}/media`, formData);
  }

  updateEventMediaStatus(
    eventId: string,
    mediaId: string,
    status: 'APPROVED' | 'REJECTED',
    reason?: string
  ): Observable<{ status: string; reason?: string }> {
    const payload = status === 'REJECTED' ? { status, reason } : { status };
    return this.http.patch<{ status: string; reason?: string }>(
      `${this.baseUrl}/events/${eventId}/media/${mediaId}/status`,
      payload
    );
  }

  deleteEventMedia(eventId: string, mediaId: string): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.baseUrl}/events/${eventId}/media/${mediaId}`);
  }

  getEventMediaFile(eventId: string, mediaId: string): string {
    const token = this.getTokenFromStorage();
    const url = `${this.baseUrl}/events/${eventId}/media/${mediaId}`;
    return token ? `${url}?token=${encodeURIComponent(token)}` : url;
  }

  getEventMediaFileUrl(eventId: string, mediaId: string): string {
    const token = this.getTokenFromStorage();
    const url = `${this.baseUrl}/events/${eventId}/media/${mediaId}`;
    return token ? `${url}?token=${encodeURIComponent(token)}` : url;
  }

  getPendingAccessRequests(): Observable<Event[]> {
    return this.http.get<Event[]>(`${this.baseUrl}/events?scope=PENDING_REQUESTS`);
  }

  getEventAccessRequests(eventId: string): Observable<EventAccessRequest[]> {
    return this.http.get<EventAccessRequest[]>(`${this.baseUrl}/events/${eventId}/access-requests`);
  }

  listAccessRequests(eventId: string): Observable<EventAccessRequest[]> {
    return this.http.get<EventAccessRequest[]>(`${this.baseUrl}/events/${eventId}/access-requests`);
  }

  updateAccessRequestStatus(
    eventId: string,
    requestId: string,
    status: 'APPROVED' | 'REJECTED' | 'REVOKED',
    details?: { approvalDuration?: string; accessExpiresAt?: string; reason?: string }
  ): Observable<AccessRequestResponse> {
    const payload = { status, ...details };
    return this.http.patch<AccessRequestResponse>(
      `${this.baseUrl}/events/${eventId}/access-requests/${requestId}/status`,
      payload
    );
  }

  requestEventAccess(eventId: string, message?: string): Observable<AccessRequestResponse> {
    const payload = message ? { message } : {};
    return this.http.post<AccessRequestResponse>(`${this.baseUrl}/events/${eventId}/access-requests`, payload);
  }

  getAccessStatus(eventId: string): Observable<AccessStatusResponse> {
    return this.http.get<AccessStatusResponse>(`${this.baseUrl}/events/${eventId}/access-requests/access-status`);
  }

  reRequestAccess(eventId: string, message?: string): Observable<AccessRequestResponse> {
    const payload = message ? { message } : {};
    return this.http.patch<AccessRequestResponse>(`${this.baseUrl}/events/${eventId}/access-requests/re-request`, payload);
  }

  getEventCollaborators(eventId: string): Observable<EventCollaborator[]> {
    return this.http.get<EventCollaborator[]>(`${this.baseUrl}/events/${eventId}/collaborators`);
  }

  addEventCollaborator(eventId: string, payload: AddCollaboratorPayload | Partial<EventCollaborator>): Observable<EventCollaborator> {
    return this.http.post<EventCollaborator>(`${this.baseUrl}/events/${eventId}/collaborators`, payload);
  }

  removeEventCollaborator(eventId: string, userId: string): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.baseUrl}/events/${eventId}/collaborators/${userId}`);
  }

  updateCollaboratorPermissions(
    eventId: string,
    userId: string,
    payload: UpdateCollaboratorPermissionsPayload | Partial<EventCollaborator>
  ): Observable<EventCollaborator> {
    return this.http.patch<EventCollaborator>(`${this.baseUrl}/events/${eventId}/collaborators/${userId}`, payload);
  }

  getEventPolicies(eventId: string): Observable<EventPolicies> {
    return this.http.get<EventPolicies>(`${this.baseUrl}/events/${eventId}/policies`);
  }

  updateEventPolicies(eventId: string, payload: Partial<EventPolicies>): Observable<EventPolicies> {
    return this.http.patch<EventPolicies>(`${this.baseUrl}/events/${eventId}/policies`, payload);
  }
}
