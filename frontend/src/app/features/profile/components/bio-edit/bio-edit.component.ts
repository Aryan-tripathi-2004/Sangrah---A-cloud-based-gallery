import { Component, ChangeDetectionStrategy, input, output, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-bio-edit',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  templateUrl: './bio-edit.component.html',
})
export class BioEditComponent {
  bio = input<string | null | undefined>('');
  isSaving = input<boolean>(false);

  bioSaved = output<string>();

  isEditing = signal(false);
  draftBio = signal('');

  constructor() {
    effect(() => {
      this.draftBio.set(this.bio() || '');
    });
  }

  startEditing(): void {
    this.draftBio.set(this.bio() || '');
    this.isEditing.set(true);
  }

  cancelEditing(): void {
    this.draftBio.set(this.bio() || '');
    this.isEditing.set(false);
  }

  save(): void {
    this.bioSaved.emit(this.draftBio().trim());
    this.isEditing.set(false);
  }
}
