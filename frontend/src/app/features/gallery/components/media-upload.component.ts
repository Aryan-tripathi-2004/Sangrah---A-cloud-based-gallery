import { Component, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GalleryService } from '../gallery.service';
import { MediaItem } from '../../../core/api/sangrah-api.service';

@Component({
  selector: 'app-media-upload',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './media-upload.component.html',
})
export class MediaUploadComponent {
  private gallery = inject(GalleryService);

  @Output() uploadComplete = new EventEmitter<MediaItem[]>();

  isDragging = false;
  uploadProgress$ = this.gallery.uploadProgress$;
  uploadedCount = 0;

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(): void {
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;

    const files = event.dataTransfer?.files;
    if (files) {
      this.handleFiles(Array.from(files));
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.handleFiles(Array.from(input.files));
    }
    // Reset input so selecting the same file again works
    input.value = '';
  }

  private handleFiles(files: File[]): void {
    if (files.length === 0) return;

    this.uploadedCount = 0;

    // Upload files sequentially
    let completed = 0;
    files.forEach(file => {
      this.gallery.uploadFile(file).subscribe({
        next: (item) => {
          completed++;
          this.uploadedCount++;
          if (completed === files.length) {
            this.uploadComplete.emit([item]);
          }
        },
        error: (error) => {
          console.error('Upload error:', error);
          completed++;
        },
      });
    });
  }
}
