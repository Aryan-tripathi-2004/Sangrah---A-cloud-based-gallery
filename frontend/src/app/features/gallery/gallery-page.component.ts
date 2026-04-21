import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LayoutComponent } from '../../shared/layout/layout.component';
import { MediaUploadComponent } from './components/media-upload.component';
import { TimelineViewComponent } from './components/timeline-view.component';
import { MediaPreviewModalComponent } from './components/media-preview-modal.component';
import { GalleryService } from './gallery.service';
import { MediaItem, StorageUsage } from '../../core/api/sangrah-api.service';

@Component({
  selector: 'app-gallery-page',
  standalone: true,
  imports: [CommonModule, LayoutComponent, MediaUploadComponent, TimelineViewComponent, MediaPreviewModalComponent],
  templateUrl: './gallery-page.component.html',
})
export class GalleryPageComponent implements OnInit {
  private gallery = inject(GalleryService);

  activeTab: 'timeline' | 'upload' | 'stats' = 'timeline';
  storageUsage: StorageUsage | null = null;

  // Preview modal state
  isPreviewOpen = false;
  selectedMedia: MediaItem | null = null;

  ngOnInit(): void {
    this.loadStorageUsage();
  }

  private loadStorageUsage(): void {
    this.gallery.loadStorageUsage().subscribe({
      next: (usage) => {
        this.storageUsage = usage;
      },
      error: (error) => {
        console.error('Failed to load storage usage:', error);
      },
    });
  }

  onUploadComplete(items: MediaItem[]): void {
    console.log('Upload complete:', items);
    // Reload storage usage after upload
    this.loadStorageUsage();
    // Switch to timeline to show new media
    setTimeout(() => {
      this.activeTab = 'timeline';
    }, 500);
  }

  onMediaPreview(media: MediaItem): void {
    this.selectedMedia = media;
    this.isPreviewOpen = true;

    // Handle ESC key press
    const handleEscKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        this.closePreview();
      }
    };
    document.addEventListener('keydown', handleEscKey);
  }

  closePreview(): void {
    this.isPreviewOpen = false;
    this.selectedMedia = null;
  }

  onMediaDelete(mediaId: string): void {
    console.log('Delete complete:', mediaId);
    // Reload storage usage after deletion
    this.loadStorageUsage();
  }

  getAverageFileSize(): string {
    if (!this.storageUsage || this.storageUsage.fileCount === 0) {
      return '—';
    }
    const avgBytes = this.storageUsage.totalBytesUsed / this.storageUsage.fileCount;
    return this.formatBytes(Math.round(avgBytes));
  }

  private formatBytes(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 10) / 10 + ' ' + sizes[i];
  }
}
