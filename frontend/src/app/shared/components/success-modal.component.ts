import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-success-modal',
  standalone: true,
  imports: [CommonModule],
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
