import { Component, Input, Output, EventEmitter, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MediaItem, SangrahApiService } from '../../../core/api/sangrah-api.service';

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
  private api = inject(SangrahApiService);

  @Input() isOpen = false;
  @Input() media: MediaItem | null = null;
  @Input() fileUrlOverride?: string | null;
  @Output() closeModal = new EventEmitter<void>();

  fileUrl: string = '';

  ngOnChanges(): void {
    if (this.isOpen) {
      if (this.fileUrlOverride && this.fileUrlOverride.length > 0) {
        this.fileUrl = this.fileUrlOverride;
      } else if (this.media) {
        this.fileUrl = this.api.getGalleryMediaFile(this.media.id);
      } else {
        this.fileUrl = '';
      }
    }
  }

  close(): void {
    this.closeModal.emit();
  }

  onMediaError(): void {
    console.error('Failed to load media:', this.media?.id);
  }

  downloadMedia(): void {
    if (!this.media) return;

    const link = document.createElement('a');
    link.href = this.fileUrl;
    link.download = this.media.originalFileName;
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
