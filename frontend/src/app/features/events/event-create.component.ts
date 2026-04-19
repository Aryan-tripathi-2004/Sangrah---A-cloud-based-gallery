import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { SangrahApiService } from '../../core/api/sangrah-api.service';
import { LayoutComponent } from '../../shared/layout/layout.component';

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

  eventForm: FormGroup;
  isSubmitting = false;
  errorMessage = '';

  constructor() {
    this.eventForm = this.fb.group({
      title: ['', Validators.required],
      description: ['', Validators.required],
      eventDate: ['', Validators.required],
      visibility: ['PUBLIC', Validators.required],
      moderationEnabled: [true],
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
