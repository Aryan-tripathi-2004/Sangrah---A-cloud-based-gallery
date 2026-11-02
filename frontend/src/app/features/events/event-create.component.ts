import { Component, inject, ChangeDetectionStrategy, signal, DestroyRef } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutComponent } from '../../shared/layout/layout.component';
import { EventService } from './event.service';

@Component({
  selector: 'app-event-create',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, LayoutComponent],
  templateUrl: './event-create.component.html',
  styles: []
})
export class EventCreateComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly eventService = inject(EventService);
  private readonly destroyRef = inject(DestroyRef);

  readonly eventForm: FormGroup;
  readonly isSubmitting = signal<boolean>(false);
  readonly errorMessage = signal<string>('');

  selectedCoverFile: File | null = null;
  readonly coverPreviewUrl = signal<string | null>(null);

  constructor() {
    this.eventForm = this.fb.group({
      title: ['', Validators.required],
      description: ['', Validators.required],
      eventDate: ['', Validators.required],
      visibility: ['PUBLIC', Validators.required],
      moderationEnabled: [true]
    });
  }

  onCoverImageSelected(event: globalThis.Event): void {
    const input = event.target as HTMLInputElement;

    if (!input.files || input.files.length === 0) {
      this.selectedCoverFile = null;
      this.coverPreviewUrl.set(null);
      return;
    }

    const file = input.files[0];
    this.selectedCoverFile = file;

    // Image preview URL
    this.coverPreviewUrl.set(URL.createObjectURL(file));
  }

  onSubmit(): void {
    if (!this.eventForm.valid) return;

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    const eventDetails = {
      title: this.eventForm.value.title,
      description: this.eventForm.value.description,
      eventDate: this.eventForm.value.eventDate,
      visibility: this.eventForm.value.visibility,
      moderationEnabled: this.eventForm.value.moderationEnabled,
    };

    const formData = new FormData();
    formData.append(
      'eventDetails',
      new Blob([JSON.stringify(eventDetails)], { type: 'application/json' })
    );

    if (this.selectedCoverFile) {
      formData.append('coverMedia', this.selectedCoverFile);
    }

    this.eventService.createEvent(formData)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (event) => {
          this.isSubmitting.set(false);
          this.router.navigate(['/event', event.id]);
        },
        error: (error) => {
          this.isSubmitting.set(false);
          this.errorMessage.set(error?.message || error?.error?.message || error?.error?.error || 'Failed to create event');
          console.error('Create event error:', error);
        },
      });
  }
}
