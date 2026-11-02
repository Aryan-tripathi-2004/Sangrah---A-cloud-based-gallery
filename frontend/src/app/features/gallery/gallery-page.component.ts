import { Component, OnInit, inject, ChangeDetectionStrategy, signal, computed, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { LayoutComponent } from '../../shared/layout/layout.component';
import { MediaUploadComponent } from './components/media-upload.component';
import { TimelineViewComponent } from './components/timeline-view.component';
import { MediaPreviewModalComponent } from './components/media-preview-modal.component';
import { GalleryService } from './gallery.service';
import { MediaItem, StorageUsage } from '../../core/api/models/api.models';

@Component({
  selector: 'app-gallery-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [LayoutComponent, MediaUploadComponent, TimelineViewComponent, MediaPreviewModalComponent],
  templateUrl: './gallery-page.component.html',
})
export class GalleryPageComponent implements OnInit {
  public readonly gallery = inject(GalleryService);
  private readonly destroyRef = inject(DestroyRef);

  // Modern Angular Signals for state
  activeTab = signal<'timeline' | 'upload' | 'stats'>('timeline');
  readonly storageUsage = this.gallery.storageUsage;
  isPreviewOpen = signal<boolean>(false);
  selectedMedia = signal<MediaItem | null>(null);

  // Computed helper
  averageFileSize = computed(() => {
    const usage = this.storageUsage();
    if (!usage || usage.fileCount === 0) return '—';
    const avgBytes = usage.totalBytesUsed / usage.fileCount;
    return this.formatBytes(Math.round(avgBytes));
  });

  ngOnInit(): void {
    this.loadStorageUsage();
  }

  loadStorageUsage(): void {
    this.gallery.loadStorageUsage().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      error: (error) => {
        console.error('Failed to load storage usage:', error);
      },
    });
  }

  onUploadComplete(items: MediaItem[]): void {
    console.log('Upload complete:', items);
    this.loadStorageUsage();
    setTimeout(() => {
      this.activeTab.set('timeline');
    }, 500);
  }

  onMediaPreview(media: MediaItem): void {
    this.selectedMedia.set(media);
    this.isPreviewOpen.set(true);
  }

  closePreview(): void {
    this.isPreviewOpen.set(false);
    this.selectedMedia.set(null);
  }

  onMediaDelete(mediaId: string): void {
    console.log('Delete complete:', mediaId);
    this.loadStorageUsage();
  }

  private formatBytes(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 10) / 10 + ' ' + sizes[i];
  }
}
