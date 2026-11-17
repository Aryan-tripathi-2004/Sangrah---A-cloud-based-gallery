import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Observable } from 'rxjs';
import { MediaItem, StorageUsage, TimelineResponse, GalleryMediaListResponse, PaginatedResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class GalleryApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/v1';
  private readonly platformId = inject(PLATFORM_ID);

  private getTokenFromStorage(): string | null {
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }
    return localStorage.getItem('token');
  }

  uploadGalleryMedia(file: File): Observable<MediaItem> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<MediaItem>(`${this.baseUrl}/gallery/media`, formData);
  }

  listGalleryMedia(page = 0, size = 50): Observable<GalleryMediaListResponse> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<GalleryMediaListResponse>(`${this.baseUrl}/gallery/media`, { params });
  }

  getGalleryMedia(mediaId: string): Observable<MediaItem> {
    return this.http.get<MediaItem>(`${this.baseUrl}/gallery/media/${mediaId}`);
  }

  deleteGalleryMedia(mediaId: string): Observable<{ message: string; mediaId: string }> {
    return this.http.delete<{ message: string; mediaId: string }>(`${this.baseUrl}/gallery/media/${mediaId}`);
  }

  getGalleryMediaFile(mediaId: string): string {
    const token = this.getTokenFromStorage();
    const url = `${this.baseUrl}/gallery/media/${mediaId}`;
    return token ? `${url}?token=${encodeURIComponent(token)}` : url;
  }

  getMediaFileUrl(mediaId: string): string {
    const token = this.getTokenFromStorage();
    const url = `${this.baseUrl}/media/${mediaId}`;
    return token ? `${url}?token=${encodeURIComponent(token)}` : url;
  }

  getAvatarUrl(avatarUrlOrMediaId?: string | null): string | null {
    if (!avatarUrlOrMediaId) return null;
    let url = avatarUrlOrMediaId.trim();
    if (!url) return null;

    if (url.startsWith('/api/v1/media/')) {
      const mediaId = url.substring('/api/v1/media/'.length);
      return this.getMediaFileUrl(mediaId);
    }
    if (url.startsWith('/media/')) {
      const mediaId = url.substring('/media/'.length);
      return this.getMediaFileUrl(mediaId);
    }
    if (url.startsWith('/api/v1/')) {
      url = `${this.baseUrl.replace(/\/api\/v1$/, '')}${url}`;
    } else if (!url.startsWith('http://') && !url.startsWith('https://')) {
      return this.getMediaFileUrl(url);
    }

    const token = this.getTokenFromStorage();
    if (token && !url.includes('token=')) {
      const sep = url.includes('?') ? '&' : '?';
      return `${url}${sep}token=${encodeURIComponent(token)}`;
    }
    return url;
  }

  getStorageUsageGallery(): Observable<StorageUsage> {
    return this.http.get<StorageUsage>(`${this.baseUrl}/gallery/usage`);
  }

  getGalleryTimeline(
    groupBy: 'DAILY' | 'MONTHLY' | 'YEARLY' = 'DAILY',
    page = 0,
    size = 10
  ): Observable<TimelineResponse> {
    const params = new HttpParams()
      .set('groupBy', groupBy)
      .set('page', page)
      .set('size', size);
    return this.http.get<TimelineResponse>(`${this.baseUrl}/gallery/timeline`, { params });
  }

  getTimelineGroupItems(
    groupBy: 'DAILY' | 'MONTHLY' | 'YEARLY',
    groupDate: string,
    page = 0,
    size = 50
  ): Observable<PaginatedResponse<MediaItem>> {
    const params = new HttpParams()
      .set('groupBy', groupBy)
      .set('groupDate', groupDate)
      .set('page', page)
      .set('size', size);
    return this.http.get<PaginatedResponse<MediaItem>>(`${this.baseUrl}/gallery/timeline/items`, { params });
  }

  /**
   * @deprecated Use getStorageUsageGallery() instead
   */
  getStorageUsage(): Observable<StorageUsage> {
    return this.getStorageUsageGallery();
  }
}
