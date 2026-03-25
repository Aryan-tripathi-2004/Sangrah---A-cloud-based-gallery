import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { tap, catchError, switchMap } from 'rxjs/operators';
import { SangrahApiService, MediaItem, StorageUsage, TimelineResponse } from '../../core/api/sangrah-api.service';

export interface UploadProgress {
  fileName: string;
  progress: number;
  status: 'pending' | 'uploading' | 'success' | 'error';
  message?: string;
}

@Injectable({ providedIn: 'root' })
export class GalleryService {
  private api = inject(SangrahApiService);

  // State management
  private mediaListSubject = new BehaviorSubject<MediaItem[]>([]);
  public mediaList$ = this.mediaListSubject.asObservable();

  private storageUsageSubject = new BehaviorSubject<StorageUsage | null>(null);
  public storageUsage$ = this.storageUsageSubject.asObservable();

  private timelineSubject = new BehaviorSubject<TimelineResponse | null>(null);
  public timeline$ = this.timelineSubject.asObservable();

  private uploadProgressSubject = new BehaviorSubject<UploadProgress[]>([]);
  public uploadProgress$ = this.uploadProgressSubject.asObservable();

  private loadingSubject = new BehaviorSubject<boolean>(false);
  public loading$ = this.loadingSubject.asObservable();

  private currentTimelineViewSubject = new BehaviorSubject<'daily' | 'monthly' | 'yearly'>('monthly');
  public currentTimelineView$ = this.currentTimelineViewSubject.asObservable();

  /**
   * Load all media for the user
   */
  loadMediaList(): Observable<{ count: number; items: MediaItem[] }> {
    this.loadingSubject.next(true);
    return this.api.listGalleryMedia().pipe(
      tap(response => {
        this.mediaListSubject.next(response.items);
        this.loadingSubject.next(false);
      }),
      catchError(error => {
        console.error('Failed to load media list:', error);
        this.loadingSubject.next(false);
        return throwError(() => error);
      })
    );
  }

  /**
   * Load storage usage stats
   */
  loadStorageUsage(): Observable<StorageUsage> {
    return this.api.getStorageUsageGallery().pipe(
      tap(usage => {
        this.storageUsageSubject.next(usage);
      }),
      catchError(error => {
        console.error('Failed to load storage usage:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Load timeline with specified view
   */
  loadTimeline(view: 'daily' | 'monthly' | 'yearly' = 'monthly'): Observable<TimelineResponse> {
    this.loadingSubject.next(true);
    this.currentTimelineViewSubject.next(view);

    let request$: Observable<TimelineResponse>;

    switch (view) {
      case 'daily':
        request$ = this.api.getGalleryDailyTimeline();
        break;
      case 'yearly':
        request$ = this.api.getGalleryYearlyTimeline();
        break;
      default:
        request$ = this.api.getGalleryMonthlyTimeline();
    }

    return request$.pipe(
      tap(response => {
        this.timelineSubject.next(response);
        this.loadingSubject.next(false);
      }),
      catchError(error => {
        console.error(`Failed to load ${view} timeline:`, error);
        this.loadingSubject.next(false);
        return throwError(() => error);
      })
    );
  }

  /**
   * Upload a single file with progress tracking
   */
  uploadFile(file: File): Observable<MediaItem> {
    const progress: UploadProgress = {
      fileName: file.name,
      progress: 0,
      status: 'pending',
    };

    // Add to progress list
    const currentProgress = this.uploadProgressSubject.value;
    this.uploadProgressSubject.next([...currentProgress, progress]);

    return this.api.uploadGalleryMedia(file).pipe(
      tap(() => {
        // Update progress to success
        const updated = this.uploadProgressSubject.value.map(p =>
          p.fileName === file.name
            ? { ...p, progress: 100, status: 'success' as const, message: 'Upload complete' }
            : p
        );
        this.uploadProgressSubject.next(updated);

        // AutoRemove after 2 seconds
        setTimeout(() => {
          this.uploadProgressSubject.next(
            this.uploadProgressSubject.value.filter(p => p.fileName !== file.name)
          );
        }, 2000);

        // Reload media list
        this.loadMediaList().subscribe();
      }),
      catchError(error => {
        // Update progress to error
        const updated = this.uploadProgressSubject.value.map(p =>
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
   * Upload multiple files sequentially
   */
  uploadFiles(files: File[]): Observable<MediaItem[]> {
    this.loadingSubject.next(true);
    const uploadedItems: MediaItem[] = [];

    const uploadNext = (index: number): Observable<MediaItem[]> => {
      if (index >= files.length) {
        this.loadingSubject.next(false);
        return new Observable(observer => {
          observer.next(uploadedItems);
          observer.complete();
        });
      }

      return this.uploadFile(files[index]).pipe(
        tap(item => {
          uploadedItems.push(item);
        }),
        catchError(() => {
          // Continue with next file even if one fails
          return new Observable<MediaItem>(observer => {
            observer.complete();
          });
        }),
        switchMap(() => uploadNext(index + 1))
      );
    };

    return uploadNext(0);
  }

  /**
   * Get a single media item
   */
  getMedia(mediaId: string): Observable<MediaItem> {
    return this.api.getGalleryMedia(mediaId);
  }

  /**
   * Delete a media item (soft delete)
   */
  deleteMedia(mediaId: string): Observable<{ message: string; mediaId: string }> {
    return this.api.deleteGalleryMedia(mediaId).pipe(
      tap(() => {
        // Remove from local state
        const current = this.mediaListSubject.value;
        this.mediaListSubject.next(current.filter(m => m.id !== mediaId));
      }),
      catchError(error => {
        console.error('Failed to delete media:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Get current state values
   */
  getMediaListValue(): MediaItem[] {
    return this.mediaListSubject.value;
  }

  getStorageUsageValue(): StorageUsage | null {
    return this.storageUsageSubject.value;
  }

  getTimelineValue(): TimelineResponse | null {
    return this.timelineSubject.value;
  }

  getUploadProgressValue(): UploadProgress[] {
    return this.uploadProgressSubject.value;
  }

  /**
   * Clear all data
   */
  clear(): void {
    this.mediaListSubject.next([]);
    this.storageUsageSubject.next(null);
    this.timelineSubject.next(null);
    this.uploadProgressSubject.next([]);
    this.loadingSubject.next(false);
  }
}
