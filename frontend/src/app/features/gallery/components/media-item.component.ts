import { Component, Input, Output, EventEmitter, inject, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MediaItem, SangrahApiService } from '../../../core/api/sangrah-api.service';

@Component({
  selector: 'app-media-item',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  templateUrl: './media-item.component.html',
})
export class MediaItemComponent implements OnInit {
  private api = inject(SangrahApiService);
  private cdr = inject(ChangeDetectorRef);

  @Input() media!: MediaItem;
  @Output() preview = new EventEmitter<MediaItem>();
  @Output() delete = new EventEmitter<string>();

  fileUrl: string = '';
  imageLoaded: boolean = false;

  ngOnInit(): void {
    this.fileUrl = this.api.getGalleryMediaFile(this.media.id);
  }

  get isImage(): boolean {
    return this.media.type === 'IMAGE';
  }

  get isVideo(): boolean {
    return this.media.type === 'VIDEO';
  }

  onPreview(): void {
    this.preview.emit(this.media);
  }

  onDelete(): void {
    if (confirm(`Delete "${this.media.originalFileName}"?`)) {
      this.delete.emit(this.media.id);
    }
  }

  onImageError(): void {
    console.error('Failed to load media:', this.media.id);
    this.imageLoaded = false;
    this.cdr.markForCheck();
  }

  onImageLoad(): void {
    this.imageLoaded = true;
    this.cdr.markForCheck();
  }

  formatSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 10) / 10 + ' ' + sizes[i];
  }
}
