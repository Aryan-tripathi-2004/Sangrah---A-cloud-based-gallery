import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-delete-event-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './delete-event-modal.component.html',
})
export class DeleteEventModalComponent implements OnChanges {
  @Input() isOpen = false;
  @Input() eventTitle = '';
  @Input() mediaCount = 0;
  @Input() collaboratorCount = 0;
  @Input() isDeleting = false;

  @Output() closeModal = new EventEmitter<void>();
  @Output() confirmDelete = new EventEmitter<void>();

  confirmed = false;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen']?.currentValue) {
      this.confirmed = false;
    }
  }

  canDelete(): boolean {
    return this.confirmed && !this.isDeleting;
  }
}
