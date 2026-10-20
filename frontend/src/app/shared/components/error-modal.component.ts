
import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-error-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [],
  templateUrl: './error-modal.component.html',
})
export class ErrorModalComponent {
  @Input() isOpen = false;
  @Input() title = 'Something went wrong';
  @Input() message = '';
  @Input() retryLabel = 'Retry';
  @Input() closeLabel = 'Close';

  @Output() retry = new EventEmitter<void>();
  @Output() closeModal = new EventEmitter<void>();
}
