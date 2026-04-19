import { Component, Input, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EventService, EventMediaStatus } from '../event.service';
import { SangrahApiService, MediaItem } from '../../../core/api/sangrah-api.service';
import { MediaPreviewModalComponent } from '../../gallery/components/media-preview-modal.component';

@Component({
  selector: 'app-event-timeline',
  standalone: true,
  imports: [CommonModule, MediaPreviewModalComponent],
  templateUrl: './event-timeline.component.html',
})
export class EventTimelineComponent implements OnInit {
  @Input() eventId!: string;

  private eventService = inject(EventService);
  private api = inject(SangrahApiService);

  sortBy: 'newest' | 'oldest' = 'newest';
  tokenReady = false;

  // Preview modal state
  previewOpen = false;
  previewMedia: MediaItem | null = null;
  previewFileUrl: string | null = null;

  ngOnInit(): void {
    this.ensureValidMediaToken();
  }

  isVideo(media: EventMediaStatus): boolean {
    const mt = (media as any).mimeType || '';
    if (typeof mt === 'string' && mt.startsWith('video/')) return true;
    const name = (media as any).fileName || '';
    return /\.(mp4|webm|mov|ogg)$/i.test(name);
  }

  private ensureValidMediaToken(): void {
    if (typeof window === 'undefined') {
      this.tokenReady = true;
      return;
    }

    const token = localStorage.getItem('token');
    if (!token) {
      this.tokenReady = true;
      return;
    }

    const expiresAt = this.getTokenExpiryMillis(token);
    const now = Date.now();

    // Refresh if expired or close to expiry so image/video URL requests do not 401.
    if (expiresAt !== null && expiresAt - now < 120000) {
      // Do not block rendering — show media immediately using current token, and refresh in background.
      this.tokenReady = true;
      this.api.refreshAccessToken().subscribe({
        next: () => {
          // token refreshed; let Angular re-evaluate bindings so URLs update from localStorage
        },
        error: () => {
          // ignore refresh errors — keep showing current media
        },
      });
      return;
    }

    this.tokenReady = true;
  }

  private getTokenExpiryMillis(token: string): number | null {
    try {
      const payload = token.split('.')[1];
      if (!payload) return null;

      const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
      const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
      const decoded = JSON.parse(atob(padded));

      if (typeof decoded.exp !== 'number') {
        return null;
      }

      return decoded.exp * 1000;
    } catch {
      return null;
    }
  }

  getMediaUrl(mediaId: string): string {
    return this.api.getEventMediaFile(this.eventId, mediaId);
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    console.warn('Failed to load image:', img.src);
    // Image will show placeholder on error due to CSS
  }

  onMediaError(event: Event): void {
    const el = event.target as HTMLVideoElement;
    console.warn('Failed to load media:', el?.currentSrc || el?.src);
    // Let the UI show the placeholder block
  }

  onTileClick(event: MouseEvent, media: EventMediaStatus): void {
    // If user clicked on a native control (video controls, button, etc.) do not open modal
    const target = event.target as HTMLElement | null;
    if (!target) return;

    if (target.closest('video') || target.closest('button') || target.closest('a') || (target.tagName === 'BUTTON')) {
      return;
    }

    // Prepare a MediaItem-like object for the preview modal
    const item: MediaItem = {
      id: media.mediaId,
      originalFileName: media.fileName || media.mediaId,
      mimeType: (media as any).mimeType as string | undefined,
      sizeBytes: (media as any).sizeBytes || 0,
      type: this.isVideo(media) ? 'VIDEO' : 'IMAGE',
      checksumSha256: '',
      metadata: {},
      uploadedAt: media.uploadedAt,
    } as MediaItem;

    this.previewMedia = item;
    this.previewFileUrl = this.getMediaUrl(media.mediaId);
    this.previewOpen = true;
  }

  closePreview(): void {
    this.previewOpen = false;
    this.previewMedia = null;
    this.previewFileUrl = null;
  }

  getApprovedMedia(): EventMediaStatus[] {
    return this.eventService
      .getEventMediaValue()
      .filter((m) => m.status === 'APPROVED');
  }

  getPendingCount(): number {
    return this.eventService
      .getEventMediaValue()
      .filter((m) => m.status === 'PENDING').length;
  }

  getRejectedCount(): number {
    return this.eventService
      .getEventMediaValue()
      .filter((m) => m.status === 'REJECTED').length;
  }

  getSortedMedia(): EventMediaStatus[] {
    const approved = this.getApprovedMedia();
    return approved.sort((a, b) => {
      const dateA = new Date(a.uploadedAt).getTime();
      const dateB = new Date(b.uploadedAt).getTime();
      return this.sortBy === 'newest' ? dateB - dateA : dateA - dateB;
    });
  }

  deleteMedia(event: Event, mediaId: string): void {
    event.stopPropagation();
    
    if (!confirm('Are you sure you want to delete this media? This action cannot be undone.')) {
      return;
    }

    this.api.deleteEventMedia(this.eventId, mediaId).subscribe({
      next: () => {
        // Reload media after deletion
        this.eventService.loadEventMedia(this.eventId).subscribe({
          next: () => {
            // Refresh UI
          },
          error: (error) => {
            console.error('Failed to reload media:', error);
          },
        });
      },
      error: (error) => {
        console.error('Delete failed:', error);
        alert('Failed to delete media: ' + (error.error?.error || error.message));
      },
    });
  }
}
