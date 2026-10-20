import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { MediaItem, StorageUsage, TimelineResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class GalleryApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/v1';

  private getTokenFromStorage(): string | null {
    if (typeof window === 'undefined') {
      return null;
    }
    return localStorage.getItem('token');
  }

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

  getGalleryTimeline(groupBy: 'DAILY' | 'MONTHLY' | 'YEARLY' = 'DAILY'): Observable<TimelineResponse> {
    const params = new HttpParams().set('groupBy', groupBy);
    return this.http.get<TimelineResponse>(`${this.baseUrl}/gallery/timeline`, { params });
  }

  /**
   * @deprecated Use getStorageUsageGallery() instead
   */
  getStorageUsage(): Observable<StorageUsage> {
    return this.getStorageUsageGallery();
  }
}
