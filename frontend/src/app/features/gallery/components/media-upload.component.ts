import { Component, output, inject, ChangeDetectionStrategy, signal, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { from, of } from 'rxjs';
import { concatMap, tap, catchError } from 'rxjs/operators';
import { GalleryService } from '../gallery.service';
import { MediaItem } from '../../../core/api/models/api.models';

@Component({
  selector: 'app-media-upload',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  templateUrl: './media-upload.component.html',
})
export class MediaUploadComponent {
  private readonly gallery = inject(GalleryService);
  private readonly destroyRef = inject(DestroyRef);

  readonly uploadComplete = output<MediaItem[]>();

  readonly isDragging = signal<boolean>(false);
  readonly uploadProgress = this.gallery.uploadProgress;
  readonly uploadedCount = signal<number>(0);

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(true);
  }

  onDragLeave(): void {
    this.isDragging.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(false);

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
    input.value = '';
  }

  private handleFiles(files: File[]): void {
    if (files.length === 0) return;

    this.uploadedCount.set(0);
    const uploadedItems: MediaItem[] = [];

    from(files).pipe(
      concatMap(file =>
        this.gallery.uploadFile(file).pipe(
          tap((item) => {
            if (item) {
              uploadedItems.push(item);
              this.uploadedCount.update(c => c + 1);
              setTimeout(() => {
                this.gallery.removeProgress(file.name);
              }, 2000);
            }
          }),
          catchError(error => {
            console.error('Upload error for file:', file.name, error);
            return of(null);
          })
        )
      ),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      complete: () => {
        if (uploadedItems.length > 0) {
          this.uploadComplete.emit(uploadedItems);
        }
      }
    });
  }
}
