import { Injectable, inject, signal, computed } from '@angular/core';
import { from, Observable, of, throwError, EMPTY } from 'rxjs';
import { tap, catchError, concatMap, toArray, finalize, switchMap, map } from 'rxjs/operators';
import { GalleryApiService } from '../../core/api/services/gallery-api.service';
import { MediaItem, StorageUsage, TimelineResponse, TimelineGroup, GalleryMediaListResponse, PaginatedResponse } from '../../core/api/models/api.models';

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
  public readonly timelineGroups = signal<TimelineGroup[]>([]);
  public readonly currentTimelinePage = signal<number>(0);
  public readonly hasMoreTimeline = signal<boolean>(true);
  public readonly isLoadingMoreTimeline = signal<boolean>(false);
  public readonly uploadProgress = signal<UploadProgress[]>([]);
  public readonly loading = signal<boolean>(false);
  public readonly currentTimelineView = signal<'daily' | 'monthly' | 'yearly'>('monthly');

  // Derived computed state
  public readonly hasMedia = computed(() => this.mediaList().length > 0);
  public readonly hasUploadsInProgress = computed(() => this.uploadProgress().length > 0);


  /**
   * Load all media for the user (paginated)
   */
  loadMediaList(page = 0, size = 50): Observable<GalleryMediaListResponse> {
    this.loading.set(true);
    return this.api.listGalleryMedia(page, size).pipe(
      tap(response => {
        if (page === 0) {
          this.mediaList.set(response.items || []);
        } else {
          this.mediaList.update(prev => [...prev, ...(response.items || [])]);
        }
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
   * Load timeline with specified view and pagination
   */
  loadTimeline(view: 'daily' | 'monthly' | 'yearly' = 'monthly', page = 0, size = 10): Observable<TimelineResponse> {
    if (page === 0) {
      this.loading.set(true);
    } else {
      this.isLoadingMoreTimeline.set(true);
    }
    this.currentTimelineView.set(view);

    let request$: Observable<TimelineResponse>;

    switch (view) {
      case 'daily':
        request$ = this.api.getGalleryTimeline('DAILY', page, size);
        break;
      case 'yearly':
        request$ = this.api.getGalleryTimeline('YEARLY', page, size);
        break;
      default:
        request$ = this.api.getGalleryTimeline('MONTHLY', page, size);
    }

    return request$.pipe(
      tap(response => {
        if (page === 0) {
          this.timelineGroups.set(response.groups || []);
        } else {
          this.timelineGroups.update(prev => [...prev, ...(response.groups || [])]);
        }
        this.currentTimelinePage.set(page);
        this.hasMoreTimeline.set(response.hasMore ?? false);
        this.timeline.set(response);
        this.loading.set(false);
        this.isLoadingMoreTimeline.set(false);
      }),
      catchError(error => {
        console.error(`Failed to load ${view} timeline:`, error);
        this.loading.set(false);
        this.isLoadingMoreTimeline.set(false);
        return throwError(() => error);
      })
    );
  }

  /**
   * Load next page of timeline groups (infinite scroll)
   */
  loadMoreTimeline(): Observable<TimelineResponse> {
    if (!this.hasMoreTimeline() || this.isLoadingMoreTimeline() || this.loading()) {
      return EMPTY;
    }
    return this.loadTimeline(this.currentTimelineView(), this.currentTimelinePage() + 1);
  }

  /**
   * Load more items for a specific timeline group (e.g. Month or Year with many photos)
   */
  loadMoreGroupItems(
    groupBy: 'DAILY' | 'MONTHLY' | 'YEARLY',
    groupDate: string,
    page: number,
    size = 20
  ): Observable<PaginatedResponse<MediaItem>> {
    return this.api.getTimelineGroupItems(groupBy, groupDate, page, size);
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
        this.timelineGroups.update(groups =>
          groups
            .map(group => {
              const hasItem = group.items.some(m => m.id === mediaId);
              if (!hasItem) return group;
              const updatedItems = group.items.filter(m => m.id !== mediaId);
              const updatedCount = Math.max(0, (group.count ?? group.items.length) - 1);
              const updatedTotal = group.totalElements !== undefined ? Math.max(0, group.totalElements - 1) : updatedCount;
              return {
                ...group,
                items: updatedItems,
                count: updatedCount,
                totalElements: updatedTotal
              };
            })
            .filter(group => group.items.length > 0)
        );
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
    this.timelineGroups.set([]);
    this.currentTimelinePage.set(0);
    this.hasMoreTimeline.set(true);
    this.isLoadingMoreTimeline.set(false);
    this.uploadProgress.set([]);
    this.loading.set(false);
  }
}
