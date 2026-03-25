import { Component, Input, Output, EventEmitter, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MediaItem, SangrahApiService } from '../../../core/api/sangrah-api.service';

@Component({
  selector: 'app-media-item',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div
      class="group relative aspect-square rounded-lg overflow-hidden bg-slate-800 cursor-pointer transition-transform hover:scale-105"
    >
      <!-- Image/Video Thumbnail -->
      <div class="w-full h-full flex items-center justify-center bg-gradient-to-br from-slate-700 to-slate-800">
        <img
          *ngIf="isImage"
          [src]="fileUrl"
          [alt]="media.originalFileName"
          class="w-full h-full object-cover cursor-pointer"
          (click)="onPreview()"
          (load)="onImageLoad()"
          (error)="onImageError()"
        />
        <video
          *ngIf="isVideo"
          [src]="fileUrl"
          class="w-full h-full object-cover cursor-pointer"
          (click)="onPreview()"
          (loadedmetadata)="onImageLoad()"
          (error)="onImageError()"
        ></video>

        <!-- Fallback Emoji if image fails to load -->
        <div *ngIf="!imageLoaded" class="text-5xl">
          <span *ngIf="isImage">🖼️</span>
          <span *ngIf="isVideo">🎬</span>
        </div>
      </div>

      <!-- Overlay -->
      <div
        class="absolute inset-0 bg-black/0 group-hover:bg-black/50 transition flex flex-col justify-between p-3"
      >
        <!-- File Info -->
        <div class="opacity-0 group-hover:opacity-100 transition self-start">
          <p class="text-xs font-semibold truncate bg-black/50 px-2 py-1 rounded">
            {{ media.type }}
          </p>
        </div>

        <!-- Actions -->
        <div class="opacity-0 group-hover:opacity-100 transition flex gap-2">
          <button
            (click)="onPreview()"
            class="flex-1 px-2 py-1 bg-blue-600 hover:bg-blue-700 rounded text-xs font-semibold transition"
            title="Preview"
          >
            👁️ Preview
          </button>
          <button
            (click)="onDelete()"
            class="px-2 py-1 bg-red-600 hover:bg-red-700 rounded text-xs font-semibold transition"
            title="Delete"
          >
            🗑️
          </button>
        </div>
      </div>

      <!-- Size Badge -->
      <div class="absolute top-2 right-2 bg-black/60 px-2 py-1 rounded text-xs font-medium text-slate-300">
        {{ formatSize(media.sizeBytes) }}
      </div>

      <!-- Date Badge -->
      <div class="absolute bottom-2 left-2 text-xs text-slate-300 bg-black/60 px-2 py-1 rounded">
        {{ media.uploadedAt | date: 'MMM d' }}
      </div>
    </div>
  `,
})
export class MediaItemComponent implements OnInit {
  private api = inject(SangrahApiService);

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
  }

  onImageLoad(): void {
    this.imageLoaded = true;
  }

  formatSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 10) / 10 + ' ' + sizes[i];
  }
}
