import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-success-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [],
  templateUrl: './success-modal.component.html',
})
export class SuccessModalComponent {
  readonly isOpen = input<boolean>(false);
  readonly title = input<string>('Success');
  readonly message = input<string>('');
  readonly icon = input<string>('✅');
  readonly countdownSeconds = input<number | null>(null);
  readonly closeLabel = input<string>('Close');

  readonly closeModal = output<void>();
}
