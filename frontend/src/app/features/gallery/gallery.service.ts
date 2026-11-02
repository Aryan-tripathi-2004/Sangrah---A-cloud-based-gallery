import { Injectable, inject, signal, computed } from '@angular/core';
import { from, Observable, of, throwError, EMPTY } from 'rxjs';
import { tap, catchError, concatMap, toArray, finalize, switchMap, map } from 'rxjs/operators';
import { GalleryApiService } from '../../core/api/services/gallery-api.service';
import { MediaItem, StorageUsage, TimelineResponse } from '../../core/api/models/api.models';

export interface UploadProgress {
  fileName: string;
  progress: number;
  status: 'pending' | 'uploading' | 'success' | 'error';
  message?: string;
}

@Injectable({ providedIn: 'root' })
export class GalleryService {
  private api = inject(GalleryApiService);

  // Modern Angular Signals state
  public readonly mediaList = signal<MediaItem[]>([]);
  public readonly storageUsage = signal<StorageUsage | null>(null);
  public readonly timeline = signal<TimelineResponse | null>(null);
  public readonly uploadProgress = signal<UploadProgress[]>([]);
  public readonly loading = signal<boolean>(false);
  public readonly currentTimelineView = signal<'daily' | 'monthly' | 'yearly'>('monthly');

  // Derived computed state
  public readonly hasMedia = computed(() => this.mediaList().length > 0);
  public readonly hasUploadsInProgress = computed(() => this.uploadProgress().length > 0);


  /**
   * Load all media for the user
   */
  loadMediaList(): Observable<{ count: number; items: MediaItem[] }> {
    this.loading.set(true);
    return this.api.listGalleryMedia().pipe(
      tap(response => {
        this.mediaList.set(response.items || []);
        this.loading.set(false);
      }),
      catchError(error => {
        console.error('Failed to load media list:', error);
        this.loading.set(false);
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
        this.storageUsage.set(usage);
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
    this.loading.set(true);
    this.currentTimelineView.set(view);

    let request$: Observable<TimelineResponse>;

    switch (view) {
      case 'daily':
        request$ = this.api.getGalleryTimeline('DAILY');
        break;
      case 'yearly':
        request$ = this.api.getGalleryTimeline('YEARLY');
        break;
      default:
        request$ = this.api.getGalleryTimeline('MONTHLY');
    }

    return request$.pipe(
      tap(response => {
        this.timeline.set(response);
        this.loading.set(false);
      }),
      catchError(error => {
        console.error(`Failed to load ${view} timeline:`, error);
        this.loading.set(false);
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
    this.uploadProgress.update(current => [...current, progress]);

    return this.api.uploadGalleryMedia(file).pipe(
      switchMap((item) => {
        // Update progress to success
        this.uploadProgress.update(current =>
          current.map(p =>
            p.fileName === file.name
              ? { ...p, progress: 100, status: 'success' as const, message: 'Upload complete' }
              : p
          )
        );

        // Reload media list within the reactive stream
        return this.loadMediaList().pipe(
          map(() => item),
          catchError(() => of(item))
        );
      }),
      catchError(error => {
        // Update progress to error
        this.uploadProgress.update(current =>
          current.map(p =>
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
   * Upload multiple files sequentially using native RxJS
   */
  uploadFiles(files: File[]): Observable<MediaItem[]> {
    if (!files || files.length === 0) return of([]);
    this.loading.set(true);

    return from(files).pipe(
      concatMap(file => this.uploadFile(file).pipe(
        // Continue with the next file even if one fails
        catchError((err) => {
          console.error(`Failed to upload ${file.name}:`, err);
          return EMPTY;
        })
      )),
      toArray(), // Wait for all sequential uploads to finish, then emit as array
      finalize(() => this.loading.set(false))
    );
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
        this.mediaList.update(current => current.filter(m => m.id !== mediaId));
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
    return this.mediaList();
  }

  getStorageUsageValue(): StorageUsage | null {
    return this.storageUsage();
  }

  getTimelineValue(): TimelineResponse | null {
    return this.timeline();
  }

  getUploadProgressValue(): UploadProgress[] {
    return this.uploadProgress();
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
    this.mediaList.set([]);
    this.storageUsage.set(null);
    this.timeline.set(null);
    this.uploadProgress.set([]);
    this.loading.set(false);
  }
}
