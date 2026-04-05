import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { tap, catchError, switchMap } from 'rxjs/operators';
import { SangrahApiService, MediaItem } from '../../core/api/sangrah-api.service';

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

  // State management
  private eventMediaSubject = new BehaviorSubject<EventMediaStatus[]>([]);
  public eventMedia$ = this.eventMediaSubject.asObservable();

  private uploadProgressSubject = new BehaviorSubject<UploadProgress[]>([]);
  public uploadProgress$ = this.uploadProgressSubject.asObservable();

  private loadingSubject = new BehaviorSubject<boolean>(false);
  public loading$ = this.loadingSubject.asObservable();

  /**
   * Load all media for an event
   */
  loadEventMedia(eventId: string): Observable<EventMediaStatus[]> {
    this.loadingSubject.next(true);
    return this.api.getEventMedia(eventId).pipe(
      tap((response: any) => {
        // Handle response format: either direct array or { media: [...] }
        const mediaList = Array.isArray(response) ? response : (response?.media || []);

        const statusList: EventMediaStatus[] = mediaList.map((m: any) => {
          const uploaderId = m.uploaderUserId || m.uploaderId;
          // Only use uploaderName if it's actually provided and not just the ID
          const uploaderName = m.uploaderName && m.uploaderName !== uploaderId 
            ? m.uploaderName 
            : undefined;
          
          console.debug('📸 Media item:', {
            mediaId: m.id || m.mediaId,
            uploaderId,
            uploaderName,
            hasUploaderNameInResponse: !!m.uploaderName,
          });
          
          return {
            mediaId: m.id || m.mediaId,
            fileName: m.originalFileName || m.fileName || m.originalFileName || m.id || m.mediaId,
            mimeType: m.mimeType || m.mime_type || null,
            status: m.status || 'PENDING',
            uploadedAt: m.uploadedAt,
            uploaderId: uploaderId,
            uploaderName: uploaderName,
          };
        });
        console.debug('✅ Loaded event media:', statusList);
        this.eventMediaSubject.next(statusList);
        this.loadingSubject.next(false);
      }),
      catchError((error) => {
        console.error('Failed to load event media:', error);
        this.loadingSubject.next(false);
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
    const currentProgress = this.uploadProgressSubject.value;
    this.uploadProgressSubject.next([...currentProgress, progress]);

    return this.api.uploadEventMedia(eventId, file).pipe(
      tap((response) => {
        // Update progress to success
        const updated = this.uploadProgressSubject.value.map((p) =>
          p.fileName === file.name
            ? { ...p, progress: 100, status: 'success' as const, message: 'Upload complete' }
            : p
        );
        this.uploadProgressSubject.next(updated);

        // Auto remove after 2 seconds
        setTimeout(() => {
          this.uploadProgressSubject.next(
            this.uploadProgressSubject.value.filter((p) => p.fileName !== file.name)
          );
        }, 2000);

        // Reload media list
        this.loadEventMedia(eventId).subscribe();
      }),
      catchError((error) => {
        // Update progress to error
        const updated = this.uploadProgressSubject.value.map((p) =>
          p.fileName === file.name
            ? {
                ...p,
                progress: 0,
                status: 'error' as const,
                message: error?.error?.error || 'Upload failed',
              }
            : p
        );
        this.uploadProgressSubject.next(updated);

        return throwError(() => error);
      })
    );
  }

  /**
   * Approve media for event
   */
  approveEventMedia(eventId: string, mediaId: string): Observable<any> {
    return this.api.approveEventMedia(eventId, mediaId).pipe(
      tap(() => {
        // Update local state
        const current = this.eventMediaSubject.value;
        const updated = current.map((m) =>
          m.mediaId === mediaId ? { ...m, status: 'APPROVED' as const } : m
        );
        this.eventMediaSubject.next(updated);
      }),
      catchError((error) => {
        console.error('Failed to approve media:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Reject media for event
   */
  rejectEventMedia(eventId: string, mediaId: string, reason: string): Observable<any> {
    return this.api.rejectEventMedia(eventId, mediaId, reason).pipe(
      tap(() => {
        // Update local state
        const current = this.eventMediaSubject.value;
        const updated = current.map((m) =>
          m.mediaId === mediaId ? { ...m, status: 'REJECTED' as const, reason } : m
        );
        this.eventMediaSubject.next(updated);
      }),
      catchError((error) => {
        console.error('Failed to reject media:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Delete media from event
   */
  deleteEventMedia(eventId: string, mediaId: string): Observable<any> {
    return this.api.deleteEventMedia(eventId, mediaId).pipe(
      tap(() => {
        // Remove from local state
        const current = this.eventMediaSubject.value;
        this.eventMediaSubject.next(current.filter((m) => m.mediaId !== mediaId));
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
    return this.eventMediaSubject.value;
  }

  getUploadProgressValue(): UploadProgress[] {
    return this.uploadProgressSubject.value;
  }

  /**
   * Clear all data
   */
  clear(): void {
    this.eventMediaSubject.next([]);
    this.uploadProgressSubject.next([]);
    this.loadingSubject.next(false);
  }
}
