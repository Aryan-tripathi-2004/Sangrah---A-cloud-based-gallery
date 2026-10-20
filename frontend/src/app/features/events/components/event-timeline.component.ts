import { Component, Input, OnInit, inject, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';

import { EventService, EventMediaStatus } from '../event.service';
import { SangrahApiService, MediaItem } from '../../../core/api/sangrah-api.service';
import { MediaPreviewModalComponent } from '../../gallery/components/media-preview-modal.component';

@Component({
  selector: 'app-event-timeline',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MediaPreviewModalComponent],
  templateUrl: './event-timeline.component.html',
})
export class EventTimelineComponent implements OnInit {
  @Input() eventId!: string;

  private eventService = inject(EventService);
  private api = inject(SangrahApiService);
  private cdr = inject(ChangeDetectorRef);

  sortBy: 'newest' | 'oldest' = 'newest';
  tokenReady = false;

  // Preview modal state
  previewOpen = false;
  previewMedia: MediaItem | null = null;
  previewFileUrl: string | null = null;

  ngOnInit(): void {
    this.tokenReady = true;
    this.cdr.markForCheck();
  }

  isVideo(media: EventMediaStatus): boolean {
    const mt = (media as any).mimeType || '';
    if (typeof mt === 'string' && mt.startsWith('video/')) return true;
    const name = (media as any).fileName || '';
    return /\.(mp4|webm|mov|ogg)$/i.test(name);
  }

  getMediaUrl(mediaId: string): string {
    return this.api.getEventMediaFile(this.eventId, mediaId);
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    console.warn('Failed to load image:', img.src);
  }

  onMediaError(event: Event): void {
    const el = event.target as HTMLVideoElement;
    console.warn('Failed to load media:', el?.currentSrc || el?.src);
  }

  onTileClick(event: MouseEvent, media: EventMediaStatus): void {
    const target = event.target as HTMLElement | null;
    if (!target) return;

    if (target.closest('video') || target.closest('button') || target.closest('a') || (target.tagName === 'BUTTON')) {
      return;
    }

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
    this.cdr.markForCheck();
  }

  closePreview(): void {
    this.previewOpen = false;
    this.previewMedia = null;
    this.previewFileUrl = null;
    this.cdr.markForCheck();
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
        this.eventService.loadEventMedia(this.eventId).subscribe({
          next: () => {
            this.cdr.markForCheck();
          },
          error: (error) => {
            console.error('Failed to reload media:', error);
            this.cdr.markForCheck();
          },
        });
      },
      error: (error) => {
        console.error('Delete failed:', error);
        alert('Failed to delete media: ' + (error.error?.error || error.message));
        this.cdr.markForCheck();
      },
    });
  }
}
