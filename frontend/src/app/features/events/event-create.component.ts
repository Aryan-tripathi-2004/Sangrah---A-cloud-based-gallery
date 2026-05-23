import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { SangrahApiService } from '../../core/api/sangrah-api.service';
import { LayoutComponent } from '../../shared/layout/layout.component';
import { GalleryService } from '../gallery/gallery.service';

@Component({
  selector: 'app-event-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, LayoutComponent],
  templateUrl: './event-create.component.html',
  styles: []
})
export class EventCreateComponent {
  private api = inject(SangrahApiService);
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private gallery = inject(GalleryService);

  eventForm: FormGroup;
  isSubmitting = false;
  errorMessage = '';

  isUploadingCover = false;
  coverPreviewUrl: string | null = null;

  constructor() {
    this.eventForm = this.fb.group({
      title: ['', Validators.required],
      description: ['', Validators.required],
      eventDate: ['', Validators.required],
      visibility: ['PUBLIC', Validators.required],
      moderationEnabled: [true],
      coverImageId: ['']
    });
  }

  onCoverImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;

    if (!input.files || input.files.length === 0) {
      return;
    }

    const file = input.files[0];

    // Image preview
    this.coverPreviewUrl = URL.createObjectURL(file);

    this.isUploadingCover = true;

    this.gallery.uploadFile(file).subscribe({
      next: (mediaItem) => {
        this.eventForm.patchValue({
          coverImageId: mediaItem.id
        });

        this.isUploadingCover = false;

        console.log('Cover image uploaded:', mediaItem);
      },
      error: (error) => {
        console.error('Cover upload failed:', error);
        this.errorMessage = 'Failed to upload cover image';
        this.isUploadingCover = false;
      }
    });
  }

  onSubmit(): void {
    if (!this.eventForm.valid) return;

    this.isSubmitting = true;
    this.errorMessage = '';

    const payload = {
      title: this.eventForm.value.title,
      description: this.eventForm.value.description,
      eventDate: this.eventForm.value.eventDate,
      visibility: this.eventForm.value.visibility,
      coverImageId: this.eventForm.value.coverImageId,
      moderationEnabled: this.eventForm.value.moderationEnabled,
    };

    this.api.createEvent(payload).subscribe({
      next: (event) => {
        this.isSubmitting = false;
        this.router.navigate(['/event', event.id]);
      },
      error: (error) => {
        this.isSubmitting = false;
        this.errorMessage = error?.error?.message || 'Failed to create event';
        console.error('Create event error:', error);
      },
    });
  }
}