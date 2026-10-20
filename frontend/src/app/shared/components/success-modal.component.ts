
import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-success-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [],
  templateUrl: './success-modal.component.html',
})
export class SuccessModalComponent {
  @Input() isOpen = false;
  @Input() title = 'Success';
  @Input() message = '';
  @Input() icon = '✅';
  @Input() countdownSeconds: number | null = null;
  @Input() closeLabel = 'Close';

  @Output() closeModal = new EventEmitter<void>();
}
