import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EventService } from '../event.service';
import { MediaItem } from '../../../core/api/sangrah-api.service';

@Component({
  selector: 'app-event-media-upload',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="space-y-6">
      <!-- Upload Area -->
      <div
        (click)="fileInput.click()"
        (dragover)="onDragOver($event)"
        (dragleave)="onDragLeave()"
        (drop)="onDrop($event)"
        [class.border-blue-500]="isDragging"
        [class.bg-blue-500/10]="isDragging"
        class="border-2 border-dashed border-slate-600 rounded-lg p-8 text-center cursor-pointer transition hover:border-blue-500 hover:bg-blue-500/5"
      >
        <div class="space-y-4">
          <div class="text-5xl">📸</div>
          <div>
            <h3 class="text-xl font-semibold mb-2">Upload Photo or Video</h3>
            <p class="text-slate-400 mb-4">Drag and drop your files here or click to browse</p>
            <p class="text-sm text-slate-500">Supports: Images (JPG, PNG, GIF) and Videos (MP4, WebM) up to 500MB</p>
          </div>
        </div>
      </div>

      <!-- Hidden File Input -->
      <input
        #fileInput
        type="file"
        multiple
        accept="image/jpeg,image/png,image/gif,video/mp4,video/webm"
        (change)="onFileSelected($event)"
        class="hidden"
      />

      <!-- Upload Progress -->
      <div *ngIf="uploadProgress$ | async as progressList" [hidden]="progressList.length === 0" class="space-y-3">
        <div *ngFor="let progress of progressList" class="bg-slate-800/50 rounded-lg p-4">
          <div class="flex items-center justify-between mb-2">
            <div class="flex items-center gap-3 flex-1 min-w-0">
              <div class="text-2xl flex-shrink-0">
                <span *ngIf="progress.status === 'pending' || progress.status === 'uploading'">⏳</span>
                <span *ngIf="progress.status === 'success'">✅</span>
                <span *ngIf="progress.status === 'error'">❌</span>
              </div>
              <div class="flex-1 min-w-0">
                <p class="font-medium truncate">{{ progress.fileName }}</p>
                <p class="text-xs text-slate-400">{{ progress.message }}</p>
              </div>
            </div>
            <div class="text-sm font-semibold flex-shrink-0">{{ progress.progress }}%</div>
          </div>

          <!-- Progress Bar -->
          <div class="w-full bg-slate-700 rounded-full h-2 overflow-hidden">
            <div
              class="h-full transition-all duration-300"
              [style.width.%]="progress.progress"
              [class.bg-blue-500]="progress.status === 'uploading' || progress.status === 'pending'"
              [class.bg-green-500]="progress.status === 'success'"
              [class.bg-red-500]="progress.status === 'error'"
            ></div>
          </div>
        </div>
      </div>

      <!-- Upload Stats -->
      <div *ngIf="uploadedCount > 0" class="bg-green-500/10 border border-green-500/30 rounded-lg p-4 flex items-center gap-3">
        <div class="text-2xl">🎉</div>
        <div>
          <p class="font-semibold text-green-400">
            {{ uploadedCount }} {{ uploadedCount === 1 ? 'photo' : 'photos' }} uploaded successfully!
          </p>
          <p class="text-sm text-green-300">
            <ng-container *ngIf="isModerated && !isOwner">
              {{ uploadedCount === 1 ? 'It' : 'They' }} will appear once approved by the event organizer
            </ng-container>
            <ng-container *ngIf="!isModerated || isOwner">
              {{ uploadedCount === 1 ? 'It is' : 'They are' }} now visible on the timeline
            </ng-container>
          </p>
        </div>
      </div>

      <!-- Info Banner -->
      <div *ngIf="isModerated && !isOwner" class="bg-blue-500/10 border border-blue-500/30 rounded-lg p-4 flex items-start gap-3">
        <div class="text-2xl flex-shrink-0">ℹ️</div>
        <div>
          <p class="font-semibold text-blue-400 mb-1">This is a moderated event</p>
          <p class="text-sm text-blue-300">Event organizer must approve your submissions before they appear publicly</p>
        </div>
      </div>
    </div>
  `,
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
