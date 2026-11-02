import { Component, OnInit, inject, ChangeDetectionStrategy, signal, computed, DestroyRef, input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { switchMap } from 'rxjs/operators';

import { EventService, EventMediaStatus } from '../event.service';
import { EventApiService } from '../../../core/api/services/event-api.service';
import { MediaItem } from '../../../core/api/models/api.models';
import { MediaPreviewModalComponent } from '../../gallery/components/media-preview-modal.component';

@Component({
  selector: 'app-event-timeline',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MediaPreviewModalComponent],
  templateUrl: './event-timeline.component.html',
})
export class EventTimelineComponent implements OnInit {
  readonly eventId = input.required<string>();

  private readonly eventService = inject(EventService);
  private readonly api = inject(EventApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly sortBy = signal<'newest' | 'oldest'>('newest');
  readonly tokenReady = signal(true);

  // Preview modal state
  readonly previewOpen = signal(false);
  readonly previewMedia = signal<MediaItem | null>(null);
  readonly previewFileUrl = signal<string | null>(null);

  // Computed media states aligning with template bindings
  readonly approvedMedia = computed(() =>
    this.eventService.eventMedia().filter((m) => m.status === 'APPROVED')
  );

  readonly sortedMedia = computed(() => {
    return [...this.approvedMedia()].sort((a, b) => {
      const ta = new Date(a.uploadedAt).getTime();
      const tb = new Date(b.uploadedAt).getTime();
      return this.sortBy() === 'newest' ? tb - ta : ta - tb;
    });
  });

  readonly pendingCount = this.eventService.pendingCount;
  readonly rejectedCount = this.eventService.rejectedCount;

  ngOnInit(): void {
    const id = this.eventId();
    if (id && this.eventService.eventMedia().length === 0) {
      this.eventService.loadEventMedia(id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          error: (error) => console.error('Failed to load media:', error),
        });
    }
  }

  toggleSort(): void {
    this.sortBy.update((v) => (v === 'newest' ? 'oldest' : 'newest'));
  }

  isVideo(item: EventMediaStatus): boolean {
    return !!(item.mimeType?.startsWith('video/'));
  }

  getMediaUrl(mediaId: string): string {
    return this.api.getEventMediaFile(this.eventId(), mediaId);
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
      mimeType: media.mimeType || 'application/octet-stream',
      sizeBytes: 0,
      type: this.isVideo(media) ? 'VIDEO' : 'IMAGE',
      checksumSha256: '',
      metadata: {},
      uploadedAt: media.uploadedAt
    };

    this.previewMedia.set(item);
    this.previewFileUrl.set(this.getMediaUrl(media.mediaId));
    this.previewOpen.set(true);
  }

  closePreview(): void {
    this.previewOpen.set(false);
    this.previewMedia.set(null);
    this.previewFileUrl.set(null);
  }

  deleteMedia(event: MouseEvent, mediaId: string): void {
    event.stopPropagation();
    const id = this.eventId();
    this.api.deleteEventMedia(id, mediaId).pipe(
      switchMap(() => this.eventService.loadEventMedia(id)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: () => this.closePreview(),
      error: (err) => console.error('Failed to delete media:', err)
    });
  }

  onDeleteFromPreview(mediaId: string): void {
    const id = this.eventId();
    this.api.deleteEventMedia(id, mediaId).pipe(
      switchMap(() => this.eventService.loadEventMedia(id)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: () => this.closePreview(),
      error: (err) => console.error('Failed to delete media from preview:', err)
    });
  }
}
