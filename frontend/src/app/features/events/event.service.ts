import { Injectable, inject, signal, computed } from '@angular/core';
import { Observable, throwError, of } from 'rxjs';
import { map, tap, catchError, switchMap } from 'rxjs/operators';
import { EventApiService } from '../../core/api/services/event-api.service';
import { Event, EventMediaUploadResponse } from '../../core/api/models/api.models';

export interface EventMediaStatus {
  mediaId: string;
  fileName: string;
  mimeType?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  uploadedAt: string;
  reason?: string;
  uploaderId?: string;
  uploaderName?: string;
}

export interface UploadProgress {
  fileName: string;
  progress: number;
  status: 'pending' | 'uploading' | 'success' | 'error';
  message?: string;
}

export interface EventMediaApprovalResponse {
  status: string;
  reason?: string;
}

interface EventMediaPayloadItem {
  id?: string;
  mediaId?: string;
  originalFileName?: string;
  fileName?: string;
  mimeType?: string;
  mime_type?: string;
  status?: 'PENDING' | 'APPROVED' | 'REJECTED';
  uploadedAt: string;
  uploaderUserId?: string;
  uploaderId?: string;
  uploaderName?: string;
}

@Injectable({ providedIn: 'root' })
export class EventService {
  private api = inject(EventApiService);

  // Modern Angular Signals state
  public readonly eventMedia = signal<EventMediaStatus[]>([]);
  public readonly uploadProgress = signal<UploadProgress[]>([]);
  public readonly loading = signal<boolean>(false);
  public readonly serverPendingCount = signal<number | null>(null);
  public readonly serverApprovedCount = signal<number | null>(null);
  public readonly serverRejectedCount = signal<number | null>(null);
  public readonly totalEventMediaCount = signal<number>(0);
  public readonly hasMoreEventMedia = signal<boolean>(false);
  public readonly currentEventMediaPage = signal<number>(0);
  public readonly currentMediaView = signal<string | undefined>(undefined);

  // Derived computed state
  public readonly hasMedia = computed(() => this.eventMedia().length > 0);
  public readonly pendingCount = computed(() => {
    const s = this.serverPendingCount();
    return s !== null ? s : this.eventMedia().filter(m => m.status === 'PENDING').length;
  });
  public readonly approvedCount = computed(() => {
    const s = this.serverApprovedCount();
    return s !== null ? s : this.eventMedia().filter(m => m.status === 'APPROVED').length;
  });
  public readonly rejectedCount = computed(() => {
    const s = this.serverRejectedCount();
    return s !== null ? s : this.eventMedia().filter(m => m.status === 'REJECTED').length;
  });

  /**
   * Load all media for an event (with support for pagination and server-side counts)
   */
  loadEventMedia(eventId: string, page = 0, size = 50, append = false, view?: string): Observable<EventMediaStatus[]> {
    this.loading.set(true);
    this.currentMediaView.set(view);
    return this.api.getEventMedia(eventId, page, size, view).pipe(
      tap((rawRes: any) => {
        if (rawRes && rawRes.pendingCount !== undefined) {
          this.serverPendingCount.set(rawRes.pendingCount);
          this.serverApprovedCount.set(rawRes.approvedCount);
          this.serverRejectedCount.set(rawRes.rejectedCount);
          this.totalEventMediaCount.set(rawRes.totalElements || 0);
          this.hasMoreEventMedia.set(rawRes.hasMore ?? false);
          this.currentEventMediaPage.set(page);
        }
      }),
      map((response: unknown): EventMediaStatus[] => {
        const mediaList: EventMediaPayloadItem[] = Array.isArray(response)
          ? (response as EventMediaPayloadItem[])
          : ((response as { media?: EventMediaPayloadItem[] })?.media || []);

        return mediaList.map((m: EventMediaPayloadItem): EventMediaStatus => {
          const uploaderId = m.uploaderUserId || m.uploaderId;
          const uploaderName = m.uploaderName && m.uploaderName !== uploaderId 
            ? m.uploaderName 
            : undefined;

          return {
            mediaId: m.id || m.mediaId || '',
            fileName: m.originalFileName || m.fileName || m.id || m.mediaId || 'media',
            mimeType: m.mimeType || m.mime_type || undefined,
            status: m.status || 'PENDING',
            uploadedAt: m.uploadedAt || new Date().toISOString(),
            uploaderId: uploaderId,
            uploaderName: uploaderName,
          };
        });
      }),
      tap((statusList: EventMediaStatus[]) => {
        if (append) {
          this.eventMedia.update(prev => [...prev, ...statusList]);
        } else {
          this.eventMedia.set(statusList);
        }
        this.loading.set(false);
      }),
      catchError((error) => {
        console.error('Failed to load event media:', error);
        this.loading.set(false);
        return throwError(() => error);
      })
    );
  }

