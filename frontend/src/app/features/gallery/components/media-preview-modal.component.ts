import { Component, inject, ChangeDetectionStrategy, HostListener, input, output, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GalleryApiService } from '../../../core/api/services/gallery-api.service';
import { MediaItem } from '../../../core/api/models/api.models';

@Component({
  selector: 'app-media-preview-modal',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './media-preview-modal.component.html',
  styles: [
    `
      :host {
        display: block;
      }
      img, video {
        -webkit-user-select: none;
        user-select: none;
      }
    `,
  ],
})
export class MediaPreviewModalComponent {
  private readonly api = inject(GalleryApiService);

  readonly isOpenInput = input<boolean>(false, { alias: 'isOpen' });
  readonly mediaInput = input<MediaItem | null>(null, { alias: 'media' });
  readonly fileUrlOverride = input<string | null | undefined>(null);
  readonly closeModal = output<void>();

  readonly fileUrlComputed = computed(() => {
    if (!this.isOpenInput()) return '';
    const override = this.fileUrlOverride();
    if (override && override.length > 0) return override;
    const m = this.mediaInput();
    return m ? this.api.getGalleryMediaFile(m.id) : '';
  });

  get isOpen(): boolean {
    return this.isOpenInput();
  }

  get media(): MediaItem | null {
    return this.mediaInput();
  }

  get fileUrl(): string {
    return this.fileUrlComputed();
  }

  @HostListener('document:keydown.escape', ['$event'])
  onEscapeKey(event: KeyboardEvent): void {
    if (this.isOpenInput()) {
      event.preventDefault();
      this.close();
    }
  }

  close(): void {
    this.closeModal.emit();
  }

  onMediaError(): void {
    console.error('Failed to load media:', this.mediaInput()?.id);
  }

  downloadMedia(): void {
    const m = this.mediaInput();
    if (!m) return;
    const link = document.createElement('a');
    link.href = this.fileUrlComputed();
    link.download = m.originalFileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  formatSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 10) / 10 + ' ' + sizes[i];
  }
}
