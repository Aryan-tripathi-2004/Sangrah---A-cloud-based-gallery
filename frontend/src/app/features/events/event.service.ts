import { Injectable, inject, signal, computed } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { SangrahApiService } from '../../core/api/sangrah-api.service';

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

@Injectable({ providedIn: 'root' })
export class EventService {
  private api = inject(SangrahApiService);

  // Modern Angular Signals state
  public readonly eventMedia = signal<EventMediaStatus[]>([]);
  public readonly uploadProgress = signal<UploadProgress[]>([]);
  public readonly loading = signal<boolean>(false);

  // Derived computed state
  public readonly hasMedia = computed(() => this.eventMedia().length > 0);
  public readonly pendingCount = computed(() => this.eventMedia().filter(m => m.status === 'PENDING').length);
  public readonly approvedCount = computed(() => this.eventMedia().filter(m => m.status === 'APPROVED').length);
  public readonly rejectedCount = computed(() => this.eventMedia().filter(m => m.status === 'REJECTED').length);

  // Backwards-compatible RxJS observables via interop
  public readonly eventMedia$: Observable<EventMediaStatus[]> = toObservable(this.eventMedia);
  public readonly uploadProgress$: Observable<UploadProgress[]> = toObservable(this.uploadProgress);
  public readonly loading$: Observable<boolean> = toObservable(this.loading);

  /**
   * Load all media for an event
   */
  loadEventMedia(eventId: string): Observable<EventMediaStatus[]> {
    this.loading.set(true);
    return this.api.getEventMedia(eventId).pipe(
      tap((response: any) => {
        // Handle response format: either direct array or { media: [...] }
        const mediaList = Array.isArray(response) ? response : (response?.media || []);

        const statusList: EventMediaStatus[] = mediaList.map((m: any) => {
          const uploaderId = m.uploaderUserId || m.uploaderId;
          const uploaderName = m.uploaderName && m.uploaderName !== uploaderId 
            ? m.uploaderName 
            : undefined;
          
          return {
            mediaId: m.id || m.mediaId,
            fileName: m.originalFileName || m.fileName || m.id || m.mediaId,
            mimeType: m.mimeType || m.mime_type || null,
            status: m.status || 'PENDING',
            uploadedAt: m.uploadedAt,
            uploaderId: uploaderId,
            uploaderName: uploaderName,
          };
        });
        this.eventMedia.set(statusList);
        this.loading.set(false);
      }),
      catchError((error) => {
        console.error('Failed to load event media:', error);
        this.loading.set(false);
        return throwError(() => error);
      })
    );
  }

  /**
   * Upload a single file to event
   */
  uploadEventMedia(eventId: string, file: File): Observable<any> {
    const progress: UploadProgress = {
      fileName: file.name,
      progress: 0,
      status: 'pending',
    };

    // Add to progress list
    this.uploadProgress.update(current => [...current, progress]);

    return this.api.uploadEventMedia(eventId, file).pipe(
      tap(() => {
        // Update progress to success
        this.uploadProgress.update(current =>
          current.map((p) =>
            p.fileName === file.name
              ? { ...p, progress: 100, status: 'success' as const, message: 'Upload complete' }
              : p
          )
        );

        // Auto remove after 2 seconds
        setTimeout(() => {
          this.uploadProgress.update(current =>
            current.filter((p) => p.fileName !== file.name)
          );
        }, 2000);

        // Reload media list
        this.loadEventMedia(eventId).subscribe();
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
  updateEventMediaStatus(eventId: string, mediaId: string, status: 'APPROVED' | 'REJECTED', reason?: string): Observable<any> {
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
  approveEventMedia(eventId: string, mediaId: string): Observable<any> {
    return this.updateEventMediaStatus(eventId, mediaId, 'APPROVED');
  }

  /**
   * Reject media for event
   */
  rejectEventMedia(eventId: string, mediaId: string, reason: string): Observable<any> {
    return this.updateEventMediaStatus(eventId, mediaId, 'REJECTED', reason);
  }

  /**
   * Delete media from event
   */
  deleteEventMedia(eventId: string, mediaId: string): Observable<any> {
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
  createEvent(eventData: any, coverMediaFile?: File): Observable<any> {
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
   * Clear all data
   */
  clear(): void {
    this.eventMedia.set([]);
    this.uploadProgress.set([]);
    this.loading.set(false);
  }
}
