import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-error-modal',
  standalone: true,
  imports: [CommonModule],
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
