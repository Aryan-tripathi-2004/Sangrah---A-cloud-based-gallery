import { Component, input, output, ChangeDetectionStrategy, signal, effect } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-delete-event-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule],
  templateUrl: './delete-event-modal.component.html',
})
export class DeleteEventModalComponent {
  readonly isOpen = input<boolean>(false);
  readonly eventTitle = input<string>('');
  readonly mediaCount = input<number>(0);
  readonly collaboratorCount = input<number>(0);
  readonly isDeleting = input<boolean>(false);

  readonly closeModal = output<void>();
  readonly confirmDelete = output<void>();

  readonly confirmed = signal<boolean>(false);

  get confirmedVal(): boolean {
    return this.confirmed();
  }
  set confirmedVal(val: boolean) {
    this.confirmed.set(val);
  }

  constructor() {
    effect(() => {
      if (this.isOpen()) {
        this.confirmed.set(false);
      }
    });
  }

  canDelete(): boolean {
    return this.confirmed() && !this.isDeleting();
  }
}
