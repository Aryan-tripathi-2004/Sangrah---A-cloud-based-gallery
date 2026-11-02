import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-error-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [],
  templateUrl: './error-modal.component.html',
})
export class ErrorModalComponent {
  readonly isOpen = input<boolean>(false);
  readonly title = input<string>('Something went wrong');
  readonly message = input<string>('');
  readonly retryLabel = input<string>('Retry');
  readonly closeLabel = input<string>('Close');

  readonly retry = output<void>();
  readonly closeModal = output<void>();
}
