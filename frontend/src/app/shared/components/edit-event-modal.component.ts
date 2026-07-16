import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-edit-event-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './edit-event-modal.component.html',
})
export class EditEventModalComponent {
  @Input() isOpen = false;
  @Input() form!: FormGroup;
  @Input() isSaving = false;
  @Input() hasChanges = false;
  @Input() currentCoverImageUrl: string | null = null;
  @Input() selectedCoverPreviewUrl: string | null = null;
  @Input() selectedCoverFileName: string | null = null;

  @Output() closeModal = new EventEmitter<void>();
  @Output() saveChanges = new EventEmitter<void>();
  @Output() coverFileSelected = new EventEmitter<File | null>();

  isInvalid(controlName: string, errorName?: string): boolean {
    const control = this.form?.get(controlName);
    if (!control) {
      return false;
    }

    if (errorName) {
      return control.hasError(errorName) && (control.dirty || control.touched);
    }

    return control.invalid && (control.dirty || control.touched);
  }

  canSave(): boolean {
    return !!this.form && this.form.valid && this.hasChanges && !this.isSaving;
  }

  onCoverFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length > 0 ? input.files[0] : null;
    this.coverFileSelected.emit(file);
  }
}