  loadMoreEventMedia(eventId: string, view?: string): Observable<EventMediaStatus[]> {
    if (!this.hasMoreEventMedia() || this.loading()) {
      return of([]);
    }
    const targetView = view !== undefined ? view : this.currentMediaView();
    return this.loadEventMedia(eventId, this.currentEventMediaPage() + 1, 50, true, targetView);
  }

  /**
   * Upload a single file to event
   */
  uploadEventMedia(eventId: string, file: File): Observable<EventMediaUploadResponse> {
    const progress: UploadProgress = {
      fileName: file.name,
      progress: 0,
      status: 'pending',
    };

    // Add to progress list
    this.uploadProgress.update(current => [...current, progress]);

    return this.api.uploadEventMedia(eventId, file).pipe(
      switchMap((response) => {
        // Update progress to success
        this.uploadProgress.update(current =>
          current.map((p) =>
            p.fileName === file.name
              ? { ...p, progress: 100, status: 'success' as const, message: 'Upload complete' }
              : p
          )
        );

        // Reload media list within reactive stream preserving current view
        return this.loadEventMedia(eventId, 0, 50, false, this.currentMediaView()).pipe(
          map(() => response),
          catchError(() => of(response))
        );
      }),
      catchError((error) => {
        // Update progress to error
        this.uploadProgress.update(current =>
          current.map((p) =>
            p.fileName === file.name
              ? {
                  ...p,
                  progress: 0,
                  status: 'error' as const,
                  message: error?.message || error?.error?.error || 'Upload failed',
                }
              : p
          )
        );

        return throwError(() => error);
      })
    );
  }

  /**
   * Update event media status (APPROVED or REJECTED)
   */
  updateEventMediaStatus(eventId: string, mediaId: string, status: 'APPROVED' | 'REJECTED', reason?: string): Observable<EventMediaApprovalResponse> {
    return this.api.updateEventMediaStatus(eventId, mediaId, status, reason).pipe(
      tap(() => {
        // Update local state
        this.eventMedia.update(current =>
          current.map((m) =>
            m.mediaId === mediaId ? { ...m, status, ...(reason ? { reason } : {}) } : m
          )
        );
      }),
      catchError((error) => {
        console.error(`Failed to update media status to ${status}:`, error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Approve media for event
   */
  approveEventMedia(eventId: string, mediaId: string): Observable<EventMediaApprovalResponse> {
    return this.updateEventMediaStatus(eventId, mediaId, 'APPROVED');
  }

  /**
   * Reject media for event
   */
  rejectEventMedia(eventId: string, mediaId: string, reason: string): Observable<EventMediaApprovalResponse> {
    return this.updateEventMediaStatus(eventId, mediaId, 'REJECTED', reason);
  }

  /**
   * Delete media from event
   */
  deleteEventMedia(eventId: string, mediaId: string): Observable<{ message: string }> {
    return this.api.deleteEventMedia(eventId, mediaId).pipe(
      tap(() => {
        // Remove from local state
        this.eventMedia.update(current => current.filter((m) => m.mediaId !== mediaId));
      }),
      catchError((error) => {
        console.error('Failed to delete media:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Get current state values
   */
  getEventMediaValue(): EventMediaStatus[] {
    return this.eventMedia();
  }

  getUploadProgressValue(): UploadProgress[] {
    return this.uploadProgress();
  }

  /**
   * Create an event with optional cover media via a single multipart request
   */
  createEvent(eventData: FormData | Partial<Event>, coverMediaFile?: File): Observable<Event & { id: string }> {
    if (eventData instanceof FormData) {
      return this.api.createEvent(eventData);
    }
    const formData = new FormData();
    formData.append(
      'eventDetails',
      new Blob([JSON.stringify(eventData)], { type: 'application/json' })
    );
    if (coverMediaFile) {
      formData.append('coverMedia', coverMediaFile);
    }
    return this.api.createEvent(formData);
  }

  /**
   * Remove a specific upload progress entry by filename
   */
  removeProgress(fileName: string): void {
    this.uploadProgress.update(current => current.filter(p => p.fileName !== fileName));
  }

  /**
   * Clear all data
   */
  clear(): void {
    this.eventMedia.set([]);
    this.uploadProgress.set([]);
    this.loading.set(false);
    this.currentMediaView.set(undefined);
  }
}
