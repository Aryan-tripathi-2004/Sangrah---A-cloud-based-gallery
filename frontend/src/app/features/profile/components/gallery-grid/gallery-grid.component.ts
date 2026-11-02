import { Component, ChangeDetectionStrategy, input, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MediaItem } from '../../../../core/api/models/api.models';
import { GalleryApiService } from '../../../../core/api/services/gallery-api.service';

@Component({
  selector: 'app-gallery-grid',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink],
  templateUrl: './gallery-grid.component.html',
})
export class GalleryGridComponent {
  private readonly galleryApi = inject(GalleryApiService);

  items = input<MediaItem[]>([]);
  selectedMedia = signal<MediaItem | null>(null);

  getMediaUrl(item: MediaItem): string {
    return this.galleryApi.getGalleryMediaFile(item.id);
  }

  isVideo(item: MediaItem): boolean {
    return !!(item.mimeType && item.mimeType.startsWith('video/'));
  }

  openPreview(item: MediaItem): void {
    this.selectedMedia.set(item);
  }

  closePreview(): void {
    this.selectedMedia.set(null);
  }
}
