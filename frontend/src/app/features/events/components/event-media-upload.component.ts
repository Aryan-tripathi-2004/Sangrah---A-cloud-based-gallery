import { Component, input, output, inject, ChangeDetectionStrategy, signal, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { from, of } from 'rxjs';
import { concatMap, tap, catchError } from 'rxjs/operators';
import { EventService } from '../event.service';
import { EventMediaUploadResponse } from '../../../core/api/models/api.models';

@Component({
  selector: 'app-event-media-upload',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  templateUrl: './event-media-upload.component.html',
})
export class EventMediaUploadComponent {
  readonly eventIdInput = input.required<string>({ alias: 'eventId' });
  readonly isOwnerInput = input<boolean>(false, { alias: 'isOwner' });
  readonly isModeratedInput = input<boolean>(false, { alias: 'isModerated' });
  readonly uploadComplete = output<EventMediaUploadResponse[]>();

  get eventId(): string {
    return this.eventIdInput();
  }

  get isOwner(): boolean {
    return this.isOwnerInput();
  }

  get isModerated(): boolean {
    return this.isModeratedInput();
  }

  private readonly eventService = inject(EventService);
  private readonly destroyRef = inject(DestroyRef);

  readonly isDragging = signal<boolean>(false);
  readonly uploadProgress = this.eventService.uploadProgress;
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

  onFileSelected(event: globalThis.Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.handleFiles(Array.from(input.files));
    }
    input.value = '';
  }

  private handleFiles(files: File[]): void {
    if (files.length === 0) return;

    this.uploadedCount.set(0);
    const uploadedItems: EventMediaUploadResponse[] = [];

    from(files).pipe(
      concatMap(file =>
        this.eventService.uploadEventMedia(this.eventId, file).pipe(
          tap((item) => {
            if (item) {
              uploadedItems.push(item);
              this.uploadedCount.update(c => c + 1);
              setTimeout(() => {
                this.eventService.removeProgress(file.name);
              }, 2000);
            }
          }),
          catchError(error => {
            console.error('Upload error for event file:', file.name, error);
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
