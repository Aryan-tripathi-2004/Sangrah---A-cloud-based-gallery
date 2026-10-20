import { Component, inject, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
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
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private eventService = inject(EventService);
  private cdr = inject(ChangeDetectorRef);

  eventForm: FormGroup;
  isSubmitting = false;
  errorMessage = '';

  selectedCoverFile: File | null = null;
  coverPreviewUrl: string | null = null;

  constructor() {
    this.eventForm = this.fb.group({
      title: ['', Validators.required],
      description: ['', Validators.required],
      eventDate: ['', Validators.required],
      visibility: ['PUBLIC', Validators.required],
      moderationEnabled: [true]
    });
  }

  onCoverImageSelected(event: any): void {
    const input = event.target as HTMLInputElement;

    if (!input.files || input.files.length === 0) {
      this.selectedCoverFile = null;
      this.coverPreviewUrl = null;
      this.cdr.markForCheck();
      return;
    }

    const file = input.files[0];
    this.selectedCoverFile = file;

    // Image preview URL
    this.coverPreviewUrl = URL.createObjectURL(file);
    this.cdr.markForCheck();
  }

  onSubmit(): void {
    if (!this.eventForm.valid) return;

    this.isSubmitting = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

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

    this.eventService.createEvent(formData).subscribe({
      next: (event) => {
        this.isSubmitting = false;
        this.router.navigate(['/event', event.id]);
      },
      error: (error) => {
        this.isSubmitting = false;
        this.errorMessage = error?.message || error?.error?.message || error?.error?.error || 'Failed to create event';
        console.error('Create event error:', error);
        this.cdr.markForCheck();
      },
    });
  }
}