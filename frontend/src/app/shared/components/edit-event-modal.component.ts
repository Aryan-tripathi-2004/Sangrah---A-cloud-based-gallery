import {
  Component,
  input,
  output,
  ChangeDetectionStrategy,
  OnDestroy,
  inject,
  DestroyRef,
  signal,
  effect,
} from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Event } from '../../core/api/models/api.models';
import { EventApiService } from '../../core/api/services/event-api.service';
import { extractErrorMessage } from '../../core/errors/api-error.model';

interface EventFormValues {
  title: string;
  description: string;
  eventDate: string;
  visibility: string;
  moderationEnabled: boolean;
}

@Component({
  selector: 'app-edit-event-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule],
  templateUrl: './edit-event-modal.component.html',
})
export class EditEventModalComponent implements OnDestroy {
  readonly isOpen = input<boolean>(false);
  readonly event = input<Event | null>(null);

  readonly closeModal = output<void>();
  readonly saved = output<Event>();
  readonly errorOccurred = output<string>();

  private readonly fb = inject(FormBuilder);
  private readonly api = inject(EventApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly form: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200)]],
    description: ['', [Validators.maxLength(2000)]],
    eventDate: ['', [Validators.required]],
    visibility: ['PUBLIC', [Validators.required]],
    moderationEnabled: [false],
  });

  readonly isSaving = signal<boolean>(false);
  readonly hasChanges = signal<boolean>(false);
  readonly currentCoverImageUrl = signal<string | null>(null);
  readonly selectedCoverPreviewUrl = signal<string | null>(null);
  readonly selectedCoverFileName = signal<string | null>(null);
  readonly selectedCoverFile = signal<File | null>(null);

  private initialFormValues: EventFormValues = {
    title: '',
    description: '',
    eventDate: '',
    visibility: 'PUBLIC',
    moderationEnabled: false,
  };

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.updateHasChanges();
    });

    effect(() => {
      const open = this.isOpen();
      const ev = this.event();
      if (open && ev) {
        this.populateForm(ev);
      }
    });
  }

  ngOnDestroy(): void {
    this.cleanupPreviewUrl();
  }

  private populateForm(ev: Event): void {
    this.cleanupPreviewUrl();
    this.selectedCoverFile.set(null);
    this.selectedCoverFileName.set(null);

    this.currentCoverImageUrl.set(ev.coverImageId ? this.api.getMediaFileUrl(ev.coverImageId) : null);

    this.initialFormValues = {
      title: (ev.title || '').trim(),
      description: (ev.description || '').trim(),
      eventDate: this.formatDateForInput(ev.eventDate),
      visibility: (ev.visibility || 'PUBLIC').trim(),
      moderationEnabled: ev.moderationEnabled === true,
    };

    this.form.reset(this.initialFormValues);
    this.form.markAsPristine();
    this.form.markAsUntouched();
    this.hasChanges.set(false);
  }

  private updateHasChanges(): void {
    if (this.selectedCoverFile() !== null) {
      this.hasChanges.set(true);
      return;
    }

    if (!this.form.dirty) {
      this.hasChanges.set(false);
      return;
    }

    const raw = this.form.getRawValue();
    const isDifferent =
      (raw.title || '').trim() !== this.initialFormValues.title ||
      (raw.description || '').trim() !== this.initialFormValues.description ||
      (raw.eventDate || '').trim() !== this.initialFormValues.eventDate ||
      (raw.visibility || 'PUBLIC').trim() !== this.initialFormValues.visibility ||
      (raw.moderationEnabled === true) !== this.initialFormValues.moderationEnabled;

    this.hasChanges.set(isDifferent);
  }

  isInvalid(controlName: string, errorName?: string): boolean {
    const control = this.form.get(controlName);
    if (!control) return false;
    if (errorName) return control.hasError(errorName) && (control.dirty || control.touched);
    return control.invalid && (control.dirty || control.touched);
  }

  canSave(): boolean {
    return this.form.valid && this.hasChanges() && !this.isSaving();
  }

  onCoverFileChange(event: any): void {
    const inputElement = event.target as HTMLInputElement;
    const file = inputElement.files && inputElement.files.length > 0 ? inputElement.files[0] : null;

    this.cleanupPreviewUrl();
    this.selectedCoverFile.set(file);
    this.selectedCoverFileName.set(file?.name ?? null);

    if (file) {
      this.selectedCoverPreviewUrl.set(URL.createObjectURL(file));
    }

    this.updateHasChanges();
  }

  saveChanges(): void {
    const ev = this.event();
    if (!ev || !this.canSave()) return;

    this.isSaving.set(true);

    const raw = this.form.getRawValue();
    const eventDetails = {
      title: (raw.title || '').trim(),
      description: (raw.description || '').trim(),
      eventDate: (raw.eventDate || '').trim(),
      visibility: (raw.visibility || 'PUBLIC').trim(),
      moderationEnabled: raw.moderationEnabled === true,
    };

    this.api.updateEvent(ev.id, eventDetails, this.selectedCoverFile() || undefined)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedEvent: Event) => {
          this.isSaving.set(false);
          this.cleanupPreviewUrl();
          this.saved.emit(updatedEvent);
        },
        error: (error: unknown) => {
          this.isSaving.set(false);
          this.errorOccurred.emit(extractErrorMessage(error, 'Failed to update event'));
        }
      });
  }

  private cleanupPreviewUrl(): void {
    const url = this.selectedCoverPreviewUrl();
    if (url) {
      URL.revokeObjectURL(url);
      this.selectedCoverPreviewUrl.set(null);
    }
  }

  private formatDateForInput(dateValue: string | Date | null | undefined): string {
    if (!dateValue) return '';
    const parsed = new Date(dateValue);
    if (isNaN(parsed.getTime())) return '';
    const year = parsed.getFullYear();
    const month = `${parsed.getMonth() + 1}`.padStart(2, '0');
    const day = `${parsed.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
