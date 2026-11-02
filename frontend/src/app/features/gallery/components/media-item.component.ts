import { Component, inject, ChangeDetectionStrategy, signal, input, output, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GalleryApiService } from '../../../core/api/services/gallery-api.service';
import { MediaItem } from '../../../core/api/models/api.models';

@Component({
  selector: 'app-media-item',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  templateUrl: './media-item.component.html',
})
export class MediaItemComponent {
  private readonly api = inject(GalleryApiService);

  readonly mediaInput = input.required<MediaItem>({ alias: 'media' });
  readonly preview = output<MediaItem>();
  readonly delete = output<string>();

  readonly imageLoaded = signal<boolean>(false);
  readonly fileUrl = computed(() => this.api.getGalleryMediaFile(this.mediaInput().id));
  readonly isImage = computed(() => this.mediaInput().type === 'IMAGE');
  readonly isVideo = computed(() => this.mediaInput().type === 'VIDEO');

  get media(): MediaItem {
    return this.mediaInput();
  }

  onPreview(): void {
    this.preview.emit(this.mediaInput());
  }

  onDelete(): void {
    const item = this.mediaInput();
    if (confirm(`Delete "${item.originalFileName}"?`)) {
      this.delete.emit(item.id);
    }
  }

  onImageError(): void {
    this.imageLoaded.set(false);
  }

  onImageLoad(): void {
    this.imageLoaded.set(true);
  }

  formatSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 10) / 10 + ' ' + sizes[i];
  }
}
