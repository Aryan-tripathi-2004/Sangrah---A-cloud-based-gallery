import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EventService } from '../event.service';
import { MediaItem } from '../../../core/api/sangrah-api.service';

@Component({
  selector: 'app-event-media-upload',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './event-media-upload.component.html',
})
export class EventMediaUploadComponent {
  @Input() eventId!: string;
  @Input() isOwner: boolean = false;
  @Input() isModerated: boolean = false;
  @Output() uploadComplete = new EventEmitter<any[]>();

  private eventService = inject(EventService);

  isDragging = false;
  uploadProgress$ = this.eventService.uploadProgress$;
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
    files.forEach((file) => {
      this.eventService.uploadEventMedia(this.eventId, file).subscribe({
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
